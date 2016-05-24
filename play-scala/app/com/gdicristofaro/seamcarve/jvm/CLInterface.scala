package com.gdicristofaro.seamcarve.jvm

import com.gdicristofaro.seamcarve.core.Resizer
import com.gdicristofaro.seamcarve.core.SeamConstants

import java.awt.image.BufferedImage
import java.io.File

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import javax.imageio.ImageIO




object CLInterface {
	def main(args : Array[String]) {
		// define options
		var options = new Options()

		options.addOption("help", false, "Displays this help message.")
		options.addOption("image", true, "The path of the image to be resized")
		options.addOption("output", true, "The path for the generated image.  Output is a png file.")
		options.addOption("video", true, "The path if you want a video of the process generated.  Video generated is an mp4.")
		options.addOption("heightToWidth", true, "Specifies a height to width ratio.  For instance," +
				" an argument of 4 would mean the generated image is four times taller than wide." + 
				"  If this argument is used, targetHeight, targetWidth, horzNum, and vertNum are ignored.")
		options.addOption("maxEnergy", true, "Specifies the maximum energy for a seam.  " +
				"The number represents the average energy each pixel in a seam will have.  "+ 
				"The number will be a number between 0 and 1.  The default is 1 as it represents the maximum possible energy.")
		options.addOption("horzNum", true, "The number of pixels that should be removed horizontally.")
		options.addOption("vertNum", true, "The number of pixels that should be removed vertically.")
		options.addOption("seamEnlarge", true, "This option specifies whether the image should be enlarged " +
				"through the process or not.  This option should either be 'true' or false.'  The default is false")

		//set up the parser for the arguments and the formatting for the help
		val parser = new DefaultParser()
		val formatter = new HelpFormatter()

		try {
			val cmd = parser.parse( options, args)

			//check if necessary arguments are present
			if (!cmd.hasOption("image") || !cmd.hasOption("output")) {
				System.out.println("Program requires argument for image and output:")
				System.out.println()
				formatter.printHelp( "Seam Carve", options )
				return
			}

			val imgUtils = new JVMImageUtils
			val img = imgUtils.readImg(cmd.getOptionValue("image"))
			var resizer : Resizer = null

			//if heightToWidth is specified, take that resize argument
			if (cmd.hasOption("heightToWidth")) {
				resizer = new Resizer(imgUtils,
						img,
						cmd.getOptionValue("heightToWidth").toDouble,
						if (cmd.hasOption("seamEnlarge")) { cmd.getOptionValue("heightToWidth").toBoolean } else { false })
			}
			//take a basic approach as necessary
			else {
				resizer = new Resizer(imgUtils, 
				    img,
						if (cmd.hasOption("maxEnergy")) cmd.getOptionValue("maxEnergy").toInt
						  else SeamConstants.SEAM_DEFAULT_MAX_SCORE,
						if (cmd.hasOption("horzNum")) cmd.getOptionValue("horzNum").toInt
						  else (SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION * img.width).toInt, 
						if (cmd.hasOption("horzNum")) cmd.getOptionValue("vertNum").toInt
						  else (SeamConstants.SEAM_DEFAULT_MAX_VERT_PROPORTION * img.height).toInt,
						if (cmd.hasOption("seamEnlarge")) cmd.getOptionValue("heightToWidth").toBoolean
						  else false)				
			}

			imgUtils.writeImg(resizer.getFinalImage.asInstanceOf[JVMImage], "png", cmd.getOptionValue("output"))

			//burn video if asked for one
			if (cmd.hasOption("video")) {
			  imgUtils.outputVideo(resizer.getAnimPics, SeamConstants.DEFAULT_FPS, cmd.getOptionValue("video"))
			}



			//print help if there is an issue somewhere	
		} catch {
		  case _ : Exception =>
  			System.out.println("Unexpected input:")
  			System.out.println()
  			formatter.printHelp( "Seam Carve", options )
		}
	}
}