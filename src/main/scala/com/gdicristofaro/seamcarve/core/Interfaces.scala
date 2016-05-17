package com.gdicristofaro.seamcarve.core

// values range from 0 - 255
class Color(val red : Int, val green : Int, val blue : Int) {
  private def _colorCheck(col : Int, name : String) {
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
  val width : Int
  val height : Int
  def getColor(x : Int, y : Int) : Color
  def setColor(x : Int, y : Int, pixel : Color)
  
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


trait ImageUtils {
  def createImage(width : Int, height : Int) : Image
  
  // creates a copy of an image
  def copyImg(img : Image) : Image
  
  def generateFadeInImages(background : Image, foreground : Image, frames : Int) : Array[ImagePointer]
  
  def resizeImage(img : Image, width : Int, height : Int) : Image
  
  def createImagePointer(img : Image) : ImagePointer
  
	//puts image in top left hand corner of new box
	def giveEdges(orig : Image, background : Color, newHeight : Int, newWidth : Int, position : ImgPosition) : Image
	
	def createManyImagePointers(img : Image, num : Int) : Array[ImagePointer] = {
    val arr = new Array[ImagePointer](num)
    
    for(i <- 0 to (num - 1)) {
      arr(i) = createImagePointer(img)
    }
    
    arr
  }
}