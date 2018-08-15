package fs2.io.usb

import cats.Show
import cats.effect.IO
import cats.implicits._
import fs2.async.mutable.Signal
import fs2.io.ConsoleIO
import fs2.io.usb.instances._
import fs2.{Pipe, Stream}
import scodec.bits.ByteVector

import scala.concurrent.ExecutionContext

object Repl extends App {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val uart: Uart = Uart()

  val decoder: Pipe[IO, String, ByteVector] =
    _.evalMap(s =>
      ByteVector.fromHex(s) match {
        case Some(hex) => IO.pure(hex)
        case None      => IO.raiseError(new IllegalArgumentException(s"$s is not a valid hex string"))
    })
  implicit val encoder: Show[ByteVector] = Show.show(_.toHex)

  val program: IO[Unit] = for {
    _ <- ConsoleIO.put("info", "enter USB ID (eg: 23d8:0285) of device ...")
    s <- ConsoleIO.get
    id <- toUsbId(s)
    stop <- Signal[IO, Boolean](false)
    host <- fs2.async.unboundedQueue[IO, ByteVector]
    _ <- scan(id).flatMap {
      case (usb, port) =>
        Stream
          .eval(ConsoleIO.put("info", "enter hex string (eg: f2000343313003b0) to write to device..."))
          .flatMap(
            _ =>
              host.dequeue
                .through(port)
                .handleErrorWith {
                  case UsbException.NoDevice =>
                    Stream.eval_(ConsoleIO.put("info", s"device removed from ${usb.port.show}"))
                  case ex => Stream.raiseError(ex)
              })
          .to(ConsoleIO.sink(usb.port.show))
    }.concurrently(
        ConsoleIO.stream
          .takeWhile(_.nonEmpty)
          .through(decoder)
          .onFinalize(stop.set(true))
          .to(host.enqueue))
      .interruptWhen(stop)
      .compile
      .drain
  } yield ()

  program.unsafeRunSync()

  def toShort(s: String): IO[Short] = IO(Integer.parseInt(s, 16).toShort)

  def toUsbId(s: String): IO[Usb.Id] =
    for {
      v <- toShort(s.take(4))
      p <- toShort(s.drop(5))
    } yield Usb.Id(v, p)
}
