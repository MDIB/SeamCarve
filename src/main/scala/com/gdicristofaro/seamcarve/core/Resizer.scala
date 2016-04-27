package com.gdicristofaro.seamcarve.core

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

class Resizer(imgUtils : ImageUtils, img: Image, targetHeight : Integer, targetWidth : Integer, 
		maxEnergy : Double, horzSeamNum : Integer, vertSeamNum : Integer, seamEnlarge : Boolean) {

	//resize picture to proper proportions (if possible)
	val carver = new SeamCarve(imgUtils, img, maxEnergy, vertSeamNum, horzSeamNum, seamEnlarge)


			def this(imgUtils : ImageUtils, img : Image, maxEnergy : Double, horzSeamNum : Integer, vertSeamNum : Integer,
					seamEnlarge : Boolean) = 
					this(imgUtils, img, null, null, maxEnergy, horzSeamNum, vertSeamNum, seamEnlarge)

			def this(imgUtils : ImageUtils, img : Image) = 
			this(imgUtils, img, SeamConstants.SEAM_DEFAULT_MAX_SCORE, 
					(SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION * img.width).toInt, 
					(SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION * img.height).toInt,
					false)

			def this(imgUtils : ImageUtils, img : Image, heightToWidthRatio : Double, seamEnlarge : Boolean) = 
			this(imgUtils, img, SeamConstants.SEAM_DEFAULT_MAX_SCORE, 
					Resizer.horzSeamsToRemove(img.height, img.width, heightToWidthRatio, seamEnlarge), 
					Resizer.vertSeamsToRemove(img.height, img.width, heightToWidthRatio, seamEnlarge), 
					seamEnlarge)	

			/**
			 * @return		returns the finished image
			 */
			def getFinalImage : Image =
			targetHeight match {
			case null => carver.getImage
			case _ => imgUtils.resizeImage(carver.getImage, targetHeight, targetWidth)
			}



			/**
			 * @return		returns Files pointing to images animating the resize
			 */
			def getAnimPics = carver.getAnimPics;


			/**
			 * @return		returns the SeamCarve associated with the image
			 */
			def getSeamCarve : SeamCarve = carver
}