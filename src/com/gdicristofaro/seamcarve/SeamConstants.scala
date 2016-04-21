package com.gdicristofaro.seamcarve

object SeamConstants {
  	//maximum energy on average for seam
	val SEAM_DEFAULT_MAX_SCORE = 1d
	
	//how much should be removed vertically or horizontally
	val SEAM_DEFAULT_MAX_HORZ_PROPORTION = 0.1
	val SEAM_DEFAULT_MAX_VERT_PROPORTION = 0.1
	
	//default energy method
	val DEFAULT_ENERGY_METHOD = new EnergyMethodE1()
	
	//how large the original image can be
	val MAX_ORIG_IMAGE_WIDTH = 1000
	val MAX_ORIG_IMAGE_HEIGHT = 1000

	//how large the resulting image can be
	val MAX_RESULT_IMAGE_WIDTH = 1000
	val MAX_RESULT_IMAGE_HEIGHT = 1000
	
	
	/*dictates how much memory an image can take up of free memory 
	 (we have to leave room for the energy and seams) */
	val IMAGE_MAX_MEM_PER = .1
	
	//default image format
	val DEFAULT_IMAGE_FORMAT = "png"

	//specifies the prefix for tmp files
	val TMP_FILE_PREFIX = "SeamCarve"
	
	//specifies the default frames per second
	val DEFAULT_FPS = 32
}