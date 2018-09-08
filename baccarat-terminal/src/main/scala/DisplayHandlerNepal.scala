
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


//The FXML Controller can be defined as simple scala class
@sfxml(additionalControls = List("customjavafx.scene.control", "customjavafx.scene.layout"))
class DisplayHandlerNepal
(
  val gameBox: VBox,
  val tableNumber: Label,
  val handBetMin: Label,
  val handBetMax: Label,
  val tieBetMin: Label,
  val tieBetMax: Label,
  val pairBetMin: Label,
  val pairBetMax: Label,
  val superBetMin: Label,
  val superBetMax: Label,
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
  val menu: BorderPane,
  val promoPane: Pane,
  val promoMediaView: MediaView,
  val footing: Label,
  val tName: TextField,
  val tHandBetMin: TextField,
  val tHandBetMax: TextField,
  val tTieBetMin: TextField,
  val tTieBetMax: TextField,
  val tPairBetMin: TextField,
  val tPairBetMax: TextField,
  val tSuperSixBetMin: TextField,
  val tSuperSixBetMax: TextField,
  val lName: Button,
  val lHandBetMin: Button,
  val lHandBetMax: Button,
  val lTieBetMin: Button,
  val lTieBetMax: Button,
  val lPairBetMin: Button,
  val lPairBetMax: Button,
  val lSuperSixBetMin: Button,
  val lSuperSixBetMax: Button,
  val info: BorderPane,
  val beadRoad: BeadRoadTilePane,
  val bigEyeRoad: BigEyeRoadTilePane,
  val bigEyeRoadDummy: BigEyeRoadDummyTilePane,
  val smallRoad: SmallRoadTilePane,
  val smallRoadDummy: SmallRoadDummyTilePane,
  val cockroachRoad: CockroachRoadTilePane,
  val cockroachRoadDummy: CockroachRoadDummyTilePane,
  val bigRoad: BigRoadTilePane
)(implicit display: fx.io.Display, writerHeader: Host[Header, Unit],writerData: Host[Data, Unit],
  data: Data, header: Header, keysMap: Map[KeyCode, String], coupsMap: Map[String, BeadRoadResult], promo: Promo,
restartWindow: Display.Window) {


  beadRoad.Initialize(8, 14)
  bigRoad.Initialize(6, 49)
  bigEyeRoad.Initialize(6, 38)
  bigEyeRoadDummy.Initialize(3, 19)
  smallRoad.Initialize(6, 38)
  smallRoadDummy.Initialize(3, 19)
  cockroachRoad.Initialize(12, 38)
  cockroachRoadDummy.Initialize(6, 19)

//  tableNumber.textProperty().bindBidirectional(tName.textProperty())
  handBetMin.textProperty().bindBidirectional(tHandBetMin.textProperty())
  handBetMax.textProperty().bindBidirectional(tHandBetMax.textProperty())
  tieBetMin.textProperty().bindBidirectional(tTieBetMin.textProperty())
  tieBetMax.textProperty().bindBidirectional(tTieBetMax.textProperty())
  pairBetMin.textProperty().bindBidirectional(tPairBetMin.textProperty())
  pairBetMax.textProperty().bindBidirectional(tPairBetMax.textProperty())
  superBetMin.textProperty().bindBidirectional(tSuperSixBetMin.textProperty())
  superBetMax.textProperty().bindBidirectional(tSuperSixBetMax.textProperty())

//  tableNumber.setText(header.name)
  handBetMin.setText(header.handBetMin)
  handBetMax.setText(header.handBetMax)
  tieBetMin.setText(header.tieBetMin)
  tieBetMax.setText(header.tieBetMax)
  pairBetMin.setText(header.pairBetMin)
  pairBetMax.setText(header.pairBetMax)
  superBetMin.setText(header.superBetMin)
  superBetMax.setText(header.superBetMax)


  val tList = Array(tHandBetMin, tHandBetMax, tTieBetMin, tTieBetMax, tPairBetMin, tPairBetMax, tSuperSixBetMin,tSuperSixBetMax)
  val lList = Array(lHandBetMin, lHandBetMax, lTieBetMin, lTieBetMax, lPairBetMin, lPairBetMax, lSuperSixBetMin,lSuperSixBetMax)
  var menuOn = false
  var infoOn = false
  var editOn = false
  var promoOn = false
  var mIndex: Int = 0

  def focusSame(): Unit = {
    lList(mIndex).requestFocus()
  }

  def focusBack(): Unit = {
    if (mIndex == 0) mIndex = 7
    else {
      mIndex = (mIndex - 1) % 8
    }
    lList(mIndex).requestFocus()
  }

  def focusNext(): Unit = {
    mIndex = (mIndex + 1) % 8
    lList(mIndex).requestFocus()
  }

  def saveMenuToDisk(): Unit = {
    val task = writerHeader.request(
      Header(
        handBetMin.getText,
        handBetMax.getText,
        tieBetMin.getText,
        tieBetMax.getText,
        pairBetMin.getText,
        pairBetMax.getText,
        superBetMin.getText,
        superBetMax.getText))
    task.run()
  }

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
          if (t2.longValue() > t1.longValue()) {
            bigRoad.AddElement(beadRoad)
          } else {
            bigRoad.RemoveElement(beadRoad)
          }
          new AudioClip(getClass.getResource(beadRoad.LastWinAudio()).toExternalForm).play()
          totalCount.setText(String.valueOf(t2.intValue()))
        } else {
          bigRoad.Reset()
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
      case (KeyCode.NUM_LOCK, _) if menuOn => menu.toBack(); gameBox.requestFocus();saveMenuToDisk(); menuOn = false; (None, None)
      case (KeyCode.NUM_LOCK, _) => menu.toFront(); focusSame(); menuOn = true; (None, None)
      case (KeyCode.DIVIDE, _) if promoOn => promoPane.toBack(); gameBox.requestFocus(); promoOn = false; (None, None)
      case (KeyCode.DIVIDE, _) => promoPane.toFront(); promoPane.requestFocus(); promoOn = true; (None, None)
      case (KeyCode.MULTIPLY, _) if infoOn => info.toBack(); gameBox.requestFocus(); infoOn = false; (None, None)
      case (KeyCode.MULTIPLY, _) => info.toFront(); info.requestFocus(); infoOn = true; (None, None)
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
    result => beadRoad.AddElement (result)
  }

  footing.setText("Powered By Tykhe Gaming Pvt. Ltd.")

  display.root.setOnCloseRequest(_ => {
    display.exit()
  })
}
