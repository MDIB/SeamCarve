package com.gdicristofaro.seamcarve

	/**
	 * Energy Map representing 
	 * @param energy	the energies of each pixel represented as [height][width]
	 * 
	 * @throws IllegalArgumentException	   throws exception for bad representation
	 */
class EnergyMap(energies : Array[Array[Double]]) {
	private val MAX_ENERGY = 1.0
	private val MIN_ENERGY = 0.0
	
	/**
	 * provides some invariant checking on the energies
	 * @param energies			the energies mapped as (height)(width) = energy
	 */
	private def validateEnergy(energies : Array[Array[Double]]) = {
	  if (energies.length == 0)
	    throw new IllegalArgumentException("Energy representation is empty")
	
	  for (i <- 0 to energies.length - 1) {
			//jagged edges won't work
			if (energies(i).length != energies(0).length)
				throw new IllegalArgumentException("Jagged edges for energy representation")
			
		  //check to see if energy is too high or too low
			for (e <- 0 to energies(i).length - 1) {
			  if (energies(i)(e) < MIN_ENERGY)
					throw new IllegalArgumentException("energy at " + e + 
						", " + i + " is less than MIN_ENERGY (" + MIN_ENERGY + ")")
						
				if (energies(i)(e) > MAX_ENERGY)
					throw new IllegalArgumentException("energy at " + e + ", " + i + 
						" is greater than MAX_ENERGY (" + MIN_ENERGY + ")")
			}
	  }
	}
	
	// run through invariants
	validateEnergy(energies)
	
	// set up the height and width
	val height = energies.length
	val width = energies(0).length
	
	
	/** returns the energy of a pixel with the given x, y coordinates in the image
	 * 
	 * @param x		x coordinate
	 * @param y		y coordinate
	 * @return		energy value
	 * 
	 * @throws IllegalArgumentException		thrown if x value or y value is out of 
	 * bounds
	 */
	def getEnergy(x : Integer, y : Integer) : Double = {
		if (x < 0 || x >= width )
			throw new IllegalArgumentException ("x value is out of bounds")

		if (y < 0 || y >= height )
			throw new IllegalArgumentException ("y value is out of bounds")
		
		return energies(y)(x)
	}
}