import java.net.NetworkInterface

import better.files.File
import cats.effect.IO
import customjavafx.scene.control._
import fs2.io.fx.{Data, Header, Host, Promo}
import fx.io._
import fx.io.syntax._
import javafx.scene.Cursor
import javafx.scene.input.KeyCode
import scodec.bits.ByteVector

import scala.collection.JavaConverters._


//Display.App makes the game object executable
//Need to define the Window to be Displayed
//Launching Window shall be done by Display.App main
object BaccaratTerminal extends Display.App {

  //Load Data From Database
  val dataFile: File = File(pureconfig.loadConfigOrThrow[String]("game.database.data"))
  val data: Data = if (dataFile.exists) {
    dataFile.readDeserialized[Data]
  } else {
    dataFile.createIfNotExists(asDirectory = false, createParents = true)
    dataFile.writeSerialized(Data(Seq.empty[BeadRoadResult]))
    dataFile.readDeserialized[Data]
  }

  //Load Data From Database
  val dbFile: File = File(pureconfig.loadConfigOrThrow[String]("game.database.menu"))
  val menu: Header = if (dbFile.exists) {
    dbFile.readDeserialized[Header]
  } else {
    dbFile.createIfNotExists(asDirectory = false, createParents = true)
    dbFile.writeSerialized(pureconfig.loadConfigOrThrow[Header]("game.menu"))
    dbFile.readDeserialized[Header]
  }


  //Promo Feature
  val promo: Promo = pureconfig.loadConfigOrThrow[Promo]("promo")

  //Load KeyBoard Information from application.conf
  implicit def keyCodeReader: pureconfig.ConfigReader[Map[KeyCode, String]] = {
    pureconfig.ConfigReader.deriveMap[String].map(_.map {
      case (k, v) => (KeyCode.valueOf(k), v)
    })
  }

  implicit def BeadRoadResultReader: pureconfig.ConfigReader[Map[String, BeadRoadResult]] = {
    pureconfig.ConfigReader.deriveMap[String].map(_.map {
      case (k, v) => (k, BeadRoadResult.valueOf(v))
    })
  }

  val keysMap: Map[KeyCode, String] = pureconfig.loadConfigOrThrow[Map[KeyCode, String]]("keyboard.keys")
  val coupsMap: Map[String, BeadRoadResult] = pureconfig.loadConfigOrThrow[Map[String, BeadRoadResult]]("keyboard.coups")

  def macAddresses: List[String] =
    NetworkInterface.getNetworkInterfaces.asScala.flatMap(i => Option(i.getHardwareAddress)).map(ByteVector(_).toHex).toList


  val mainWindow: IO[Display.Window] = for {
    _ <- IO(println("starting billboard..."))
    actual = macAddresses
    expected = List("1c1b0d9c24e0", "e0d55e55809c", "80ce62eb8f72", "70c94e69f961", "80ce62ebc36c",
      "70c94e69c271", "70c94e69c293", "70c94e69c2a3", "70c94e69d0bf", "70c94e69d9b7", "70c94e69ec3f",
      "70c94e69f961", "70c94e69f9df", "70c94e6a073f", "70c94e6c6e45")
    _ <- IO(require(actual.exists(expected.contains), "unauthorized system"))
    _ <- IO(println("loading window..."))
  } yield Display.Window(
    fxml = "baccarat-nepal.fxml",
    cursor= Cursor.NONE,
    resolver = menu.fxResolver ++ data.fxResolver ++ keysMap.fxResolver ++ coupsMap.fxResolver ++ promo.fxResolver
      ++ Host[Data, Unit](data => IO[Unit] {
      dataFile.writeSerialized(data)
    }).fxResolver
      ++ Host[Header, Unit](menu => IO[Unit] {
      dbFile.writeSerialized(menu)
    }).fxResolver
  )

  override def window: IO[Display.Window] = mainWindow

}