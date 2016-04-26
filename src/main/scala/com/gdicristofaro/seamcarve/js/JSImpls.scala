package com.gdicristofaro.seamcarve.js

import com.gdicristofaro.seamcarve.core.Image
import com.gdicristofaro.seamcarve.core.Color
import com.gdicristofaro.seamcarve.core.ImagePointer
import com.gdicristofaro.seamcarve.core.ImageUtils
import com.gdicristofaro.seamcarve.core.ImgPosition
import com.gdicristofaro.seamcarve.core.TopLeftPosition
import com.gdicristofaro.seamcarve.core.SeamConstants
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.CanvasRenderingContext2D
import org.scalajs.dom.raw.ImageData
import org.scalajs.dom.document
import org.scalajs.dom.raw.HTMLCanvasElement


object JSCommon {
  def createCanvas(width : Integer, height : Integer) = {
    val newCanvas = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
    newCanvas.width = width
    newCanvas.height = height
    newCanvas
  }
  
  def getCtx(canvas : HTMLCanvasElement) = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
}

class JSImage(val canvas : Canvas) extends Image {
  private val ctx = JSCommon.getCtx(canvas)
  private val imgData = ctx.getImageData(0, 0, canvas.width, canvas.height)

  def copy: Image = {
    val newCanvas = JSCommon.createCanvas(canvas.width, canvas.height)
    val newCtx = JSCommon.getCtx(newCanvas)
    newCtx.drawImage(canvas, 0, 0)
    new JSImage(newCanvas)
  }

  val height: Integer = canvas.height

  val width: Integer = canvas.width
    
  def getColor(x: Integer, y: Integer): Color = {
    val pixel = ctx.getImageData(x.toDouble, y.toDouble, 1, 1).data
    new Color(pixel(0), pixel(1), pixel(2))
  }

  
  def setColor(x: Integer, y: Integer, pixel: Color) = {
    // img data is an array of values (0 - 255) where every 4 values represents 1 pixel, row 2 begins at 4 * width
    val offset = ((y * (canvas.width * 4)) + (x*4))
    
    imgData.data(offset) = pixel.red
    imgData.data(offset + 1) = pixel.green
    imgData.data(offset + 2) = pixel.blue
    imgData.data(offset + 3) = 255
    
    ctx.putImageData(imgData, 0, 0)
  }
}


// don't do anything to load or unload in DOM
class JSImgPointer(image : JSImage) extends ImagePointer {
  def load: Image = image
  def unload: Unit = ()
}


class JSImageUtils extends ImageUtils {
  def createImage(width: Integer, height: Integer): Image = new JSImage(JSCommon.createCanvas(width, height))

  def createImagePointer(img: Image): ImagePointer = new JSImgPointer(img.asInstanceOf[JSImage])


				
  def generateFadeInImages(background: Image, foreground: Image, frames: Integer): Array[ImagePointer] = {
		if (frames < 1)
			throw new IllegalArgumentException("frames amount needs to be greater than 0")
		
		if (background.width != foreground.width || background.height != foreground.height)
			throw new IllegalArgumentException("Images should have the same dimensions")
		
		
		val ptrs = new Array[ImagePointer](frames)
		
		for (i <- 0 until frames) {
		  val foregroundAmount = (i.toFloat) / (frames - 1)
		  val thisCanvas = JSCommon.createCanvas(background.width, background.height)
		  val ctx = JSCommon.getCtx(thisCanvas)
		  
  		//draw background
		  ctx.globalAlpha = 1
		  ctx.drawImage(background.asInstanceOf[JSImage].canvas, 0, 0)
		  
		  //draw foreground
		  ctx.globalAlpha = foregroundAmount
		  ctx.drawImage(foreground.asInstanceOf[JSImage].canvas, 0, 0)
		  
		  ptrs(i) = new JSImgPointer(new JSImage(thisCanvas)) 
		}
		
		ptrs
  }

  def giveEdges(orig: Image, background: Color, newHeight: Integer, newWidth: Integer, position: ImgPosition): Image = {
		if (orig.height > newHeight || orig.width > newWidth)
			throw new IllegalArgumentException("dimensions of original image are greater than new image dimensions")
		
		
		val toReturn = JSCommon.createCanvas(newWidth, newHeight)
		val ctx = JSCommon.getCtx(toReturn)
		ctx.fillStyle = "rgb(" + background.red + ", " + background.green + ", " + background.blue + ")"
		ctx.fillRect(0, 0, newWidth.toDouble, newHeight.toDouble)
		
		if (position == TopLeftPosition()) {
			ctx.drawImage(orig.asInstanceOf[JSImage].canvas,0,0)
		}
		else {
			val widthMargin = (newWidth - orig.width) / 2
			val heightMargin = (newHeight - orig.height) / 2
			ctx.drawImage(orig.asInstanceOf[JSImage].canvas,widthMargin,heightMargin)
		}
		
		new JSImage(toReturn)
  }

  def resizeImage(img: Image, width: Integer, height: Integer): Image = {
		val w = img.width
		val h = img.height
		val canvas = img.asInstanceOf[JSImage].canvas
		val newCanvas = JSCommon.createCanvas(width, height)
		val ctx = JSCommon.getCtx(newCanvas)
		ctx.drawImage(canvas, 0, 0, width.toDouble, height.toDouble)
		new JSImage(newCanvas)
  }
}



