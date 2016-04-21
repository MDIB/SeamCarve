package com.gdicristofaro.seamcarve


import com.gdicristofaro.seamcarve.jvm.Utils

object Resizer {
  	private def vertSeamsToRemove(imgHeight : Integer, 
  	    imgWidth : Integer, newRatio : Double, seamEnlarge : Boolean) : Integer =
  		// h = c * w
  	  seamEnlarge match {
  	    case true => Math.max(0, ((newRatio * imgWidth).toInt) - imgHeight)
        case false => Math.max(0, imgHeight - (newRatio * imgWidth).toInt)
  	  }
  	  
    private def horzSeamsToRemove(imgHeight : Integer, 
  	    imgWidth : Integer, newRatio : Double, seamEnlarge : Boolean) : Integer =
		//   w = h/c
  	  seamEnlarge match {
        case true => Math.max(0, ((imgHeight / newRatio).toInt) - imgWidth)
        case false => Math.max(0, imgWidth - ((imgHeight / newRatio).toInt))
      }
	
}

class Resizer(img: Image, targetHeight : Integer, targetWidth : Integer, 
    maxEnergy : Double, horzSeamNum : Integer, vertSeamNum : Integer, eMeth : EnergyMethod, seamEnlarge : Boolean) {
  
	//resize picture to proper proportions (if possible)
  val carver = new SeamCarve(img, maxEnergy, vertSeamNum, horzSeamNum, eMeth, seamEnlarge)
  
  
  def this(img : Image, maxEnergy : Double, horzSeamNum : Integer, vertSeamNum : Integer,
      eMeth : EnergyMethod, seamEnlarge : Boolean) = 
        this(img, null, null, maxEnergy, horzSeamNum, vertSeamNum, eMeth, seamEnlarge)
        
  def this(img : Image) = 
    this(img, SeamConstants.SEAM_DEFAULT_MAX_SCORE, 
				(SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION * img.width).toInt, 
				(SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION * img.height).toInt,
				SeamConstants.DEFAULT_ENERGY_METHOD, false)
  
  def this(img : Image, heightToWidthRatio : Double, eMeth : EnergyMethod, seamEnlarge : Boolean) = 
  		this(img, SeamConstants.SEAM_DEFAULT_MAX_SCORE, 
				Resizer.horzSeamsToRemove(img.height, img.width, heightToWidthRatio, seamEnlarge), 
				Resizer.vertSeamsToRemove(img.height, img.width, heightToWidthRatio, seamEnlarge), 
				SeamConstants.DEFAULT_ENERGY_METHOD, seamEnlarge)	
	
	/**
	 * @return		returns the finished image
	 */
	def getFinalImage : Image =
    targetHeight match {
      case null => carver.getImage
      case _ => ImageUtils.DEFAULT.resizeImage(carver.getImage, targetHeight, targetWidth)
    }
  
  
	
	/**
	 * @return		returns Files pointing to images animating the resize
	 */
	def getAnimPics = carver.getAnimPics;
	
	def getAnimMovie : Movie = ImageUtils.DEFAULT.createAnimMovie(getAnimPics)
	
	
	/**
	 * @return		returns the SeamCarve associated with the image
	 */
	def getSeamCarve : SeamCarve = carver
}