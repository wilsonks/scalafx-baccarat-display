import java.net.{InetAddress, NetworkInterface}
import java.util.ResourceBundle

import better.files._
import cats.Show
import cats.effect.IO
import com.typesafe.config.ConfigFactory
import fs2.io.fx.{Display, Header, Host}
import javafx.scene.Cursor
import pureconfig.ConfigReader
import scalafxml.core.{ControllerDependencyResolver, NoDependencyResolver}
import scodec.bits.ByteVector

import scala.collection.JavaConverters._
import scala.util.Try

object Terminal extends App {

  implicit def show[A]: Show[A] = Show.fromToString[A]

  val conf = ConfigFactory.load
  val databaseFile = File(conf.getString("database.file"))

  implicit def controllerReader: ConfigReader[ControllerDependencyResolver] = ConfigReader.fromString(_ => Right(NoDependencyResolver))

  implicit def resourceReader: ConfigReader[ResourceBundle] = ConfigReader.fromStringTry(s => Try(ResourceBundle.getBundle(s)))

  implicit def cursorReader: ConfigReader[Cursor] = ConfigReader.fromNonEmptyString(s => Right(Cursor.cursor(s)))

  def macAddresses: List[String] =
      NetworkInterface.getNetworkInterfaces.asScala.flatMap(i => Option(i.getHardwareAddress)).map(ByteVector(_).toHex).toList

  Display
    .launch(
      for {
        _ <- IO(println("starting billboard..."))
        startMenu <- IO[Header] {
          // read previous state from file
          if (databaseFile.exists) {
            databaseFile.readDeserialized[Header]

          } else {
            databaseFile.createIfNotExists(asDirectory = false, createParents = true)
            databaseFile.writeSerialized(Header("1", "100", "10000", "100", "10000", "500", "5000"))
            databaseFile.readDeserialized[Header]
          }
        }

        actual = macAddresses
        expected = List("1c1b0d9c24e0", "e0d55e55809c", "80ce62eb8f72", "70c94e69f961", "80ce62ebc36c")
        _ <- IO(require(actual.exists(expected.contains)))
        _ <- IO(println("loading window..."))
        window = pureconfig.loadConfigOrThrow[Display.Window]("window").copy(resolver = Display.resolve(
          Display.resolveBySubType(Host[Header, Unit](menu => IO[Unit] {
            // serialize and write to file
            databaseFile.writeSerialized(menu)
          })),
          Display.resolveBySubType(startMenu),
          Display.resolveBySubType(conf)
        )
        )
      } yield window
    )(args)
    .unsafeRunSync()
}
