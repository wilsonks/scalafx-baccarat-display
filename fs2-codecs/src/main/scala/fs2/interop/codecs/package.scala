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

package fs2.interop

import java.nio.charset.Charset

import cats.effect.Sync
import fs2.interop.codecs.CodecException.{DecodeFailed, EncodeFailed}
import fs2.{Pipe, Stream}
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs.{filtered, fixedSizeBytes, mappedEnum, string}
import scodec.{Attempt, Codec, DecodeResult, Decoder, Encoder, Err, SizeBound}

import scala.annotation.tailrec

package object codecs {

  object codecs {

    def char(implicit charset: Charset): Codec[Char] =
      fixedSizeBytes(1, string(charset))
        .xmap[Char](_.head, String.valueOf)

    def switch[A](choices: Codec[A]*): Codec[A] = Codec(
      Encoder.choiceEncoder(choices: _*),
      new Decoder[A] {
        def decode(bits: BitVector): Attempt[DecodeResult[A]] = loop(choices, bits, Errs(Nil, Nil))

        @tailrec private def loop(stack: Seq[Decoder[A]], bits: BitVector, err: Errs): Attempt[DecodeResult[A]] =
          if (stack.isEmpty) Attempt.failure(err)
          else
            stack.head.decode(bits) match {
              case res @ Attempt.Successful(_) => res
              case Attempt.Failure(e)          => loop(stack.tail, bits, err push e)
            }
      }
    )

    @tailrec
    private def decodeAccumulate[F[_], A](decoder: Decoder[A])(bits: BitVector, acc: List[A], suspend: Err => Boolean)(
      implicit F: Sync[F]): F[DecodeResult[List[A]]] =
      decoder.decode(bits) match {
        case Attempt.Failure(e) =>
          if (suspend(e)) F.pure(DecodeResult(acc.reverse, bits)) else F.raiseError(DecodeFailed(bits, e))
        case Attempt.Successful(DecodeResult(a, r)) =>
          if (r.isEmpty) F.pure(DecodeResult((a :: acc).reverse, r))
          else decodeAccumulate(decoder)(r, a :: acc, suspend)
      }

    private def suspendOnInsufficientBits(err: Err): Boolean = err match {
      case _: Err.InsufficientBits                   => true
      case e: Errs if e.exists[Err.InsufficientBits] => true
      case _                                         => false
    }

    implicit class CodecFs2Ops[A](val self: Codec[A]) extends AnyVal {

      def <=>[B](mappings: (B, A)*): Codec[B] = translate(mappings: _*)

      def <<~|(etx: ByteVector): Codec[A] = until(etx)

      def --(byte: Byte): Codec[A] = drop(byte)

      def drop(byte: Byte): Codec[A] = dropWhile(_ == byte)

      def dropWhile(p: Byte => Boolean): Codec[A] =
        filtered(
          self,
          new Codec[BitVector] {

            def decode(bits: BitVector): Attempt[DecodeResult[BitVector]] =
              Attempt.successful(DecodeResult(bits.bytes.dropWhile(p).bits, BitVector.empty))

            def encode(value: BitVector): Attempt[BitVector] = Attempt.successful(BitVector.empty)

            def sizeBound: SizeBound = SizeBound.exact(0)
          }
        )

      def translate[B](mappings: (B, A)*): Codec[B] = mappedEnum(self, mappings: _*)

      def until(etx: ByteVector): Codec[A] = new Codec[A] {

        private[this] def loop(bits: BitVector, from: Long): Attempt[DecodeResult[A]] =
          if (bits.size < from) Attempt.failure(Err.insufficientBits(bits.size + etx.bits.size, bits.size))
          else
            bits.bytes.indexOfSlice(etx, from) match {
              case -1 => Attempt.failure(Err.insufficientBits(bits.size + etx.bits.size, bits.size))
              case i =>
                bits.bytes.splitAt(i) match {
                  case (b1, b2) =>
                    self.decode(b1.bits).map(_.mapRemainder(_ ++ b2.drop(etx.size).bits)).orElse(loop(bits, i + 1))
                }
            }

        def decode(bits: BitVector): Attempt[DecodeResult[A]] = loop(bits, 0)

        def encode(value: A): Attempt[BitVector] = self.encode(value).map(_ ++ etx.bits)

        def sizeBound: SizeBound = self.sizeBound + SizeBound.exact(etx.bits.size)

        override def toString: String = s"$self<<~${etx.toHex}"
      }
    }

    implicit class DecoderFs2Ops[A](val self: Decoder[A]) extends AnyVal {

      def reader[F[_]](suspend: Err => Boolean = suspendOnInsufficientBits)(
        implicit F: Sync[F]): Pipe[F, ByteVector, A] =
        bytes =>
          bytes
            .evalScan[DecodeResult[List[A]]](DecodeResult(Nil, BitVector.empty)) {
              case (acc, next) => decodeAccumulate(self)(acc.remainder.bytes.bits ++ next.bits, Nil, suspend)
            }
            .flatMap(result => Stream(result.value: _*))
    }

    implicit class EncoderFs2Ops[A](val self: Encoder[A]) extends AnyVal {

      def writer[F[_]](implicit F: Sync[F]): Pipe[F, A, ByteVector] =
        _.evalMap(a =>
          self.encode(a).fold(err => F.raiseError[ByteVector](EncodeFailed(a, err)), bits => F.pure(bits.bytes)))
    }

    implicit class StreamCodecOps[F[_], A](val self: Stream[F, A]) {

      def decode[B](decoder: Decoder[B], suspend: Err => Boolean = suspendOnInsufficientBits)(
        implicit F: Sync[F],
        ev: A =:= ByteVector): Stream[F, B] =
        decoder.reader(suspend).apply(self.map(ev))

      def encode(encoder: Encoder[A])(implicit F: Sync[F]): Stream[F, ByteVector] =
        encoder.writer.apply(self)
    }

  }

}
