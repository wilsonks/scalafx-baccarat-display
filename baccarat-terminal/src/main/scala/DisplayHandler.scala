
import java.io.{File => JFile}

import com.typesafe.config.Config
import customjavafx.scene.control._
import customjavafx.scene.layout._
import fs2.io.fx.{Display, Host, Menu}
import javafx.animation._
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.collections.ObservableList
import javafx.event.{ActionEvent, EventHandler}
import javafx.scene.control.{Button, Label, TextField}
import javafx.scene.effect.Glow
import javafx.scene.image.ImageView
import javafx.scene.input.{KeyCode, KeyEvent}
import javafx.scene.layout.{BorderPane, Pane, Region, VBox}
import javafx.scene.media.{Media, MediaPlayer, MediaView}
import javafx.scene.transform.Rotate
import javafx.util.Duration
import scalafx.scene.media.AudioClip
import scalafxml.core.macros.sfxml

@sfxml(additionalControls = List("customjavafx.scene.layout", "customjavafx.scene.control"))
class DisplayHandler(
  val gameBox: VBox,
  val tableNumber: Label,
  val handBetMin: Label,
  val handBetMax: Label,
  val tieBetMin: Label,
  val tieBetMax: Label,
  val pairBetMin: Label,
  val pairBetMax: Label,
  val playerWinCount: Label,
  val bankerWinCount: Label,
  val tieWinCount: Label,
  val playerPairCount: Label,
  val bankerPairCount: Label,
  val naturalCount: Label,
  val totalCount: Label,
  val b1: BigEyeRoadLabel,
  val b2: SmallRoadLabel,
  val b3: CockroachRoadLabel,
  val p1: BigEyeRoadLabel,
  val p2: SmallRoadLabel,
  val p3: CockroachRoadLabel,
  val lastWin: LastWinLabel,
  val logo: Region,
  val smallLogo: ImageView,
  val menu: BorderPane,
  val promoPane: Pane,
  val promoMediaView: MediaView,
  val tName: TextField,
  val tHandBetMin: TextField,
  val tHandBetMax: TextField,
  val tTieBetMin: TextField,
  val tTieBetMax: TextField,
  val tPairBetMin: TextField,
  val tPairBetMax: TextField,
  val lName: Button,
  val lHandBetMin: Button,
  val lHandBetMax: Button,
  val lTieBetMin: Button,
  val lTieBetMax: Button,
  val lPairBetMin: Button,
  val lPairBetMax: Button,
  val info: BorderPane,
  val beadRoad: BeadRoadTilePane,
  val bigEyeRoad: BigEyeRoadTilePane,
  val smallRoad: SmallRoadTilePane,
  val cockroachRoad: CockroachRoadTilePane,
  val bigRoad: BigRoadTilePane)(implicit display: Display, writer: Host[Menu, Unit], startMenu: Menu, conf: Config) {

  beadRoad.Initialize(8, 14)
  bigRoad.Initialize(6, 49)
  bigEyeRoad.Initialize(6, 30)
  smallRoad.Initialize(6, 30)
  cockroachRoad.Initialize(6, 30)

  tableNumber.textProperty().bindBidirectional(tName.textProperty())
  handBetMin.textProperty().bindBidirectional(tHandBetMin.textProperty())
  handBetMax.textProperty().bindBidirectional(tHandBetMax.textProperty())
  tieBetMin.textProperty().bindBidirectional(tTieBetMin.textProperty())
  tieBetMax.textProperty().bindBidirectional(tTieBetMax.textProperty())
  pairBetMin.textProperty().bindBidirectional(tPairBetMin.textProperty())
  pairBetMax.textProperty().bindBidirectional(tPairBetMax.textProperty())

  tableNumber.setText(startMenu.name)
  handBetMin.setText(startMenu.handBetMin)
  handBetMax.setText(startMenu.handBetMax)
  tieBetMin.setText(startMenu.tieBetMin)
  tieBetMax.setText(startMenu.tieBetMax)
  pairBetMin.setText(startMenu.pairBetMin)
  pairBetMax.setText(startMenu.pairBetMax)

  if (conf.getBoolean("promoEnabled")) {
    //A Media Player creates a player for a specific media
    val f = new JFile(conf.getString("promoMedia"))
    val media: Media = new Media(f.toURI.toString)
    val mediaPlayer = new MediaPlayer(media)
    mediaPlayer.setCycleCount(-1)
    mediaPlayer.setMute(true)
    promoMediaView.setMediaPlayer(mediaPlayer)
    mediaPlayer.play()
  }

  beadRoad.getCountProperty
    .addListener(new ChangeListener[Number] {
      override def changed(observableValue: ObservableValue[_ <: Number], t1: Number, t2: Number): Unit = {
        if (t2.intValue() > 0) {
          lastWinAnimation.play()
          if (t2.longValue() > t1.longValue()) {
            bigRoad.AddElement(beadRoad)
          } else {
            bigRoad.RemoveElement(beadRoad)
          }
          new AudioClip(getClass.getResource(beadRoad.LastWinAudio()).toExternalForm).play()
          lastWin.setResult(beadRoad.LastWin())
          totalCount.setText(String.valueOf(t2.intValue()))
        } else {
          bigRoad.Reset()
          lastWin.setResult(LastWinResult.EMPTY)
          totalCount.setText("")
        }
        bigRoad.UpdatePredictions(b1, b2, b3, p1, p2, p3)
      }
    })

  beadRoad.getBankerWinCount
    .addListener(new ChangeListener[Number] {
      override def changed(observableValue: ObservableValue[_ <: Number], t1: Number, t2: Number): Unit = {
        if (t2.intValue() > 0) {
          bankerWinCount.setText(String.valueOf(t2.intValue()))
        } else {
          bankerWinCount.setText("")
        }
      }
    })

  beadRoad.getPlayerWinCount
    .addListener(new ChangeListener[Number] {
      override def changed(observableValue: ObservableValue[_ <: Number], t1: Number, t2: Number): Unit = {
        if (t2.intValue() > 0) {
          playerWinCount.setText(String.valueOf(t2.intValue()))
        } else {
          playerWinCount.setText("")
        }
      }
    })

  beadRoad.getTieWinCount
    .addListener(new ChangeListener[Number] {
      override def changed(observableValue: ObservableValue[_ <: Number], t1: Number, t2: Number): Unit = {
        if (t2.intValue() > 0) {
          tieWinCount.setText(String.valueOf(t2.intValue()))
        } else {
          tieWinCount.setText("")
        }
      }
    })

  beadRoad.getBankerPairCount
    .addListener(new ChangeListener[Number] {
      override def changed(observableValue: ObservableValue[_ <: Number], t1: Number, t2: Number): Unit = {
        if (t2.intValue() > 0) {
          bankerPairCount.setText(String.valueOf(t2.intValue()))
        } else {
          bankerPairCount.setText("")
        }
      }
    })

  beadRoad.getPlayerPairCount
    .addListener(new ChangeListener[Number] {
      override def changed(observableValue: ObservableValue[_ <: Number], t1: Number, t2: Number): Unit = {
        if (t2.intValue() > 0) {
          playerPairCount.setText(String.valueOf(t2.intValue()))
        } else {
          playerPairCount.setText("")
        }
      }
    })

  beadRoad.getNaturalCount
    .addListener(new ChangeListener[Number] {
      override def changed(observableValue: ObservableValue[_ <: Number], t1: Number, t2: Number): Unit = {
        if (t2.intValue() > 0) {
          naturalCount.setText(String.valueOf(t2.intValue()))
        } else {
          naturalCount.setText("")
        }
      }
    })

  bigRoad.bigEyeRoadListProperty
    .addListener(new ChangeListener[ObservableList[BigEyeRoadLabel]] {
      override def changed(
        observableValue: ObservableValue[_ <: ObservableList[BigEyeRoadLabel]],
        t: ObservableList[BigEyeRoadLabel],
        t1: ObservableList[BigEyeRoadLabel]): Unit = {
        if (!t1.isEmpty) bigEyeRoad.ReArrangeElements(t1)
        else {
          bigEyeRoad.Reset()
        }
      }
    })

  bigRoad.smallRoadListProperty
    .addListener(new ChangeListener[ObservableList[SmallRoadLabel]] {
      override def changed(
        observableValue: ObservableValue[_ <: ObservableList[SmallRoadLabel]],
        t: ObservableList[SmallRoadLabel],
        t1: ObservableList[SmallRoadLabel]): Unit = {
        if (!t1.isEmpty) smallRoad.ReArrangeElements(t1)
        else {
          smallRoad.Reset()
        }
      }
    })

  bigRoad.cockroachRoadListProperty
    .addListener(new ChangeListener[ObservableList[CockroachRoadLabel]] {
      override def changed(
        observableValue: ObservableValue[_ <: ObservableList[CockroachRoadLabel]],
        t: ObservableList[CockroachRoadLabel],
        t1: ObservableList[CockroachRoadLabel]): Unit = {
        if (!t1.isEmpty) cockroachRoad.ReArrangeElements(t1)
        else {
          cockroachRoad.Reset()
        }
      }
    })

  val lastWinAnimation: RotateTransition = new RotateTransition(Duration.millis(50), lastWin)
  lastWinAnimation.setAxis(Rotate.Y_AXIS)
  lastWinAnimation.setByAngle(180)
  lastWinAnimation.setCycleCount(2)
  lastWinAnimation.setInterpolator(Interpolator.LINEAR)
  lastWinAnimation.setAutoReverse(true)

  val logoAnimation: RotateTransition = new RotateTransition(Duration.millis(5000), logo)
  logoAnimation.setAxis(Rotate.Y_AXIS)
  logoAnimation.setByAngle(180)
  logoAnimation.setCycleCount(2)
  logoAnimation.setInterpolator(Interpolator.LINEAR)
  logoAnimation.setAutoReverse(true)
  logoAnimation.setDelay(Duration.millis(3000))
  logoAnimation.play()

  logoAnimation.setOnFinished(new EventHandler[ActionEvent] {
    override def handle(t: ActionEvent): Unit = {
      logoAnimation.play()
    }
  })

  val logoGlow = new Glow()
  logoGlow.setLevel(.9)

//  logo.setEffect(logoGlow)
  smallLogo.setEffect(logoGlow)

  display.root.addEventHandler(
    KeyEvent.KEY_PRESSED,
    new EventHandler[KeyEvent] {
      var menuOn = false
      var infoOn = false
      var editOn = false
      var pPair = false
      var bPair = false
      var bothPair = false
      var natural = false
      var bWin = false
      var pWin = false
      var tWin = false
      var promoOn = false
      val tList = Array(tName, tHandBetMin, tHandBetMax, tTieBetMin, tTieBetMax, tPairBetMin, tPairBetMax)
      val lList = Array(lName, lHandBetMin, lHandBetMax, lTieBetMin, lTieBetMax, lPairBetMin, lPairBetMax)
      var mIndex: Int = 0

      def focusSame(): Unit = {
        lList(mIndex).requestFocus()
      }

      def focusBack(): Unit = {
        if (mIndex == 0) mIndex = 6
        else {
          mIndex = (mIndex - 1) % 7
        }
        lList(mIndex).requestFocus()
      }

      def focusNext(): Unit = {
        mIndex = (mIndex + 1) % 7
        lList(mIndex).requestFocus()
      }

      def saveMenuToDisk(): Unit = {
        val task = writer.request(
          Menu(
            tableNumber.getText,
            handBetMin.getText,
            handBetMax.getText,
            tieBetMin.getText,
            tieBetMax.getText,
            pairBetMin.getText,
            pairBetMax.getText))
        task.run()
      }

      if (java.awt.Toolkit.getDefaultToolkit.getLockingKeyState(java.awt.event.KeyEvent.VK_NUM_LOCK)) {
        menu.toFront()
        focusSame()
        menuOn = true
      }

      override def handle(t: KeyEvent): Unit = {
        if (!menuOn) {
          t.getCode match {
            case KeyCode.ENTER =>
              (bPair, pPair, bothPair, natural, bWin, pWin, tWin) match {
                case (false, false, false, false, true, false, false) => beadRoad.AddElement(BeadRoadResult.BANKER_WIN)
                case (true, false, false, false, true, false, false) =>
                  beadRoad.AddElement(BeadRoadResult.BANKER_WIN_BANKER_PAIR)
                case (false, true, false, false, true, false, false) =>
                  beadRoad.AddElement(BeadRoadResult.BANKER_WIN_PLAYER_PAIR)
                case (false, false, true, false, true, false, false) =>
                  beadRoad.AddElement(BeadRoadResult.BANKER_WIN_BOTH_PAIR)
                case (false, false, false, true, true, false, false) =>
                  beadRoad.AddElement(BeadRoadResult.BANKER_WIN_NATURAL)
                case (true, false, false, true, true, false, false) =>
                  beadRoad.AddElement(BeadRoadResult.BANKER_WIN_BANKER_PAIR_NATURAL)
                case (false, true, false, true, true, false, false) =>
                  beadRoad.AddElement(BeadRoadResult.BANKER_WIN_PLAYER_PAIR_NATURAL)
                case (false, false, true, true, true, false, false) =>
                  beadRoad.AddElement(BeadRoadResult.BANKER_WIN_BOTH_PAIR_NATURAL)

                case (false, false, false, false, false, true, false) => beadRoad.AddElement(BeadRoadResult.PLAYER_WIN)
                case (true, false, false, false, false, true, false) =>
                  beadRoad.AddElement(BeadRoadResult.PLAYER_WIN_BANKER_PAIR)
                case (false, true, false, false, false, true, false) =>
                  beadRoad.AddElement(BeadRoadResult.PLAYER_WIN_PLAYER_PAIR)
                case (false, false, true, false, false, true, false) =>
                  beadRoad.AddElement(BeadRoadResult.PLAYER_WIN_BOTH_PAIR)
                case (false, false, false, true, false, true, false) =>
                  beadRoad.AddElement(BeadRoadResult.PLAYER_WIN_NATURAL)
                case (true, false, false, true, false, true, false) =>
                  beadRoad.AddElement(BeadRoadResult.PLAYER_WIN_BANKER_PAIR_NATURAL)
                case (false, true, false, true, false, true, false) =>
                  beadRoad.AddElement(BeadRoadResult.PLAYER_WIN_PLAYER_PAIR_NATURAL)
                case (false, false, true, true, false, true, false) =>
                  beadRoad.AddElement(BeadRoadResult.PLAYER_WIN_BOTH_PAIR_NATURAL)

                case (false, false, false, false, false, false, true) => beadRoad.AddElement(BeadRoadResult.TIE_WIN)
                case (true, false, false, false, false, false, true) =>
                  beadRoad.AddElement(BeadRoadResult.TIE_WIN_BANKER_PAIR)
                case (false, true, false, false, false, false, true) =>
                  beadRoad.AddElement(BeadRoadResult.TIE_WIN_PLAYER_PAIR)
                case (false, false, true, false, false, false, true) =>
                  beadRoad.AddElement(BeadRoadResult.TIE_WIN_BOTH_PAIR)
                case (false, false, false, true, false, false, true) =>
                  beadRoad.AddElement(BeadRoadResult.TIE_WIN_NATURAL)
                case (true, false, false, true, false, false, true) =>
                  beadRoad.AddElement(BeadRoadResult.TIE_WIN_BANKER_PAIR_NATURAL)
                case (false, true, false, true, false, false, true) =>
                  beadRoad.AddElement(BeadRoadResult.TIE_WIN_PLAYER_PAIR_NATURAL)
                case (false, false, true, true, false, false, true) =>
                  beadRoad.AddElement(BeadRoadResult.TIE_WIN_BOTH_PAIR_NATURAL)
                case _ =>
              }
              bPair = false
              pPair = false
              bothPair = false
              natural = false
              bWin = false
              pWin = false
              tWin = false
              gameBox.requestFocus()

            case KeyCode.END | KeyCode.NUMPAD1       => pWin = !pWin
            case KeyCode.DOWN | KeyCode.NUMPAD2      => bWin = !bWin
            case KeyCode.PAGE_DOWN | KeyCode.NUMPAD3 => tWin = !tWin

            case KeyCode.UP | KeyCode.NUMPAD8      => natural = !natural
            case KeyCode.PAGE_UP | KeyCode.NUMPAD9 => natural = !natural

            case KeyCode.LEFT | KeyCode.NUMPAD4  => pPair = !pPair
            case KeyCode.RIGHT | KeyCode.NUMPAD6 => bothPair = !bothPair
            case KeyCode.CLEAR | KeyCode.NUMPAD5 => bPair = !bPair
            case KeyCode.MULTIPLY =>
              infoOn = !infoOn
              if (infoOn) info.toFront()
              else {
                info.toBack()
              }

            case KeyCode.SUBTRACT  => beadRoad.RemoveElement()
            case KeyCode.HOME | KeyCode.NUMPAD7  => beadRoad.Reset()
            case KeyCode.DIVIDE =>
              promoOn = !promoOn
              if (promoOn) promoPane.toFront()
              else {
                promoPane.toBack()
                gameBox.requestFocus()
              }
            case KeyCode.NUM_LOCK =>
              menuOn = !menuOn
              if (menuOn) {
                menu.toFront()
                focusSame()
              } else {
                menu.toBack()
                gameBox.requestFocus()
                saveMenuToDisk
              }

            case _ => println(t)
          }
        } else {
          if (!editOn) {
            t.getCode match {
              case KeyCode.NUM_LOCK =>
                menuOn = !menuOn
                if (menuOn) menu.toFront()
                else {
                  menu.toBack()
                  gameBox.requestFocus()
                  saveMenuToDisk
                }
              case KeyCode.ENTER =>
                tList(mIndex).requestFocus()
                editOn = !editOn
              case KeyCode.DOWN | KeyCode.NUMPAD2 => focusNext()
              case KeyCode.UP | KeyCode.NUMPAD8   => focusBack()
              case _                              =>
            }
          } else {
            t.getCode match {
              case KeyCode.ENTER | KeyCode.NUM_LOCK =>
                focusSame()
                editOn = false
              case KeyCode.MULTIPLY =>
                infoOn = !infoOn
                if (infoOn) info.toFront()
                else {
                  info.toBack()
                }
              case _ =>
            }

          }
        }
      }
    }
  )

  display.root.setOnCloseRequest(_ => {
    display.exit()
  })

}
