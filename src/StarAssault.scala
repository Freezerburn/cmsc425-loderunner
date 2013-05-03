import com.badlogic.gdx.backends.lwjgl.{LwjglApplicationConfiguration, LwjglApplication}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.graphics.{Texture, FPSLogger, OrthographicCamera}
import com.badlogic.gdx.math.{Rectangle, Vector2}
import com.badlogic.gdx._

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 4/4/13
 * Time: 5:32 PM
 */
object StarAssault {
  val fpsLogger = new FPSLogger;
  val LOG = getClass.getSimpleName;
  val WIDTH = 640
  val HEIGHT = 480

  val out = (str: String) => println(LOG + ": " + str)
  val log = (str: String) => Gdx.app.log(LOG, str)
  val error = (str: String) => Gdx.app.error(LOG, str)
  val debug = (str: String) => Gdx.app.debug(LOG, str)
  val file = (str: String) => Gdx.files.internal(str)
  val getTexture = (str: String) => new Texture(Gdx.files.internal(str))

  def main(args: Array[String]) {
    val config = new LwjglApplicationConfiguration
    config.title = "Game Test"
    config.useGL20 = true
    config.width = StarAssault.WIDTH
    config.height = StarAssault.HEIGHT
    config.resizable = false
    config.vSyncEnabled = true
    new LwjglApplication(new StarAssault, config)
  }
}
class StarAssault extends Game {
  import StarAssault._
  def create() {
    out("Running game with --DEBUG-- log level")
    Gdx.app.setLogLevel(Application.LOG_DEBUG)
    debug("Creating game")

    setScreen(new GameScreen)
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
  }

  override def setScreen(screen: Screen) {
    debug("Setting screen to: " + screen.getClass.getSimpleName)
    super.setScreen(screen);
  }
}

object Block {
  val SIZE = 1f;
}
class Block(pos: Vector2) {
  val position = new Vector2(pos)
  val bounds = new Rectangle(0, 0, Block.SIZE, Block.SIZE)
}

object Bob {
  object State extends Enumeration {
    type State = Value
    val IDLE, WALKING, JUMPING, DYING = Value
  }

  val SPEED = 2f
  val JUMP_VELOCITY = 1f
  val SIZE = 0.5f
}
class Bob(position: Vector2) {
  val pos = new Vector2(position)
  val acceleration = new Vector2
  val velocity = new Vector2
  val bounds = new Rectangle(0, 0, Bob.SIZE, Bob.SIZE)
  var state = Bob.State.IDLE
  var facingLeft = true
}

class World {
  import com.badlogic.gdx.utils.{Array => GdxArr}

  val blocks = new GdxArr[Block]()
  val bob = new Bob(new Vector2(7, 2))

  for (i <- 0 to 9) {
    blocks.add(new Block(new Vector2(i, 0)))
    blocks.add(new Block(new Vector2(i, 7)))
    if(i > 2) {
      blocks.add(new Block(new Vector2(i, 1)))
    }
  }
  for(i <- 2 to 5) {
    blocks.add(new Block(new Vector2(9, i)))
  }
  for(i <- 3 to 5) {
    blocks.add(new Block(new Vector2(6, i)))
  }
}
class WorldRenderer {
  val world = new World;
  val cam = new OrthographicCamera(10, 7)
  cam.position.set(5, 3.5f, 0)
  cam.update()

  val debugRenderer = new ShapeRenderer()

  def render {
    val j = 1
    debugRenderer.setProjectionMatrix(cam.combined)
    debugRenderer.begin(ShapeType.Rectangle)
    debugRenderer.setColor(1, 0, 0, 1)
    for(i <- 0 to (world.blocks.size - 1)) {
      val block = world.blocks.get(i)
      val rect = block.bounds
      val x1 = block.position.x + rect.x
      val y1 = block.position.y + rect.y
      debugRenderer.rect(x1, y1, rect.width, rect.height)
    }
    val bob = world.bob
    val rect = bob.bounds
    val x1 = bob.pos.x + rect.x
    val y1 = bob.pos.y + rect.y
    debugRenderer.setColor(0, 1, 0, 1)
    debugRenderer.rect(x1, y1, rect.width, rect.height)
    debugRenderer.end()
  }
}

class GameScreen extends Screen {
  var world: World = null;
  var renderer: WorldRenderer = null;

  def render(delta: Float) {
    import Gdx.gl
    import com.badlogic.gdx.graphics.GL10.GL_COLOR_BUFFER_BIT
    gl.glClearColor(0.1f, 0.1f, 0.1f, 1)
    gl.glClear(GL_COLOR_BUFFER_BIT)
    renderer.render
  }

  def resize(width: Int, height: Int) {}

  def show() {
    world = new World
    renderer = new WorldRenderer
  }

  def hide() {}

  def pause() {}

  def resume() {}

  def dispose() {}
}

