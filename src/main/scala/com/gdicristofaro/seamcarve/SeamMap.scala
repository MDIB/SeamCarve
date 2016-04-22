package com.gdicristofaro.seamcarve

import scala.collection.immutable.HashMap


//TODO invariant that image should be larger than 3 pixels

/**
 * the representation of a seam
 */
abstract class Seam

/**
 * a node in the seam
 * 
 * @param prev			the previous seam item (seam is kind of like a linked list)
 * @param thisItem		the actual Link for the current place of this seam
 * @param totalScore	the total score for this seam at this point
 * @param fromLast		the direction the seam traveled to get to the link from the previous
 */
case class SeamNode(val prev: Seam, val thisItem: Node, val totalScore: Double, val direction : SeamDirection) extends Seam {
  	/**
 	 * creates list of node link
 	 * @return		the list of links
 	 */
	def toList : List[Node] = {
			prev match {
				case Top() => thisItem::Nil
				case (prevSeam : SeamNode) => thisItem::(prevSeam.toList)
			}
	}
}

abstract class SeamDirection
case class VertSeam() extends SeamDirection
case class HorzSeam() extends SeamDirection


/**
 * the very first element in a seam, this is used for the prev in the very first seamnode
 */
case class Top() extends Seam








/** A Node representing a pixel
  *
  * @constructor creates a pixel node
  * @param x 		x location
  * @param y 		y location
  * @param energy 	the energy of pixel
  * @param color	int value for color 
  */
 abstract class NodeItem
 case class Node(val x: Int, val y: Int, val energy: Double, val color: Color) extends NodeItem {

  override def hashCode(): Int = { (SeamConstants.MAX_ORIG_IMAGE_WIDTH)*y + x}
  
  //TODO: this might be a bad idea...
  //derived from here: http://stackoverflow.com/questions/3793141/override-equals-method-of-pair
  override def equals(obj:Any) = {
    //derived from http://stackoverflow.com/questions/931463/scala-how-do-i-cast-a-variable
    obj match {
	    case other : Node =>  (this.x == other.x && this.y == other.y)
	  	case _ => false
    }
  }
  
  def copy : Node = { new Node(this.x, this.y, this.energy, this.color)}
}
 case class End() extends NodeItem

 
 
 /**
  * keeps track of directions with successors
  * @param first	left/up node that is a successor of node
  * @param center	center node that is a successor of node
  * @param last		right/down node that is a successor of node
  */
 class Successors(val first: NodeItem, val center: NodeItem, val last: NodeItem)
 
 /**
  * represents the edges that proceed from node (minimized to keep fixes to a minimum)
  * @param up		up node is directly up from current node in graph
  * @param right	right node is directly right of current node in graph
  * @param down		down node is directly down from current node in graph
  * @param left		left node is directly left of current node in graph
  */
 class Neighbors(val up: NodeItem, val right: NodeItem, val down: NodeItem, val left: NodeItem)
	

 /**
  * Object that contains the factory for brand new PixelGraph
  */
private object Factory {
	/**
	 * convert pixels to link nodes (only on initialization)
	 * @param emap		energy map
	 * @param img		original image
	 * 
	 * @return			returns items necessary for creating pixel graph
	 */
	def getNeighbors(emap : EnergyMap, img : Image) : (HashMap[Node, Neighbors], Node) = {
	  //get node links
	  
	  //TODO: provide check to make sure they are of similar size
		val nodes = Array.ofDim[Node](img.width, emap.height)
		for (x <- 0 to (emap.width - 1); y <- 0 to (emap.height - 1)) {
			nodes(x)(y) = new Node(x,y,emap.getEnergy(x, y), img.getColor(x, y))
		}
		
		var hashmap = new HashMap[Node, Neighbors]
		
		//set links as necessary
		for (x <- 0 to (emap.width - 1); y <- 0 to (emap.height - 1)) {
		    hashmap += (nodes(x)(y) -> (
			    new Neighbors(
			          //up
			    	  if (y > 0) nodes(x)(y-1) else new End,
			    	  //right
  			    	  if (x < (nodes.length - 1)) nodes(x+1)(y) else new End,
  			    	  //down
  			    	  if (y < (nodes(0).length - 1)) nodes(x)(y+1) else new End,
  			    	  //left
			    	  if (x > 0) nodes(x-1)(y) else new End)
			    )
		   )

		}
		(hashmap, nodes(0)(0))
	}
}
 
 
 


 class SeamMap private(neighbormap : (HashMap[Node, Neighbors], Node)) {

   val neighbors = neighbormap._1
   val upperLeft = neighbormap._2

   def this(emap : EnergyMap, img : Image) = this(Factory.getNeighbors(emap : EnergyMap, img : Image))
   
   

	/**
	 * extracts neighbors given node (assumes one exists)
	 * @param node		the node to get neighbors for
	 * @return			the neighbors
	 */
	 private def getNeighbors(node : Node, map : HashMap[Node, Neighbors]) : Neighbors = {
	  map.get(node) match {
	    case Some(n) => n
	    case None => throw new IllegalArgumentException("Broken Invariant: Edges did not contain node being searched for")
	  }
	}
	 
		
	/**
	 * runs some invariant tests
	 */
	private def checkInvariant = {
	  def checkLeftRightHelper(cur : Node, curTot : Int) : Int = {
	      	getNeighbors(cur, neighbors).right match {
	      	  case (right : Node) =>
	      	  	if (cur.x > right.x)
	      	  		throw new IllegalArgumentException("current node at " + cur.x + " should be less than neighbor of " + right.x)
	    	    else if (cur != getNeighbors(right, neighbors).left)
	    	        throw new IllegalArgumentException("current node does not go back to previous node.  this point (" +
	    	        	cur.x + "," + cur.y + ") and other: (" + 
	    	        	right.x + "," + right.y + ").")
	    	    else
	      	  	    checkLeftRightHelper(right, curTot + 1)
	      	  case End() => curTot
	      	}
	  }
	  
  	  def checkLeftRight(cur : Node, totalsize : Int) : Unit = {
  	    val total = checkLeftRightHelper(cur, 1)
  	    if (total != totalsize)
  	      throw new IllegalArgumentException("size of this row is " + total + " but should be " + totalsize)
  	    else { 
	  	    getNeighbors(cur, neighbors).down match {
	  	      case (next : Node) => checkLeftRight(next, totalsize)
	  	      case End() =>
	  	    }
  	    }
  	  }
  	  
	  def checkUpDownHelper(cur : Node, curTot : Int) : Int = {
	      	getNeighbors(cur, neighbors).down match {
	      	  case (down : Node) =>
	      	  	if (cur.y > down.y)
	      	  		throw new IllegalArgumentException("current node at " + cur.y + " should be less than neighbor of " + down.y)
	    	    else if (cur != getNeighbors(down, neighbors).up)
	    	        throw new IllegalArgumentException("current node does not go back to previous node.  this point (" +
	    	        	cur.x + "," + cur.y  + ") and other: (" + 
	    	        	down.x + "," + down.y  + ") but right now pointing at (" + (getNeighbors(down, neighbors).up.asInstanceOf[Node]).x + ", " + (getNeighbors(down, neighbors).up.asInstanceOf[Node]).y + ").")
	    	    else
	      	  	    checkUpDownHelper(down, curTot + 1)
	      	  case End() => curTot
	      	}
	  }
	  
  	  def checkUpDown(cur : Node, totalsize : Int) : Unit = {
  	    val total = checkUpDownHelper(cur, 1)
  	    if (total != totalsize)
  	      throw new IllegalArgumentException("size of this column is " + total + " but should be " + totalsize)
  	    else { 
	  	    getNeighbors(cur, neighbors).right match {
	  	      case (next : Node) => checkUpDown(next, totalsize)
	  	      case End() =>
	  	    }
  	    }
  	  }
	  
      checkLeftRight(upperLeft, checkLeftRightHelper(upperLeft, 1))
      checkUpDown(upperLeft, checkUpDownHelper(upperLeft, 1))
	}

	
	
	/**
	 * gets first row
	 * 
	 * @return	an array of links representing first row
	 */
	def getFirst(seamdir : SeamDirection) : List[Node] = {
	  //TODO for testing purposes: checkInvariant
	  def getVertNodeHelper(n: Node) : List[Node] = {
	    getNeighbors(n, neighbors).right match {
        	case (next : Node) => n::(getVertNodeHelper(next))
        	case End() => Nil
	    }
	  }
  	  def getHorzNodeHelper(n: Node) : List[Node] = {
	    getNeighbors(n, neighbors).down match {
        	case (next : Node) => n::(getHorzNodeHelper(next))
        	case End() => Nil
	    }
	  }
  	  
  	  seamdir match {
  	    case (_ : HorzSeam) => getHorzNodeHelper(upperLeft)
  	    case (_ : VertSeam) => getVertNodeHelper(upperLeft)
  	  }
	}


	/**
	 * draws a seam on an image
	 * 
	 * @param nodes		list of nodes representing a seam
	 * @param img		the buffered image to draw on
	 * 
	 * @return			the drawn upon image
	 */
	def drawSeam(nodes : List[Node], img : Image) : Image = {
	  def drawPixel(nodes : List[Node], img: Image) : Unit = {
		  nodes match {
		    case node::tl => 
		    	img.setColor(node.x, node.y, Color.RED);
		    	drawPixel(tl, img)
		    case Nil => ()
		  }
	  }
	  
	  var newImg = img.copy
	  drawPixel(nodes, newImg)
	  newImg
	}
	
	
	/**
	 * gets all nodes that can proceed from current node: adds pixel that is below, 
	 * to left and below, and to right and below
	 * 
	 * @param node		a node to derive successor node
	 * @return 			links that succeed from n in a list
	 */
	def getSuccessors (node: Node, dir : SeamDirection) : Option[Successors] = {
		  def getSuccDown(node: Node) = {
			  getNeighbors(node, neighbors).down match {
			    case (down : Node) => 
			      val downNeighbors = getNeighbors(down, neighbors)
			      Some(new Successors(downNeighbors.left, down, downNeighbors.right))
			    case End() => None 
		  	}
		  }
  		  def getSuccRight(node: Node) = {
			  getNeighbors(node, neighbors).right match {
			    case (right : Node) => 
			      val rightNeighbors = getNeighbors(right, neighbors)
			      Some(new Successors(rightNeighbors.up, right, rightNeighbors.down))
			    case End() => None 
		  	}
		  }
  		  
  		  dir match {
  		    case (_ : VertSeam) => getSuccDown(node)
  		    case (_ : HorzSeam) => getSuccRight(node)
  		  }
	}

   
	/**
	 * get Length
	 * 
	 * @param dir		the direction of the seam
	 * @return			the length of image
	 */
	def getLength(dir : SeamDirection) : Int = {
	  dir match {
	    case (_ : HorzSeam) => getWidth(upperLeft, 0)
	    case (_ : VertSeam) => getHeight(upperLeft, 0)
	  }
	}
	
	
		  
	  private def getHeight(node : Node, num : Int) : Int = {
		  getNeighbors(node, neighbors).down match {
		    case (next : Node) => getHeight(next, (num + 1))
		    case _ => num
		  }
	  }
	
	  private def getWidth(node : Node, num : Int) : Int = {
		  getNeighbors(node, neighbors).right match {
		    case (next : Node) => getWidth(next, (num + 1))
		    case _ => num
		  }
	  }
	  
	
	   /**
	   * sets a up and down connection for two nodes
	   * @param nodeUp			upper node
	   * @param nodeDown		lower node
	   * @param neighborList	the list of neighbors
	   */
	  private def setUpDown(nodeUp : NodeItem, nodeDown : NodeItem, neighborList : HashMap[Node, Neighbors]) : HashMap[Node, Neighbors] = {
	    (nodeUp, nodeDown) match {
	      case (nUp : Node, nDown : Node) => 
	        val upNeighbors = getNeighbors(nUp, neighborList)
	        val downNeighbors = getNeighbors(nDown, neighborList)	        
    		((neighborList 
    		    + (nDown -> new Neighbors(nUp, downNeighbors.right, downNeighbors.down, downNeighbors.left)))
    		    	+ (nUp -> new Neighbors(upNeighbors.up, upNeighbors.right, nDown, upNeighbors.left)))
	      case (nUp : Node, End()) => 
	        val upNeighbors = getNeighbors(nUp, neighborList)
    		(neighborList 
		    	+ (nUp -> new Neighbors(upNeighbors.up, upNeighbors.right, End(), upNeighbors.left)))
	      case (End(), nDown : Node) => 
	        val downNeighbors = getNeighbors(nDown, neighborList)
    		(neighborList
    		    + (nDown -> new Neighbors(End(), downNeighbors.right, downNeighbors.down, downNeighbors.left)))
	      case (End(), End()) => throw new IllegalArgumentException("one of the nodeitems should be a node")
	    }
	  }
	  
	  /**
	   * sets a left and right connection for two nodes
	   * @param nodeLeft		left node
	   * @param nodeRight		right node
	   * @param neighborList	the list of neighbors
	   */
  	  private def setLeftRight(nodeLeft : NodeItem, nodeRight : NodeItem, neighborList : HashMap[Node, Neighbors]) : HashMap[Node, Neighbors] = {
	    (nodeLeft, nodeRight) match {
	      case (nLeft : Node, nRight : Node) => 
	        val leftNeighbors = getNeighbors(nLeft, neighborList)
	        val rightNeighbors = getNeighbors(nRight, neighborList)	        
    		((neighborList 
    		    + (nRight -> new Neighbors(rightNeighbors.up, rightNeighbors.right, rightNeighbors.down, nLeft)))
    		    	+ (nLeft -> new Neighbors(leftNeighbors.up, nRight, leftNeighbors.down, leftNeighbors.left)))
	      case (nLeft : Node, End()) => 
	        val leftNeighbors = getNeighbors(nLeft, neighborList)
    		(neighborList 
		    	+ (nLeft -> new Neighbors(leftNeighbors.up, End(), leftNeighbors.down, leftNeighbors.left)))
	      case (End(), nRight : Node) => 
	        val rightNeighbors = getNeighbors(nRight, neighborList)
    		(neighborList
    		    + (nRight -> new Neighbors(rightNeighbors.up, rightNeighbors.right, rightNeighbors.down, End())))
	    }
	  }
  	
  	 
  	
	/**
	 * deletes a seam and repairs links accordingly
	 * @param nodes			the nodes to delete
	 * @return 				new PixelGraph 
	 * 
	 */
	def deleteNodes(nodes : List[Node], dir : SeamDirection) : SeamMap = {
	  
	  /**
	   * for a vertical seam fix it's up down connections for surrounding nodes
	   * 
	   * @param thisNodeForRemoval		the node that is in the seam
	   * @param nextNodeForRemoval		the node that is next in the seam
	   * @param newNeighbors			the neighbors list to fix
	   * 
	   * @return						the new neighbors list
	   */
		  def vertSeamUD (thisNodeForRemoval : Node, nextNodeForRemoval: Node, newNeighbors : HashMap[Node, Neighbors]) : 
			  HashMap[Node, Neighbors]= {
		    val thisNodeNeighs = getNeighbors(thisNodeForRemoval, newNeighbors)
    		    //look around after going down one node
			  thisNodeNeighs.down match {
		      case (thisDown : Node) =>
		        val thisDownNeighs = getNeighbors(thisDown, newNeighbors)
		        //next check to see next node for removal is center, left, or right of node above (also extract thisNode's left and right neighbor)
		        ((nextNodeForRemoval==thisDown), (nextNodeForRemoval==thisDownNeighs.left), (nextNodeForRemoval==thisDownNeighs.right)) match {  
		          //since next seam is straight down, do nothing
		          case (true, _, _) => newNeighbors
		          //since next seam node is to the left, adjust accordingly
		          case (_, true, _) => setUpDown(thisNodeNeighs.left, thisDown, newNeighbors)
		          //since next seam node is to the right, adjust accordingly
		          case (_, _, true) => setUpDown(thisNodeNeighs.right, thisDown, newNeighbors)
      		      case _ => throw new IllegalArgumentException("links given do not meet invariant for a seam for nodes at " + thisNodeForRemoval.x + ", " + thisNodeForRemoval.y + " and " + nextNodeForRemoval.x + ", " + nextNodeForRemoval.y )
		        }
			  	case _ => throw new IllegalArgumentException("fix up and down was passed something it shouldn't have been")
		    }
	  }
		  
		  
	 /**
	   * for a horizontal seam fix it's left right connections for surrounding nodes
	   * 
	   * @param thisNodeForRemoval		the node that is in the seam
	   * @param nextNodeForRemoval		the node that is next in the seam
	   * @param newNeighbors			the neighbors list to fix
	   * 
	   * @return						the new neighbors list
	   */
		  def horzSeamLR (thisNodeForRemoval : Node, nextNodeForRemoval: Node, newNeighbors : HashMap[Node, Neighbors]) : 
			  HashMap[Node, Neighbors]= {
		    val thisNodeNeighs = getNeighbors(thisNodeForRemoval, newNeighbors)
    		    //look around after going down one node
			  thisNodeNeighs.right match {
		      case (thisRight : Node) =>
		        val thisRightNeighs = getNeighbors(thisRight, newNeighbors)
		        //next check to see next node for removal is center, left, or right of node above (also extract thisNode's left and right neighbor)
		        ((nextNodeForRemoval==thisRight), (nextNodeForRemoval==thisRightNeighs.up), (nextNodeForRemoval==thisRightNeighs.down)) match {
		          //since next seam is straight to right, do nothing
		          case (true, _, _) => newNeighbors
		          //since next seam node is up, adjust accordingly
		          case (_, true, _) => setLeftRight(thisNodeNeighs.up, thisRight, newNeighbors)
  		          //since next seam node is down, adjust accordingly
		          case (_, _, true) => setLeftRight(thisNodeNeighs.down, thisRight, newNeighbors)
      		      case _ => throw new IllegalArgumentException("links given do not meet invariant for a seam for nodes at " + thisNodeForRemoval.x + ", " + thisNodeForRemoval.y + " and " + nextNodeForRemoval.x + ", " + nextNodeForRemoval.y )

		        }
			  	case _ => throw new IllegalArgumentException("fix left and right was passed something it shouldn't have been")
		    }
	  	} 
	  
	  /**
	   * repairs all nodes for a vertical seam being removed
	   * @param nodeList		List of nodes to remove
	   * @param newNeighbors	the neighbors to remove
	   */
	  def repairVertSeamLinks (nodeList : List[Node], newNeighbors : HashMap[Node, Neighbors]) : HashMap[Node, Neighbors] = {
			  nodeList match {
			  	case node::nextNode::tl =>
			  	  val nodeNeighs = getNeighbors(node, newNeighbors)
			  	  val fixedUpDown = vertSeamUD(node, nextNode, newNeighbors)
			  	  val fixedLinks = setLeftRight(nodeNeighs.left, nodeNeighs.right, fixedUpDown)
			  	  val fixedHash = fixedLinks - node
			  	  repairVertSeamLinks(nextNode::tl, fixedHash)
			  	  		
			  	case node::Nil =>
			  	  val nodeNeighs = getNeighbors(node, newNeighbors)
			  	  nodeNeighs.down match {
			  	    case End() => setLeftRight(nodeNeighs.left, nodeNeighs.right, newNeighbors)
			  	    case _ => throw new IllegalArgumentException("reached last node in list but not at the bottom of graph")
			  	  }  	  
		  	  }
	  }  
	  
  	  /**
	   * repairs all nodes for a horizontal seam being removed
	   * @param nodeList		List of nodes to remove
	   * @param newNeighbors	the neighbors to remove
	   */
	  def repairHorzSeamLinks (nodeList : List[Node], newNeighbors : HashMap[Node, Neighbors]) : HashMap[Node, Neighbors] = {
			  nodeList match {
			  	case node::nextNode::tl =>
			  	  val nodeNeighs = getNeighbors(node, newNeighbors)
			  	  val fixedLeftRight = horzSeamLR(node, nextNode, newNeighbors)
			  	  val fixedLinks = setUpDown(nodeNeighs.up, nodeNeighs.down, fixedLeftRight)
			  	  val fixedHash = fixedLinks - node
			  	  repairHorzSeamLinks(nextNode::tl, fixedHash)
			  	  		
			  	case node::Nil =>
			  	  val nodeNeighs = getNeighbors(node, newNeighbors)
			  	  nodeNeighs.right match {
			  	    case End() => setUpDown(nodeNeighs.up, nodeNeighs.down, newNeighbors)
			  	    case _ => throw new IllegalArgumentException("reached last node in list but not at the bottom of graph")
			  	  }  	  
		  	  }
	  } 
		
	  nodes match {
		    case hd::tl =>
		      val firstRow = getFirst(dir)
		      
		      //do we need a new upperleft?
		      val newUpperLeft = 
		        if (hd == firstRow(0)) firstRow(1) 
		        else firstRow(0)  
		      
		      // does first item in nodes belong in first row?
		      if (firstRow.foldLeft(false)(
		          (prev, thisItem) => (prev || thisItem == hd ))) {	
		    	  dir match {
		    	    case (_ : HorzSeam) => new SeamMap((repairHorzSeamLinks(nodes, neighbors), newUpperLeft))
		    	    case (_ : VertSeam) => new SeamMap((repairVertSeamLinks(nodes, neighbors), newUpperLeft))

		    	  }
		      }
		      else
		    	  throw new IllegalArgumentException("first link in list should be in top row")
			case Nil =>  throw new IllegalArgumentException("there is nothing in nodes supplied to delete")
		  }
	 }
	
	
	/**
	 * transforms graph to image
	 * 
	 * @return the bufferedimage
	 */
	def getImage : Image = {
	  
	  val width = getWidth(upperLeft, 1)
	  val height = getHeight(upperLeft, 1)
	  
	  val img = ImageUtils.DEFAULT.createImage(width, height)
	  
	  def setRowItem(node : Node, x : Int, y : Int) : Unit = {
	    img.setColor(x, y, node.color)
	    getNeighbors(node, neighbors).right match {
	      case (next : Node) => 
	        setRowItem(next, (x+1), y)
	      case _ => ()
	    }
	  }
	  
	  def setRows(leftNode : Node, y : Int) : Unit = {
	    setRowItem(leftNode, 0, y)
	    getNeighbors(leftNode, neighbors).down match {
	      case (next : Node) => setRows(next, (y+1))
	      case _ => ()
	    }
	  }
	    
	  setRows(upperLeft, 0)
	  img
	}
	
	def getSeamInsertionImage(vertSeams : List[Node], horzSeams : List[Node]) : Image = {
	  def doubleXinHash(n : Node, doubleXY : HashMap[Node, (Boolean, Boolean)]) : HashMap[Node, (Boolean, Boolean)] = {
	    doubleXY.get(n) match {
	      case Some((doubleX, doubleY)) => (doubleXY + (n -> (true, doubleY)))
	      case None => (doubleXY + (n -> (true, false)))
	    }
	  }
	  
  	  def doubleYinHash(n : Node, doubleXY : HashMap[Node, (Boolean, Boolean)]) : HashMap[Node, (Boolean, Boolean)] = {
	    doubleXY.get(n) match {
	      case Some((doubleX, doubleY)) => (doubleXY + (n -> (doubleX, true)))
	      case None => (doubleXY + (n -> (true, false)))
	    }
	  }
	  
	  def determineHash(vertSeams : List[Node], horzSeams : List[Node], doubleXY : HashMap[Node, (Boolean, Boolean)]) : HashMap[Node, (Boolean, Boolean)] = {
	    (vertSeams, horzSeams) match {
	      case (verthd::verttl, _) => determineHash(verttl, horzSeams, doubleXinHash(verthd, doubleXY))
	      case (_, horzhd::horztl) =>determineHash(vertSeams, horztl, doubleYinHash(horzhd, doubleXY))
	      case _ => doubleXY
	    }
	  }
	  
  	  def setRow(node : Node, x : Int,  Dstruct: Array[List[Color]], doubleXY : HashMap[Node, (Boolean, Boolean)]) 
  	  		: Unit = {
  	    
  	    val newXVal =
	  	    doubleXY.get(node) match {
	  	      case None | Some((false, false)) => node.color::(Dstruct(x)); (x+1)
	  	      case Some((true, false)) => node.color::(Dstruct(x)); node.color::(Dstruct(x+1)); (x+2)
	  	      case Some((false, true)) => node.color::node.color::(Dstruct(x)); (x+1)
	  	      case Some((true, true)) =>
	  	        node.color::node.color::(Dstruct(x)); node.color::node.color::(Dstruct(x+1)); (x+2)
	  	    }
	    getNeighbors(node, neighbors).right match {
	      case (next : Node) => setRow(next, newXVal, Dstruct, doubleXY)
	      case _ => ()
	    }
	  }
	  
	  def setRows(leftNode : Node, Dstruct: Array[List[Color]], doubleXY : HashMap[Node, (Boolean, Boolean)]) : Unit = {
	    setRow(leftNode, 0, Dstruct, doubleXY)
	    getNeighbors(leftNode, neighbors).down match {
	      case (next : Node) => setRows(next, Dstruct, doubleXY)
	      case _ => ()
	    }
	  }
	  
	  def doCheck(Dstruct : Array[List[Color]], vertSize : Int, curX : Int) : Unit = {
	    if (Dstruct(curX).length == vertSize)
	      if (curX < Dstruct.length) doCheck(Dstruct, vertSize, curX+1) else ()
	    else
	      throw new IllegalArgumentException("datastructure should be of same height throughout")
	  }

	  def writeToImage(img : Image, curList : List[Color], curY : Int, height : Int, curX : Int, DStruct : Array[List[Color]]) : Unit = {
	    curList match {
	      case hd::tl => img.setColor(curX, curY, hd); writeToImage(img, tl, (curY - 1), height, curX, DStruct)
	      case Nil => writeToImage(img, DStruct(curX+1), height, height, curX+1, DStruct)
	    }
	  }
	  
	  //will hold pixel color while determining
	  val Dstruct = new Array[List[Color]](getWidth(upperLeft, 0) + vertSeams.length)
	  setRows(upperLeft, Dstruct, 
	      determineHash(vertSeams, horzSeams, new HashMap[Node, (Boolean, Boolean)]()))
	      
      val height = Dstruct(0).length
	  doCheck(Dstruct, height, 0)
	  
	  val img = ImageUtils.DEFAULT.createImage(Dstruct.length, height)
	  
	  writeToImage(img, Nil, 0, height, -1, Dstruct)
	  
	 img
	}

}
