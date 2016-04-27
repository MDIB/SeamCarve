//documentation http://docs.scala-lang.org/style/scaladoc.html
package com.gdicristofaro.seamcarve.core


object DynamicProgramming {
	
	/**
	 * isLast returns true if s is at the end
	 * @param s		the seamNode to test
	 * @return		true if last
	 */
	private def isLast (graph: SeamMap, s: SeamNode, dir: SeamDirection) = {
	  graph.getSuccessors(s.thisItem, dir) match {
	    case Some(successors) =>
	      successors.center match {
		    case End() => throw new IllegalArgumentException("error with item at " + s.thisItem.x + ", " + s.thisItem.y)
		    case _ => false
		  }
	    case None => 
	      true
	  }
	}

	/**
	 * determines next min-value seam
	 * @param avgEnergy		specifies the maximum average energy for a seam
	 * @return				the SeamNode and Graph with seams removed (or none if none found)
	 */
	def getNextSeam(graph : SeamMap, avgEnergy : Double, dir: SeamDirection) : Option[(SeamMap,SeamNode,Int)] = {
		//maintain length of graph in val
		val length : Int = graph.getLength(dir)
	  
		//gets first row of nodes and transforms to seam buds
		def getFirstSeams : List[SeamNode] = {
		  (graph.getFirst(dir).map(node => new SeamNode(new Top, node, node.energy, dir)))
		}
		
		//returns which ever one is better
		def getBestSeam(s1: SeamNode, s2 :SeamNode) : SeamNode = {
		  if (s2.totalScore < s1.totalScore) s2
		  else s1
		}
		
		//transforms a previous seam and a node into a new seam
		def getSeam(nodeitem : NodeItem, parent: SeamNode) : SeamNode = {
		  nodeitem match {
		    case (node : Node) =>  new SeamNode(parent, node, node.energy + parent.totalScore, dir)
		    case _ => throw new IllegalArgumentException("node expected for nodeitem")
		  }
		}
		
		//use only if getting a node: gets a node from nodeitem or throws exception if not
		def getNode(nodeItem : NodeItem) : Node = {
		  nodeItem match {
		    case (n : Node) => n
		    case _ => throw new IllegalArgumentException("get node given an unacceptable argument of End")
		  }
		}
		
		//given a list of seams, return which ever one is the best
		def bestSeam(lst : List[SeamNode], maxScore:Double) : Option[SeamNode]  = {
		  lst.foldLeft[Option[SeamNode]](None)((prev, item) => 
		    prev match {
		      case None => 
		        if (item.totalScore <= maxScore) Some(item)
		        else None
		      case Some(s) => Some(getBestSeam(s, item))
		    })
		}
	  
		//TODO width must be greater than 3 pixels
		//identify seams for each consequent row
		def nextRow (prevRow : List[SeamNode], thisRow : List[SeamNode]) : List[SeamNode] = {
		  (prevRow, thisRow) match {
		    //first most column - if list taken is Nil, we are at very first.
		    case ((prevFirst::prevTl), Nil) => 
		      val succ = graph.getSuccessors(prevFirst.thisItem, dir).get
		      //seam first should be empty
		      val seamCent = getSeam(succ.center, prevFirst)
		      val seamLast = getSeam(succ.last, prevFirst)
		      nextRow(prevTl, seamCent::seamLast::Nil)
		    
		    //most instances (in the middle) - we need two items from previous to guarantee we are not on last
		    case ((prevItem::nextPrev::prevTl), (thisFirst::thisCent::Nil)) =>
		      val succ = graph.getSuccessors(prevItem.thisItem, dir).get
		      val newSeamFirst = getBestSeam(getSeam(succ.first, prevItem), thisFirst)
		      val newSeamCent = getBestSeam(getSeam(succ.center, prevItem), thisCent)
		      val newSeamLast = getSeam(succ.last, prevItem)
		      // left should be set by this point, pass it on to the next recurssion
		      newSeamFirst :: 
		    	  (nextRow(nextPrev::prevTl, newSeamCent::newSeamLast::Nil))
		    	  
		    //right most...
		    case ((prevLast::Nil), (thisFirst::thisCent::Nil)) =>
		      val succ = graph.getSuccessors(prevLast.thisItem, dir).get
		      val newSeamFirst = getBestSeam(getSeam(succ.first, prevLast), thisFirst)
		      val newSeamCent = getBestSeam(getSeam(succ.center, prevLast), thisCent)
		      newSeamFirst::newSeamCent::Nil
		      
		    case _ => throw new IllegalArgumentException("either empty or some invariant broken")
		  }
		}
		
		//recurses continuing to get seams until at last row and then returns the seams gathered
		def getLastSeams(thisRow : List[SeamNode]) : List[SeamNode] = {
		  thisRow match {
		    case front::tl =>
		      if (isLast(graph, front, dir)) thisRow
		      else getLastSeams(nextRow(thisRow, Nil))
		    case Nil => throw new IllegalArgumentException("the length of the list is empty")
		  }
		}
		
		
		bestSeam(getLastSeams(getFirstSeams), (avgEnergy * length)) match {
		  case None => None
		  case Some(seam) => 
		    val newGraph = graph.deleteNodes(seam.toList.reverse, dir)
		    Some(newGraph, seam, graph.getLength(dir))
		}
	}
}



//maxVertEnergy and maxHorzEnergy set to 1 when being ignored
//seamnum should be >= 0

/**
 * carves up the image and provides object for solution
 * @param imgUtils				the utils to be used for processing the image
 * @param img							the image to be carved
 * @param maxEnergy				the maximum allowable energy to remove
 * @param vertShrinkNum		how much to shrink vertically
 * @param horzShrinkNum		how much to shrink horizontally
 * @param emeth						the energy method to use
 * @param imageEnlarge		whether or not to enlarge the image using this process
 */
class SeamCarve (imgUtils : ImageUtils, img : Image, maxEnergy : Double, 
    vertShrinkNum: Int, horzShrinkNum: Int, imageEnlarge : Boolean) {
  
	/**
	 * default constructor for SeamCarve where all defaults are assumed
	 * @param img		the image to carve
	 */
	def this(imgUtils : ImageUtils, img : Image) = this(imgUtils, img, 1,
	    (SeamConstants.SEAM_DEFAULT_MAX_VERT_PROPORTION * img.height).toInt, 
	    (SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION * img.width).toInt, false)
	
	
	val eget = new EnergyRetriever(imgUtils, img)
	val emap = eget.getEnergyMap
	
	val originalGraph = new SeamMap(imgUtils, emap, img)
	
	// vals for the finished graph and the reverse order of Seams - 
	// if image enlarge, gets both horizontal and vertical seams
	val (finishedGraph, revOrderSeams, possVertSeams) : (SeamMap, List[SeamNode], List[SeamNode]) = 
	  if (imageEnlarge) {
		  val (_, horzSeams) = getSeams(originalGraph, vertShrinkNum, 0, maxEnergy, Nil)
	      val (_, vertSeams) = getSeams(originalGraph, 0, horzShrinkNum, maxEnergy, Nil)
	      (originalGraph, horzSeams, vertSeams)
	  }
	  else {
	      val (newGraph, seams) = getSeams(originalGraph, vertShrinkNum, horzShrinkNum, maxEnergy, Nil)
	      (newGraph, seams, Nil)
	  }
		  
	  
	  
	
	/**
	 * recursive function that gets seams until seams are removed or maxEnergy limit is hit
	 * @param graph			the current SeamMap representing pixels remaining
	 * @param vertShrink	the current amount to shrink vertically
	 * @param horzShrink	the current amount to shrink horizontally
	 * @param maxEnergy		the maximum energy for a seam
	 * @param prevSeams		the seams currently accrued at this point in recursion
	 * @return				the finished SeamMap and list of SeamNodes representing all seams to remove
	 */
	private def getSeams(graph : SeamMap, vertShrink : Int, horzShrink : Int, maxEnergy : Double, prevSeams : List[SeamNode]) : (SeamMap, List[SeamNode]) = {
	  println("vertical seams remaining: " + vertShrink + "  horizontal seams remaining: " + horzShrink)

		((vertShrink > 0), (horzShrink > 0)) match {
		  //still need at least one of each
		  case (true, true) => 
		    //get a vertical seam (shrinking horizontally) and a horizontal seam (shrinking vertically) and compare
		  	(DynamicProgramming.getNextSeam(graph, maxEnergy, VertSeam()), 
		  	    DynamicProgramming.getNextSeam(graph, maxEnergy, HorzSeam())) match {
		  	  case (Some((horGraph, horSeam, horLength)), 
		  	      Some((vertGraph, vertSeam, vertLength))) => 
		  	        if ((vertSeam.totalScore / vertLength) < (horSeam.totalScore / horLength))
		  	          getSeams(vertGraph, (vertShrink - 1), horzShrink, maxEnergy, vertSeam::prevSeams)
		  	        else
		  	          getSeams(horGraph, vertShrink, (horzShrink - 1), maxEnergy, horSeam::prevSeams)
  		  	  case (Some((horGraph, horSeam, horLength)), None) => 
  		  	    getSeams(horGraph, vertShrink, (horzShrink - 1), maxEnergy, horSeam::prevSeams)
		  	  case (None, Some((vertGraph, vertSeam, _))) => 
		  	    getSeams(vertGraph, (vertShrink - 1), horzShrink, maxEnergy, vertSeam::prevSeams)
		  	  case (None, None) => (graph, prevSeams)
		  	}
		  //need to shrink vertically
		  case (true, false) => 
		    //just get a horizontal seam since shrinking vertically
		  	DynamicProgramming.getNextSeam(graph, maxEnergy, HorzSeam()) match {
		  	  case Some((newGraph, seam, length)) => 
		  	    getSeams(newGraph, (vertShrink - 1), horzShrink, maxEnergy, seam::prevSeams)
		  	  case None => (graph, prevSeams)
		  	}
		  //need to shrink horizontally
		  case (false, true) =>
		    //just get a vertical seam since shrinking horizontally
		  	DynamicProgramming.getNextSeam(graph, maxEnergy, VertSeam()) match {
		  	  case Some((newGraph, seam, length)) => 
		  	    getSeams(newGraph, vertShrink, (horzShrink - 1), maxEnergy, seam::prevSeams)
		  	  case None => (graph, prevSeams)
		  	}
		  //all done
		  case (false, false) => (graph, prevSeams)  
		}
	}
	
	/**
	 * gets an image representing finished product
	 * @return		the finished image
	 */ 
	def getImage : Image = {
	  if (imageEnlarge) 
		  finishedGraph.getSeamInsertionImage(
		      possVertSeams.foldLeft[List[Node]](Nil)((prevList, seam) => List.concat(seam.toList, prevList)),
		      revOrderSeams.foldLeft[List[Node]](Nil)((prevList, seam) => List.concat(seam.toList, prevList)))
	  else
		  finishedGraph.getImage
	}
	

	
	def getAnimPics = {
      if (imageEnlarge) throw new IllegalArgumentException("currently imageEnlarge and animpics don't play nice")
	  
	  val (animpics, seampic) = 
	    revOrderSeams.reverse.foldLeft((Array[ImagePointer](), img.copy)) {
	        (arrAndImage, seam) => 
	        	val (curArray, curImage) = arrAndImage

		        //get image with seam drawn on it
		        val nextImage = drawSeamNodes(curImage.copy, seam, Color.RED)
		        val nextImgPtr = imgUtils.createImagePointer(nextImage)
		        (curArray :+ nextImgPtr, nextImage)		        
	}

	  val graphWithSeams = new SeamMap(imgUtils, emap, seampic)
	  
	  //show seams being drawn
	  val (_, seamRemoval) = revOrderSeams.reverse.foldLeft( (graphWithSeams, Array[ImagePointer]() ) ) {
	    (graphAndArray, seam) =>
	      val (oldGraph, theArray) =  graphAndArray
	      
	      val newGraph = oldGraph.deleteNodes(seam.toList.reverse, seam.direction)
	      
	      (newGraph,
	          theArray :+ 
	            imgUtils.createImagePointer(
	              imgUtils.giveEdges(
	                newGraph.getImage, Color.BLACK, img.height, img.width, CenterPosition())))
	  }

	  
	  println("combining arrays")
	  //combine with energy movie pictures

	  eget.getAnimPics ++ animpics ++ seamRemoval ++ 
	    imgUtils.createManyImagePointers(
        imgUtils.giveEdges(getImage, Color.BLACK, img.height, img.width, CenterPosition()), 60)
	}
	
	
	private def drawSeamNodes(img:Image, node:SeamNode, color:Color) : Image = {
	  img.setColor(node.thisItem.x, node.thisItem.y, color)
	  node.prev match {
	    case (prev : SeamNode) => drawSeamNodes(img, prev, color)
	    case _ => img
	  }
	}
	
	//TODO test
	def getSeamImage : Image = {
      if (imageEnlarge) throw new IllegalArgumentException("currently imageEnlarge and getSeamImage don't play nice")

	  revOrderSeams.foldLeft(img.copy) { (img, seam) => drawSeamNodes(img, seam, Color.RED) }
	}
	
	
	/**
	 * gets the GetEnergy object associated with the image
	 * @return		the GetEnergy object associated with an image
	 */	
	def getEnergyGetter : EnergyRetriever = { eget }
}