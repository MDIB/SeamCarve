package com.cs51.seam;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class Resizer {
	
	private final SeamCarve carver;
	
	private final Integer targetHeight;
	private final Integer targetWidth;
	
	//base constructor
	public Resizer(BufferedImage img, int targetHeight, int targetWidth, double maxEnergy, 
			int HorzSeamNum, int VertSeamNum, EnergyMethod emeth, boolean SeamEnlarge) {
		
		//resize picture to proper proportions (if possible)
		carver = new SeamCarve(img, maxEnergy, VertSeamNum, HorzSeamNum, emeth, SeamEnlarge);
		this.targetHeight = targetHeight;
		this.targetWidth = targetWidth;
		//set to targetHeight / width
		
		
	}
	
	//base constructor
	public Resizer(BufferedImage img, double maxEnergy, int HorzSeamNum, int VertSeamNum, 
			EnergyMethod emeth,	boolean SeamEnlarge) {
		carver = new SeamCarve(img, maxEnergy, VertSeamNum, HorzSeamNum, emeth, SeamEnlarge);
		this.targetHeight = null;
		this.targetWidth = null;
	}
	
	//easy constructor : horizontal resize up to default seam proportion
	public Resizer (BufferedImage img) {
		this(img, SeamConstants.SEAM_DEFAULT_MAX_SCORE, 
				(int) SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION * img.getWidth(), 
				(int) SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION * img.getHeight(),
				SeamConstants.DEFAULT_ENERGY_METHOD, false);
	}
	
	
	public Resizer (BufferedImage img, double heightToWidthRatio, EnergyMethod emeth, boolean SeamEnlarge) {
		this(img, SeamConstants.SEAM_DEFAULT_MAX_SCORE, 
				horzSeamsToRemove(img.getHeight(), img.getWidth(), heightToWidthRatio, SeamEnlarge), 
				vertSeamsToRemove(img.getHeight(), img.getWidth(), heightToWidthRatio, SeamEnlarge), 
				SeamConstants.DEFAULT_ENERGY_METHOD, SeamEnlarge);
	}
	
	private static int vertSeamsToRemove (int imgHeight, int imgWidth, double newRatio, boolean SeamEnlarge) {
		// h = c * w
		if (SeamEnlarge)
			return Math.max(0, ((int) (newRatio * imgWidth)) - imgHeight);
		else
			return Math.max(0, imgHeight - ((int) (newRatio * imgWidth)));
	}

	private static int horzSeamsToRemove (int imgHeight, int imgWidth, double newRatio, boolean SeamEnlarge) {
		// w = h/c
		if (SeamEnlarge)
			return Math.max(0, ((int) (imgHeight / newRatio)) - imgWidth);
		else
			return Math.max(0, imgWidth - ((int) (imgHeight / newRatio)));
	}
	
	
	
	/**
	 * @return		returns the finished image
	 */
	public BufferedImage getFinalImage() {
		if (targetHeight == null)
			return carver.getImage();
		else
			return Utils.resizeImage(carver.getImage(), targetHeight, targetWidth);
	}
	
	
	//TODO anim pics
	/**
	 * @return		returns Files pointing to images animating the resize
	 */
	public File[] getAnimPics() {
		return carver.getAnimPics();
	}
	
	
	
	
	
	public File getAnimMovie() throws IOException {
		File file = Utils.getTempFile(); 
		Utils.createMovie(getAnimPics(), SeamConstants.DEFAULT_FPS, file);
		return file;
	}
	
	
	/**
	 * @return		returns the SeamCarve associated with the image
	 */
	public SeamCarve getSeamCarve() {
		return carver;
	}
}