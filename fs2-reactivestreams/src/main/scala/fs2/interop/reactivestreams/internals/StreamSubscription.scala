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

package fs2.interop.reactivestreams.internals

import cats.effect.{Effect, IO}
import cats.implicits._
import fs2.async.mutable.{Queue, Signal}
import fs2.{async, Pipe, Pull, Stream}
import org.reactivestreams.{Subscriber, Subscription}

import scala.concurrent.ExecutionContext

final class StreamSubscription[F[_], A](
  requests: Queue[F, StreamSubscription.Request],
  cancelled: Signal[F, Boolean],
  sub: Subscriber[A],
  stream: Stream[F, A])(implicit F: Effect[F], ec: ExecutionContext)
    extends Subscription {

  import StreamSubscription._

  def cancel(): Unit =
    IO.async[Unit] { cb =>
        async.unsafeRunAsync {
          cancelled.set(true) *> F.delay(cb(Right(())))
        }(_ => IO.unit)
      }
      .unsafeRunSync

  def onComplete: F[Unit] =
    cancelled.set(true) *> F.delay(sub.onComplete())

  def onError(e: Throwable): F[Unit] =
    cancelled.set(true) *> F.delay(sub.onError(e))

  def request(n: Long): Unit = {
    val request: F[Request] =
      if (n == java.lang.Long.MAX_VALUE) F.pure(Infinite)
      else if (n > 0) F.pure(Finite(n))
      else F.raiseError(new IllegalArgumentException(s"$n elements requested"))
    val pull = cancelled.get.ifM(ifTrue = F.unit, ifFalse = request.flatMap(requests.enqueue1).handleErrorWith(onError))
    async.unsafeRunAsync(pull)(_ => IO.unit)
  }

  private def loop(s: Stream[F, A]): Pull[F, A, Unit] =
    Pull.eval(requests.dequeue1).flatMap[F, A, Unit] {
      case Infinite => s.pull.echo
      case Finite(n) =>
        s.pull.take(n).flatMap[F, A, Unit] {
          case Some(rem) => loop(rem)
          case None      => Pull.done
        }
    }

  private def subscriptionPipe: Pipe[F, A, A] =
    in => loop(in).stream

  private[reactivestreams] def unsafeStart(): Unit = {
    val s = stream
      .through(subscriptionPipe)
      .interruptWhen(cancelled)
      .evalMap(x => F.delay(sub.onNext(x)))
      .handleErrorWith(e => Stream.eval(onError(e)))
      .onFinalize(cancelled.get.ifM(ifTrue = F.unit, ifFalse = onComplete))
      .compile
      .drain

    async.unsafeRunAsync(s)(_ => IO.unit)
  }
}

object StreamSubscription {

  def apply[F[_], A](sub: Subscriber[A], stream: Stream[F, A])(
    implicit F: Effect[F],
    ec: ExecutionContext): F[StreamSubscription[F, A]] =
    async.signalOf[F, Boolean](false).flatMap { cancelled =>
      async.unboundedQueue[F, Request].map { requests =>
        new StreamSubscription(requests, cancelled, sub, stream)
      }
    }

  sealed trait Request

  case class Finite(n: Long) extends Request

  case object Infinite extends Request

}
