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

case class Usb(id: Usb.Id, port: Usb.Port)

object Usb {

  case class Id(vendor: Short, product: Short)

  case class Port(number: Int, bus: Int, device: Int)

  object Pl2303 extends Id(0x067b.toByte, 0x2303)

}

sealed trait UsbException extends RuntimeException

object UsbException {

  case object AccessDenied extends UsbException

  case object ResourceBusy extends UsbException

  case object SystemCallInterrupted extends UsbException

  case object InvalidParameter extends UsbException

  case object IoFailed extends UsbException

  case object NoDevice extends UsbException

  case object NoMemory extends UsbException

  case object NoEntity extends UsbException

  case object UnsupportedOperation extends UsbException

  case object DataOverflow extends UsbException

  case object BrokenPipe extends UsbException

  case object OperationTimeout extends UsbException

  case object Other extends UsbException

}
