
import java.util.ArrayList

import customjavafx.scene.control.BeadRoadResult
import fs2.io.fx.{Data, Header}
import javafx.beans.property.{ListProperty, SimpleListProperty, SimpleStringProperty, StringProperty}
import javafx.collections.FXCollections

class BaccaratViewModel {

  //Define Data Elements & Getter Methods
  private val tableId: StringProperty = new SimpleStringProperty("")
  private val handBetMin: StringProperty = new SimpleStringProperty("")
  private val handBetMax: StringProperty = new SimpleStringProperty("")
  private val tieBetMin: StringProperty = new SimpleStringProperty("")
  private val tieBetMax: StringProperty = new SimpleStringProperty("")
  private val pairBetMin: StringProperty = new SimpleStringProperty("")
  private val pairBetMax: StringProperty = new SimpleStringProperty("")
  private val superSixBetMin: StringProperty = new SimpleStringProperty("")
  private val superSixBetMax: StringProperty = new SimpleStringProperty("")
  private val beadRoadList: ListProperty[BeadRoadResult] = new SimpleListProperty[BeadRoadResult](FXCollections.observableList(new ArrayList[BeadRoadResult]))

  def tableIdProperty: StringProperty =  tableId

  def handBetMinProperty: StringProperty =  handBetMin

  def handBetMaxProperty: StringProperty =  handBetMax

  def tieBetMinProperty: StringProperty =  tieBetMin

  def tieBetMaxProperty: StringProperty =  tieBetMax

  def pairBetMinProperty: StringProperty =  pairBetMin

  def pairBetMaxProperty: StringProperty =  pairBetMax

  def superSixBetMinProperty: StringProperty =  superSixBetMin

  def superSixBetMaxProperty: StringProperty =  superSixBetMax

  def beadRoadListProperty: ListProperty[BeadRoadResult] =  beadRoadList

  def saveHeader() = {
//    println(Header(tableId.get,handBetMin.get,handBetMax.get,tieBetMin.get,tieBetMax.get,pairBetMin.get,pairBetMax.get,superSixBetMin.get,superSixBetMax.get))
  }

  def saveData() = {
//    println(Data(beadRoadList.get.toArray.map(x => x.asInstanceOf[BeadRoadResult])))
  }


}
