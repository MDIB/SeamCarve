package testing

import com.gdicristofaro.seamcarve.Resizer
import javax.imageio.ImageIO
import java.io.File

object Main {
  def main(args:Array[String]) {
    val inputPath = "/Users/gregdicristofaro/Desktop/profile.jpg"
    val outputPath = "/Users/gregdicristofaro/Desktop/profile(out).jpg"
    val resizer = new Resizer(ImageIO.read(new File(inputPath)))
    ImageIO.write(resizer.getFinalImage, "PNG", new File(outputPath))
  }
}