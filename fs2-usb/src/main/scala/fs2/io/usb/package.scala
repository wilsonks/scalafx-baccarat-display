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

import cats.effect.IO
import fs2.{Pipe, Stream}
import org.usb4java.Context
import scodec.bits.ByteVector

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

package object usb {

  import internals._

  def scan(id: Usb.Id*)(implicit EC: ExecutionContext, U: Uart): Stream[IO, (Usb, Pipe[IO, ByteVector, ByteVector])] =
    scan(usb => id.contains(usb.id))

  def scan(
    predicate: Usb => Boolean = _ => true,
    writeTimeout: FiniteDuration = 1.second,
    pollTimeout: FiniteDuration = 1.second,
    scanInterval: FiniteDuration = 1.second)(
    implicit EC: ExecutionContext,
    U: Uart): Stream[IO, (Usb, Pipe[IO, ByteVector, ByteVector])] =
    Stream
      .eval(IO(new Context))
      .flatMap(ctx =>
        Stream.bracket(ctx.init)(
          _ =>
            (if (pluggable) ctx.scan(predicate) else ctx.scan(predicate, scanInterval))
              .evalMap(usb =>
                for {
                  device <- ctx.find(_.port == usb.port)
                  driver <- Driver(device)
                } yield (usb, driver.open(device, writeTimeout.toMillis)))
              .concurrently(Stream.repeatEval(ctx.update(pollTimeout.toMillis)).drain),
          _ => ctx.exit
      ))
}
