package com.gdicristofaro.seamcarve.jvm

import java.io.File
import javax.imageio.ImageIO

import com.gdicristofaro.seamcarve.core.{Color, Point, Resizer, SeamVisualization}

object Testing {
  val input = "/Users/gregdicristofaro/Desktop/background.png"
  val output = "/Users/gregdicristofaro/Desktop/output.png"
  val outputvid = "/Users/gregdicristofaro/Desktop/output.mp4"
  
  def main(args : Array[String]) {
    //seamInsertionTest
    //seamRemovalTest(false)
    //seamRemovalVideo
    seamInsertionVideo
    //jvmImageTest
  }


  def seamRemovalVideo(): Unit = {
    val imgUtils = new JVMImageUtils
    val startTime = System.currentTimeMillis()
    val r = new Resizer(imgUtils, new JVMImage(ImageIO.read(new File(input))), 1, 20, 20, true)
    imgUtils.outputVideo(r.getAnimPics, 30, outputvid)
    val endTime = System.currentTimeMillis()
    val timeDiff = (endTime - startTime).toDouble / 1000
    println(s"it took $timeDiff seconds")
  }

  def seamInsertionVideo(): Unit = {
    val imgUtils = new JVMImageUtils
    val startTime = System.currentTimeMillis()
    val r = new Resizer(imgUtils, new JVMImage(ImageIO.read(new File(input))), 1, 20, 20, false)
    imgUtils.outputVideo(r.getAnimPics, 30, outputvid)
    val endTime = System.currentTimeMillis()
    val timeDiff = (endTime - startTime).toDouble / 1000
    println(s"it took $timeDiff seconds")
  }

  def seamInsertionTest() {
    val imgUtils = new JVMImageUtils
    val startTime = System.currentTimeMillis()
    val r = new Resizer(imgUtils, new JVMImage(ImageIO.read(new File(input))), 1, 50, 50, false)
    ImageIO.write(r.getFinalImage.asInstanceOf[JVMImage].bufferedImage, "PNG", new File(output))
    val endTime = System.currentTimeMillis()
    val timeDiff = (endTime - startTime).toDouble / 1000
    println(s"it took $timeDiff seconds")

  }

  def seamRemovalTest(drawSeams : Boolean) {
    val imgUtils = new JVMImageUtils
    val startTime = System.currentTimeMillis()
    val origImg = new JVMImage(ImageIO.read(new File(input)))
    val r = new Resizer(imgUtils, origImg, 1, 50, 50, true)
    val img =
      if (drawSeams) {
        SeamVisualization.drawSeam(origImg, Color.RED, List[Point]() ++ r.vertSeamPts ++ r.horzSeamPts)
        origImg
      }
      else {
        r.getFinalImage
      }

    ImageIO.write(img.asInstanceOf[JVMImage].bufferedImage, "PNG", new File(output))
    val endTime = System.currentTimeMillis()
    val timeDiff = (endTime - startTime).toDouble / 1000
    println(s"it took $timeDiff seconds")
  }

  def jvmImageTest() {
    val imgUtils = new JVMImageUtils
    val img = imgUtils.createImage(1, 1)
    val col = new Color(0xFF, 0x0, 0xFF) 
    img.setColor(0,0,col)

    
    ImageIO.write(img.asInstanceOf[JVMImage].bufferedImage, "PNG", new File(output))
  }
}