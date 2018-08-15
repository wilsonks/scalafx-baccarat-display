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

import cats.Show
import cats.effect.IO
import cats.implicits._
import fs2.{Pipe, Sink, Stream}

import scala.io.StdIn

object ConsoleIO {

  implicit val showEx: Show[Throwable] = _.toString

  def get: IO[String] =
    IO(StdIn.readLine())

  def logger[A](prefix: String)(implicit show: Show[A]): Pipe[IO, A, A] =
    s =>
      Stream.bracket(put(prefix, "started"))(
        _ =>
          s.evalMap(a => put(prefix, a).map(_ => a))
            .handleErrorWith(ex => Stream.eval(put(prefix, ex)).flatMap(_ => Stream.raiseError(ex))),
        _ => put(prefix, "terminated")
    )

  def put[A](prefix: String, a: A)(implicit S: Show[A]): IO[Unit] =
    IO(println(s"[$prefix] ${a.show}"))

  def sink[A](prefix: String)(implicit S: Show[A]): Sink[IO, A] =
    s => logger[A](prefix).apply(s).map(_ => ())

  def sinkN(prefix: String): Sink[IO, Nothing] =
    s => Stream.bracket(put(prefix, "started"))(_ => s, _ => put(prefix, "terminated"))

  def stream: Stream[IO, String] =
    Stream.repeatEval(get)
}
