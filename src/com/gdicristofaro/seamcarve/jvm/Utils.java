package com.gdicristofaro.seamcarve.jvm;


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;

import org.monte.media.Format;
import org.monte.media.quicktime.QuickTimeWriter;

import com.gdicristofaro.seamcarve.SeamConstants;



public class Utils {

	//get all available write formats
	public static final ArrayList<String> IMAGE_FORMATS = 
			new ArrayList<String>(Arrays.asList(ImageIO.getWriterFormatNames()));
	
	
	//public static void main(String[] Args) throws IllegalArgumentException, IOException {
		//attempt to short out memory
		/*BufferedImage[] list = new BufferedImage[10000];
		
		for (int i = 0; i < list.length; i++)
			list[i] = getImage("/Users/gregdicristofaro/Pictures/background/landscape/02207_venividivenice_1280x800.jpeg");
		
		for (int i = 0; i < list.length; i++)
			System.out.println(list[i].getMinX());
		 */
	
		//test of reading and writing
		/*writeImage(getImage("/Users/gregdicristofaro/Pictures/background/landscape/02207_venividivenice_1280x800.jpeg"), 
				"BMP", "/Users/gregdicristofaro/Desktop/stuff.bmp");
		*/
		
		//test of video
		
		/*BufferedImage img1 = getImage("/Users/gregdicristofaro/Desktop/grazingcattle.jpg");
		GetEnergy eg = new GetEnergy(img1);
		
		//createAnimatedGif(images, 32, new File("/Users/gregdicristofaro/Desktop/merged.gif"));
		createMovie(eg.getAnimPics(), 32, new File("/Users/gregdicristofaro/Desktop/merged.mp4"));
		*/
		
	//}
	
	
	
	
	/**
	 * gets an image from a path
	 * 
	 * @param path						the string of the path
	 * @return							the BufferedImage
	 * 
	 * @throws IOException 				thrown when there is a problem reading the file	
	 * @throws IllegalArgumentException	thrown when image would take up too much memory
	 */
	public static BufferedImage getImage (String path) 
			throws IOException, IllegalArgumentException {
		return getImage(new File(path));
	}
	
	
	/**
	 * gets an image from file
	 * 
	 * @param file						the file
	 * @return							the BufferedImage
	 * 
	 * @throws IOException 				thrown when there is a problem reading the file	
	 * @throws IllegalArgumentException	thrown when image would take up too much memory
	 */
	public static BufferedImage getImage(File file) throws IOException {
		//check the size of file
		double filesize = file.length();

		/*taken from http://stackoverflow.com/questions/5512378/
		how-to-get-ram-size-and-size-of-hard-disk-using-java*/
		double freeMemory = Runtime.getRuntime().freeMemory();
		
		if (filesize / freeMemory > SeamConstants.IMAGE_MAX_MEM_PER())
			throw new IllegalArgumentException("This image has size of " 
					+ filesize + " which will take up too much memory.");
		
		
		//if we are good to go, read it in
		return ImageIO.read(file);
	}
	
	
	
	/**
	 * gets an image from input stream
	 * 
	 * @param istream					input stream to convert to image
	 * @return							the BufferedImage
	 * 
	 * @throws IOException 				thrown when there is a problem reading the file	
	 * @throws IllegalArgumentException	thrown when image would take up too much memory
	 */
	public static BufferedImage getImage (InputStream istream) 
			throws IOException, IllegalArgumentException {
		//if we are good to go, read it in
		BufferedImage img = ImageIO.read(istream);
		
		/*taken from http://stackoverflow.com/questions/5512378/
		 	how-to-get-ram-size-and-size-of-hard-disk-using-java */
		double freeMemory = Runtime.getRuntime().freeMemory();
		
		/*using http://stackoverflow.com/questions/8351155/
		   check-how-much-memory-bufferedimage-in-java-uses */
		DataBuffer buff = img.getRaster().getDataBuffer();
		double filesize = buff.getSize() * DataBuffer.getDataTypeSize(buff.getDataType()) / 8;
		
		if (filesize / freeMemory > SeamConstants.IMAGE_MAX_MEM_PER())
			throw new IllegalArgumentException("This image has size of " 
					+ filesize + " which will take up too much memory.");
		
		return img;
	}

	
	/** Writes bufferedimage to disk
	 * 
	 * @param img							the image to be saved
	 * @param type							the type of Image (look to FORMATS if problem)
	 * @param path							the String as a path
	 * @return								returns true if successful
	 * 
	 * @throws IOException					thrown when problem writing
	 * @throws IllegalArgumentException		thrown when type is unknown to ImageIO
	 */
	public static boolean writeImage (BufferedImage img, String type, String path) 
			throws IOException, IllegalArgumentException {
		if (!IMAGE_FORMATS.contains(type))
			throw new IllegalArgumentException("Image Type of " + type + 
					" is not known to ImageIO");
		
		return ImageIO.write(img, type, new File(path));	
	}
	
	
	/** Writes bufferedimage to stream
	 * 
	 * @param img							the image to be written
	 * @param type							the type of Image (look to FORMATS if problem)
	 * @param ostream						the stream to write to
	 * @return								returns true if successful
	 * 
	 * @throws IOException					thrown when problem writing
	 * @throws IllegalArgumentException		thrown when type is unknown to ImageIO
	 */	
	public static boolean writeImage (BufferedImage img, String type, OutputStream ostream) 
			throws IOException {
		if (!IMAGE_FORMATS.contains(type))
			throw new IllegalArgumentException("Image Type of " + type + 
					" is not known to ImageIO");
		
		return ImageIO.write(img, type, ostream);
	}
	
	/**
	 * Resizes the image
	 * 
	 * @param img			the image to be resized
	 * @param newHeight		the new height
	 * @param newWidth		the new width
	 * @return				the resized bufferedimage
	 */
	public static BufferedImage resizeImage (BufferedImage img, int newHeight, int newWidth) {
		//TODO this will get the job done, but there is probably a better way to do this
		
		//taken from http://www.javalobby.org/articles/ultimate-image/
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage dimg = new BufferedImage(newWidth, newHeight, img.getType());
		Graphics2D g = dimg.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(img, 0, 0, newWidth, newHeight, 0, 0, w, h, null);
		g.dispose();
		return dimg;
	}
	
	
	/**
	 * generates a pointer to a temporary file
	 * 
	 * @return				temporary file that will be deleted on exit
	 * @throws IOException
	 */
	public static File getTempFile() throws IOException {
		/*from http://stackoverflow.com/questions/4863658/
		how-to-get-system-time-in-java-without-creating-a-new-date*/
		long currentTime = System.currentTimeMillis();
		File file = File.createTempFile(SeamConstants.TMP_FILE_PREFIX() + currentTime, ".png");
		file.deleteOnExit();
		return file;
	}
	
	/**
	 * writes an image to a temporary file and returns the file
	 * 
	 * @param img				the image
	 * @return					the file
	 * 
	 * @throws IOException		thrown if error writing
	 */
	public static File imageToTempFile(BufferedImage img) throws IOException {
		File file = getTempFile();
		ImageIO.write(img, "png", file);
		return file;
	}
	
	/**
	 * get RGB int value from RGB values
	 * 
	 * @param red		red value
	 * @param green		green value
	 * @param blue		blue value
	 * 
	 * @return			int value
	 */
	public static int getIntFromRGB(int red, int green, int blue) {
		if (red > 255 || red < 0 || green > 255 || green < 0 || blue > 255 || blue < 0)
			throw new IllegalArgumentException("rgb value out of bounds  red: " + red + 
					", green: " + green + " , blue: " + blue);
		
		return (red * 256 * 256 + green * 256 + blue);
	}
	
	
	/**
	 * Class that gives RGB value breakdown from an int value
	 */
	public static class RGB {
		public final int red;
		public final int green;
		public final int blue;
		
		
		public RGB(int val) {
			// taken from here: http://stackoverflow.com/questions/2183240/java-integer-to-byte-array
			byte[] bytes = ByteBuffer.allocate(4).putInt(val).array();
			
			/*taken from http://stackoverflow.com/questions/7401550/
			how-to-convert-int-to-unsigned-byte-and-back*/
			red = bytes[1] & 0xFF;
			green = bytes[2] & 0xFF;
			blue = bytes[3] & 0xFF;
			
			/*//for testing purposes
			if (red > 255 || red < 0 || green > 255 || green < 0 || blue > 255 || blue < 0)
				System.out.println("red is: " + red + " green is: " + green + " blue is: " + blue + " and something else: " + bytes[0]);
			*/
		}
		
	}
	
	/**
	 * generates a composite image where foreground is transparency to foreGroundAmount
	 * 
	 * @param background		background image
	 * @param foreground		foreground image to be made transparency
	 * @param foregroundAmount	number between 0 -1 for amount of transparency
	 * @return					generated image
	 */
	public static BufferedImage getCompositeImage 
			(BufferedImage background, BufferedImage foreground, double foregroundAmount) {
		if (background.getWidth() != foreground.getWidth() 
				|| background.getHeight() != foreground.getHeight())
			throw new IllegalArgumentException("Images should have the same dimensions");
		
		if (foregroundAmount < 0 || foregroundAmount > 1)
			throw new 
				IllegalArgumentException("foregroundAmount should be a number between 0 and 1");

		//taken from http://blg.trrrm.com/?tag=alphacomposite
		BufferedImage toReturn = 
				new BufferedImage(background.getWidth(), background.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = toReturn.createGraphics();
		
		//draw background
		g.drawImage(background,0,0,null);
		
		//set composite
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, (float) foregroundAmount));
		g.drawImage(foreground,0,0,null);
		
		return toReturn;
	}
	
	
	/**
	 * generates images for fade in from background to foreground 
	 * 
	 * @param background		background image for image to start at
	 * @param foreground		image to fade in
	 * @param frames			how many frames to take to fade in
	 * @return					generate File Array that points to the images generated
	 * @throws IOException 
	 */
	public static File[] generateFadeInImages (BufferedImage background, BufferedImage foreground, int frames)
			throws IOException {
		if (frames < 1)
			throw new IllegalArgumentException("frames amount needs to be greater than 0");
		
		File[] toReturn = new File[frames];
		
		for (int i = 0; i < frames; i++)
			toReturn[i] = 
				imageToTempFile(
						getCompositeImage (background, foreground, ((double) i) / (frames - 1)));  
		
		return toReturn;
	}
	
	
	
	public static enum MergeDirection {Up, Down, Left, Right };
	
	/**
	 * generated BufferedImage by overlaying overlay on original from direction 
	 * MergeDirection by amount howMuch
	 * 
	 * @param original		original image
	 * @param overlay		image to be overlayed
	 * @param overlayFrom	direction to overlay from
	 * @param howMuch		number from 0 -1 for how much to overlay
	 * @return				the generated image
	 */
	public static BufferedImage overlaySidePortion(BufferedImage original, BufferedImage overlay, 
			MergeDirection overlayFrom, Double howMuch) {
		if (original.getWidth() != overlay.getWidth() || original.getHeight() != overlay.getHeight())
			throw new IllegalArgumentException("Images should have the same dimensions");
		
		if (howMuch < 0 || howMuch > 1)
			throw new 
				IllegalArgumentException("foregroundAmount should be a number between 0 and 1");
		
		BufferedImage toReturn = 
				new BufferedImage(original.getWidth(), original.getHeight(), 
						BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = toReturn.createGraphics();
		
		//draw original
		g.drawImage(original,0,0,null);	
		
		//for up, down, left, = 0 ;  right = image end - (image width * howMuch)  (-1 so we don't fall off the edge)
		int startX = (overlayFrom == MergeDirection.Right) ? 
				((int) ((toReturn.getWidth() - 1) * (1.0 - howMuch))) : 0;
		
		//for up, down, right = image end;  left = image width * howMuch
		int endX = (overlayFrom == MergeDirection.Left) ? 
				((int) ((toReturn.getWidth() - 1) * howMuch)) : (toReturn.getWidth() - 1);
		
		//for left, right, up = 0; down ...
		int startY = (overlayFrom == MergeDirection.Down) ? 
				((int) ((toReturn.getHeight() - 1) * (1.0 - howMuch))) : 0;
		
		//for left, right, down = image bottom
		int endY = (overlayFrom == MergeDirection.Up) ? 
				((int) ((toReturn.getHeight() - 1) * howMuch)) : (toReturn.getHeight() - 1);
		
		if ((endX - startX) > 0 && (endY - startY) > 0) {
			BufferedImage subImage = 
				overlay.getSubimage(startX, startY, (endX - startX), (endY - startY));
			
			//draw sub image
			g.drawImage(subImage, startX, startY, null);
		}
		
		return toReturn;
	}
	
	
	
	public static enum ImgPosition {TopLeft, Center} 
	
	//puts image in top left hand corner of new box
	public static BufferedImage giveEdges(BufferedImage orig, Color background, int newHeight, int newWidth, ImgPosition position) throws IllegalArgumentException {
		if (orig.getHeight() > newHeight || orig.getWidth() > newWidth)
			throw new IllegalArgumentException("dimensions of original image are greater than new image dimensions");
		
		
		BufferedImage toReturn = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = toReturn.createGraphics();
		
		if (position == ImgPosition.TopLeft)
			g.drawImage(orig,0,0,null);

		else {
			int widthMargin = (newWidth - orig.getWidth()) / 2;
			int heightMargin = (newHeight - orig.getHeight()) / 2;
			g.drawImage(orig,widthMargin,heightMargin,null);
		}
		
		return toReturn;
	}
	
	/**
	 * generates files associated with unwrapping in overlay
	 * 
	 * @param original		original image
	 * @param overlay		original overlay image
	 * @param direction		direction that overlay comes from
	 * @param frames		number of frames
	 * 
	 * @return				files associated with the animation 
	 * @throws IOException
	 */
	public static File[] generateSlideInImages (BufferedImage original, 
			BufferedImage overlay, MergeDirection direction, int frames) throws IOException {
		if (frames < 1)
			throw new IllegalArgumentException("frames amount needs to be greater than 0");
		
		File[] toReturn = new File[frames];
		
		for (int i = 0; i < frames; i++)
			toReturn[i] = imageToTempFile(
					overlaySidePortion (original, overlay, direction, 
							((double) i) / (frames - 1)));  
		
		return toReturn;
	}
	

	
	
	//primarily taken from http://elliot.kroo.net/software/java/GifSequenceWriter/
	
	/**
	 * creates an animated gif with images given to it
	 * 
	 * @param images 			an array of files pointing to images
	 * @param FPS				frames per second
	 * @param outputFile		the path to the animated gif
	 * @return					the file pointing to the animated gif
	 * @throws IOException
	 */
	public static File createAnimatedGif(File[] images, int FPS, File outputFile) 
				throws IOException {
	    BufferedImage firstImage = ImageIO.read(images[0]);
	    ImageOutputStream output = new FileImageOutputStream(outputFile);

	    // create a gif sequence with the type of the first image, 1 second
	    // between frames, which loops continuously
	    GifSequenceWriter writer = 
	      new GifSequenceWriter(output, BufferedImage.TYPE_3BYTE_BGR, FPS, true);

	    // write out the first image to our sequence
	    writer.writeToSequence(firstImage);
	    
	    //write rest
	    for(int i=1; i<images.length; i++) {
	      BufferedImage nextImage = ImageIO.read(images[i]);
	      writer.writeToSequence(nextImage);
	    }

	    writer.close();
	    output.close();
	    
	    return outputFile;
	}
	
	
	//taken from http://www.randelshofer.ch/monte/
	
	/**
	 * Create a movie from file array of images
	 *  
	 * @param imgFiles		the images to attach into a movie
	 * @param fps			frames per second
	 * @param outputFile	string for where to output the file
	 * @return				returns the file pointing to the file
	 * @throws IOException	
	 */
	   @SuppressWarnings("deprecation")
	public static File createMovie(File[] imgFiles, int fps, File movieFile) throws IOException {
		BufferedImage frame = ImageIO.read(imgFiles[0]);
		int width = frame.getWidth();
		int height = frame.getHeight();
		
	   Format videoFormat = QuickTimeWriter.VIDEO_JPEG;
	   boolean passThrough = false;
	   String streaming = "fastStartCompressed";
	   
	   
        File tmpFile = streaming.equals("none") ? movieFile : new File(movieFile.getPath() + ".tmp");
        Graphics2D g = null;
        BufferedImage img = null;
        BufferedImage prevImg = null;
        int[] data = null;
        int[] prevData = null;
        QuickTimeWriter qtOut = null;
        try {
            int timeScale = (int) (fps * 100.0);
            int duration = 100;

            qtOut = new QuickTimeWriter(tmpFile);
            int vt = qtOut.addVideoTrack(videoFormat, timeScale, width, height);
            qtOut.setSyncInterval(0, 30);

            if (!passThrough) {
                img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
                prevImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                prevData = ((DataBufferInt) prevImg.getRaster().getDataBuffer()).getData();
                g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            }
            int prevImgDuration = 0;
            for (int i = 0; i < imgFiles.length; i++) {
                File f = imgFiles[i];

                if (passThrough) {
                    qtOut.writeSample(vt, f, duration, true);
                } else {
                    BufferedImage fImg = ImageIO.read(f);
                    g.drawImage(fImg, 0, 0, width, height, null);
                    if (i != 0 && Arrays.equals(data, prevData)) {
                        prevImgDuration += duration;
                    } else {
                        if (prevImgDuration != 0) {
                            qtOut.write(vt, prevImg, prevImgDuration);
                        }
                        prevImgDuration = duration;
                        System.arraycopy(data, 0, prevData, 0, data.length);
                    }
                }
            }
            if (prevImgDuration != 0) {
                qtOut.write(vt, prevImg, prevImgDuration);
            }
            if (streaming.equals("fastStart")) {
                qtOut.toWebOptimizedMovie(movieFile, false);
                tmpFile.delete();
            } else if (streaming.equals("fastStartCompressed")) {
                qtOut.toWebOptimizedMovie(movieFile, true);
                tmpFile.delete();
            }
            qtOut.close();
            qtOut = null;
        } finally {
            if (g != null) {
                g.dispose();
            }
            if (img != null) {
                img.flush();
            }
            if (qtOut != null) {
                qtOut.close();
            }
        }
        return movieFile;
    }
	
	
	/**
	 * generates array of files from a given file
	 * 
	 * @param file		the file to be repeated
	 * @param number	how many times to repeat
	 * @return			the file array
	 */
	public static File[] generateLots(File file, int number) {
		File[] toReturn = new File[number];
		for (int i = 0; i < number; i++)
			toReturn[i] = file;
		
		return toReturn;
	}
	
	/**
	 * combines file arrays into one large file array
	 * 
	 * @param fileArrays		the file arrays to combine
	 * @return					the combined array
	 */
	public static File[] combineArrays(File[]...fileArrays) {
		int totalSize = 0;
		
		for (int i = 0; i < fileArrays.length; i++)
			totalSize += fileArrays[i].length;
		
		File[] toReturn = new File[totalSize];
		
		int counter = 0;
		
		for (int a = 0; a < fileArrays.length; a++)
			for (int b = 0; b < fileArrays[a].length; b++)
				toReturn[counter++] = fileArrays[a][b];

		return toReturn;
	}
	
	/**
     * Encodes the byte array into base64 string
     *
     * @param imageByteArray - byte array
     * @return String a {@link java.lang.String}
	 * @throws IOException 
     */
	/*
    public static String encodeImage(BufferedImage img, String type) throws IOException {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	writeImage(img, type, baos);
    	baos.flush();
    	byte[] imageByteArray = baos.toByteArray();
        return Base64.encode(imageByteArray);
        //encodeBase64URLSafeString(imageByteArray);
    }
	*/
	
   /** Used to write array of files in to response object. objects are converted into JSON objects
 * @param response
 * @param files
 * @param img
 * @throws ServletException
 * @throws IOException
 */
/*
public static void writeResponse (HttpServletResponse response,File[] files,BufferedImage img) throws ServletException, IOException
   {

		StringBuffer    jsonString = new StringBuffer();
		//JSONArray jsonArray  = new JSONArray();
		BufferedImage imgage;
		
		ServletOutputStream             stream    = null;
		try{
				//stream   = response.getOutputStream();
			
					for (int i = 0; i < files.length; i++) {
					//for (int i = 0; i < 3; i++) {
						imgage=getImage(files[i]);
						//jsonArray.add(encodeImage(getImage(files[i]),""));
						//jsonArray.add(encodeImage(img,"png"));
						jsonString.append(encodeImage(imgage,"png")).append(",");
					}

				response.setHeader    ("Cache-Control", "no-store");
				response.setHeader    ("Pragma", "no-cache");
				response.setDateHeader("Expires", 0);

				response.setContentType("application/json; charset=UTF-8");
				response.setContentLength(jsonString.length());
				response.getWriter().write(jsonString.toString());
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new ServletException(ioe.getMessage());
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	
   }
	*/
	
	
	

}