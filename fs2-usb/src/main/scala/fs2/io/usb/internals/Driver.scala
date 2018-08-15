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

package fs2.io.usb.internals

import java.nio.{ByteBuffer, ByteOrder}

import cats.effect.IO
import cats.implicits._
import fs2.io.usb.Uart.{DataBits, Parity, StopBits}
import fs2.io.usb.{Uart, Usb}
import fs2.{Pipe, Stream}
import org.usb4java.{ConfigDescriptor, Device, DeviceDescriptor, DeviceHandle}
import scodec.bits.ByteVector

import scala.concurrent.ExecutionContext

trait Driver {

  // TODO replace with IO bracket operation on upgrade
  def open(device: Device, timeout: Long)(implicit EC: ExecutionContext): Pipe[IO, ByteVector, ByteVector] =
    host =>
      Stream.bracket(device.open)(
        handle =>
          Stream
            .eval(device.configuration(connection(device, _)))
            .flatMap(
              conn =>
                Stream.bracket(handle.claim(conn._1) *> init(handle))(
                  _ =>
                    handle
                      .reader(conn._3)
                      .concurrently(host.to(handle.writer(conn._2, timeout)).drain),
                  _ => handle.release(conn._1)
              )),
        // handle.close hangs if called in the usb event poll context, so shift
        handle => IO.shift(EC) *> handle.close
    )

  protected def connection(device: Device, config: ConfigDescriptor): IO[Connection] =
    for {
      interface <- config.interfaces.headOption.toIO()
      endpoints = interface.endpoints
      out <- endpoints.find(_.isOutput).toIO()
      in <- endpoints.find(_.isInput).toIO()
    } yield (interface.number, out.connector, in.connector)

  protected def init(handle: DeviceHandle): IO[Unit] = IO.unit
}

object Driver extends Driver {

  def apply(device: Device)(implicit U: Uart): IO[Driver] = device.descriptor.map {
    case Serial(driver) => driver
    case _              => Driver
  }
}

sealed abstract class Serial(id: Usb.Id) extends Driver

object Serial {

  def unapply(descriptor: DeviceDescriptor)(implicit U: Uart): Option[Serial] = descriptor match {
    case Pl2303(serial) => Some(serial)
    case _              => None
  }

  sealed abstract class Pl2303(uart: Uart) extends Serial(Usb.Pl2303) {

    private[this] val bufferR = ByteBuffer.allocateDirect(1)
    private[this] val bufferW = ByteBuffer.allocateDirect(0)
    private[this] val bufferL = ByteBuffer
      .allocateDirect(7)
      .order(ByteOrder.LITTLE_ENDIAN) // for 4 bytes containing baud rate
      .putInt(uart.baudRate)
      .put(uart.stopBits match {
        case StopBits.`1`   => 0.toByte
        case StopBits.`1.5` => 1.toByte
        case _              => 2.toByte
      })
      .put(uart.parity match {
        case Parity.None  => 0.toByte
        case Parity.Odd   => 1.toByte
        case Parity.Even  => 2.toByte
        case Parity.Mark  => 3.toByte
        case Parity.Space => 4.toByte
      })
      .put(uart.dataBits match {
        case DataBits.`5` => 5.toByte
        case DataBits.`6` => 6.toByte
        case DataBits.`7` => 7.toByte
        case _            => 8.toByte
      })

    override protected def connection(device: Device, config: ConfigDescriptor): IO[Connection] =
      for {
        interface <- config.interfaces.find(_.number == 0x0).toIO()
        endpoints = interface.endpoints
        out <- endpoints.find(_.address == 0x02).toIO()
        in <- endpoints.find(_.address == 0x83).toIO()
      } yield (interface.number, out.connector, in.connector)

    protected def control(
      handle: DeviceHandle,
      requestType: Int,
      request: Byte,
      value: Int,
      index: Int,
      buffer: ByteBuffer): IO[Int] =
      handle.control(requestType.toByte, request, value.toShort, index.toShort, buffer, 100L)

    override protected def init(handle: DeviceHandle): IO[Unit] =
      for {
        _ <- startup(handle)
        _ <- termios(handle)
      } yield ()

    protected def readLine(handle: DeviceHandle): IO[ByteBuffer] =
      for {
        buffer <- IO(ByteBuffer.allocateDirect(bufferL.capacity()))
        _ <- control(handle, 0xa1, 0x21, 0, 0, buffer)
      } yield buffer

    protected def readVendor(handle: DeviceHandle, value: Int): IO[Int] =
      control(handle, 0xc0, 0x01, value, 0, bufferR)

    protected def startup(handle: DeviceHandle): IO[Unit] =
      for {
        _ <- readVendor(handle, 0x8384)
        _ <- writeVendor(handle, 0x0404, 0)
        _ <- readVendor(handle, 0x8384)
        _ <- readVendor(handle, 0x8383)
        _ <- readVendor(handle, 0x8384)
        _ <- writeVendor(handle, 0x0404, 1)
        _ <- readVendor(handle, 0x8384)
        _ <- readVendor(handle, 0x8383)
        _ <- writeVendor(handle, 0, 1)
        _ <- writeVendor(handle, 1, 0)
      } yield ()

    protected def termios(handle: DeviceHandle): IO[Unit] =
      for {
        _ <- readLine(handle)
        _ <- writeLine(handle)
        _ <- writeVendor(handle, 0, 0)
        _ <- writeFlow(handle, 3)
        _ <- writeFlow(handle, 0)
        _ <- writeBreak(handle, if (uart.flowControl) 0xffff else 0)
        _ <- writeVendor(handle, 0, 0)
        _ <- writeFlow(handle, 3)
        _ <- writeFlow(handle, 3)
      } yield ()

    protected def writeBreak(handle: DeviceHandle, value: Int): IO[Int] =
      control(handle, 0x21, 0x23, value, 0, bufferW)

    protected def writeFlow(handle: DeviceHandle, value: Int): IO[Int] =
      control(handle, 0x21, 0x22, value, 0, bufferW)

    protected def writeLine(handle: DeviceHandle): IO[Int] =
      control(handle, 0x21, 0x20, 0, 0, bufferL)

    protected def writeVendor(handle: DeviceHandle, value: Int, index: Int): IO[Int] =
      control(handle, 0x40, 0x01, value, index, bufferW)
  }

  object Pl2303 {

    def unapply(descriptor: DeviceDescriptor)(implicit U: Uart): Option[Pl2303] =
      if (descriptor.id == Usb.Pl2303) Some(if (descriptor.size0 == 0x40) new TypeHx else new Type01) else None

    class Type01(implicit uart: Uart) extends Pl2303(uart) {
      override protected def startup(dh: DeviceHandle): IO[Unit] =
        for {
          _ <- super.startup(dh)
          _ <- writeVendor(dh, 2, 0x24)
          _ <- dh.clearHalt(0x02)
          _ <- dh.clearHalt(0x83.toByte)
        } yield ()
    }

    class TypeHx(implicit uart: Uart) extends Pl2303(uart) {
      override protected def startup(dh: DeviceHandle): IO[Unit] =
        for {
          _ <- super.startup(dh)
          _ <- writeVendor(dh, 2, 0x44)
          _ <- writeVendor(dh, 8, 0)
          _ <- writeVendor(dh, 9, 0)
        } yield ()
    }

  }

}
