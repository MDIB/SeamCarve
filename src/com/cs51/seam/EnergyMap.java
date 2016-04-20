package com.cs51.seam;

public class EnergyMap {
	private static final double MAX_ENERGY = 1.0;
	private static final double MIN_ENERGY = 0.0;
	
	//represented as [height][width]  [0][0] being top left corner
	private final double[][] energies;
	private final int width;
	private final int height;

	/**
	 * Constructor of EnergyMap
	 * @param energy	the energies of each pixel represented as [height][width]
	 * 
	 * @throws IllegalArgumentException	   throws exception for bad representation
	 */
	public EnergyMap(double[][] energy) throws IllegalArgumentException {
		//empty array won't cut it
		if (energy.length <= 0)
			throw new IllegalArgumentException("Energy representation is empty");

		for (int i = 0; i < energy.length; i++) {
			if (energy[i].length != energy[0].length)
				//jagged edges won't work
				throw new 
					IllegalArgumentException("Jagged edges for energy representation");
			
			//check to see if energy is too high or too low
			for (int e = 0; e < energy[i].length; e++) {
				if (energy[i][e] < MIN_ENERGY)
					throw new IllegalArgumentException("energy at " + e + 
						", " + i + " is less than MIN_ENERGY (" + MIN_ENERGY + ")");
						
				if (energy[i][e] > MAX_ENERGY)
					throw new IllegalArgumentException("energy at " + e + ", " + i + 
						" is greater than MAX_ENERGY (" + MIN_ENERGY + ")");		
			}	
		}				
		
		energies = energy;
		height = energy.length;
		width = energy[0].length;		
	}
	
	/** get the height of the image in pixels
	 * 
	 * @return		returns height
	 */
	public int getHeight() { return height; }
	
	
	/** get the width of the image in pixels
	 * 
	 * @return		returns width
	 */	
	public int getWidth() { return width; }
	
	
	/** returns the energy of a pixel with the given x, y coordinates in the image
	 * 
	 * @param x		x coordinate
	 * @param y		y coordinate
	 * @return		energy value
	 * 
	 * @throws IllegalArgumentException		thrown if x value or y value is out of 
	 * bounds
	 */
	public double getEnergy(int x, int y) throws IllegalArgumentException {
		if (x < 0 || x >= width )
			throw new IllegalArgumentException ("x value is out of bounds");

		if (y < 0 || y >= height )
			throw new IllegalArgumentException ("y value is out of bounds");
		
		return energies[y][x];
	}
}