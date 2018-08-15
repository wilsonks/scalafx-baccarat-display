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

import cats.syntax.show._
import cats.{Eq, Show}

package object instances {

  implicit val catsStdShowForUsbId: Show[Usb.Id] =
    Show.show(id => "ID %04x:%04x".format(id.vendor, id.product))
  implicit val catsStdShowForUsbPort: Show[Usb.Port] =
    Show.show(port => "Bus %02d.Port %d: Dev %d".format(port.bus, port.number, port.device))
  implicit val catsStdShowForUsb: Show[Usb] =
    Show.show(usb => "%s %s".format(usb.id.show, usb.port.show))
  implicit val catsStdEqForUsb: Eq[Usb] =
    Eq.fromUniversalEquals[Usb]
}
