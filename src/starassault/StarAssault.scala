import com.badlogic.gdx.backends.lwjgl.{LwjglApplication, LwjglApplicationConfiguration}
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx._
import com.badlogic.gdx.graphics.g2d.{Animation, TextureRegion, SpriteBatch}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.graphics.{OrthographicCamera, Texture, FPSLogger}
import com.badlogic.gdx.math.{Rectangle, Vector2}
import com.badlogic.gdx.utils.GdxNativesLoader
import java.util

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 4/14/13
 * Time: 5:25 PM
 */
class StarAssault extends Game {
  var screen: GameScreen = null
  val inputProcessor = new InputProcessor {
    def keyTyped(character: Char): Boolean = false

    def mouseMoved(screenX: Int, screenY: Int): Boolean = false

    def keyDown(keycode: Int): Boolean = {
      keycode match {
        case Keys.Q => Gdx.app.exit(); true
        case _ => screen.keyDown(keycode)
      }
    }

    def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean =
      screen.touchDown(screenX, screenY, pointer, button)

    def keyUp(keycode: Int): Boolean = screen.keyUp(keycode)

    def scrolled(amount: Int): Boolean = screen.scrolled(amount)

    def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false

    def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false
  }

  def create() {
    StarAssault.game = this
//    out("Running game with --DEBUG-- log level")
    Gdx.app.setLogLevel(Application.LOG_DEBUG)
//    debug("Creating game")

    screen = new GameScreen
    setScreen(screen)
  }

  def show() {
    Gdx.input.setInputProcessor(inputProcessor)
  }

  def hide() {
    Gdx.input.setInputProcessor(null)
  }

  override def setScreen(screen: Screen) {
//    debug("Setting screen to: " + screen.getClass.getSimpleName)
    super.setScreen(screen);
  }
}

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 4/4/13
 * Time: 5:32 PM
 */
object StarAssault {
  var game: StarAssault = null
  val fpsLogger = new FPSLogger
  val LOG = getClass.getSimpleName
  val WIDTH = 640
  val HEIGHT = 480
  val FPS = 60
  val GRAVITY = -20f

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

  def main(args: Array[String]) {
    GdxNativesLoader.load()
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

object Block {
  val SIZE = 1f;
}

class Block(var position: Vector2) {
  position = new Vector2(position)
  val bounds = new Rectangle(0, 0, Block.SIZE, Block.SIZE)

  val mass = Float.PositiveInfinity
  val static = true
}

object Bob {
  object State extends Enumeration {
    type State = Value
    val IDLE, WALKING, JUMPING, DYING = Value
  }

  val SPEED = 4f
  val JUMP_VELOCITY = 1f
  val SIZE = 0.5f
}

class Bob(var position: Vector2) {
  position = new Vector2(position)
  val bounds = new Rectangle(0, 0, Bob.SIZE, Bob.SIZE)
  var state = Bob.State.IDLE
  var facingLeft = true
  var stateTime = 0.0f
  val velocity = new Vector2()

  val mass: Float = 1
  val static = false

  def update(dt: Float) {
    stateTime += dt
    position.add(velocity.tmp().mul(dt))
  }

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

object WorldRenderer {
  val CADENCE = 180
  val NUM_FRAMES = 5
  val RUNNING_FRAME_DURATION: Float = 1.0f / (CADENCE / StarAssault.FPS * NUM_FRAMES)
}

class WorldRenderer(val world: World, var debug: Boolean) {
  import StarAssault.getTexture
  val cam = new OrthographicCamera(10, 7)
  cam.position.set(5, 3.5f, 0)
  cam.update()
  val spriteBatch = new SpriteBatch()

  val debugRenderer = new ShapeRenderer()
  val bobIdleTexture = getTexture("bob_01.png")
  val blockTexture = getTexture("block.png")
  val bobWalkTextures = 2.to(6).foldLeft(Array[Texture]())((arr, i) => {
    arr :+ getTexture("bob_0" + i + ".png")
  })

  val bobIdleLeftRegion = new TextureRegion(bobIdleTexture)
  val bobIdleRightRegion = new TextureRegion(bobIdleTexture)
  bobIdleRightRegion.flip(true, false)
  val bobWalkLeftRegions: Array[TextureRegion] = bobWalkTextures.map((tex) => {
    new TextureRegion(tex)
  })
  val bobWalkRightRegions: Array[TextureRegion] = bobWalkTextures.map((tex) => {
    val ret = new TextureRegion(tex)
    ret.flip(true, false)
    ret
  })
  import com.badlogic.gdx.utils.{Array => GdxArray}
  val bobWalkLeftAnimation = new Animation(WorldRenderer.RUNNING_FRAME_DURATION, new GdxArray[TextureRegion](bobWalkLeftRegions))
  val bobWalkRightAnimation = new Animation(WorldRenderer.RUNNING_FRAME_DURATION, new GdxArray[TextureRegion](bobWalkRightRegions))

  def render() {
    val j = 1
    if(debug) {
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
      val x1 = bob.position.x + rect.x
      val y1 = bob.position.y + rect.y
      debugRenderer.setColor(0, 1, 0, 1)
      debugRenderer.rect(x1, y1, rect.width, rect.height)
      debugRenderer.end()
    }
    val bob = world.bob
    var frame = if(bob.facingLeft) bobIdleLeftRegion else bobIdleRightRegion
    if(bob.state == (Bob.State.WALKING)) {
      frame = if(bob.facingLeft) bobWalkLeftAnimation.getKeyFrame(bob.stateTime, true) else bobWalkRightAnimation.getKeyFrame(bob.stateTime, true)
    }
    spriteBatch.setProjectionMatrix(cam.combined)
    spriteBatch.begin()
    spriteBatch.draw(frame, bob.position.x, bob.position.y, Bob.SIZE, Bob.SIZE)
    spriteBatch.end()
  }
}

object WorldController {
  object Keys extends Enumeration {
    type Keys = Value
    val LEFT, RIGHT, JUMP, FIRE = Value
  }

  val LONG_JUMP_PRESS = 150l
  val ACCELERATION = 20f
  val MAX_JUMP_SPEED = 7f
  val DAMP = 0.9f
  val MAX_VEL = 4f

  import WorldController.Keys._
  val keys = new util.HashMap[WorldController.Keys.Value, Boolean]
  keys.put(LEFT, false)
  keys.put(RIGHT, false)
  keys.put(JUMP, false)
  keys.put(FIRE, false)
}

class WorldController(val world: World) {
  import WorldController.Keys._
  val bob = world.bob

  def leftPressed() {
    WorldController.keys.get(WorldController.keys.put(LEFT, true))
    bob.facingLeft = true
    bob.velocity.x -= Bob.SPEED
  }
  def rightPressed() {
    WorldController.keys.get(WorldController.keys.put(RIGHT, true))
    bob.facingLeft = false
    bob.velocity.x += Bob.SPEED
  }
  def jumpPressed() = {
    WorldController.keys.get(WorldController.keys.put(JUMP, true))
  }
  def firePressed() = {
    WorldController.keys.get(WorldController.keys.put(FIRE, true))
  }

  def leftReleased() {
    WorldController.keys.get(WorldController.keys.put(LEFT, false))
    bob.state = Bob.State.IDLE
    bob.velocity.x += Bob.SPEED
  }
  def rightReleased() {
    WorldController.keys.get(WorldController.keys.put(RIGHT, false))
    bob.state = Bob.State.IDLE
    bob.velocity.x -= Bob.SPEED
  }
  def jumpReleased() = {
    WorldController.keys.get(WorldController.keys.put(JUMP, false))
  }
  def fireReleased() = {
    WorldController.keys.get(WorldController.keys.put(FIRE, false))
  }

  def update(dt: Float) {
    if(bob.velocity.x == 0) {
      bob.state = Bob.State.IDLE
    }
    else if(bob.velocity.x > 0) {
      bob.state = Bob.State.WALKING
      bob.facingLeft = false
    }
    else if(bob.velocity.x < 0) {
      bob.state = Bob.State.WALKING
      bob.facingLeft = true
    }
    bob.update(dt)
  }
}

class GameScreen extends Screen with InputProcessor {
  import StarAssault.log
  var world: World = null
  var renderer: WorldRenderer = null
  var controller: WorldController = null

  def render(delta: Float) {
    import com.badlogic.gdx.graphics.GL10.GL_COLOR_BUFFER_BIT
    Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1)
    Gdx.gl.glClear(GL_COLOR_BUFFER_BIT)
    controller.update(delta)
    renderer.render()
    StarAssault.fpsLogger.log()
  }

  def resize(width: Int, height: Int) {}

  def show() {
    log("Showing GameScreen")
    log("Creating world")
    world = new World
    log("Creating renderer")
    renderer = new WorldRenderer(world, true)
    log("Creating controller")
    controller = new WorldController(world)
    StarAssault.game.show()
  }

  def hide() {
    log("Hiding GameScreen")
    StarAssault.game.hide()
  }

  def pause() {}

  def resume() {}

  def dispose() {
  }

  def keyDown(keycode: Int): Boolean = {
    import com.badlogic.gdx.Input.Keys
    keycode match {
      case Keys.LEFT => controller.leftPressed(); true
      case Keys.RIGHT => controller.rightPressed(); true
      case Keys.Z => controller.jumpPressed(); true
      case Keys.X => controller.firePressed(); true
      case _ => false
    }
  }

  def keyUp(keycode: Int): Boolean = {
    import com.badlogic.gdx.Input.Keys
    keycode match {
      case Keys.LEFT => controller.leftReleased(); true
      case Keys.RIGHT => controller.rightReleased(); true
      case Keys.Z => controller.jumpReleased(); true
      case Keys.X => controller.fireReleased(); true
      case _ => false
    }
  }

  def keyTyped(character: Char): Boolean = false

  def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false

  def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = false

  def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = false

  def mouseMoved(screenX: Int, screenY: Int): Boolean = false

  def scrolled(amount: Int): Boolean = false
}