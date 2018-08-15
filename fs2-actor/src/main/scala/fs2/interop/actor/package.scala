/*
 * Copyright (c) 2018 by Tykhe Gaming Private Limited
 *
 * Licensed under the Software License Agreement (the "License") of Tykhe Gaming Private Limited.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://tykhegaming.github.io/LICENSE.txt.
 *
 * NOTICE
 * ALL INFORMATION CONTAINED HEREIN IS, AND REMAINS THE PROPERTY OF TYKHE GAMING PRIVATE LIMITED.
 * THE INTELLECTUAL AND TECHNICAL CONCEPTS CONTAINED HEREIN ARE PROPRIETARY TO TYKHE GAMING PRIVATE LIMITED AND
 * ARE PROTECTED BY TRADE SECRET OR COPYRIGHT LAW. DISSEMINATION OF THIS INFORMATION OR REPRODUCTION OF THIS
 * MATERIAL IS STRICTLY FORBIDDEN UNLESS PRIOR WRITTEN PERMISSION IS OBTAINED FROM TYKHE GAMING PRIVATE LIMITED.
 */

package fs2.interop

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Scheduler}
import akka.event.LoggingAdapter
import akka.serialization.Serializer
import cats.effect.{Concurrent, IO, Sync, Timer}
import cats.implicits._
import fs2.{Pipe, Sink, Stream}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, TimeUnit}
import scala.reflect.ClassTag

package object actor {

  implicit class LoggingAdapterFs2Ops(val self: LoggingAdapter) extends AnyVal {

    def pipe[F[_], A](prefix: String)(implicit F: Sync[F]): Pipe[F, A, A] =
      src =>
        Stream.bracket(F.delay(self.info("{}: started", prefix)))(
          _ =>
            src
              .handleErrorWith(ex =>
                Stream.eval_(F.delay(self.error(ex, "{}: failed", prefix)).flatMap(_ => F.raiseError[A](ex))))
              .through[A](s =>
                if (self.isDebugEnabled) s.evalMap(a => F.delay(self.debug("{}: {}", prefix, a)).map(_ => a)) else s),
          _ => F.delay(self.warning("{}: terminated", prefix))
      )

    def sink[F[_], A](prefix: String)(implicit F: Sync[F]): Sink[F, A] =
      src =>
        Stream.bracket(F.delay(self.info("{}: started", prefix)))(
          _ =>
            src
              .handleErrorWith(ex =>
                Stream.eval_(F.delay(self.error(ex, "{}: failed", prefix)).flatMap(_ => F.raiseError[A](ex))))
              .through(s =>
                if (self.isDebugEnabled) s.to(Sink(a => F.delay(self.debug("{}: {}", prefix, a)))) else s.drain),
          _ => F.delay(self.warning("{}: terminated", prefix))
      )
  }

  implicit class SystemFs2Ops(val self: ActorSystem) extends AnyVal {

    def fs2Scheduler: fs2.Scheduler =
      self.scheduler.toFs2(self.dispatcher)

    def timer[F[_]](implicit F: Concurrent[F]): Timer[F] =
      self.scheduler.timer[F](F, self.dispatcher)
  }

  implicit class SchedulerFs2Ops(val self: Scheduler) extends AnyVal {

    def timer[F[_]](implicit F: Concurrent[F], EC: ExecutionContext): Timer[F] = new Timer[F] {

      def clockMonotonic(unit: TimeUnit): F[Long] =
        F.delay(unit.convert(System.nanoTime(), TimeUnit.NANOSECONDS))

      def clockRealTime(unit: TimeUnit): F[Long] =
        F.delay(unit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS))

      def shift: F[Unit] =
        F.async(cb => EC.execute(() => cb(rightUnit)))

      def sleep(duration: FiniteDuration): F[Unit] =
        F.cancelable { cb =>
          val task = self.scheduleOnce(duration)(cb(rightUnit))
          IO(task.cancel())
        }
    }

    def toFs2(implicit EC: ExecutionContext): fs2.Scheduler = new fs2.Scheduler {

      protected def scheduleOnce(delay: FiniteDuration)(thunk: => Unit): () => Unit = {
        val task = self.scheduleOnce(delay)(thunk)
        () =>
          task.cancel()
      }

      protected def scheduleAtFixedRate(period: FiniteDuration)(thunk: => Unit): () => Unit = {
        val task = self.schedule(period, period)(thunk)
        () =>
          task.cancel()
      }
    }
  }

  implicit class SerializerFs2Ops(val self: Serializer) extends AnyVal {

    def decode[F[_], A <: AnyRef](bytes: Array[Byte])(implicit F: Sync[F], T: ClassTag[A]): F[A] =
      F.delay(self.fromBinary(bytes)).flatMap {
        case a: A => F.pure(a)
        case a    => F.raiseError(new RuntimeException(s"decode expected type ${T.runtimeClass} for value $a"))
      }

    def decoder[F[_], A <: AnyRef: ClassTag](implicit F: Sync[F]): Pipe[F, Array[Byte], A] =
      _.evalMap(decode[F, A])

    def encode[F[_], A <: AnyRef](a: A)(implicit F: Sync[F]): F[Array[Byte]] =
      F.delay(self.toBinary(a))

    def encoder[F[_], A <: AnyRef](a: A)(implicit F: Sync[F]): Pipe[F, A, Array[Byte]] =
      _.evalMap(encode[F, A])
  }

  private[actor] val rightUnit: Either[Throwable, Unit] = Right(())

}
