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

import scala.reflect.ClassTag

case class Errs(errors: List[Err], context: List[String]) extends Err {

  def exists[E <: Err: ClassTag]: Boolean = errors.exists {
    case _: E    => true
    case e: Errs => e.exists[E]
    case _       => false
  }

  def message: String = errors.view.map(_.message).mkString("(", ", ", ")")

  def push(err: Err): Errs = copy(err :: errors)

  def pushContext(ctx: String): Err = copy(context = ctx :: context)
}
