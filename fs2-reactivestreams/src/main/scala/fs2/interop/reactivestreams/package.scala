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

import cats.effect.Effect
import cats.implicits._
import fs2.Stream
import fs2.interop.reactivestreams.internals.{StreamPublisher, StreamSubscriber}
import org.reactivestreams.Publisher

import scala.concurrent.ExecutionContext

package object reactivestreams {

  def fromPublisher[F[_], A](p: Publisher[A])(implicit F: Effect[F], ec: ExecutionContext): Stream[F, A] =
    Stream
      .eval(StreamSubscriber[F, A].map { s =>
        p.subscribe(s)
        s
      })
      .flatMap(_.stream)

  implicit final class PublisherOps[A](val pub: Publisher[A]) extends AnyVal {

    def toStream[F[_]](implicit F: Effect[F], ec: ExecutionContext): Stream[F, A] =
      fromPublisher(pub)
  }

  implicit final class StreamOps[F[_], A](val stream: Stream[F, A]) {

    def toPublisher(implicit F: Effect[F], ec: ExecutionContext): Publisher[A] =
      StreamPublisher(stream)
  }

}
