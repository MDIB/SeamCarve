package com.gdicristofaro.seamcarve

// values range from 0 - 255
class Color(val red : Integer, val green : Integer, val blue : Integer) {
  private def _colorCheck(col : Integer, name : String) {
    if (col < 0 || col > 255)
      throw new IllegalArgumentException(name + " must be [0,255] but was " + col)
  }
  
  _colorCheck(red, "red")
  _colorCheck(green, "green")
  _colorCheck(blue, "blue")
}

object Color {
  val RED = new Color(255,0,0)
  val BLACK = new Color(0,0,0)
}



trait Image {
  val width : Integer
  val height : Integer
  def getColor(x : Integer, y : Integer) : Color
  def setColor(x : Integer, y : Integer, pixel : Color)
  
  def copy : Image
}

// means of loading and unloading image files to save memory
trait ImagePointer {
  def load : Image
  def unload
}
 
abstract class ImgPosition
case class TopLeftPosition() extends ImgPosition
case class CenterPosition() extends ImgPosition

trait Movie {
  
}

trait ImageUtils {
  def createImage(width : Integer, height : Integer) : Image
  
  def generateFadeInImages(background : Image, foreground : Image, frames : Integer) : Array[ImagePointer]
  
  def resizeImage(img : Image, width : Integer, height : Integer) : Image
  
  def createImagePointer(img : Image) : ImagePointer
  
	//puts image in top left hand corner of new box
	def giveEdges(orig : Image, background : Color, newHeight : Integer, newWidth : Integer, position : ImgPosition) : Image
	
	def createManyImagePointers(img : Image, num : Integer) : Array[ImagePointer] = {
    val arr = new Array[ImagePointer](num)
    
    for(i <- 0 to (num - 1)) {
      arr(i) = createImagePointer(img)
    }
    
    arr
  }
  
  def createAnimMovie(imgs : Array[ImagePointer]) : Movie
}

object ImageUtils {
  	// initially set at runtime
	var DEFAULT : ImageUtils = null
}