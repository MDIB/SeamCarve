package com.gdicristofaro.seamcarve.jvm

import java.awt.image.BufferedImage
import com.gdicristofaro.seamcarve.Image
import com.gdicristofaro.seamcarve.Color
import com.gdicristofaro.seamcarve.ImagePointer
import javax.imageio.ImageIO
import com.gdicristofaro.seamcarve.ImageUtils
import com.gdicristofaro.seamcarve.Movie
import java.awt.image.ImageObserver
import java.awt.AlphaComposite
import com.gdicristofaro.seamcarve.ImgPosition
import com.gdicristofaro.seamcarve.TopLeftPosition
import java.awt.RenderingHints
import com.gdicristofaro.seamcarve.SeamConstants
import java.io.File




class JVMImage(val bufferedImage : BufferedImage) extends Image {
  def copy: Image = {
    val cm = bufferedImage.getColorModel
	  val newImg = new BufferedImage(cm, bufferedImage.copyData(null), cm.isAlphaPremultiplied, null)
    new JVMImage(newImg)
  }

  val height: Integer = bufferedImage.getHeight

  val width: Integer = bufferedImage.getWidth
  
  
  private def parse(combined : Integer, shift : Integer) : Integer = {
    (combined >>> shift) & 0xFF
  }
    
  def getColor(x: Integer, y: Integer): Color = {
    val col = bufferedImage.getRGB(x, y)
    new Color(parse(col, 4), parse(col, 2), parse(col, 0))
  }

  
  private def shifted(num : Integer, shift : Integer) : Integer = {
    (num & 0xFF) << shift
  }
  
  def setColor(x: Integer, y: Integer, pixel: Color) = {
    val rgb = shifted(0xFF, 6) | shifted(pixel.red, 4) | shifted(pixel.green, 2) | shifted(pixel.blue, 0)
    bufferedImage.setRGB(x, y, rgb)
  }
}

object JVMCommon {
   def getTmpFile = {
    val currentTime = System.currentTimeMillis
    val file = File.createTempFile(SeamConstants.TMP_FILE_PREFIX + currentTime, ".png")
    file.deleteOnExit
    file
  }
}

class JVMImgPointer(buffImg : BufferedImage) extends ImagePointer {
  

  
  val file = JVMCommon.getTmpFile
  ImageIO.write(buffImg, "png", file)

  var img : JVMImage = null
  
  
  def load: Image = {
    if (img == null)
      img = new JVMImage(ImageIO.read(file))
    
    img
  }

  def unload: Unit = {
    if (img != null) {
      ImageIO.write(img.bufferedImage, "png", file)
      img = null 
    }
  }
}


class JVMImageUtils extends ImageUtils {
  def createAnimMovie(imgs: Array[ImagePointer]): Movie = {
    ???
  }

  def createImage(width: Integer, height: Integer): Image = {
    new JVMImage(new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR))
  }

  def createImagePointer(img: Image): ImagePointer = {
    new JVMImgPointer(img.asInstanceOf[JVMImage].bufferedImage)
  }

				
  def generateFadeInImages(background: Image, foreground: Image, frames: Integer): Array[ImagePointer] = {
		if (frames < 1)
			throw new IllegalArgumentException("frames amount needs to be greater than 0")
		
		if (background.width != foreground.width || background.height != foreground.height)
			throw new IllegalArgumentException("Images should have the same dimensions")
		
		
		val ptrs = new Array[ImagePointer](frames)
		
		for (i <- 0 until frames) {
		  val foregroundAmount = (i.toFloat) / (frames - 1)
		  val thisImg = new BufferedImage(background.width, background.height, BufferedImage.TYPE_3BYTE_BGR)
		  val g = thisImg.createGraphics
		
		  val observer : ImageObserver = null
  		//draw background
  		g.drawImage(background.asInstanceOf[JVMImage].bufferedImage, 0, 0, null)
  		
  		//set composite
  		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, foregroundAmount))
  		g.drawImage(foreground.asInstanceOf[JVMImage].bufferedImage,0,0,null)
		  g.dispose
		  
		  ptrs(i) = new JVMImgPointer(thisImg) 
		}
		
		ptrs
  }

  def giveEdges(orig: Image, background: Color, newHeight: Integer, newWidth: Integer, position: ImgPosition): Image = {
		if (orig.height > newHeight || orig.width > newWidth)
			throw new IllegalArgumentException("dimensions of original image are greater than new image dimensions")
		
		
		val toReturn = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR)
		val g = toReturn.createGraphics()
		
		if (position == TopLeftPosition()) {
			g.drawImage(orig.asInstanceOf[JVMImage].bufferedImage,0,0,null)
		}
		else {
			val widthMargin = (newWidth - orig.width) / 2
			val heightMargin = (newHeight - orig.height) / 2
			g.drawImage(orig.asInstanceOf[JVMImage].bufferedImage,widthMargin,heightMargin,null)
			g.dispose
		}
		
		new JVMImage(toReturn)
  }

  def resizeImage(img: Image, width: Integer, height: Integer): Image = {
		val w = img.width
		val h = img.height
		val buffimg = img.asInstanceOf[JVMImage].bufferedImage
		val dimg = new BufferedImage(width, height, buffimg.getType)
		val g = dimg.createGraphics
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
				RenderingHints.VALUE_INTERPOLATION_BILINEAR)
		g.drawImage(buffimg, 0, 0, width, height, 0, 0, w, h, null)
		g.dispose
		new JVMImage(dimg)
  }
}



