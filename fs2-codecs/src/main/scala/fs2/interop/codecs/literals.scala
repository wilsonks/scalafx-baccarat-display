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

package fs2.interop.codecs

import java.nio.charset.Charset

import scodec.{Codec, Err}
import scodec.bits.ByteVector
import scodec.codecs.{constant, fail}

object literals {

  implicit def constantStringCodec(s: String)(implicit charset: Charset): Codec[Unit] =
    ByteVector.encodeString(s) match {
      case Left(t)  => fail(Err.General(s"${charset.displayName()} cannot encode $s", List(t.getMessage)))
      case Right(b) => constant(b)
    }

  implicit def string2ByteVector(s: String)(implicit charset: Charset): ByteVector = ByteVector.encodeString(s) match {
    case Left(t)  => throw new IllegalArgumentException(s"${charset.displayName()} cannot encode $s", t)
    case Right(b) => b
  }
}
