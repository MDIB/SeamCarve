package com.cs51.seam;

public class SeamConstants {
	//maximum energy on average for seam
	public static final double SEAM_DEFAULT_MAX_SCORE = 1;
	
	//how much should be removed vertically or horizontally
	public static final double SEAM_DEFAULT_MAX_HORZ_PROPORTION = 0.1;
	public static final double SEAM_DEFAULT_MAX_VERT_PROPORTION = 0.1;
	
	//default energy method
	public static final EnergyMethod DEFAULT_ENERGY_METHOD = new EnergyMethodE1();
	
	//how large the original image can be
	public static final int MAX_ORIG_IMAGE_WIDTH = 1000;
	public static final int MAX_ORIG_IMAGE_HEIGHT = 1000;

	//how large the resulting image can be
	public static final int MAX_RESULT_IMAGE_WIDTH = 1000;
	public static final int MAX_RESULT_IMAGE_HEIGHT = 1000;
	
	
	/*dictates how much memory an image can take up of free memory 
	 (we have to leave room for the energy and seams) */
	public static final double IMAGE_MAX_MEM_PER = .1;
	
	//default image format
	public static final String DEFAULT_IMAGE_FORMAT = "png";

	//specifies the prefix for tmp files
	public static final String TMP_FILE_PREFIX = "SeamCarve";
	
	//specifies the default frames per second
	public static final int DEFAULT_FPS = 32;
}
