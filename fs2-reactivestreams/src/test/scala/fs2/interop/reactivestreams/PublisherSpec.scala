package fs2.interop.reactivestreams

import cats.effect.IO
import fs2.Stream
import org.reactivestreams.tck.{PublisherVerification, TestEnvironment}
import org.reactivestreams.{Publisher, Subscriber, Subscription}
import org.scalatest.testng.TestNGSuiteLike

import scala.concurrent.ExecutionContextExecutor

class PublisherSpec extends PublisherVerification[Int](new TestEnvironment(1000L)) with TestNGSuiteLike {

  implicit val context: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  def createFailedPublisher(): Publisher[Int] = new Publisher[Int] {

    def subscribe(s: Subscriber[_ >: Int]): Unit = {
      s.onSubscribe(new Subscription {
        def request(n: Long): Unit = ()

        def cancel(): Unit = ()
      })
      s.onError(new Error("BOOM"))
    }
  }

  def createPublisher(elements: Long): Publisher[Int] = {
    val stream =
      if (Long.MaxValue == elements) Stream.range(0, 9).repeat
      else Stream.iterate(0)(_ + 1).takeWhile(_ < elements)
    stream.covary[IO].toPublisher
  }
}
