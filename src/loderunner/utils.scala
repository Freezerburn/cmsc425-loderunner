package loderunner

import com.badlogic.gdx.{ApplicationListener, Gdx}
import com.badlogic.gdx.graphics.{FPSLogger, Texture}
import com.badlogic.gdx.utils.GdxNativesLoader
import com.badlogic.gdx.backends.lwjgl.{LwjglApplication, LwjglApplicationConfiguration}

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 4/14/13
 * Time: 7:06 PM
 */
object Utils {
  var LOG: String = "DEFAULT"
  val fpsLooger = new FPSLogger

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

  val out = (str: String) => {
    val trace = Thread.currentThread().getStackTrace
    val stack = trace(3)
    println(LOG + ": [OUT] " + stack.getClassName + "." + stack.getMethodName + "(" + stack.getLineNumber + "): " + str)
  }
  val log = (str: String) => {
    val trace = Thread.currentThread().getStackTrace
    val stack = trace(3)
    Gdx.app.log(LOG, "[LOG] " + stack.getClassName + "." + stack.getMethodName + "(" + stack.getLineNumber + "): " + str)
  }
  val error = (str: String) => {
    val trace = Thread.currentThread().getStackTrace
    val stack = trace(3)
    Gdx.app.error(LOG, "[ERROR] " + stack.getClassName + "." + stack.getMethodName + "(" + stack.getLineNumber + "): " + str)
  }
  val debug = (str: String) => {
    val trace = Thread.currentThread().getStackTrace
    val stack = trace(3)
    Gdx.app.debug(LOG, "[DEBUG] " + stack.getClassName + "." + stack.getMethodName + "(" + stack.getLineNumber + "): " + str)
  }
  val file = (str: String) => Gdx.files.internal(str)
  val getTexture = (str: String) => new Texture(Gdx.files.internal("assets/starassault/" + str))
}
