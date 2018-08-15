package fs2.interop.reactivestreams.internals

import cats.effect._
import fs2.Stream
import fs2.interop.reactivestreams._
import org.scalatest._
import org.scalatest.prop._

import scala.concurrent.ExecutionContext

class PublisherToStreamSpec extends FlatSpec with Matchers with PropertyChecks {

  implicit val ec: ExecutionContext = ExecutionContext.global

  it should "propagate values downstream" in {
    forAll { xs: Seq[Int] =>
      Stream
        .emits(xs)
        .covary[IO]
        .toPublisher
        .toStream[IO]
        .compile
        .toVector
        .unsafeRunSync() shouldEqual xs.toVector
    }
  }

  object Boom extends Exception("BOOM")

  it should "propagate error downstream" in {
    Stream(1, 2, 3)
      .covary[IO]
      .onComplete(Stream.raiseError(Boom))
      .toPublisher
      .toStream[IO]
      .compile
      .drain
      .attempt
      .unsafeRunSync() shouldEqual Left(Boom)
  }

  it should "propagate cancellation upstream" in {
    forAll { (as: Seq[Int], bs: Seq[Int]) =>
      Stream
        .emits(as ++ bs)
        .covary[IO]
        .toPublisher
        .toStream[IO]
        .take(as.size)
        .compile
        .toVector
        .unsafeRunSync() shouldEqual as.toVector
    }
  }
}
