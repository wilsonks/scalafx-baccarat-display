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

package fs2.io.store

import cats.data.NonEmptyList
import cats.effect.{Async, Effect, IO}
import cats.implicits._
import eventstore._
import fs2.interop.reactivestreams._
import fs2.{Sink, Stream}

import scala.concurrent.ExecutionContext

class Store(connection: EsConnection) {

  def read[F[_]](streamId: EventStream.Id, number: EventNumber = EventNumber.Last)(
    implicit F: Async[F],
    EC: ExecutionContext): F[Option[Event]] =
    F.liftIO(
      IO.fromFuture(IO(connection(ReadEvent(streamId, number))))
        .map(out => Option(out.event))
        .recover {
          case _: StreamNotFoundException => None
        })

  def reader[F[_]](streamId: EventStream.Id, offset: Option[EventNumber] = Some(EventNumber.Last))(
    implicit F: Effect[F],
    EC: ExecutionContext): Stream[F, Event] =
    connection.streamPublisher(streamId, offset).toStream

  def write[F[_]](streamId: EventStream.Id, events: NonEmptyList[EventData])(
    implicit F: Async[F],
    EC: ExecutionContext): F[WriteEventsCompleted] =
    F.liftIO(IO.fromFuture(IO(connection(WriteEvents(streamId, events.toList)))))

  def writer[F[_]](
    streamId: EventStream.Id)(implicit F: Async[F], EC: ExecutionContext): Sink[F, NonEmptyList[EventData]] =
    Sink(write(streamId, _).void)
}

object Store {

  def apply(connection: EsConnection): Store = new Store(connection)
}
