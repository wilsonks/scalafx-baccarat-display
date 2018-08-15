package fs2.io.fx

import better.files._
import cats.Show
import cats.effect.IO
import com.typesafe.config.ConfigFactory
import fs2.io.fx.Display.{Bounds, Dimension, Position}
import javafx.stage.StageStyle

object Terminal extends App {

  implicit def show[A]: Show[A] = Show.fromToString[A]

  val conf = ConfigFactory.load
  val databaseFile = File(conf.getString("database.file"))

  Display
    .launch(
      for {
        startMenu <- IO[Menu] {
          // read previous state from file
          if (databaseFile.exists) {
            databaseFile.readDeserialized[Menu]

          } else {
            databaseFile.createIfNotExists(asDirectory = false, createParents = true)
            databaseFile.writeSerialized(Menu("1", "100", "10000", "100", "10000", "500", "5000"))
            databaseFile.readDeserialized[Menu]
          }
        }
        window = Display.Window(
          title = conf.getString("title"),
          fullscreen = conf.getBoolean("window.fullscreen"),
          resizable = conf.getBoolean("window.resizable"),
          alwaysOnTop = conf.getBoolean("window.alwaysOnTop"),
          style = { if (conf.getBoolean("window.decorated")) StageStyle.DECORATED else StageStyle.UNDECORATED },
          position = Position(Option(conf.getDouble("window.positionX")), Option(conf.getDouble("window.positionY"))),
          bounds = Bounds(
            width = Dimension(
              min = Option(conf.getDouble("window.width.min")),
              max = Option(conf.getDouble("window.width.max"))),
            height = Dimension(
              min = Option(conf.getDouble("window.height.min")),
              max = Option(conf.getDouble("window.height.max")))
          ),
          fxml = "baccarat.fxml",
          resolver = Display.resolve(
            Display.resolveBySubType(Host[Menu, Unit](menu =>
              IO[Unit] {
                // serialize and write to file
                databaseFile.writeSerialized(menu)
            })),
            Display.resolveBySubType(startMenu)
          )
        )
      } yield window
    )(args)
    .unsafeRunSync()
}

case class Menu(
  name: String,
  handBetMin: String,
  handBetMax: String,
  tieBetMin: String,
  tieBetMax: String,
  pairBetMin: String,
  pairBetMax: String)
