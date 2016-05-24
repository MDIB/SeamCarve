package com.gdicristofaro.seamcarve.js

import com.gdicristofaro.seamcarve.core.Resizer
import com.gdicristofaro.seamcarve.core.Image
import org.scalajs.dom.html
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined


object ResizerUtils {
  val imgUtils = new JSImageUtils
}


object EntryPoint extends js.JSApp {
  def main() {
     println("hello")
  }
}


@JSExport("SeamCarverFactory")
object Main {

  @JSExport
  def getCarver(canvas : html.Canvas, maxEnergy : Double, horzSeamNum : Integer, 
      vertSeamNum : Integer, seamRemoval : Boolean) =
        new Main(new Resizer(ResizerUtils.imgUtils, new JSImage(canvas), maxEnergy, 
          horzSeamNum, vertSeamNum, seamRemoval))
  
  @JSExport
  def getCarver(canvas : html.Canvas) = {
    new Main(new Resizer(ResizerUtils.imgUtils, new JSImage(canvas))) 
  }
  
  @JSExport
  def getCarver(canvas : html.Canvas, heightToWidthRatio : Double, seamRemoval : Boolean) =
    new Main(new Resizer(
      ResizerUtils.imgUtils, new JSImage(canvas), heightToWidthRatio, seamRemoval))

}

@JSExport("SeamCarver")
class Main(resizer : Resizer) {
    
  @JSExport
	def getFinalImage = resizer.getFinalImage.asInstanceOf[JSImage].canvas
  
	@JSExport
	def getEnergyImage = resizer.energyRetriever.getImage.asInstanceOf[JSImage].canvas

	/**
	 * @return		returns Files pointing to images animating the resize
	 */
	@JSExport
	def getAnimPics = {
    val jsArr = new js.Array[html.Canvas]
    resizer.getAnimPics.map { imgPtr => jsArr.push(imgPtr.load.asInstanceOf[JSImage].canvas) }
    jsArr
  }
}