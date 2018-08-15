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

package fs2

import java.util.concurrent.TimeUnit

import cats.data.NonEmptyList
import cats.effect.{Sync, Timer}
import cats.implicits._
import cats.{FlatMap, Functor, Monad}
import fs2.async.mutable.Queue

import scala.annotation.tailrec
import scala.concurrent.duration.{Duration, FiniteDuration}

package object patterns {

  def stepper[F[_], Input, Event, State](load: (Event, State) => F[Input])(
    boot: F[(Event, State)],
    exec: (Input, State) => Event,
    step: (State, Event) => State)(implicit F: Monad[F]): Stream[F, (Event, State)] =
    Stream
      .eval(boot)
      .flatMap(init =>
        Stream.iterateEval(init) {
          case (event, state) =>
            load(event, state).map(input => exec(input, state)).map(event => (event, step(state, event)))
      })

  def machine[F[_], Input, Event, State](load: (Event, State) => F[Input])(
    boot: F[(Event, State)],
    exec: (Input, State) => Event,
    step: (State, Event) => State,
    trip: PartialFunction[(Event, State), Event],
    tripLimit: Int = 1000)(implicit F: Sync[F]): Stream[F, NonEmptyList[(Event, State)]] = {
    type Transition = (Event, State)
    type Transaction = NonEmptyList[Transition]

    @tailrec
    def loop(count: Int, last: State, event: Event, z: List[Transition]): Transaction = {
      if (count > tripLimit) throw new RuntimeException(s"trip limit exceeded at $event after $last")
      val next = step(last, event)
      val head = (event, next)
      if (trip.isDefinedAt(head)) loop(count + 1, next, trip(head), head :: z)
      else NonEmptyList.of(head, z: _*)
    }

    def eval(load: Input, last: State): Transaction = {
      val head = exec(load, last)
      loop(0, last, head, Nil)
    }

    Stream
      .eval(boot)
      .flatMap(init =>
        Stream.iterateEval(NonEmptyList.of(init)) {
          case NonEmptyList((event, state), _) => load(event, state).map(eval(_, state))
      })
      .map(_.reverse)
  }

  implicit class TimerAutomataOps[F[_]](val self: Timer[F]) extends AnyVal {

    def duration(time: Long, unit: TimeUnit)(implicit F: Functor[F]): F[FiniteDuration] =
      self
        .clockRealTime(unit)
        .map(now => if (now >= time) Duration.Zero else FiniteDuration(time - now, unit))
  }

  implicit class QueueAutomataOps[F[_], A](val self: Queue[F, A]) extends AnyVal {

    def dequeue1Timed[AA <: A](time: Long, unit: TimeUnit, fallback: AA)(
      implicit F: FlatMap[F],
      S: Scheduler,
      T: Timer[F]): F[A] =
      T.duration(time, unit).flatMap(dequeue1Timed(_, fallback))

    def dequeue1Timed[AA <: A](timeout: FiniteDuration, fallback: AA)(implicit F: Functor[F], S: Scheduler): F[A] =
      self.timedDequeue1(timeout, S).map(_.getOrElse(fallback))
  }

}
