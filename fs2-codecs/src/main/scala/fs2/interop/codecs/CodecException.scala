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

import scodec.Err
import scodec.bits.BitVector

sealed trait CodecException extends Exception {

  def err: Err
}

object CodecException {

  case class DecodeFailed(bits: BitVector, err: Err)
      extends Exception(s"$err while decoding ${bits.toHex}") with CodecException

  case class EncodeFailed[A](value: A, err: Err) extends Exception(s"$err while encoding $value") with CodecException
}
