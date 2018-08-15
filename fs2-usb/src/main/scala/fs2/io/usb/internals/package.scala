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

package fs2.io.usb

import java.nio.ByteBuffer

import cats.effect.IO
import cats.implicits._
import cats.kernel.instances.list.catsKernelStdEqForList
import fs2.io.usb.instances.catsStdEqForUsb
import fs2.{Scheduler, Sink, Stream}
import org.usb4java._
import scodec.bits.ByteVector

import scala.annotation.switch
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

package object internals {

  import LibUsb._
  import UsbException._
  import cats.instances.list.{catsStdInstancesForList => L}

  import scala.collection.JavaConverters._

  type Endpoint = (Byte, Int, Int)
  type Connection = (Int, Endpoint, Endpoint)

  def pluggable: Boolean = hasCapability(CAP_HAS_HOTPLUG)

  private def lift(op: => Int): IO[Unit] =
    IO.suspend((op: @switch) match {
      case ERROR_ACCESS        => IO.raiseError(AccessDenied)
      case ERROR_BUSY          => IO.raiseError(ResourceBusy)
      case ERROR_INTERRUPTED   => IO.raiseError(SystemCallInterrupted)
      case ERROR_INVALID_PARAM => IO.raiseError(InvalidParameter)
      case ERROR_IO            => IO.raiseError(IoFailed)
      case ERROR_NO_DEVICE     => IO.raiseError(NoDevice)
      case ERROR_NO_MEM        => IO.raiseError(NoMemory)
      case ERROR_NOT_FOUND     => IO.raiseError(NoEntity)
      case ERROR_NOT_SUPPORTED => IO.raiseError(UnsupportedOperation)
      case ERROR_OTHER         => IO.raiseError(Other)
      case ERROR_OVERFLOW      => IO.raiseError(DataOverflow)
      case ERROR_PIPE          => IO.raiseError(BrokenPipe)
      case ERROR_TIMEOUT       => IO.raiseError(OperationTimeout)
      case _                   => IO.unit
    })

  private def liftI(op: => Int): IO[Int] =
    IO(op).flatMap(i => lift(i).map(_ => i))

  implicit class UsbContextOps(val self: Context) extends AnyVal {

    def devices: IO[List[Device]] =
      iterate(_.toList)

    def enumerate(predicate: Usb => Boolean): IO[List[Usb]] =
      devices.flatMap(
        L.traverse(_)(_.usb.attempt)
          .map(_.collect {
            case Right(usb) if predicate(usb) => usb // ignore error that may occur while reading descriptor
          }))

    def exit: IO[Unit] =
      IO(LibUsb.exit(self))

    def find(f: Device => Boolean): IO[Device] =
      iterate(xs => xs.find(f).toIO()).flatMap(io => io)

    def init: IO[Unit] =
      lift(LibUsb.init(self))

    // TODO replace with IO.bracket on upgrade
    def iterate[A](use: Iterator[Device] => A): IO[A] =
      IO.suspend {
        val dl = new DeviceList
        lift(getDeviceList(self, dl))
          .map(_ => dl.iterator().asScala)
          .map(xs =>
            try use(xs)
            finally freeDeviceList(dl, true))
      }

    def scan(predicate: Usb => Boolean)(implicit ec: ExecutionContext): Stream[IO, Usb] =
      Stream.bracket(IO(new HotplugCallbackHandle))(
        handle =>
          Stream
            .eval(fs2.async.unboundedQueue[IO, Usb])
            .flatMap(
              q =>
                Stream
                  .eval(lift(hotplugRegisterCallback(
                    self,
                    HOTPLUG_EVENT_DEVICE_ARRIVED,
                    HOTPLUG_ENUMERATE,
                    HOTPLUG_MATCH_ANY,
                    HOTPLUG_MATCH_ANY,
                    HOTPLUG_MATCH_ANY,
                    (_: Context, device: Device, event: Int, _: scala.Any) => {
                      device.usb.attempt.flatMap {
                        case Right(usb) if predicate(usb) =>
                          (event: @switch) match {
                            case HOTPLUG_EVENT_DEVICE_ARRIVED => q.enqueue1(usb)
                            case _                            => IO.unit
                          }
                        case _ => IO.unit
                      }.unsafeRunSync()
                      0
                    },
                    null,
                    handle
                  )))
                  .flatMap(_ => q.dequeue)),
        handle => IO(hotplugDeregisterCallback(self, handle))
      )

    def scan(predicate: Usb => Boolean, interval: FiniteDuration)(implicit ec: ExecutionContext): Stream[IO, Usb] =
      Scheduler[IO](1, threadPrefix = "usb-scan")
        .flatMap(s => s.fixedDelay[IO](interval))
        .evalMap(_ => enumerate(predicate))
        .changes
        .scan(List.empty[Usb])((prev, next) => next diff prev)
        .flatMap(Stream(_: _*))

    def update(timeout: Long): IO[Unit] =
      lift(handleEventsTimeout(self, timeout))
  }

  implicit class UsbDeviceOps(val self: Device) extends AnyVal {

    // TODO replace with IO.bracket on upgrade
    def configuration[A](use: ConfigDescriptor => IO[A]): IO[A] =
      IO.suspend {
        val cd = new ConfigDescriptor
        lift(getActiveConfigDescriptor(self, cd))
          .flatMap(_ => use(cd).flatMap(a => IO(freeConfigDescriptor(cd)).map(_ => a)))
      }

    def descriptor: IO[DeviceDescriptor] =
      IO.suspend {
        val dd = new DeviceDescriptor
        lift(getDeviceDescriptor(self, dd)).map(_ => dd)
      }

    def open: IO[DeviceHandle] =
      IO.suspend {
        val dh = new DeviceHandle
        lift(LibUsb.open(self, dh)).map(_ => dh)
      }

    def port: Usb.Port =
      Usb.Port(getPortNumber(self), getBusNumber(self), getDeviceAddress(self))

    def usb: IO[Usb] =
      descriptor.map(dd => Usb(dd.id, port))
  }

  implicit class UsbHandleOps(val self: DeviceHandle) extends AnyVal {

    def claim(interface: Int): IO[Unit] =
      for {
        i <- isKernelActive(interface).attempt
        _ <- i match {
          case Right(true)                               => detachKernel(interface)
          case Right(false) | Left(UnsupportedOperation) => IO.unit
          case Left(ex)                                  => IO.raiseError(ex)
        }
        _ <- lift(claimInterface(self, interface))
      } yield ()

    def clearHalt(endpoint: Byte): IO[Unit] =
      lift(LibUsb.clearHalt(self, endpoint))

    def close: IO[Unit] =
      IO(LibUsb.close(self))

    def control(
      requestType: Byte,
      request: Byte,
      value: Short,
      index: Short,
      buffer: ByteBuffer,
      timeout: Long): IO[Int] =
      liftI(controlTransfer(self, requestType, request, value, index, buffer, timeout))

    def detachKernel(interface: Int): IO[Unit] =
      lift(detachKernelDriver(self, interface)).attempt.flatMap {
        case Left(NoEntity) | Right(_) => IO.unit
        case Left(ex)                  => IO.raiseError(ex)
      }

    def device: Device =
      getDevice(self)

    def isKernelActive(interface: Int): IO[Boolean] =
      liftI(kernelDriverActive(self, interface)).map {
        case 1 => true
        case _ => false
      }

    def reader(endpoint: Endpoint)(implicit ec: ExecutionContext): Stream[IO, ByteVector] =
      Stream
        .eval(fs2.async.unboundedQueue[IO, Either[Throwable, ByteVector]])
        .flatMap(
          q =>
            Stream
              .eval(fs2.async.signalOf[IO, Boolean](false))
              .flatMap(halted =>
                Stream.bracket(IO(transfer(
                  endpoint,
                  ByteBuffer.allocateDirect(endpoint._3),
                  (transfer: Transfer) =>
                    ((transfer.status(): @switch) match {
                      case TRANSFER_COMPLETED => q.enqueue1(Right(transfer.bytes)) *> transfer.submit // reuse transfer
                      case TRANSFER_TIMED_OUT => transfer.submit // reuse transfer
                      case TRANSFER_CANCELLED => transfer.free
                      case TRANSFER_NO_DEVICE => halted.set(true) *> q.enqueue1(Left(NoDevice))
                      case TRANSFER_OVERFLOW  => halted.set(true) *> q.enqueue1(Left(DataOverflow))
                      case _                  => halted.set(true) *> q.enqueue1(Left(IoFailed))
                    }).unsafeRunSync(),
                  0
                )))(
                  read => Stream.eval(read.submit).flatMap(_ => q.dequeue.rethrow),
                  read => halted.get.ifM(ifTrue = read.free, ifFalse = read.cancel)
              )))

    def release(interface: Int): IO[Unit] =
      lift(releaseInterface(self, interface))

    def transfer(endpoint: Endpoint, buffer: ByteBuffer, callback: TransferCallback, timeout: Long): Transfer = {
      val t = allocTransfer()
      endpoint._2 match {
        case TRANSFER_TYPE_CONTROL   => fillControlTransfer(t, self, buffer, callback, null, timeout)
        case TRANSFER_TYPE_INTERRUPT => fillInterruptTransfer(t, self, endpoint._1, buffer, callback, null, timeout)
        case TRANSFER_TYPE_BULK      => fillBulkTransfer(t, self, endpoint._1, buffer, callback, null, timeout)
        // setting number of iso packets to 1 (default for high speed devices)
        // if support for high speed devices is required, this will have to be calculated as described here:
        // https://docs.microsoft.com/en-us/windows-hardware/drivers/usbcon/transfer-data-to-isochronous-endpoints
        case TRANSFER_TYPE_ISOCHRONOUS => fillIsoTransfer(t, self, endpoint._1, buffer, 1, callback, null, timeout)
      }
      t
    }

    def write(bytes: Array[Byte], endpoint: Endpoint, timeout: Long): IO[Unit] =
      if (bytes.isEmpty) IO.unit
      else if (bytes.length > endpoint._3) bytes.splitAt(endpoint._3) match {
        case (head, tail) =>
          write(head, endpoint, timeout)
            .flatMap(_ => write(tail, endpoint, timeout))
      } else
        IO.cancelable[Unit] { cb =>
          val t = transfer(
            endpoint,
            ByteBuffer.allocateDirect(bytes.length).put(bytes),
            (transfer: Transfer) => {
              (transfer.status(): @switch) match {
                case TRANSFER_COMPLETED => cb(Right(()))
                case TRANSFER_CANCELLED =>
                case TRANSFER_TIMED_OUT => cb(Left(OperationTimeout))
                case TRANSFER_NO_DEVICE => cb(Left(NoDevice))
                case TRANSFER_OVERFLOW  => cb(Left(DataOverflow))
                case _                  => cb(Left(IoFailed))
              }
              transfer.free.unsafeRunSync() // ignore result of resource release
            },
            timeout
          )
          t.submit.attempt.unsafeRunSync() match {
            case Right(_) => t.cancel
            case Left(ex) => cb(Left(ex)); IO.unit
          }
        }

    def writer(endpoint: Endpoint, timeout: Long): Sink[IO, ByteVector] =
      Sink(bytes => write(bytes.toArray, endpoint, timeout))
  }

  implicit class UsbConfigOps(val self: ConfigDescriptor) extends AnyVal {

    def interfaces: Array[InterfaceDescriptor] =
      self.iface().flatMap(_.altsetting())
  }

  implicit class UsbDescriptorOps(val self: DeviceDescriptor) extends AnyVal {

    def id: Usb.Id =
      Usb.Id(self.idVendor(), self.idProduct())

    /** returns the maximum packet size for the control endpoint **/
    def size0: Byte =
      self.bMaxPacketSize0()
  }

  implicit class UsbInterfaceOps(val self: InterfaceDescriptor) extends AnyVal {

    def endpoints: Array[EndpointDescriptor] =
      self.endpoint()

    def number: Byte =
      self.bInterfaceNumber()
  }

  implicit class UsbEndpointOps(val self: EndpointDescriptor) extends AnyVal {

    def address: Byte =
      self.bEndpointAddress()

    def connector: Endpoint =
      (address, transferType, limit)

    def isInput: Boolean =
      ENDPOINT_IN == (address & ENDPOINT_DIR_MASK)

    def isOutput: Boolean =
      ENDPOINT_OUT == (address & ENDPOINT_DIR_MASK)

    def limit: Short =
      self.wMaxPacketSize()

    def transferType: Int =
      self.bmAttributes() & TRANSFER_TYPE_MASK
  }

  implicit class UsbTransferOps(val self: Transfer) extends AnyVal {

    def bytes: ByteVector =
      ByteVector(self.buffer()).take(self.actualLength())

    def cancel: IO[Unit] =
      lift(cancelTransfer(self))

    def free: IO[Unit] =
      IO(freeTransfer(self))

    def submit: IO[Unit] =
      lift(submitTransfer(self))
  }

  implicit class UsbOptionOps[A](val self: Option[A]) extends AnyVal {

    def toIO(ex: UsbException = InvalidParameter): IO[A] =
      self match {
        case Some(a) => IO.pure(a)
        case None    => IO.raiseError(ex)
      }
  }

}
