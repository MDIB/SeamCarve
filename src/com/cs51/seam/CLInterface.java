package com.cs51.seam;

import java.awt.image.BufferedImage;
import java.io.File;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

//command line parser from http://commons.apache.org/proper/commons-cli/usage.html
public class CLInterface {
	public static void main(String[] Args) {
		// define options
		Options options = new Options();

		options.addOption("help", false, "Displays this help message.");
		options.addOption("image", true, "The path of the image to be resized");
		options.addOption("output", true, "The path for the generated image.  Output is a png file.");
		options.addOption("video", true, "The path if you want a video of the process generated.  Video generated is an mp4.");
		options.addOption("heightToWidth", true, "Specifies a height to width ratio.  For instance," +
				" an argument of 4 would mean the generated image is four times taller than wide." + 
				"  If this argument is used, targetHeight, targetWidth, horzNum, and vertNum are ignored.");
		options.addOption("targetHeight", true, "Specifies the target height for the generated image.  " + 
				"If the image will not be resized to that point through seam carving, the image will be resized in the " + 
				"conventional manner to get the image to the height specified in this option.");
		options.addOption("targetWidth", true, "Specifies the target width for the generated image.  " + 
				"If the image will not be resized to that point through seam carving, the image will be resized in the " + 
				"conventional manner to get the image to the width specified in this option.");
		options.addOption("maxEnergy", true, "Specifies the maximum energy for a seam.  " +
				"The number represents the average energy each pixel in a seam will have.  "+ 
				"The number will be a number between 0 and 1.  The default is 1 as it represents the maximum possible energy.");
		options.addOption("horzNum", true, "The number of pixels that should be removed horizontally.");
		options.addOption("vertNum", true, "The number of pixels that should be removed vertically.");
		options.addOption("seamEnlarge", true, "This option specifies whether the image should be enlarged " +
				"through the process or not.  This option should either be 'true' or false.'  The default is false");

		
		
		
		//set up the parser for the arguments and the formatting for the help
		CommandLineParser parser = new BasicParser();
		HelpFormatter formatter = new HelpFormatter();
		
		try {
			CommandLine cmd = parser.parse( options, Args);

			//check if necessary arguments are present
			if (!cmd.hasOption("image") || !cmd.hasOption("output")) {
				System.out.println("Program requires argument for image and output:");
				System.out.println();
				formatter.printHelp( "Seam Carve", options );
				return;
			}
			
			Resizer resizer;

			//if heightToWidth is specified, take that resize argument
			if (cmd.hasOption("heightToWidth")) {
				resizer = new Resizer(
					Utils.getImage(cmd.getOptionValue("image")),
					Double.parseDouble(cmd.getOptionValue("heightToWidth")),
					SeamConstants.DEFAULT_ENERGY_METHOD,
					((cmd.hasOption("seamEnlarge")) ? Boolean.parseBoolean(cmd.getOptionValue("heightToWidth")) : false));
			}
			//if has targetHeight or targetWidth, take that route
			else if (cmd.hasOption("targetHeight") || cmd.hasOption("targetWidth")) {
				//must have both args for this to work
				if (!cmd.hasOption("targetHeight") || !cmd.hasOption("targetWidth")) {
					System.out.println("Sorry, but both targetHeight and targetWidth should be specified.");
					return;
				}				
				
				//create the resize object
				BufferedImage img = Utils.getImage(cmd.getOptionValue("image"));
				resizer = new Resizer(img,
						Integer.parseInt(cmd.getOptionValue("targetHeight")),
						Integer.parseInt(cmd.getOptionValue("targetWidth")),
						((cmd.hasOption("maxEnergy")) ? 
								Integer.parseInt(cmd.getOptionValue("maxEnergy")) : 
								SeamConstants.SEAM_DEFAULT_MAX_SCORE),
						((cmd.hasOption("horzNum")) ? 
								Integer.parseInt(cmd.getOptionValue("horzNum")) : 
								(int) ((SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION) * img.getWidth())), 
						((cmd.hasOption("horzNum")) ? 
								Integer.parseInt(cmd.getOptionValue("vertNum")) :
								(int) ((SeamConstants.SEAM_DEFAULT_MAX_VERT_PROPORTION) * img.getHeight())),
						SeamConstants.DEFAULT_ENERGY_METHOD,
						((cmd.hasOption("seamEnlarge")) ? 
								Boolean.parseBoolean(cmd.getOptionValue("heightToWidth")) : 
								false));
			}
			//take a basic approach as necessary
			else {
				BufferedImage img = Utils.getImage(cmd.getOptionValue("image"));				
				resizer = new Resizer(img,
						((cmd.hasOption("maxEnergy")) ? 
								Integer.parseInt(cmd.getOptionValue("maxEnergy")) : 
								SeamConstants.SEAM_DEFAULT_MAX_SCORE),
						((cmd.hasOption("horzNum")) ? 
								Integer.parseInt(cmd.getOptionValue("horzNum")) : 
								(int) ((SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION) * img.getWidth())), 
						((cmd.hasOption("horzNum")) ? 
								Integer.parseInt(cmd.getOptionValue("vertNum")) :
								(int) ((SeamConstants.SEAM_DEFAULT_MAX_VERT_PROPORTION) * img.getHeight())),
						SeamConstants.DEFAULT_ENERGY_METHOD,
						((cmd.hasOption("seamEnlarge")) ? 
								Boolean.parseBoolean(cmd.getOptionValue("heightToWidth")) : 
								false));				
			}
			
			Utils.writeImage(resizer.getFinalImage(), "png", cmd.getOptionValue("output"));
			
			//burn video if asked for one
			if (cmd.hasOption("video")) {
				Utils.createMovie(resizer.getAnimPics(), SeamConstants.DEFAULT_FPS, new File(cmd.getOptionValue("video")));
			}

			
			
		//print help if there is an issue somewhere	
		} catch (Exception e) {
			System.out.println("Unexpected input:");
			System.out.println();
			formatter.printHelp( "Seam Carve", options );
		}
	}
}
