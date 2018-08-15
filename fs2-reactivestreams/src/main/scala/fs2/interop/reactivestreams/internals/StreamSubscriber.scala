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

import cats.Applicative
import cats.effect.{Effect, IO}
import cats.implicits._
import fs2.async.Promise
import fs2.{async, Stream}
import org.reactivestreams.{Subscriber, Subscription}

import scala.concurrent.ExecutionContext

final class StreamSubscriber[F[_], A](private[internals] val fsm: StreamSubscriber.FSM[F, A])(
  implicit F: Effect[F],
  ec: ExecutionContext)
    extends Subscriber[A] {

  def onComplete(): Unit = {
    async.unsafeRunAsync(fsm.onComplete)(_ => IO.unit)
  }

  def onError(t: Throwable): Unit = {
    nonNull(t)
    async.unsafeRunAsync(fsm.onError(t))(_ => IO.unit)
  }

  def onNext(a: A): Unit = {
    nonNull(a)
    async.unsafeRunAsync(fsm.onNext(a))(_ => IO.unit)
  }

  def onSubscribe(s: Subscription): Unit = {
    nonNull(s)
    async.unsafeRunAsync(fsm.onSubscribe(s))(_ => IO.unit)
  }

  def stream: Stream[F, A] =
    fsm.stream
}

object StreamSubscriber {

  def apply[F[_], A](implicit F: Effect[F], ec: ExecutionContext): F[StreamSubscriber[F, A]] =
    fsm[F, A].map(new StreamSubscriber(_))

  private[reactivestreams] def fsm[F[_], A](implicit F: Effect[F], ec: ExecutionContext): F[FSM[F, A]] = {

    type Out = Either[Throwable, Option[A]]

    sealed trait Input
    case class OnSubscribe(s: Subscription) extends Input
    case class OnNext(a: A) extends Input
    case class OnError(e: Throwable) extends Input
    case object OnComplete extends Input
    case object OnFinalize extends Input
    case class OnDequeue(response: Promise[F, Out]) extends Input

    sealed trait State
    case object Uninitialized extends State
    case class Idle(sub: Subscription) extends State
    case class RequestBeforeSubscription(req: Promise[F, Out]) extends State
    case class WaitingOnUpstream(sub: Subscription, elementRequest: Promise[F, Out]) extends State
    case object UpstreamCompletion extends State
    case object DownstreamCancellation extends State
    case class UpstreamError(err: Throwable) extends State

    def step(in: Input): State => (State, F[Unit]) = in match {
      case OnSubscribe(s) => {
        case RequestBeforeSubscription(req) => WaitingOnUpstream(s, req) -> F.delay(s.request(1))
        case Uninitialized                  => Idle(s) -> F.unit
        case o                              => o -> F.delay(s.cancel()) *> F.raiseError(new Error(s"received subscription in invalid state [$o]"))
      }
      case OnNext(a) => {
        case WaitingOnUpstream(s, r) => Idle(s) -> r.complete(a.some.asRight)
        case DownstreamCancellation  => DownstreamCancellation -> F.unit
        case o                       => o -> F.raiseError(new Error(s"received next [$a] in invalid state [$o]"))
      }
      case OnComplete => {
        case WaitingOnUpstream(_, r) => UpstreamCompletion -> r.complete(None.asRight)
        case o                       => UpstreamCompletion -> F.unit
      }
      case OnError(e) => {
        case WaitingOnUpstream(_, r) => UpstreamError(e) -> r.complete(e.asLeft)
        case o                       => UpstreamError(e) -> F.unit
      }
      case OnFinalize => {
        case WaitingOnUpstream(sub, r) =>
          DownstreamCancellation -> F.delay(sub.cancel()) *> r.complete(None.asRight)
        case Idle(sub) => DownstreamCancellation -> F.delay(sub.cancel())
        case o         => o -> F.unit
      }
      case OnDequeue(r) => {
        case Uninitialized          => RequestBeforeSubscription(r) -> F.unit
        case Idle(sub)              => WaitingOnUpstream(sub, r) -> F.delay(sub.request(1))
        case err @ UpstreamError(e) => err -> r.complete(e.asLeft)
        case UpstreamCompletion     => UpstreamCompletion -> r.complete(None.asRight)
        case o                      => o -> r.complete(new Error(s"received request in invalid state [$o]").asLeft)
      }
    }

    async.refOf[F, State](Uninitialized).map { ref =>
      new FSM[F, A] {
        def nextState(in: Input): F[Unit] = ref.modify2(step(in)).flatMap(_._2)

        def onSubscribe(s: Subscription): F[Unit] = nextState(OnSubscribe(s))

        def onNext(a: A): F[Unit] = nextState(OnNext(a))

        def onError(t: Throwable): F[Unit] = nextState(OnError(t))

        def onComplete: F[Unit] = nextState(OnComplete)

        def onFinalize: F[Unit] = nextState(OnFinalize)

        def dequeue1: F[Either[Throwable, Option[A]]] =
          async.promise[F, Out].flatMap(p => ref.modify2(step(OnDequeue(p))).flatMap(_._2) *> p.get)
      }
    }
  }

  private[reactivestreams] trait FSM[F[_], A] {

    def dequeue1: F[Either[Throwable, Option[A]]]

    def onComplete: F[Unit]

    def onError(t: Throwable): F[Unit]

    def onFinalize: F[Unit]

    def onNext(a: A): F[Unit]

    def onSubscribe(s: Subscription): F[Unit]

    def stream(implicit ev: Applicative[F]): Stream[F, A] =
      Stream.eval(dequeue1).repeat.rethrow.unNoneTerminate.onFinalize(onFinalize)
  }

}
