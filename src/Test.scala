
/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 4/3/13
 * Time: 10:39 AM
 */

package com.unlockeddoors.main {

  import com.unlockeddoors.stuff.TempVars
  import com.badlogic.gdx.backends.lwjgl.{LwjglApplicationConfiguration, LwjglApplication}
  import com.badlogic.gdx.graphics.g2d.SpriteBatch
  import com.badlogic.gdx._
  import com.badlogic.gdx.graphics.{OrthographicCamera, Texture, FPSLogger}
  import com.badlogic.gdx.math.{Vector2, Rectangle}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType

object StartingPoint {
    def main(args: Array[String]) {
      val config = new LwjglApplicationConfiguration
      config.title = "Game Test"
      config.useGL20 = true
      config.width = MyGame.WIDTH
      config.height = MyGame.HEIGHT
      config.resizable = false
      config.vSyncEnabled = true
      new LwjglApplication(new MyGame, config)
    }
  }

  object MyGame {
    val fpsLogger = new FPSLogger;
    val LOG = getClass.getSimpleName;
    val WIDTH = 640
    val HEIGHT = 480

    val log = (str: String) => Gdx.app.log(LOG, str)
    val error = (str: String) => Gdx.app.error(LOG, str)
    val debug = (str: String) => Gdx.app.debug(LOG, str)
    val file = (str: String) => Gdx.files.internal(str)
    val getTexture = (str: String) => new Texture(Gdx.files.internal(str))
  }

  class MyGame extends ApplicationListener {

    import MyGame.{log, getTexture}

    private var ticks: Long = 0;

    private var testTex: Texture = null
    private val rect = new Rectangle()
    rect.width = 200
    rect.height = 200

    private var batch: SpriteBatch = null
    private var camera: OrthographicCamera = null

    def create() {
      Gdx.app.setLogLevel(Application.LOG_DEBUG)
      testTex = getTexture("splash.png")
      batch = new SpriteBatch()
      camera = new OrthographicCamera()
      camera.setToOrtho(false, MyGame.WIDTH, MyGame.HEIGHT)
      Gdx.input.setInputProcessor(new InputProcessor {
        def keyTyped(character: Char): Boolean = false

        def mouseMoved(screenX: Int, screenY: Int): Boolean = false

        def keyDown(keycode: Int): Boolean = {
          import Input.Keys
          keycode match {
            case Keys.Q => Gdx.app.exit(); true
            case _ => false
          }
        }

        def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false

        def keyUp(keycode: Int): Boolean = false

        def scrolled(amount: Int): Boolean = false

        def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false

        def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false
      })
      log("Creating game")
    }

    def resize(width: Int, height: Int) {
      log("Resizing game: " + width + ", " + height)
    }

    def render() {
      import Gdx.gl
      import com.badlogic.gdx.graphics.GL10.GL_COLOR_BUFFER_BIT
      import Input.Keys
      gl.glClearColor(0.2f, 0.2f, 0.2f, 1)
      gl.glClear(GL_COLOR_BUFFER_BIT)

      camera.update()
      batch.setProjectionMatrix(camera.combined)
      batch.begin()
      batch.draw(testTex, rect.x, rect.y, rect.width, rect.height)
      batch.end()

      if (Gdx.input.isKeyPressed(Keys.LEFT)) {
        rect.x -= 200 * Gdx.graphics.getDeltaTime
      }
      if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
        rect.x += 200 * Gdx.graphics.getDeltaTime
      }
      if (Gdx.input.isTouched()) {
        TempVars.use((vars) => {
          val touchPos = vars.vect1
          touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0)
          camera.unproject(touchPos)
          rect.x = touchPos.x - 48 / 2
        })
      }
    }

    def pause() {
      log("Pausing game")
    }

    def resume() {
      log("Resuming game")
    }

    def dispose() {
      log("Disposing game")
      testTex.dispose()
    }
  }

}

