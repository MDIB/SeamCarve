package com.gdicristofaro.seamcarve.core

import scala.collection.mutable.HashSet
import scala.collection.{mutable, Set}
import scala.collection.mutable.ListBuffer


/**
 * x,y -> refers to the original image x,yposition
 * energy -> the energy for this pixel
 * upper, lower, left, right -> the current neighbors of this pixel in the seam map
 * totSeamEnergy -> the aggregate energy at this point
 * seamPredecessor -> the previous node that this node is using as a seam (the previous neighbor that has minimal energy)
 * seamSuccessors -> the next set of PixelNodes that use this node as part of their seam
 */
class PixelNode(
    val x : Int, val y : Int,
    val energy : Double,
    var upper : Option[PixelNode], var lower : Option[PixelNode],
    var left : Option[PixelNode], var right : Option[PixelNode],
    var totSeamEnergy : Double, 
    var seamPredecessor : Option[PixelNode], var seamSuccessors : HashSet[PixelNode]) {

  override def toString = s"PixelNode($x, $y, $energy)"
}
    

class Point(val x : Int, val y : Int) {
  override def toString = s"Point($x, $y)"

  def canEqual(other: Any): Boolean = other.isInstanceOf[Point]

  override def equals(other: Any): Boolean = other match {
    case that: Point =>
      (that canEqual this) &&
        x == that.x &&
        y == that.y
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(x, y)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}


// vert seams run up to down
object VertSeamFuncts {
  def updateNodeEnergy(node : PixelNode) {
    val upperNode = node.upper
    val (left, right) = upperNode match {
      case Some(n) => (n.left, n.right) 
      case None => (None, None)
    }
    
    SeamCarver.setPixelSeam(node, left, upperNode, right)
  }
  
  def removeNode(node : PixelNode) {
    val left = node.left
    val right = node.right
    
    if (left.isDefined)
      left.get.right = right
      
    if (right.isDefined)
      right.get.left = left      

    val upper = node.upper
    val prevSeamNode = node.seamPredecessor

    // repair upper connection if relevant 
    (upper, prevSeamNode) match {
      // if upper/prevSeamNode are None don't worry about it
      case (None, None) => ()
      case (Some(upNd), Some(prevSeamNd)) =>
        // if they are the same then straight up doesn't need to be repaired
        if (upNd != prevSeamNd) {
          val upperLeft = upNd.left
          val upperRight = upNd.right
          
          if (upperLeft.isDefined && upperLeft.get == prevSeamNd) {
            upNd.lower = left
            left.get.upper = Some(upNd)
          }
          else if (upperRight.isDefined && upperRight.get == prevSeamNd) {
            upNd.lower = right
            right.get.upper = Some(upNd)            
          }
          else {
           throw new IllegalStateException("Seam node does not go to upper left or upper right") 
          }
        }
      case _ => throw new IllegalStateException("upper node and previous seam node should either be None or Some")
    }
  }
  
  // gets next node in same level
  def getNextInLevel(p : PixelNode) = p.right
  
  def getPrevInLevel(p : PixelNode) = p.left
    
  // gets next node in next level
  def getNextLevel(p : PixelNode) = p.lower
  
  def getBestSeam(startingNode : PixelNode) = SeamCarver.getBestSeam(getNextInLevel, getPrevInLevel, startingNode)

  
  def removeSeam(seamNode : PixelNode) = SeamCarver.removeSeam(seamNode, updateNodeEnergy, removeNode)
  
  def setEnergy(upperLeft : PixelNode) = SeamCarver.setEnergy(getNextInLevel, getNextLevel, upperLeft)
}

// horz seams run left to right
object HorzSeamFuncts {
  def updateNodeEnergy(node : PixelNode) {
    val leftNode = node.left
    val (up, down) = leftNode match {
      case Some(n) => (n.upper, n.lower) 
      case None => (None, None)
    }
    
    SeamCarver.setPixelSeam(node, up, leftNode, down)
  }
  
  def removeNode(node : PixelNode) {
    val upper = node.upper
    val lower = node.lower
    
    if (upper.isDefined)
      upper.get.lower = lower
      
    if (lower.isDefined)
      lower.get.upper = upper      

    val left = node.left
    val prevSeamNode = node.seamPredecessor

    // repair left connection if relevant 
    (left, prevSeamNode) match {
      // if left/prevSeamNode are None don't worry about it
      case (None, None) => ()
      case (Some(leftNd), Some(prevSeamNd)) =>
        // if they are the same then straight left doesn't need to be repaired
        if (leftNd != prevSeamNd) {
          val upperLeft = leftNd.upper
          val lowerLeft = leftNd.lower
          
          if (upperLeft.isDefined && upperLeft.get == prevSeamNd) {
            leftNd.right = upper
            upper.get.left = Some(leftNd)
          }
          else if (lowerLeft.isDefined && lowerLeft.get == prevSeamNd) {
            leftNd.right = lower
            lower.get.left = Some(leftNd)            
          }
          else {
           throw new IllegalStateException("Seam node does not go to upper left or lower left")
          }
        }
      case _ => throw new IllegalStateException("left node and previous seam node should either be None or Some at left: "
                                                  + left + " and previous " + prevSeamNode + " from node: " + node)
    }
  }
  
  // gets next node in same level
  def getNextInLevel(p : PixelNode) = p.lower
  
  def getPrevInLevel(p : PixelNode) = p.upper
  
  // gets next node in next level
  def getNextLevel(p : PixelNode) = p.right
  
  def getBestSeam(startingNode : PixelNode) = SeamCarver.getBestSeam(getNextInLevel, getPrevInLevel, startingNode)
    
  def removeSeam(seamNode : PixelNode) = SeamCarver.removeSeam(seamNode, updateNodeEnergy, removeNode)
  
  def setEnergy(upperLeft : PixelNode) = SeamCarver.setEnergy(getNextInLevel, getNextLevel, upperLeft)
}


object SeamCarver {

  /**
   * removes seam nodes re-calculating seam energy for any affected remaining nodes
    * seamNode -> pixel node as a part of the seam that needs to be removed
   * updateNodeEnergy -> re-detertermines appropriate seam predecessor (min energy) and reevaluates total energy
   * removeNode -> repairs connections and removes node
   * returns (set of nodes in this level that need to be reevaluated, points in this seam)
   */
  def removeSeam(
      seamNode : PixelNode, 
      updateNodeEnergy : (PixelNode) => Unit, 
      removeNode: (PixelNode) => Unit) : 
      (Set[PixelNode], Set[Point]) = {
    
    // any node that derives a seam from this node will have to be corrected
    val dirtyNodes = new HashSet[PixelNode]
    dirtyNodes++=seamNode.seamSuccessors
      
    // repair the connections on either side of this node
    // done here so that we can keep track of where previous seam node is in relation to this node
    removeNode(seamNode)
    
    val seamPoints =
      // if we have a predecessor, then we have to deal with dirty nodes from that level
      if (seamNode.seamPredecessor.isDefined) {
        val predecessor = seamNode.seamPredecessor.get
           
        val (thisLevelDirty, seamPoints) = removeSeam(predecessor, updateNodeEnergy, removeNode)
        for (dirtyNode <- thisLevelDirty) { 
          val prevEnergy = dirtyNode.totSeamEnergy
          
          // update the seam and total energy for the dirty node by determining the best new seam predecessor for this item
          updateNodeEnergy(dirtyNode)
          // if the energy has increased, then it is possible that successors might choose a different
          // predecessor and are therefore potentially dirty
          if (dirtyNode.totSeamEnergy > prevEnergy)
            dirtyNodes++=dirtyNode.seamSuccessors
            
        }
        
        // return the previous seam points
        seamPoints
      }
      else {
        new HashSet[Point]
      }
    
    // return the dirty nodes and the previous seam points plus this
    (dirtyNodes, seamPoints+(new Point(seamNode.x, seamNode.y)))
  }
  
  /* this sets the energy for one level of pixels (i.e. for vertical seams, sets one rows)
   * based on previous level of pixels
   * 
   * getNextInLevel -> next pixel adjacent in same level (i.e. for vertical seams, gets next pixel to right)
   * prevLevelStart -> the first node in the previous level
   * thislevelStart -> the first node in this level
   * 
   */
  def setLevelEnergy(getNextInLevel : (PixelNode) => Option[PixelNode], prevLevelStart : PixelNode,
      thisLevelStart : PixelNode) {
    // determine best option and act accordingly
    var thisNode = thisLevelStart
    var predA : Option[PixelNode] = None
    var predB : Option[PixelNode] = Some(prevLevelStart)
    var predC = getNextInLevel(prevLevelStart)
    
    // loop through all nodes
    while(true) {
      setPixelSeam(thisNode, predA, predB, predC)
      
      getNextInLevel(thisNode) match {
        // if there is no next node, we are done
        case None => return
        // otherwise move over a node
        case Some(newNode) => 
          thisNode = newNode
          predA = predB
          predB = predC
          predC = predC match {
            case Some(cNode) => getNextInLevel(cNode)
            case None => None
          }
      }
    }      
  }
  
  /*
   * sets the energy for every node in the node map
   * getNextInLevel -> next pixel adjacent in same level (i.e. for vertical seams, gets next pixel to right)
   * getNextLevel -> next pixel adjacent in next level (i.e. for vertical seams, gets next pixel lower)
   * firstNode -> first node - upper left node
   * 
   * returns pixel node in last level whose seam has minimum energy 
   */
  def setEnergy(getNextInLevel : (PixelNode) => Option[PixelNode], getNextLevel: (PixelNode) => Option[PixelNode],
      firstNode : PixelNode) : PixelNode = {
    
    // set first level total energy to just the energy
    var firstLevelNode : Option[PixelNode] = Some(firstNode)
    while (firstLevelNode.isDefined) {
      var extractedNode = firstLevelNode.get
      extractedNode.totSeamEnergy = extractedNode.energy
      extractedNode.seamPredecessor = None
      firstLevelNode = getNextInLevel(extractedNode)
    }
    
    // go through all levels
    var prevLevelNode = firstNode
    var thisLevel = getNextLevel(prevLevelNode)
    while (thisLevel.isDefined) {
      val thisLevelNode = thisLevel.get
      setLevelEnergy(getNextInLevel, prevLevelNode, thisLevelNode)
      prevLevelNode = thisLevelNode
      thisLevel = getNextLevel(thisLevelNode)
    }
    
    // at this point, previous level is the last level of the image
    // find best seam
    // we don't need a previous finder here because this is the first node
    getBestSeam(getNextInLevel, {(p) => None }, prevLevelNode)
  }
  
  // gets the seam with the least energy from a pixel node in last row/column where energy has been determined
  def getBestSeam(
      getNextInLevel : (PixelNode) => Option[PixelNode], 
      getPrevInLevel : (PixelNode) => Option[PixelNode], 
      startingNode : PixelNode) : PixelNode = {
    
    def _getBest(funct : (PixelNode) => Option[PixelNode], startingNode : PixelNode) : PixelNode = {
      var bestChoice = startingNode
      var curItem : Option[PixelNode] = Some(startingNode)
      while (curItem.isDefined) {
        val curItemNode = curItem.get
        
        if (curItemNode.totSeamEnergy < bestChoice.totSeamEnergy)
          bestChoice = curItemNode
          
        curItem = getNextInLevel(curItemNode)
      }
      
      bestChoice
    }
    
    val prevNode = _getBest(getPrevInLevel, startingNode)
    val nextNode = _getBest(getNextInLevel, startingNode)
    
    if (prevNode.totSeamEnergy < nextNode.totSeamEnergy)
      prevNode
    else
      nextNode
  }

  // traverses to upper left node from any node in mapping
  def getUpperLeft(starting : PixelNode): PixelNode = {
    var toRet = starting
    while (toRet.upper.isDefined)
      toRet = toRet.upper.get

    while (toRet.left.isDefined)
      toRet = toRet.left.get

    toRet
  }




  // maxTotEnergy is the total energy for the seam (avg node energy * img height)
  // returns a list of seam points and the upper left pixel node of the resulting seam map
  def getVertSeams(upperLeft : PixelNode, maxTotEnergy : Double, vertSeamNum : Int) :
  (Seq[Set[Point]], PixelNode) = {

    val vertSeams = new ListBuffer[Set[Point]]

    // tracks a node to use to return
    var startNode = upperLeft

    if (vertSeamNum > 0) {
      // max energy refers to average seam node energy so total energy has to be less than image height * maxEnergy
      var bestSeam = VertSeamFuncts.setEnergy(upperLeft)

      while (vertSeams.length < vertSeamNum && bestSeam.totSeamEnergy < maxTotEnergy) {
        startNode = (bestSeam.left, bestSeam.right) match {
          case (Some(pixNode), _) => pixNode
          case (None, Some(pixNode)) => pixNode
          case (None, None) => throw new IllegalStateException("no left or right from this node")
        }

        val (_, seamPts) = VertSeamFuncts.removeSeam(bestSeam)
        vertSeams += seamPts
        bestSeam = VertSeamFuncts.getBestSeam(startNode)
      }
    }

    (vertSeams, getUpperLeft(startNode))
  }

  // maxTotEnergy is the total energy for the seam (avg node energy * img width)
  // returns a list of seam points and the upper left pixel node of the resulting seam map
  def getHorzSeams(upperLeft : PixelNode, maxTotEnergy : Double, horzSeamNum : Int) :
  (Seq[Set[Point]], PixelNode) = {

    val horzSeams = new ListBuffer[Set[Point]]

    // tracks a node to use to return
    var startNode = upperLeft

    if (horzSeamNum > 0) {
      // max energy refers to average seam node energy so total energy has to be less than image height * maxEnergy

      var bestSeam = HorzSeamFuncts.setEnergy(startNode)

      while (horzSeams.length < horzSeamNum && bestSeam.totSeamEnergy < maxTotEnergy) {
        val startNode = (bestSeam.upper, bestSeam.lower) match {
          case (Some(pixNode), _) => pixNode
          case (None, Some(pixNode)) => pixNode
          case (None, None) => throw new IllegalStateException("no upper or lower from this node")
        }

        val (_, seamPts) = HorzSeamFuncts.removeSeam(bestSeam)
        horzSeams+=seamPts
        bestSeam = HorzSeamFuncts.getBestSeam(startNode)
      }
    }

    (horzSeams, getUpperLeft(startNode))
  }


  /**
    * creates a new pixelnode mapping doubling points found in vertical seam points
    *
    * @param eMap                 the original energy map
    * @param verticalSeamPoints   the vertical seam points
    * @return                     (pixelnode in upper left, mapping of new x,y mapped to old x,y
    */
  def insertVertSeams(eMap : EnergyMap, verticalSeamPoints : Set[Point]) : (PixelNode, mutable.HashMap[Point, Point]) = {
    // maps new points in new pixel mapping to original image x,y
    val mapping = new mutable.HashMap[Point, Point]

    // temporary location for pixel nodes
    val tempItems = new Array[Array[PixelNode]](eMap.width + (verticalSeamPoints.size / eMap.height))

    // set up the array with empty arrays (so we get 2d array)
    for (x <- 0 until tempItems.length)
      tempItems(x) = new Array[PixelNode ](eMap.height)

    // set up the nodes (and mapping)
    for (y <- 0 until eMap.height) {
      var newX = 0

      for (x <- 0 until eMap.width) {
        // place the point in array and set mapping new point => old point
        tempItems(newX)(y) = new PixelNode(newX, y, eMap.getEnergy(x, y),
          None, None, None, None, 0, None, new HashSet[PixelNode]())
        mapping.put(new Point(newX,y), new Point(x,y))
        newX += 1

        // if contained in vertical seam points, add the point again
        if (verticalSeamPoints.contains(new Point(x,y))) {
          tempItems(newX)(y) = new PixelNode(newX, y, eMap.getEnergy(x, y),
            None, None, None, None, 0, None, new HashSet[PixelNode]())
          mapping.put(new Point(newX,y), new Point(x,y))
          newX += 1
        }
      }
    }

    // set up node links
    for (y <- 0 until eMap.height) {
      for (x <- 0 until tempItems.length) {
        tempItems(x)(y).left = if (x == 0) None else Some(tempItems(x - 1)(y))
        tempItems(x)(y).right = if (x == tempItems.length - 1) None else Some(tempItems(x + 1)(y))
        tempItems(x)(y).upper = if (y == 0) None else Some(tempItems(x)(y - 1))
        tempItems(x)(y).lower = if (y == eMap.height - 1) None else Some(tempItems(x)(y + 1))
      }
    }

    (tempItems(0)(0), mapping)
  }

  
  /**
   * sets the total seam energy as well as predecessor/successors for curNode
   * curNode - the node that will derive a seam from predA, predB, or predC
   * returns the best predecessor node option
   */
  def setPixelSeam(curNode : PixelNode, predA : Option[PixelNode], predB : Option[PixelNode], predC : Option[PixelNode]) : Option[PixelNode] = {
    // returns an actual node (not none) with the lowest total seam energy
    def getBest(curBest : Option[PixelNode], test : Option[PixelNode]) : Option[PixelNode] = {
      (curBest, test) match {
        case (None, None) => None
        case (None, Some(_)) => test
        case (Some(_), None) => curBest
        case (Some(curBestNode), Some(testNode)) => 
          if (testNode.totSeamEnergy < curBestNode.totSeamEnergy)
            test
          else
            curBest
      }
    }
    
    // get the best node item
    var bestNode = getBest(None, predA)
    bestNode = getBest(bestNode, predB)
    bestNode = getBest(bestNode, predC)
    
    // remove the current node from the previous seam predecessor's successors
    if (curNode.seamPredecessor.isDefined)
      curNode.seamPredecessor.get.seamSuccessors.remove(curNode)
      
    // add cur node to best node's successors
    if (bestNode.isDefined)
      bestNode.get.seamSuccessors.add(curNode)
      
    // set up seam predecessor
    curNode.seamPredecessor = bestNode
    
    // determine total energy
    val bestNodeVal = bestNode match {
      case Some(bn) => bn.totSeamEnergy
      case None => 0
    }
    curNode.totSeamEnergy = curNode.energy + bestNodeVal
    
    // return the best node
    bestNode
  }

  // retrieves pixel node mapping based on image energy
  def getPixelNodes(eMap : EnergyMap) : PixelNode = {
    val tempItems = new Array[Array[PixelNode]](eMap.width)
    for (x <- 0 until eMap.width) {
      tempItems(x) = new Array[PixelNode](eMap.height)

      for (y <- 0 until eMap.height) {
        tempItems(x)(y) = new PixelNode(x, y, eMap.getEnergy(x, y),
          None, None, None, None, 0, None, new HashSet[PixelNode]())
      }
    }

    for (y <- 0 until eMap.height) {
      for (x <- 0 until eMap.width) {
        tempItems(x)(y).left = if (x == 0) None else Some(tempItems(x - 1)(y))
        tempItems(x)(y).right = if (x == eMap.width - 1) None else Some(tempItems(x + 1)(y))
        tempItems(x)(y).upper = if (y == 0) None else Some(tempItems(x)(y - 1))
        tempItems(x)(y).lower = if (y == eMap.height - 1) None else Some(tempItems(x)(y + 1))
      }
    }

    tempItems(0)(0)
  }
}


object SeamVisualization {
  def drawSeam(img : Image, color : Color, seamPoints : Iterable[Point]) {
    seamPoints.foreach { p => img.setColor(p.x, p.y, color) }
  }

  // check some invariants for remove seam if debug
  private def seamInvariantCheck(
     origImg : Image, newImg : Image,
     vertSeamPoints : Set[Point], horzSeamPoints : Set[Point]) {

    // check that seams cover whole length of image
     assert(vertSeamPoints.size % origImg.height == 0,
       s"there are ${vertSeamPoints.size} vertical seam points which should have a modulus of 0 with ${origImg.height}")

    // width once vert seams are removed
    val testWidth = origImg.width - (vertSeamPoints.size / origImg.height)
    val newWidth = testWidth
    assert(horzSeamPoints.size % testWidth == 0,
      s"there are ${horzSeamPoints.size} horizontal seam points which should have a modulus of 0 with $testWidth")

    val newHeight = origImg.height - (horzSeamPoints.size / testWidth)

     // make sure image is of right dimensions
     assert(newImg.width == newWidth, s"new image width is ${newImg.width} but the new width should be $newWidth")

     assert(newImg.height == newHeight, s"new image height is ${newImg.height} but the new height should be $newHeight")
  }


  /**
    * creates an image doubling any points included seams
    *
    * @param imgUtils         image utils
    * @param origImg          original image
    * @param horzSeamPoints   horizontal seam points corresponding to coordinates of image after vertical
    *                         seam points have been inserted
    * @param mapping          mapping of x,y coordinates after vertical seam points have been inserted to
    *                         original x,y coordinates (if size == 0 then it is assumed that there are no vertical seams)
    * @return                 the resulting image
    */
  def getSeamInsertionImage(imgUtils: ImageUtils, origImg : Image,
                            horzSeamPoints : Set[Point],
                            mapping : mutable.HashMap[Point, Point]): Image = {

    val newWidth =
      if (mapping.size == 0)
        origImg.width
      else
        mapping.size / origImg.height

    val newHeight = origImg.height + horzSeamPoints.size / newWidth
    val retImg = imgUtils.createImage(newWidth, newHeight)


    def setColorFromMapping(xSrc : Int, ySrc : Int, xDest : Int, yDest : Int) {
      val realSrcPt = mapping.get(new Point(xSrc, ySrc)).get
      val color = origImg.getColor(realSrcPt.x, realSrcPt.y)
      retImg.setColor(xDest, yDest, color)
    }

    def setColorDirect(xSrc : Int, ySrc : Int, xDest : Int, yDest : Int) {
      retImg.setColor(xDest,yDest,origImg.getColor(xSrc, ySrc))
    }

    val setColor : (Int,Int,Int,Int) => Unit =
      if (mapping.size == 0) setColorDirect
      else setColorFromMapping

    for (x <- 0 until newWidth) {
      var newY = 0

      for (y <- 0 until origImg.height) {
        setColor(x,y,x,newY)
        newY += 1

        if (horzSeamPoints.contains(new Point(x,y))) {
          setColor(x,y,x,newY)
          newY += 1
        }
      }
    }

    retImg
  }

  def getSeamRemovedImage(imgUtils : ImageUtils, origImg : Image,
                          vertSeamPoints : Set[Point], horzSeamPoints : Set[Point]) : Image = {

    // width after seams have been removed
    val testWidth = origImg.width - (vertSeamPoints.size / origImg.height)

    val newWidth = testWidth
    val newHeight = origImg.height - (horzSeamPoints.size / newWidth)

    val newImg = imgUtils.createImage(newWidth, newHeight)
    getSeamRemovedImage(origImg, newImg, vertSeamPoints, horzSeamPoints)
  }

  /**
   * renders origImg to newImg excluding points from vertical seams in vertSeamPoints and points from horizontal seams
   * in horzSeamPoints
   *
   * NOTE: for this to work properly, vertical seams must be removed prior to any horizontal seams being removed
   * 			also, the new image must be of the proper size (i.e. newImg.width = origImg.width - # of vertical seams)
   */
  def getSeamRemovedImage(
      origImg : Image, newImg : Image,
      vertSeamPoints : Set[Point], horzSeamPoints : Set[Point]) : Image = {

    if (SeamConstants.DEBUG)
      seamInvariantCheck(origImg, newImg, vertSeamPoints, horzSeamPoints)

    // the y position at any given x position in new image
    val yNew = new Array[Int](newImg.width)
    // 0 it out
    for (x <- 0 until newImg.width)
      yNew(x) = 0

    // the x position for original image
    var xNew = 0

    for (y <- 0 until origImg.height) {
      xNew = 0

      for (x <- 0 until origImg.width) {
        val thisPt = new Point(x, y)
        (vertSeamPoints.contains(thisPt), horzSeamPoints.contains(thisPt)) match {
          // if this point is not a part of a vertical seam or horizontal seam draw at new location
          // and increment both x and y because something was drawn there
          case (false, false) =>
            newImg.setColor(xNew, yNew(xNew), origImg.getColor(x, y))
            yNew(xNew) += 1
            xNew += 1
          // if a vertical seam, wait for the next x
          // (treat combo vertical & horizontal seams as vertical only)
          case (true, true) => println("combo point")
          case (true, false) => ()
          // if horizontal seam, wait for appropriate element in next y row
          // and skip this element in the new image for now
          case (false, true) =>
            xNew += 1

        }
      }
    }
    newImg
  }


  def getDrawnSeamAnim(origImg : Image, imgUtils : ImageUtils, seams : Seq[Set[Point]], color : Color) : List[ImagePointer] = {
    val (_, imgPtrLst) =
      seams.foldRight((origImg, List[ImagePointer]()))({
        (thisSeam, prevTuple) =>
          val (lastImg, imgPtrs) = prevTuple
          val newImg = imgUtils.copyImg(lastImg)
          drawSeam(newImg, color, thisSeam)

          (newImg, imgPtrs:+imgUtils.createImagePointer(newImg))
      })

    imgPtrLst
  }


  // TODO fix this
  def getSeamRemoveAddAnim(
      origImg : Image, imgUtils : ImageUtils,
      vertSeams : Seq[Set[Point]], horzSeams : Seq[Set[Point]],
      seamRemoval : Boolean) : List[ImagePointer] = {

    // fold over the vertical seams and create images where the seam is removed
    val (lastWidth, vertSeamPts, vertImgPtrs) =
      vertSeams.foldRight(origImg.width, new HashSet[Point](), List[ImagePointer]())({
        (thisSeam, prevTuple) =>
          // extract previous image width, all current seam pts, and image pointers accrued
          val (prevWidth, prevSeamPts, imgPtrs) = prevTuple

          val newWidth = prevWidth - 1
          // add in these seam pts
          prevSeamPts ++= thisSeam

          // create a new image and pass along
          val newImg = {
            val newImg = imgUtils.createImage(newWidth, origImg.height)
            getSeamRemovedImage(origImg, newImg, prevSeamPts, Set[Point]())
          }

          (newWidth, prevSeamPts, imgPtrs:+imgUtils.createImagePointer(newImg))
      })

    // do the same for horizontal seams
    val (_, _, imgPtrs) =
      // start with last image and last set of anim pictures
      horzSeams.foldRight((origImg.height, new HashSet[Point](), vertImgPtrs))({
        (thisSeam, prevTuple) =>
          // extract previous image height, all current horizontal seam pts, and image pointers accrued
          val (prevHeight, prevHorzSeamPts, imgPtrs) = prevTuple

          val newHeight = prevHeight - 1
          prevHorzSeamPts ++= thisSeam

          val newImg = {
            val newImg = imgUtils.createImage(lastWidth, newHeight)
            getSeamRemovedImage(origImg, newImg, vertSeamPts, prevHorzSeamPts)
          }

          (newHeight, prevHorzSeamPts, imgPtrs:+imgUtils.createImagePointer(newImg))
      })

    imgPtrs
  }
}



object Resizer {
  // new ratio represents height/width
	private def vertSeamsToRemove(imgHeight : Integer,
		  imgWidth : Integer, newRatio : Double, seamRemoval : Boolean) : Integer =
		// height = constant * width
    seamRemoval match {
		  case false => Math.max(0, ((newRatio * imgWidth).toInt) - imgHeight)
		  case true => Math.max(0, imgHeight - (newRatio * imgWidth).toInt)
    }

	private def horzSeamsToRemove(imgHeight : Integer,
			imgWidth : Integer, newRatio : Double, seamRemoval : Boolean) : Integer =
		//   width = height / constant
    seamRemoval match {
  		case false => Math.max(0, ((imgHeight / newRatio).toInt) - imgWidth)
  		case true => Math.max(0, imgWidth - ((imgHeight / newRatio).toInt))
	  }
}

class Resizer(
    imgUtils : ImageUtils, img: Image,
    targetHeight : Integer, targetWidth : Integer,
		maxEnergy : Double,
    vertSeamNum : Integer,horzSeamNum : Integer,
		seamRemoval : Boolean) {

  val energyRetriever = new EnergyRetriever(imgUtils, img)

  // gather vertical and horizontal seams
  val (vertSeams, vertSeamPts, horzSeams, horzSeamPts, insertionMapping) = {
    val energyMap = energyRetriever.getEnergyMap
    val upperLeftNode = SeamCarver.getPixelNodes(energyMap)
    val totVertSeamEnergy = maxEnergy * img.height
    val (vertSeams, vertUpperLeft) = SeamCarver.getVertSeams(upperLeftNode, totVertSeamEnergy, vertSeamNum)

    val vertSeamPts = vertSeams.foldRight(new HashSet[Point]())({ (pts, set) => set++=pts })

    val (horzSeams, mapping) =
      if (seamRemoval) {
        val totHorzSeamEnergy = maxEnergy * img.width
        val (horzSeams, _) = SeamCarver.getHorzSeams(vertUpperLeft, totHorzSeamEnergy, horzSeamNum)
        (horzSeams, None)
      }
      else {
        val totHorzSeamEnergy = maxEnergy * img.width
        val (newUpperLeftNode, mapping) = SeamCarver.insertVertSeams(energyMap, vertSeamPts)
        val (horzSeams, _) = SeamCarver.getHorzSeams(newUpperLeftNode, totHorzSeamEnergy, horzSeamNum)
        (horzSeams, Some(mapping))
      }

    val horzSeamPts = horzSeams.foldRight(new HashSet[Point]())({ (pts, set) => set++=pts })

    (vertSeams, vertSeamPts, horzSeams, horzSeamPts, mapping)
  }


  // do not do any resizing of image once seams have been removed
	def this(imgUtils : ImageUtils, img : Image, maxEnergy : Double, vertSeamNum : Integer, horzSeamNum : Integer,
       seamRemoval : Boolean) =
		this(imgUtils, img, null, null, maxEnergy, vertSeamNum, horzSeamNum, seamRemoval)

  // remove default proportion vertically and horizontally
	def this(imgUtils : ImageUtils, img : Image) =
    this(imgUtils, img, SeamConstants.SEAM_DEFAULT_MAX_SCORE,
      (SeamConstants.SEAM_DEFAULT_MAX_VERT_PROPORTION * img.width).toInt,
      (SeamConstants.SEAM_DEFAULT_MAX_HORZ_PROPORTION * img.height).toInt,
      true)

  // resizes image by only removing seams and enforces that the image will have a height to width ratio as specified
	def this(imgUtils : ImageUtils, img : Image, heightToWidthRatio : Double, seamRemoval : Boolean) =
	  this(imgUtils, img, SeamConstants.SEAM_DEFAULT_MAX_SCORE,
			Resizer.horzSeamsToRemove(img.height, img.width, heightToWidthRatio, seamRemoval),
			Resizer.vertSeamsToRemove(img.height, img.width, heightToWidthRatio, seamRemoval),
      seamRemoval)


  var _finalImg : Option[Image] = None

	/**
	 * @return		returns the finished image
	 */
	def getFinalImage : Image = {
    _finalImg match {
      case None =>
        val finalImg = seamRemoval match {
          case true => SeamVisualization.getSeamRemovedImage(imgUtils, img, vertSeamPts, horzSeamPts)
          case false => SeamVisualization.getSeamInsertionImage(imgUtils, img, horzSeamPts, insertionMapping.get)
        }

        _finalImg = Some(finalImg)
        finalImg
      case Some(finalImg) => finalImg
    }
  }

  var _animPics : Option[Array[ImagePointer]] = None

	/**
	 * @return		returns Files pointing to images animating the resize
	 */
	def getAnimPics = {
    _animPics match {
      case None =>
        val pics = {
          val (animWidth, animHeight) =
            if (seamRemoval) (img.width, img.height)
            else (getFinalImage.width, getFinalImage.height)
          
          def letterbox(toBox : Image) : Image =
            imgUtils.giveEdges(toBox, Color.BLACK, animWidth, animHeight, CenterPosition())
          
         def letterboxArr(imgPtrs : Seq[ImagePointer], doLetterbox : Boolean) = 
           if (doLetterbox) imgPtrs.map({ imgPtr => imgUtils.createImagePointer(letterbox(imgPtr.load)) })
           else imgPtrs
             
          val energyPics = letterboxArr(energyRetriever.getAnimPics, !seamRemoval).toArray
          
          val drawnSeamAnim = {
            val seamDrawing = SeamVisualization.getDrawnSeamAnim(
              img, imgUtils, vertSeams++horzSeams, SeamConstants.DEFAULT_SEAM_COLOR)
            letterboxArr(seamDrawing, !seamRemoval)
          }
          
          val seamRemovalAnim = {
            val seamRemovalPics = SeamVisualization.getSeamRemoveAddAnim(img, imgUtils, vertSeams, horzSeams, seamRemoval)
            letterboxArr(seamRemovalPics, true)
          }
          
          val finalImages = imgUtils.createManyImagePointers(letterbox(getFinalImage), 60)
          
          energyPics ++ drawnSeamAnim ++ seamRemovalAnim ++ finalImages
        }
        
       _animPics = Some(pics)
       pics
      case Some(pics) => pics
    }
	}
}