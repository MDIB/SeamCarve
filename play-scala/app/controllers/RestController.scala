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

  def minRead(body : JsValue) : Option[Result] = {
    body.validate[String](minReader).map{
      case(imageDataURL) =>
        getImgResp(new Resizer(imgUtils, new JVMImage(dataURLtoImage(imageDataURL))))
    }.recoverTotal{ _ => None }
  }

  val heightWidthReader = (
    (__ \ 'image).read[String] and
      (__ \ 'heightWidthRatio).read[Double] and
      (__ \ 'seamRemoval).read[Boolean]
    )tupled

  def heightWidthRatioRead(body : JsValue) : Option[Result] = {
    body.validate[(String,Double,Boolean)](heightWidthReader).map{
      case(imageDataURL,heightWidthRatio,seamRemoval) =>
        getImgResp(new Resizer(imgUtils, new JVMImage(dataURLtoImage(imageDataURL)), heightWidthRatio, seamRemoval))
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

  def seamNumRead(body : JsValue) : Option[Result] = {
    body.validate[(String,Double,Int,Int,Boolean)](seamNumReader).map{
      case(imageDataURL,maxEnergy, vertSeamNum, horzSeamNum, seamRemoval) =>
        getImgResp(new Resizer(imgUtils, new JVMImage(dataURLtoImage(imageDataURL)),
          maxEnergy, vertSeamNum, horzSeamNum, seamRemoval))
    }.recoverTotal{ _ => None }
  }

  val readerList : List[(JsValue) => Option[Result]] = List(seamNumRead, heightWidthRatioRead, minRead)

  def readJson(body : JsValue) : Option[Result] = {
    def _read(body : JsValue, lst : List[(JsValue) => Option[Result]]) : Option[Result] = {
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
      "energyImage" -> JsString(imgToDataURL(resizer.energyRetriever.getImage.asInstanceOf[JVMImage].bufferedImage))
    ))
  }

  // streamlines getting from resizer to a response option
  def getImgResp(resizer : Resizer) : Option[Result] = Some(Ok(getImgs(resizer).toString))

  def getVideo(resizer : Resizer) : JsValue = {
    val dataUrlLst = resizer.getAnimPics.map{
      (imgPtr) =>
        JsString(
          imgToDataURL(
            imgPtr.load.asInstanceOf[JVMImage].bufferedImage))
    }

    JsArray(dataUrlLst)
  }

  // request entry
  def seamCarve = Action(parse.json(maxLength = 1024 * 1024 * 10)) { request =>
    readJson(request.body) match {
      case Some(status) =>
        status
      case None => BadRequest("Invalid JSON Arguments")
    }
  }
}
