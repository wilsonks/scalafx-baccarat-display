
import java.io.{File => JFile}
import java.util

import customjavafx.scene.control._
import customjavafx.scene.layout._
import fs2.io.fx.{Data, Header, Host, Promo}
import fx.io.Display
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
import sodium.syntax._

@sfxml
class ResetHandler
(
  val yes: Button,
  val no: Button
)(implicit display: fx.io.Display) {
  display.root
    .handle(KeyEvent.KEY_RELEASED)
    .map(_.getCode)
    .foreach(result => println(result))
}

//The FXML Controller can be defined as simple scala class
@sfxml(additionalControls = List("customjavafx.scene.control", "customjavafx.scene.layout"))
class DisplayHandler
(
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
  val footing: Label,
  //  val tName: TextField,
  val tHandBetMin: TextField,
  val tHandBetMax: TextField,
  val tTieBetMin: TextField,
  val tTieBetMax: TextField,
  val tPairBetMin: TextField,
  val tPairBetMax: TextField,
  //  val lName: Button,
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
  val bigRoad: BigRoadTilePane
)(implicit display: fx.io.Display, writerHeader: Host[Header, Unit], writerData: Host[Data, Unit],
  data: Data, header: Header, keysMap: Map[KeyCode, String], coupsMap: Map[String, BeadRoadResult], promo: Promo,
  restartWindow: Display.Window) {

  val lastWinAnimation: RotateTransition = new RotateTransition(Duration.millis(50), lastWin)
  val logoAnimation: RotateTransition = new RotateTransition(Duration.millis(5000), logo)
  val logoGlow = new Glow()

  beadRoad.Initialize(8, 14)
  bigRoad.Initialize(6, 49)
  bigEyeRoad.Initialize(6, 30)
  smallRoad.Initialize(6, 30)
  cockroachRoad.Initialize(6, 30)

  //  tableNumber.textProperty().bindBidirectional(tName.textProperty())
  handBetMin.textProperty().bindBidirectional(tHandBetMin.textProperty())
  handBetMax.textProperty().bindBidirectional(tHandBetMax.textProperty())
  tieBetMin.textProperty().bindBidirectional(tTieBetMin.textProperty())
  tieBetMax.textProperty().bindBidirectional(tTieBetMax.textProperty())
  pairBetMin.textProperty().bindBidirectional(tPairBetMin.textProperty())
  pairBetMax.textProperty().bindBidirectional(tPairBetMax.textProperty())

  //  tableNumber.setText(header.name)
  handBetMin.setText(header.handBetMin)
  handBetMax.setText(header.handBetMax)
  tieBetMin.setText(header.tieBetMin)
  tieBetMax.setText(header.tieBetMax)
  pairBetMin.setText(header.pairBetMin)
  pairBetMax.setText(header.pairBetMax)


  val tList = Array(tHandBetMin, tHandBetMax, tTieBetMin, tTieBetMax, tPairBetMin, tPairBetMax)
  val lList = Array(lHandBetMin, lHandBetMax, lTieBetMin, lTieBetMax, lPairBetMin, lPairBetMax)
  var menuOn = false
  var infoOn = false
  var editOn = false
  var promoOn = false
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

  //  def saveMenuToDisk(): Unit = {
  //    val task = writerHeader.request(
  //      Header(
  ////        tableNumber.getText,
  //        handBetMin.getText,
  //        handBetMax.getText,
  //        tieBetMin.getText,
  //        tieBetMax.getText,
  //        pairBetMin.getText,
  //        pairBetMax.getText))
  //    task.run()
  //  }

  import scala.collection.JavaConverters._

  def saveDataToDisk(): Unit = {
    val array: util.ArrayList[BeadRoadResult] = beadRoad.getElements
    val task = writerData.request(Data(array.asScala.toList.filter(result => result != BeadRoadResult.EMPTY)))
    task.run()
  }

  if (java.awt.Toolkit.getDefaultToolkit.getLockingKeyState(java.awt.event.KeyEvent.VK_NUM_LOCK)) {
    menu.toFront()
    lList(mIndex).requestFocus()
    menuOn = true
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


  lastWinAnimation.setAxis(Rotate.Y_AXIS)
  lastWinAnimation.setByAngle(180)
  lastWinAnimation.setCycleCount(2)
  lastWinAnimation.setInterpolator(Interpolator.LINEAR)
  lastWinAnimation.setAutoReverse(true)


  logoAnimation.setAxis(Rotate.Y_AXIS)
  logoAnimation.setByAngle(180)
  logoAnimation.setCycleCount(2)
  logoAnimation.setInterpolator(Interpolator.LINEAR)
  logoAnimation.setAutoReverse(true)
  logoAnimation.setDelay(Duration.millis(10000))
  logoAnimation.play()

  logoAnimation.setOnFinished(new EventHandler[ActionEvent] {
    override def handle(t: ActionEvent): Unit = {
      logoAnimation.play()
    }
  })


  logoGlow.setLevel(.9)

  smallLogo.setEffect(logoGlow)

  if (promo.enabled) {
    //A Media Player creates a player for a specific media
    val f = new JFile(promo.media)
    println(f.toURI.toString)
    val media: Media = new Media(f.toURI.toString)
    val mediaPlayer = new MediaPlayer(media)
    mediaPlayer.setCycleCount(-1)
    mediaPlayer.setMute(true)
    promoMediaView.setMediaPlayer(mediaPlayer)
    mediaPlayer.play()
  }

  (display.root
    .handle(KeyEvent.KEY_RELEASED)
    .map(_.getCode)
    .filter(key => keysMap.contains(key))
    .transform(Option.empty[String]) {
      case (KeyCode.ENTER, _) if menuOn && editOn => lList(mIndex).requestFocus(); editOn = !editOn; (None, None)
      case (KeyCode.ENTER, _) if menuOn => tList(mIndex).requestFocus(); editOn = !editOn; (None, None)
      case (KeyCode.ENTER, result) => gameBox.requestFocus(); (result, None)
      case (KeyCode.NUMPAD2, _) if menuOn && !editOn => focusNext(); (None, None)
      case (KeyCode.NUMPAD8, _) if menuOn && !editOn => focusBack(); (None, None)
      case (KeyCode.NUM_LOCK, _) if menuOn => menu.toBack(); gameBox.requestFocus(); menuOn = false; (None, None)
      case (KeyCode.NUM_LOCK, _) => menu.toFront(); focusSame(); menuOn = true; (None, None)
      case (KeyCode.DIVIDE, _) if promoOn => promoPane.toBack(); gameBox.requestFocus(); promoOn = false; (None, None)
      case (KeyCode.DIVIDE, _) => promoPane.toFront(); promoPane.requestFocus(); promoOn = true; (None, None)
      case (KeyCode.MULTIPLY, _) if infoOn => info.toBack(); gameBox.requestFocus(); infoOn = false; (None, None)
      case (KeyCode.MULTIPLY, _) => info.toFront(); info.requestFocus(); infoOn = true; (None, None)
      //      case (KeyCode.MULTIPLY, _) => println("Add pressed"); display.root.setScene( display.load(restartWindow)); (None, None)
      case (key, result) if result.isEmpty => (None, Some(keysMap(key)))
      case (key, result) if result.get eq keysMap(key) => (None, None)
      case (key, result) if result.get.contains(keysMap(key)) => (None, Some(result.get.replaceAll(keysMap(key), "")))
      case (key, result) => (None, Some((result.get + keysMap(key)).toCharArray.sorted.mkString))
    } unNone)
    .map(coupsMap.get)
    .filter(result => result.isDefined)
    .map(result => result.get)
    .foreach { result => {
      result match {
        case BeadRoadResult.EXIT => display.exit()
        case BeadRoadResult.UNDO => beadRoad.RemoveElement()
        case BeadRoadResult.CLEAR => beadRoad.Reset()
        case _ => {
          beadRoad.AddElement(result)
        }
      }
      saveDataToDisk()
    }
    }


  //Load the saved results
  data.results.foreach {
    result => beadRoad.AddElement(result)
  }

  footing.setText("Powered By Tykhe Gaming Pvt. Ltd.")

  display.root.setOnCloseRequest(_ => {
    display.exit()
  })
}
