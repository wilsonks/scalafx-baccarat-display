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
import fs2.{async, Stream}
import org.reactivestreams.{Publisher, Subscriber}

import scala.concurrent.ExecutionContext

final class StreamPublisher[F[_], A](s: Stream[F, A])(implicit F: Effect[F], ec: ExecutionContext)
    extends Publisher[A] {

  def subscribe(subscriber: Subscriber[_ >: A]): Unit = {
    nonNull(subscriber)
    async.unsafeRunAsync {
      StreamSubscription(subscriber, s).map { subscription =>
        subscriber.onSubscribe(subscription)
        subscription
      }
    } {
      case Left(err)           => IO(ec.reportFailure(err))
      case Right(subscription) => IO(subscription.unsafeStart())
    }
  }
}

object StreamPublisher {

  def apply[F[_], A](s: Stream[F, A])(implicit F: Effect[F], ec: ExecutionContext): StreamPublisher[F, A] =
    new StreamPublisher(s)
}
