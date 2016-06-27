package controllers

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.Base64
import javax.imageio.ImageIO
import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.gdicristofaro.seamcarve.core._
import com.gdicristofaro.seamcarve.jvm.{JVMImage, JVMImageUtils}
import play.api.Logger
import play.api.libs.streams.ActorFlow


// methods for parsing json requests and creating json response
object JSONProcessing {
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

  def readStr(body : String) : Option[Resizer] = readJson(Json.parse(body))

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
}


// WebSocket pictures creator actor
object PicWSActor {
  def props(out: ActorRef) = Props(new PicWSActor(out))
}

class PicWSActor(out: ActorRef) extends Actor {
  // the first message from the client is going to be the content and the next messages will be pings
  // to keep the socket alive
  var initialMessage = true
  var pingNum = 1

  def receive = {
    case msg: String =>
      if (initialMessage) {
        initialMessage = false
        Logger.info("received initial message")
        JSONProcessing.readStr(msg) match {
          case Some(resizer) => out ! Json.stringify((JSONProcessing.getImgs(resizer)))
          case None => out ! ("Invalid JSON Arguments")
        }
      }
      else {
        Logger.info(s" received ping #(${pingNum}) of (${msg})")
        pingNum += 1
      }
  }

  override def postStop() = {
    Logger.info("client disconnected")
  }
}

class RestController @Inject() (implicit system: ActorSystem, materializer: Materializer) extends Controller {

  def seamCarvePics = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => PicWSActor.props(out))
  }


  /*
  // entry for receiving pictures requests
  def seamCarvePics = Action(parse.json(maxLength = 1024 * 1024 * 10)) { request =>
    readJson(request.body) match {
      case Some(resizer) => Ok(getImgs(resizer))
      case None => BadRequest("Invalid JSON Arguments")
    }
  }
  */
}