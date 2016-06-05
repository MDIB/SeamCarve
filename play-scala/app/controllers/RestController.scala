package controllers

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.Base64
import javax.imageio.ImageIO

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.gdicristofaro.seamcarve.core._
import com.gdicristofaro.seamcarve.jvm.{JVMImage, JVMImageUtils}


class RestController extends Controller {
  val imgUtils = new JVMImageUtils

  val minReader = (
    __ \ 'image).read[String]

  def minRead(body : JsValue) : Option[Resizer] = {
    body.validate[String](minReader).map{
      case(imageDataURL) =>
        Some(new Resizer(imgUtils, new JVMImage(dataURLtoImage(imageDataURL))))
    }.recoverTotal{ _ => None }
  }

  val heightWidthReader = (
    (__ \ 'image).read[String] and
      (__ \ 'heightWidthRatio).read[Double] and
      (__ \ 'seamRemoval).read[Boolean]
    )tupled

  def heightWidthRatioRead(body : JsValue) : Option[Resizer] = {
    body.validate[(String,Double,Boolean)](heightWidthReader).map{
      case(imageDataURL,heightWidthRatio,seamRemoval) =>
        Some(new Resizer(imgUtils, new JVMImage(dataURLtoImage(imageDataURL)), heightWidthRatio, seamRemoval))
    }.recoverTotal{ e =>
      None }
  }

  val seamNumReader = (
    (__ \ 'image).read[String] and
      (__ \ 'maxEnergy).read[Double] and
      (__ \ 'vertSeamNum).read[Int] and
      (__ \ 'horzSeamNum).read[Int] and
      (__ \ 'seamRemoval).read[Boolean]
    ) tupled

  def seamNumRead(body : JsValue) : Option[Resizer] = {
    body.validate[(String,Double,Int,Int,Boolean)](seamNumReader).map{
      case(imageDataURL,maxEnergy, vertSeamNum, horzSeamNum, seamRemoval) =>
        Some(new Resizer(imgUtils, new JVMImage(dataURLtoImage(imageDataURL)),
          maxEnergy, vertSeamNum, horzSeamNum, seamRemoval))
    }.recoverTotal{ _ => None }
  }

  val readerList : List[(JsValue) => Option[Resizer]] = List(seamNumRead, heightWidthRatioRead, minRead)

  def readJson(body : JsValue) : Option[Resizer] = {
    def _read(body : JsValue, lst : List[(JsValue) => Option[Resizer]]) : Option[Resizer] = {
      lst match {
        case f::tl =>
          f(body) match {
            case Some(r) => Some(r)
            case None => _read(body,tl)
          }
        case _ => None
      }
    }

    _read(body, readerList)
  }

  def dataURLtoImage(dataUrl: String) : BufferedImage = {
    // taken from http://stackoverflow.com/questions/19743851/base64-java-encode-and-decode-a-string
    val imagedata = Base64.getDecoder.decode(dataUrl.substring(dataUrl.indexOf(",") + 1))
    ImageIO.read(new ByteArrayInputStream(imagedata))
  }

  def imgToDataURL(img : BufferedImage) : String = {
    // taken from http://stackoverflow.com/questions/19743851/base64-java-encode-and-decode-a-string
    val out = new ByteArrayOutputStream
    ImageIO.write(img, "PNG", out)
    val bytes = out.toByteArray
    val base64bytes = Base64.getEncoder.encodeToString(bytes)
    "data:image/png;base64," + base64bytes
  }

  // gets images as json object
  def getImgs(resizer : Resizer) : JsValue = {
    JsObject(Seq(
      "finalImage" -> JsString(imgToDataURL(resizer.getFinalImage.asInstanceOf[JVMImage].bufferedImage)),
      "energyImage" -> JsString(imgToDataURL(resizer.energyRetriever.getImage.asInstanceOf[JVMImage].bufferedImage)),
      "seamImage" -> JsString(imgToDataURL(resizer.getSeamImage.asInstanceOf[JVMImage].bufferedImage))
    ))
  }

  // gets the video as an array of images
  def getVideo(resizer : Resizer) : JsValue = {
    val dataUrlLst = resizer.getAnimPics.map{
      (imgPtr) =>
        JsString(
          imgToDataURL(
            imgPtr.load.asInstanceOf[JVMImage].bufferedImage))
    }

    JsArray(dataUrlLst)
  }

  // entry for receiving pictures requests
  def seamCarvePics = Action(parse.json(maxLength = 1024 * 1024 * 10)) { request =>
    readJson(request.body) match {
      case Some(resizer) => Ok(getImgs(resizer))
      case None => BadRequest("Invalid JSON Arguments")
    }
  }

  // entry for receiving video requests
  def seamCarveVideo = Action(parse.json(maxLength = 1024 * 1024 * 10)) { request =>
    readJson(request.body) match {
      case Some(resizer) => Ok(getVideo(resizer))
      case None => BadRequest("Invalid JSON Arguments")
    }
  }
}
