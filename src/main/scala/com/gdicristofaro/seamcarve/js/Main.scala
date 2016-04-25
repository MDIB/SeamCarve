package com.gdicristofaro.seamcarve.js

import com.gdicristofaro.seamcarve.core.Resizer
import com.gdicristofaro.seamcarve.core.Image
import org.scalajs.dom.html
import com.gdicristofaro.seamcarve.core.EnergyMethodE1
import com.gdicristofaro.seamcarve.core.Movie
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js
import scala.scalajs.js.annotation.ScalaJSDefined


object ResizerUtils {
  val imgUtils = new JSImageUtils
  val energyMethod = new EnergyMethodE1
}


object EntryPoint extends js.JSApp {
  def main() {
     println("hello")
  }
}


@JSExport("SeamCarverFactory")
object Main {
  
  @JSExport
  def getCarver(image: html.Canvas, targetHeight : Integer, targetWidth : Integer, 
    maxEnergy : Double, horzSeamNum : Integer, vertSeamNum : Integer, seamEnlarge : Boolean) =
      new Main(new Resizer(ResizerUtils.imgUtils, new JSImage(image), targetHeight, targetWidth, 
        maxEnergy, horzSeamNum, vertSeamNum, ResizerUtils.energyMethod, seamEnlarge))

  @JSExport
  def getCarver(canvas : html.Canvas, maxEnergy : Double, horzSeamNum : Integer, 
      vertSeamNum : Integer, seamEnlarge : Boolean) =
        new Main(new Resizer(ResizerUtils.imgUtils, new JSImage(canvas), maxEnergy, 
          horzSeamNum, vertSeamNum, ResizerUtils.energyMethod, seamEnlarge))
  
  @JSExport
  def getCarver(canvas : html.Canvas) = {
    new Main(new Resizer(ResizerUtils.imgUtils, new JSImage(canvas))) 
  }
  
  @JSExport
  def getCarver(canvas : html.Canvas, heightToWidthRatio : Double, seamEnlarge : Boolean) =
    new Main(new Resizer(
      ResizerUtils.imgUtils, new JSImage(canvas), heightToWidthRatio, ResizerUtils.energyMethod, seamEnlarge))

}

@JSExport("SeamCarver")
class Main(resizer : Resizer) {
    
  @JSExport
	def getFinalImage = resizer.getFinalImage.asInstanceOf[JSImage].canvas
  
	@JSExport
	def getEnergyImage = resizer.getSeamCarve.getEnergyGetter.getImage.asInstanceOf[JSImage].canvas
  
	
	/**
	 * @return		returns Files pointing to images animating the resize
	 */
	//def getAnimPics = resizer.getAnimPics.map { imgPtr => imgPtr.load.asInstanceOf[JSImage].canvas };
	
	//def getAnimMovie : Movie = resizer.getAnimMovie
}