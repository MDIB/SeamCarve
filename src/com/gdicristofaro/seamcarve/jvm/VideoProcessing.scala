package com.gdicristofaro.seamcarve.jvm

import com.xuggle.mediatool.ToolFactory
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.awt.Robot
import java.awt.Rectangle
import java.awt.AWTException
import com.xuggle.xuggler.ICodec
import java.util.concurrent.TimeUnit

object ScreenRecordingExample {
  private val FRAME_RATE = 50
	private val SECONDS_TO_RUN_FOR = 20
	private val outputFilename = "c:/mydesktop.mp4"
	private var screenBounds : java.awt.Dimension = null

	def main(args : Array[String]) {
		// let's make a IMediaWriter to write the file.
		val writer = ToolFactory.makeWriter(outputFilename)
		screenBounds = Toolkit.getDefaultToolkit.getScreenSize
		// We tell it we're going to add one video stream, with id 0,
		// at position 0, and that it will have a fixed frame rate of FRAME_RATE.
		writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, screenBounds.width/2, screenBounds.height/2)
		val startTime = System.nanoTime()
		for (index <- 0 until SECONDS_TO_RUN_FOR * FRAME_RATE) {
			// take the screen shot
			val screen = getDesktopScreenshot
			// convert to the right image type
			val bgrScreen = convertToType(screen, BufferedImage.TYPE_3BYTE_BGR)
			// encode the image to stream #0
			writer.encodeVideo(0, bgrScreen, System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
			// sleep for frame rate milliseconds
			try {
				Thread.sleep((1000 / FRAME_RATE).toLong)
			} 
			catch {
				// ignore  
				case e : InterruptedException => e.printStackTrace()
			}
		}
		// tell the writer to close and write the trailer if  needed
  	writer.close()
  }


  private def convertToType(sourceImage : BufferedImage, targetType : Integer) : BufferedImage = {
		val image =  (sourceImage.getType == targetType) match {
			// if the source image is already the target type, return the source image
		  case true => sourceImage
			// otherwise create a new image of the target type and draw the new image
		  case false => new BufferedImage(sourceImage.getWidth, sourceImage.getHeight, targetType)
		}

		image.getGraphics.drawImage(sourceImage, 0, 0, null)
		image
  }


  private def getDesktopScreenshot : BufferedImage = {
		try {
			val robot = new Robot()
			val captureSize = new Rectangle(screenBounds)
			return robot.createScreenCapture(captureSize)
		} 
		catch {
		  case e : AWTException => 
		  e.printStackTrace
		  null
		}
	}
}