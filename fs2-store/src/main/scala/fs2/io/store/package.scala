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

package fs2.io

import java.nio.ByteBuffer

import akka.util.ByteString
import cats.data.NonEmptyList
import cats.implicits._
import cats.{FlatMap, Monad, Show}
import eventstore.{Content, ContentType, Event, EventData}
import fs2.{Chunk, Pipe}

package object store {

  type Decode[F[_], A] = Event => F[A]
  type Decoder[F[_], A] = Pipe[F, Event, A]
  type Encode[F[_], A] = A => F[EventData]
  type Encoder[F[_], A] = Pipe[F, NonEmptyList[A], NonEmptyList[EventData]]

  import NonEmptyList.{catsDataInstancesForNonEmptyList => L}

  def decodeContent[F[_], Data](fd: Content => F[Data])(implicit F: FlatMap[F]): Decode[F, Data] =
    e => fd(e.data.data)

  def decodeContent[F[_], Data, Meta](fd: Content => F[Data], fm: Content => F[Meta])(
    implicit F: FlatMap[F]): Decode[F, (Data, Meta)] =
    e =>
      for {
        d <- fd(e.data.data)
        m <- fm(e.data.metadata)
      } yield (d, m)

  def decodeBytes[F[_], Data](fd: Array[Byte] => F[Data])(implicit F: FlatMap[F]): Decode[F, Data] =
    decodeContent(c => fd(c.value.toArray))

  def decodeBytes[F[_], Data, Meta](fd: Array[Byte] => F[Data], fm: Array[Byte] => F[Meta])(
    implicit F: FlatMap[F]): Decode[F, (Data, Meta)] =
    decodeContent(c => fd(c.value.toArray), c => fm(c.value.toArray))

  def decodeBuffer[F[_], Data](fd: ByteBuffer => F[Data])(implicit F: FlatMap[F]): Decode[F, Data] =
    decodeContent(c => fd(c.value.toByteBuffer))

  def decodeBuffer[F[_], Data, Meta](fd: ByteBuffer => F[Data], fm: ByteBuffer => F[Meta])(
    implicit F: FlatMap[F]): Decode[F, (Data, Meta)] =
    decodeContent(c => fd(c.value.toByteBuffer), c => fm(c.value.toByteBuffer))

  def decodeChunk[F[_], Data](fd: Chunk[Byte] => F[Data])(implicit F: FlatMap[F]): Decode[F, Data] =
    decodeContent(c => fd(Chunk.bytes(c.value.toArray)))

  def decodeChunk[F[_], Data, Meta](fd: Chunk[Byte] => F[Data], fm: Chunk[Byte] => F[Meta])(
    implicit F: FlatMap[F]): Decode[F, (Data, Meta)] =
    decodeContent(c => fd(Chunk.bytes(c.value.toArray)), c => fm(Chunk.bytes(c.value.toArray)))

  def decodeJson[F[_], Data](fd: String => F[Data])(implicit F: FlatMap[F]): Decode[F, Data] =
    decodeContent(c => fd(c.value.utf8String))

  def decodeJson[F[_], Data, Meta](fd: String => F[Data], fm: String => F[Meta])(
    implicit F: FlatMap[F]): Decode[F, (Data, Meta)] =
    decodeContent(c => fd(c.value.utf8String), c => fm(c.value.utf8String))

  def contentDecoder[F[_], Data](fd: Content => F[Data])(implicit F: FlatMap[F]): Decoder[F, Data] =
    _.evalMap(decodeContent(fd))

  def contentDecoder[F[_], Data, Meta](fd: Content => F[Data], fm: Content => F[Meta])(
    implicit F: FlatMap[F]): Decoder[F, (Data, Meta)] =
    _.evalMap(decodeContent(fd, fm))

  def bytesDecoder[F[_], Data](fd: Array[Byte] => F[Data])(implicit F: FlatMap[F]): Decoder[F, Data] =
    _.evalMap(decodeBytes(fd))

  def bytesDecoder[F[_], Data, Meta](fd: Array[Byte] => F[Data], fm: Array[Byte] => F[Meta])(
    implicit F: FlatMap[F]): Decoder[F, (Data, Meta)] =
    _.evalMap(decodeBytes(fd, fm))

  def bufferDecoder[F[_], Data](fd: ByteBuffer => F[Data])(implicit F: FlatMap[F]): Decoder[F, Data] =
    _.evalMap(decodeBuffer(fd))

  def bufferDecoder[F[_], Data, Meta](fd: ByteBuffer => F[Data], fm: ByteBuffer => F[Meta])(
    implicit F: FlatMap[F]): Decoder[F, (Data, Meta)] =
    _.evalMap(decodeBuffer(fd, fm))

  def chunkDecoder[F[_], Data](fd: Chunk[Byte] => F[Data])(implicit F: FlatMap[F]): Decoder[F, Data] =
    _.evalMap(decodeChunk(fd))

  def chunkDecoder[F[_], Data, Meta](fd: Chunk[Byte] => F[Data], fm: Chunk[Byte] => F[Meta])(
    implicit F: FlatMap[F]): Decoder[F, (Data, Meta)] =
    _.evalMap(decodeChunk(fd, fm))

  def jsonDecoder[F[_], Data](fd: String => F[Data])(implicit F: FlatMap[F]): Decoder[F, Data] =
    _.evalMap(decodeJson(fd))

  def jsonDecoder[F[_], Data, Meta](fd: String => F[Data], fm: String => F[Meta])(
    implicit
    F: FlatMap[F]): Decoder[F, (Data, Meta)] =
    _.evalMap(decodeJson(fd, fm))

  def encodeContent[F[_], Data](fd: Data => F[Content])(implicit F: FlatMap[F], S: Show[Data]): Encode[F, Data] =
    d =>
      for {
        cd <- fd(d)
      } yield EventData(eventType = d.show, data = cd)

  def encodeContent[F[_], Data, Meta](fd: Data => F[Content], fm: Meta => F[Content])(
    implicit F: FlatMap[F],
    S: Show[Data]): Encode[F, (Data, Meta)] = {
    case (d, m) =>
      for {
        cd <- fd(d)
        cm <- fm(m)
      } yield EventData(eventType = d.show, data = cd, metadata = cm)
  }

  def encodeBytes[F[_], Data](fd: Data => F[Array[Byte]])(implicit F: FlatMap[F], S: Show[Data]): Encode[F, Data] =
    encodeContent(fd(_).map(a => Content(ByteString.fromArray(a), ContentType.Binary)))

  def encodeBytes[F[_], Data, Meta](fd: Data => F[Array[Byte]], fm: Meta => F[Array[Byte]])(
    implicit F: FlatMap[F],
    S: Show[Data]): Encode[F, (Data, Meta)] =
    encodeContent(
      fd(_).map(a => Content(ByteString.fromArray(a), ContentType.Binary)),
      fm(_).map(a => Content(ByteString.fromArray(a), ContentType.Binary)))

  def encodeBuffer[F[_], Data](fd: Data => F[ByteBuffer])(implicit F: FlatMap[F], S: Show[Data]): Encode[F, Data] =
    encodeContent(fd(_).map(b => Content(ByteString.fromByteBuffer(b), ContentType.Binary)))

  def encodeBuffer[F[_], Data, Meta](fd: Data => F[ByteBuffer], fm: Meta => F[ByteBuffer])(
    implicit F: FlatMap[F],
    S: Show[Data]): Encode[F, (Data, Meta)] =
    encodeContent(
      fd(_).map(b => Content(ByteString.fromByteBuffer(b), ContentType.Binary)),
      fm(_).map(b => Content(ByteString.fromByteBuffer(b), ContentType.Binary)))

  def encodeChunk[F[_], Data](fd: Data => F[Chunk[Byte]])(implicit F: FlatMap[F], S: Show[Data]): Encode[F, Data] =
    encodeContent(fd(_).map(c => Content(ByteString.fromArray(c.toArray), ContentType.Binary)))

  def encodeChunk[F[_], Data, Meta](fd: Data => F[Chunk[Byte]], fm: Meta => F[Chunk[Byte]])(
    implicit F: FlatMap[F],
    S: Show[Data]): Encode[F, (Data, Meta)] =
    encodeContent(
      fd(_).map(c => Content(ByteString.fromArray(c.toArray), ContentType.Binary)),
      fm(_).map(c => Content(ByteString.fromArray(c.toArray), ContentType.Binary))
    )

  def encodeJson[F[_], Data](fd: Data => F[String])(implicit F: FlatMap[F], S: Show[Data]): Encode[F, Data] =
    encodeContent(fd(_).map(s => Content(ByteString.fromString(s), ContentType.Json)))

  def encodeJson[F[_], Data, Meta](fd: Data => F[String], fm: Meta => F[String])(
    implicit F: FlatMap[F],
    S: Show[Data]): Encode[F, (Data, Meta)] =
    encodeContent(
      fd(_).map(s => Content(ByteString.fromString(s), ContentType.Json)),
      fm(_).map(s => Content(ByteString.fromString(s), ContentType.Json))
    )

  def contentEncoder[F[_], Data](fd: Data => F[Content])(implicit F: Monad[F], S: Show[Data]): Encoder[F, Data] =
    _.evalMap(L.traverse(_)(encodeContent(fd)))

  def contentEncoder[F[_], Data, Meta](fd: Data => F[Content], fm: Meta => F[Content])(
    implicit F: Monad[F],
    S: Show[Data]): Encoder[F, (Data, Meta)] =
    _.evalMap(L.traverse(_)(encodeContent(fd, fm)))

  def bytesEncoder[F[_], Data](fd: Data => F[Array[Byte]])(implicit F: Monad[F], S: Show[Data]): Encoder[F, Data] =
    _.evalMap(L.traverse(_)(encodeBytes(fd)))

  def bytesEncoder[F[_], Data, Meta](fd: Data => F[Array[Byte]], fm: Meta => F[Array[Byte]])(
    implicit F: Monad[F],
    S: Show[Data]): Encoder[F, (Data, Meta)] =
    _.evalMap(L.traverse(_)(encodeBytes(fd, fm)))

  def bufferEncoder[F[_], Data](fd: Data => F[ByteBuffer])(implicit F: Monad[F], S: Show[Data]): Encoder[F, Data] =
    _.evalMap(L.traverse(_)(encodeBuffer(fd)))

  def bufferEncoder[F[_], Data, Meta](fd: Data => F[ByteBuffer], fm: Meta => F[ByteBuffer])(
    implicit F: Monad[F],
    S: Show[Data]): Encoder[F, (Data, Meta)] =
    _.evalMap(L.traverse(_)(encodeBuffer(fd, fm)))

  def chunkEncoder[F[_], Data](fd: Data => F[Chunk[Byte]])(implicit F: Monad[F], S: Show[Data]): Encoder[F, Data] =
    _.evalMap(L.traverse(_)(encodeChunk(fd)))

  def chunkEncoder[F[_], Data, Meta](fd: Data => F[Chunk[Byte]], fm: Meta => F[Chunk[Byte]])(
    implicit F: Monad[F],
    S: Show[Data]): Encoder[F, (Data, Meta)] =
    _.evalMap(L.traverse(_)(encodeChunk(fd, fm)))

  def jsonEncoder[F[_], Data](fd: Data => F[String])(implicit F: Monad[F], S: Show[Data]): Encoder[F, Data] =
    _.evalMap(L.traverse(_)(encodeJson(fd)))

  def jsonEncoder[F[_], Data, Meta](fd: Data => F[String], fm: Meta => F[String])(
    implicit F: Monad[F],
    S: Show[Data]): Encoder[F, (Data, Meta)] =
    _.evalMap(L.traverse(_)(encodeJson(fd, fm)))

}
