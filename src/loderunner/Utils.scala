package loderunner

import com.badlogic.gdx.{ApplicationListener, Gdx}
import com.badlogic.gdx.graphics.{FPSLogger, Texture}
import com.badlogic.gdx.utils.GdxNativesLoader
import com.badlogic.gdx.backends.lwjgl.{LwjglApplication, LwjglApplicationConfiguration}
import scala.util.Random

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 4/14/13
 * Time: 7:06 PM
 */
object Utils {
  var LOG: String = "DEFAULT"
  val fpsLooger = new FPSLogger
  var numLogs = 0
  val GC_AFTER = 60
  val GC_TIMES = 3

  val rand = new Random()

  def newGame(game: ApplicationListener, width: Int, height: Int) {
    val config = new LwjglApplicationConfiguration
    config.title = "Game Test"
    config.useGL20 = true
    config.width = width
    config.height = height
    config.resizable = false
    config.vSyncEnabled = true
    new LwjglApplication(game, config)
  }

  def checkGc() {
    numLogs += 1
    if(numLogs > GC_AFTER) {
      numLogs = 0
      for(i <- 0 to 2) {
        System.gc()
      }
    }
  }

  val out = (str: String) => {
    val trace = Thread.currentThread().getStackTrace
    val stack = trace(3)
    println(LOG + ": [OUT] " + stack.getClassName + "." + stack.getMethodName + "(" + stack.getLineNumber + "): " + str)
    checkGc()
  }
  val log = (str: String) => {
    val trace = Thread.currentThread().getStackTrace
    val stack = trace(3)
    Gdx.app.log(LOG, "[LOG] " + stack.getClassName + "." + stack.getMethodName + "(" + stack.getLineNumber + "): " + str)
    checkGc()
  }
  val error = (str: String) => {
    val trace = Thread.currentThread().getStackTrace
    val stack = trace(3)
    Gdx.app.error(LOG, "[ERROR] " + stack.getClassName + "." + stack.getMethodName + "(" + stack.getLineNumber + "): " + str)
    checkGc()
  }
  val debug = (str: String) => {
    val trace = Thread.currentThread().getStackTrace
    val stack = trace(3)
    Gdx.app.debug(LOG, "[DEBUG] " + stack.getClassName + "." + stack.getMethodName + "(" + stack.getLineNumber + "): " + str)
    checkGc()
  }
  val file = (str: String) => Gdx.files.internal(str)
  val getTexture = (str: String) => new Texture(Gdx.files.internal("assets/starassault/" + str))
}
