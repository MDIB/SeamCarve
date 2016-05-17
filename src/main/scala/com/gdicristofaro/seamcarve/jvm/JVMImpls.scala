package com.gdicristofaro.seamcarve.jvm

import java.awt.image.BufferedImage
import com.gdicristofaro.seamcarve.core.Image
import com.gdicristofaro.seamcarve.core.Color
import com.gdicristofaro.seamcarve.core.ImagePointer
import javax.imageio.ImageIO
import com.gdicristofaro.seamcarve.core.ImageUtils
import java.awt.image.ImageObserver
import java.awt.AlphaComposite
import com.gdicristofaro.seamcarve.core.ImgPosition
import com.gdicristofaro.seamcarve.core.TopLeftPosition
import java.awt.RenderingHints
import com.gdicristofaro.seamcarve.core.SeamConstants
import java.io.File
import com.xuggle.mediatool.ToolFactory
import com.xuggle.xuggler.ICodec
import java.util.concurrent.TimeUnit




class JVMImage(val bufferedImage : BufferedImage) extends Image {
  def copy: Image = {
    val cm = bufferedImage.getColorModel
	  val newImg = new BufferedImage(cm, bufferedImage.copyData(null), cm.isAlphaPremultiplied, null)
    new JVMImage(newImg)
  }

  val height: Int = bufferedImage.getHeight

  val width: Int = bufferedImage.getWidth
  
  
  private def parse(combined : Int, shift : Int) : Int = {
    (combined >>> shift) & 0xFF
  }
    
  def getColor(x: Int, y: Int): Color = {
    val col = bufferedImage.getRGB(x, y)
    new Color(parse(col, 16), parse(col, 8), parse(col, 0))
  }

  
  private def shifted(num : Int, shift : Int) : Int = {
    (num & 0xFF) << shift
  }
  
  def setColor(x: Int, y: Int, pixel: Color) = {
    val rgb = shifted(0xFF,24) | shifted(pixel.red, 16) | shifted(pixel.green, 8) | shifted(pixel.blue, 0)
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
  private def getBufferedImg(width: Int, height: Int) = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
  
  def createImage(width: Int, height: Int): Image = {
    new JVMImage(getBufferedImg(width, height))
  }

  def createImagePointer(img: Image): ImagePointer = {
    new JVMImgPointer(img.asInstanceOf[JVMImage].bufferedImage)
  }
  
  def readImg(str : String) : JVMImage = new JVMImage(ImageIO.read(new File(str)))
  
  def writeImg(image : JVMImage, format : String, path : String) =
		ImageIO.write(image.bufferedImage, "png", new File(path))
				
  def generateFadeInImages(background: Image, foreground: Image, frames: Int): Array[ImagePointer] = {
		if (frames < 1)
			throw new IllegalArgumentException("frames amount needs to be greater than 0")
		
		if (background.width != foreground.width || background.height != foreground.height)
			throw new IllegalArgumentException("Images should have the same dimensions")
		
		
		val ptrs = new Array[ImagePointer](frames)
		
		for (i <- 0 until frames) {
		  val foregroundAmount = (i.toFloat) / (frames - 1)
		  val thisImg = getBufferedImg(background.width, background.height)
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

  def giveEdges(orig: Image, background: Color, newHeight: Int, newWidth: Int, position: ImgPosition): Image = {
		if (orig.height > newHeight || orig.width > newWidth)
			throw new IllegalArgumentException("dimensions of original image are greater than new image dimensions")
		
		
		val toReturn = getBufferedImg(newWidth, newHeight)
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

  def resizeImage(img: Image, width: Int, height: Int): Image = {
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
  
  def copyImg(img: Image): Image = {
		val w = img.width
		val h = img.height
		val buffimg = img.asInstanceOf[JVMImage].bufferedImage
		val dimg = new BufferedImage(w, h, buffimg.getType)
		val g = dimg.createGraphics
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
				RenderingHints.VALUE_INTERPOLATION_BILINEAR)
		g.drawImage(buffimg, 0, 0, w, h, null)
		g.dispose
		new JVMImage(dimg)
  }
  
  // based on: https://examples.javacodegeeks.com/desktop-java/xuggler/create-video-from-image-frames-with-xuggler/
	def outputVideo(imgs : Array[ImagePointer], fps : Int, filePath : String) {
    if (imgs.length == 0)
      return
    
    // the xuggle writer
		val writer = ToolFactory.makeWriter(filePath)

		val img0 = imgs(0).load.asInstanceOf[JVMImage].bufferedImage
		
		// set up the video stream
		writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, img0.getWidth, img0.getHeight)

		for (index <- 0 until imgs.length) {
		  // add image at right location
		  val img = imgs(index).load.asInstanceOf[JVMImage].bufferedImage
			writer.encodeVideo(0, img, ((1000.toDouble / fps) * index).toInt, TimeUnit.MILLISECONDS)
		}
    
		// tell the writer to close and write the trailer if  needed
  	writer.close()
  }
}



