

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * this class has the purpose of allowing testing of results programmatically instead of using the CLInterface
 */
public class Tester {
	public static void main(String[] Args) throws IllegalArgumentException, IOException {
		/*String[] files = {"/Users/gregdicristofaro/Desktop/groupfolder/testimages/test1.png",
				"/Users/gregdicristofaro/Desktop/groupfolder/testimages/test2.png",
				"/Users/gregdicristofaro/Desktop/groupfolder/testimages/test3.png",
				"/Users/gregdicristofaro/Desktop/groupfolder/testimages/test4.png",
				"/Users/gregdicristofaro/Desktop/groupfolder/testimages/test5.png",
				"/Users/gregdicristofaro/Desktop/groupfolder/testimages/test6.png",
				"/Users/gregdicristofaro/Desktop/groupfolder/testimages/test7.png",
				"/Users/gregdicristofaro/Desktop/groupfolder/testimages/test8.png",
				"/Users/gregdicristofaro/Desktop/groupfolder/testimages/test9.png",
				"/Users/gregdicristofaro/Desktop/groupfolder/testimages/test10.png"
		};
		String outputDir = "/Users/gregdicristofaro/Desktop/groupfolder/testimages/results";
		int[] testNums = {0,5,10};
		
		for (int i = 0; i < files.length; i++) 
			testImg(new File(files[i]), testNums, testNums, new File(outputDir));*/
		
		//getEnergyMap("/Users/gregdicristofaro/Desktop/grazingcattle.jpg", "/Users/gregdicristofaro/Desktop/energy.png");
		
		/*removeLowEnergy("/Users/gregdicristofaro/Desktop/grazingcattle.jpg", 
				"/Users/gregdicristofaro/Desktop/grazingcattlelow.jpg", 100);*/
		File folder = new File("/Users/gregdicristofaro/Desktop/cowpictures/images");
		File[] files = folder.listFiles();
		
		for (File f : files) {
			System.out.println("getting " + f.getAbsolutePath());
			try {
				testImg(f, new int[]{0, Utils.getImage(f).getWidth()/5/*50*/}, new int[]{0, Utils.getImage(f).getHeight()/5/*50*/}, 
						new File("/Users/gregdicristofaro/Desktop/cowpictures/images/stuff"));
			} catch (Exception e) {System.out.println("error: " + e.getLocalizedMessage());}
		}
			
	}
	
	public static void testImg(File imgfile, int[] vertSeamNums, int[] horzSeamNums, File dir) throws IOException {
		BufferedImage img = Utils.getImage(imgfile);
		for (int a = 0; a < vertSeamNums.length; a++)
			for (int b = 0; b < horzSeamNums.length; b++) {
				Resizer resize = new Resizer(img, 1., vertSeamNums[a], horzSeamNums[b], SeamConstants.DEFAULT_ENERGY_METHOD, false);
				String str = imgfile.getName().substring(0, imgfile.getName().lastIndexOf("."));
				
				Utils.writeImage(resize.getFinalImage(), "png", dir.getAbsolutePath() + "/" + imgfile.getName() + "final-v" + vertSeamNums[a] + "-h" + horzSeamNums[b] + ".png");
				Utils.writeImage(resize.getSeamCarve().getSeamImage(), "png", dir.getAbsolutePath() + "/" + str + "seamImg-v" + vertSeamNums[a] + "-h" + horzSeamNums[b] + ".png");
			}
	}
	
	public static void getEnergyMap(String imgfile, String output) throws IOException {
		GetEnergy energy = new GetEnergy(Utils.getImage(imgfile), SeamConstants.DEFAULT_ENERGY_METHOD);
		Utils.writeImage(energy.getImage(), "png", output);
	}
	
	public static class colorEnergy {
		public final int color;
		public final double energy;
		public final int x;
		public final int y;
		
		public colorEnergy(int color, double energy, int x, int y) { this.color = color; this.energy = energy; this.x = x; this.y = y; }
	}
	
	public static class CompEnergy implements Comparator<colorEnergy> {
		@Override
		public int compare(colorEnergy o1, colorEnergy o2) {return Double.compare(o1.energy, o2.energy);}
	}
	
	public static class CompXCoord implements Comparator<colorEnergy> {
		@Override
		public int compare(colorEnergy o1, colorEnergy o2) {return Double.compare(o1.x, o2.x);}
	}
	
	public static void removeLowEnergy(String imgfile, String output, int pixelnumber) throws IllegalArgumentException, IOException {
		BufferedImage img = Utils.getImage(imgfile);
		EnergyMap emap = (new GetEnergy(img)).getEnergyMap();
		BufferedImage newimg = new BufferedImage(img.getWidth() - pixelnumber, img.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		PriorityQueue<colorEnergy> energyQ = new PriorityQueue<colorEnergy>(img.getWidth(), new CompEnergy());
		PriorityQueue<colorEnergy> xQ = new PriorityQueue<colorEnergy>(img.getWidth() - pixelnumber, new CompXCoord());
		
		for (int y = 0; y < img.getHeight(); y++) {
			energyQ.clear();
			xQ.clear();
			
			for (int x = 0; x < img.getWidth(); x++)
				energyQ.add(new colorEnergy(img.getRGB(x, y), emap.getEnergy(x, y), x, y));

			for (int i = 0; i < pixelnumber; i++)
				energyQ.poll();
			
			while (!energyQ.isEmpty()) {
				xQ.add(energyQ.poll());
			}
			
			int xCounter = 0;
			while (xCounter < (img.getWidth() - pixelnumber) && !xQ.isEmpty()) {
				newimg.setRGB(xCounter, y, xQ.poll().color);
				xCounter++;
			}
			
		}
			
		Utils.writeImage(newimg, "png", output);
	}
}
