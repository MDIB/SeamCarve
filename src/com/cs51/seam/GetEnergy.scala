package com.cs51.seam

import java.awt.image.BufferedImage
import java.io.File


abstract class EnergyMethod
case class EnergyMethodE1 extends EnergyMethod
case class EnergyMethodHoG extends EnergyMethod


class GetEnergy(img: BufferedImage, emeth: EnergyMethod) {
  
  //TODO define caching mechanism to hold on to energymap / files once determined
  //TODO provide switch to use HoG as well
  
  def this(img: BufferedImage) = this(img, SeamConstants.DEFAULT_ENERGY_METHOD)
  
    
  /**
   * compares color of pixel to square with size of 2 * radius where the pixel is the center
   * 
   * @param pixelX		the pixel's x location
   * @param pixelY		the pixel's y location
   * @param radius		how big of a radius for the square
   * @param img			the img to use
   */
  def getEnergyAverage
  	(pixelX: Int, pixelY: Int, radius: Int, img: BufferedImage) : Double = {
    /*get center of region to sample (might not always be where pixel is located due to 
    radius going outside image borders)*/
    val centerX = Math.max(Math.min((img.getWidth() - (radius + 1)), pixelX), radius)
    val centerY = Math.max(Math.min((img.getHeight() - (radius + 1)), pixelY), radius)
	
	/*get the average red, blue, and green color of the area marked out by startX to endX 
    by startY to endY*/
	var red = 0;
	var blue = 0;
	var green = 0;
	
	for (x <- (centerX - radius) to (centerX + radius); 
			y <- (centerY - radius) to (centerY + radius)) {
		val curRGB = new Utils.RGB(img.getRGB(x, y))
		red += curRGB.red
		blue += curRGB.blue
		green += curRGB.green
	}
	
	val total = Math.pow((radius * 2 + 1), 2).toInt
	
	red = red / total
    blue = blue / total
	green = green / total
	
	//get the difference between this value and average values
	val thisRGB = new Utils.RGB(img.getRGB(pixelX, pixelY))
		
	val totalDif : Double = (Math.abs(thisRGB.red - red) + Math.abs(thisRGB.blue - blue) + 
	    Math.abs(thisRGB.green - green))
	
	//the power pushes things closer to 0 even closer to 0; 765 represents max difference
	Math.pow(totalDif / 765, 2)

  }
  
  
  
  
  /*adapted from http://stackoverflow.com/questions/2381908/
  	how-to-create-and-use-a-multi-dimensional-array-in-scala-2-8*/
  /**
   * creates energy map from buffered image
   * 
   * @param img		the bufferedimage
   * @return 		the energy map
   */
  def getEnergyMap() : EnergyMap = {
    val toReturn = Array.ofDim[Double](img.getHeight, img.getWidth)
    val imgheight = img.getHeight
    val imgwidth = img.getWidth
    
    for (x <- 0 to (imgwidth - 1); y <- 0 to (imgheight - 1)) {
      val rgbVal = new Utils.RGB(img.getRGB(x, y))

      toReturn(y)(x) = getEnergyAverage(x, y, 5, img)
    } 
    new EnergyMap(toReturn)
  }

  /**
   * sets color from black to white based on energy
   * 
   * @param num		the number to convert 
   * @return 		the color
   */
  def getEnergyColor(num : Double) : Int = {
    val colorVal = Math.min((num * 2550).toInt, 255)
    Utils.getIntFromRGB(colorVal, colorVal, colorVal);
  }
  
  
  /**
   * gets image based on energy
   * 
   * @return 	the image to return
   */
  def getImage() : BufferedImage = {
    val emap = getEnergyMap()
    val newImage = new BufferedImage(emap.getWidth(), emap.getHeight(), 
    	BufferedImage.TYPE_3BYTE_BGR)
    for (x <- 0 to (emap.getWidth() - 1); y <- 0 to (emap.getHeight() - 1)) {
      newImage.setRGB(x, y, getEnergyColor(emap.getEnergy(x, y)));
    }
    newImage
  }
  
/**
 * returns the Files that point to the animation for determining energy
 *  
 * @return		the files for animation
 */
  def getAnimPics() = {
		val energyImg = getImage
		val images = Utils.generateFadeInImages (img, energyImg, 80);
		val goback = Utils.generateFadeInImages (energyImg, img, 20);

		Utils.combineArrays(
		    Utils.generateLots(images(0), 20), 
		    images, 
		    Utils.generateLots(images(images.length -1), 20),
		    goback);
  }
}