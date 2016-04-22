import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.raw.DragEvent
import org.scalajs.dom.document
import org.scalajs.dom.raw.Event
import org.scalajs.dom.raw.MouseEvent
import org.scalajs.dom.raw.XMLHttpRequest
import org.scalajs.dom
import org.scalajs.dom.raw.FileReader
import scala.scalajs.js.annotation.JSExport
import org.scalajs.dom.raw.FileList

// borrowed heavily from http://www.sitepoint.com/html5-file-drag-and-drop/ and https://github.com/hussachai/play-scalajs-showcase
object FileUpload {
  def $id(s: String) = dom.document.getElementById(s).asInstanceOf[HTMLElement]

  def output(msg: String) = {
    val m = $id("messages")
    m.innerHTML = msg + m.innerHTML
  }  
  
  def fileDragHover(e: dom.Event) = {
    e.stopPropagation()
    e.preventDefault()
    e.target.asInstanceOf[HTMLElement].className = if (e.`type` == "dragover") "hover" else ""
  }
  
      def handleFiles(files : FileList) {
       (0 until files.length).foreach{ i =>
          handleFile(files(i))
      }
    }

    def fileSelectHandler(e: dom.Event) = {
      fileDragHover(e)
      val files = e.asInstanceOf[dom.DragEvent].dataTransfer.files
      handleFiles(files)
    }
    


    def handleFile(file: dom.File) = {
      output(
        s"""
          |<p>File information: <strong>${file.name}</strong>
          | type: <strong>${file.`type`}</strong>
          | size: <strong>${file.size}</strong> bytes</p>
        """.stripMargin)
      val reader = new FileReader()

      if(file.`type`.indexOf("image") != -1) {
        reader.onload = (e: dom.UIEvent) => {
          output(
            s"""
              |<p><strong>${file.name}:</strong><br />
              |<img src="${reader.result}"/></p>
            """.stripMargin)
        }
        reader.readAsDataURL(file)
      }else if(file.`type`.indexOf("text") != -1){
        reader.onload = (e: dom.UIEvent) => {
          output(
            s"""
              |<p><strong>${file.name}:</strong></p>
              |<pre>${reader.result}</pre>
            """.stripMargin)
        }
        reader.readAsText(file)
      }
    }

  @JSExport
  def init {
	  $id("fileSelect").addEventListener("change", handleFiles _, false)

	  val xhr = new dom.XMLHttpRequest
	  if(xhr.upload != null){
		  val fileDrag = $id("fileDrag")
				  fileDrag.addEventListener("dragover", fileDragHover _, false)
				  fileDrag.addEventListener("dragleave", fileDragHover _, false)
				  fileDrag.addEventListener("drop", fileSelectHandler _, false)


				  fileDrag.style.display = "block";

		  // remove submit button
		  $id("submitButton").style.display = "none";
	  }
  }


}