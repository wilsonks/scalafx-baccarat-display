package fs2.interop.reactivestreams.internals

import java.util.concurrent.atomic.AtomicBoolean

import cats.effect._
import fs2.Stream
import org.reactivestreams._
import org.scalatest.FunSuite

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * This behaviour is already tested by the Reactive Stream test
  * suite, but it's proven difficult to enforce, so we add our own
  * tests that run the assertions multiple times to make possible
  * failures due to race conditions more repeatable
  */
class CancellationSpec extends FunSuite {

  val s: Stream[IO, Int] = Stream.range(0, 5).covary[IO]
  val attempts = 10000

  case class SetTrue[A](b: AtomicBoolean) extends Subscriber[A] {
    def onComplete(): Unit = b.set(true)

    def onError(e: Throwable): Unit = b.set(true)

    def onNext(t: A): Unit = b.set(true)

    def onSubscribe(s: Subscription): Unit = b.set(true)
  }

  test("request must be no-op after subscription is cancelled") {
    var i = 0
    val b = new AtomicBoolean(false)
    while (i < attempts) {
      val sub = StreamSubscription(SetTrue[Int](b), s).unsafeRunSync
      sub.unsafeStart()
      sub.cancel()
      sub.request(1)
      sub.request(1)
      sub.request(1)
      i = i + 1
    }
    if (b.get) fail("onNext called after subscription was cancelled")
  }

  test("cancel must be a no-op after subscription is cancelled") {
    var i = 0
    val b = new AtomicBoolean(false)
    while (i < attempts) {
      val sub = StreamSubscription(SetTrue[Int](b), s).unsafeRunSync
      sub.unsafeStart()
      sub.cancel()
      sub.cancel()
      i = i + 1
    }
    if (b.get) fail("onCancel called after subscription was cancelled")
  }
}
