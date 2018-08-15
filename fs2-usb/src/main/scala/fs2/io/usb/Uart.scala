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

import fs2.io.usb.Uart.DataBits.DataBits
import fs2.io.usb.Uart.Parity.Parity
import fs2.io.usb.Uart.StopBits.StopBits

case class Uart(
  baudRate: Int = 9600,
  dataBits: DataBits = Uart.DataBits.`8`,
  parity: Parity = Uart.Parity.None,
  stopBits: StopBits = Uart.StopBits.`1`,
  flowControl: Boolean = false)

object Uart {

  object DataBits extends Enumeration {
    type DataBits = Value
    val `5`, `6`, `7`, `8`, `9` = Value
  }

  object Parity extends Enumeration {
    type Parity = Value
    val None, Odd, Even, Mark, Space = Value
  }

  object StopBits extends Enumeration {
    type StopBits = Value
    val `1`, `1.5`, `2` = Value
  }

}
