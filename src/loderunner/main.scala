package loderunner

import com.badlogic.gdx.{InputProcessor, Gdx, ApplicationListener}
import com.badlogic.gdx.graphics.{GL10, OrthographicCamera}
import com.badlogic.gdx.physics.box2d._
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.GdxNativesLoader
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.badlogic.gdx.utils.{Array => GdxArray}

import scala.collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 4/14/13
 * Time: 7:06 PM
 */

object Main {
  import loderunner.Utils._

  val WIDTH = 640
  val HEIGHT = 480

  val WORLD_TO_BOX = 0.01f
  val BOX_TO_WORLD = 100.0f

  def boxToWorld(value: Float):Float = {
    value * BOX_TO_WORLD
  }

  def worldToBox(value: Float):Float = {
    value * WORLD_TO_BOX
  }

  var instance: Main = null
  def main(args: Array[String]) {
    GdxNativesLoader.load()
    instance = new Main
    newGame(instance, WIDTH, HEIGHT)
  }
}
class Main extends ApplicationListener {
  import Utils._
  import Gdx.gl
  import GL10._
  var width = Main.WIDTH
  var height = Main.HEIGHT
  var camera: OrthographicCamera = null

  val worldGravity = new Vector2(0, -10)
  val worldStep = 1 / 60.0f
  val worldVelocitySteps = 6
  val worldPositionSteps = 2
  val world = new World(worldGravity, true)
  var renderer: Box2DDebugRenderer = null

  val bodies: GdxArray[StaticBox] = new GdxArray[StaticBox]()
  var circle: DynamicCircle = null
  var box: DynamicBox = null
  val entities: GdxArray[Entity] = new GdxArray[Entity]()

  val clearColor: Array[Float] = Array(0.2f, 0.2f, 0.2f)

  def create() {
    log("Creating game")
    log("Creating orthographic camera")
    camera = new OrthographicCamera()
    camera.viewportHeight = width
    camera.viewportWidth = height
    camera.zoom = Main.WORLD_TO_BOX
//    camera.zoom = 0.5f
    camera.update()

    renderer = new Box2DDebugRenderer()

    log("Setting clear color: %.2f %.2f %.2f".format(clearColor(0), clearColor(1), clearColor(2)))
    gl.glClearColor(clearColor(0), clearColor(1), clearColor(2), 1.0f)

    log("Creating some physics objects")
//    log("Making body: " + (0, 0, 20, 20))
//    bodies.add(new StaticBox(0, 0, 20, 20))

    log("Making body: " + (40, -60, 70, 20))
    bodies.add(new StaticBox(40, -60, 70, 20))
    log("Body 2 position: " + bodies.get(0).position)

    log("Making body: " + (-90, -160, 70, 20))
    bodies.add(new StaticBox(-90, -160, 70, 20))
    log("Body 3 position: " + bodies.get(1).position)

    log("Making body: " + (-90, -160, 70, 20))
    bodies.add(new StaticBox(0, -260, 800, 20))
    log("Body 3 position: " + bodies.get(2).position)

//    log("Making dynamic circle")
//    circle = new DynamicCircle(0, 40, 10)
//    circle.body.applyForceToCenter(Main.worldToBox(20), 0)

//    log("Making dynamic box")
//    box = new DynamicBox(0, 40, 10, 10)

    log("Making the player")
    entities.add(new Player(0, 40))

    log("Setting input listener to custom one")
    val force = 180
    Gdx.input.setInputProcessor(new InputProcessor {
      def keyTyped(character: Char): Boolean = {
        false
      }

      def mouseMoved(screenX: Int, screenY: Int): Boolean = {
        false
      }

      def keyDown(keycode: Int): Boolean = {
        import com.badlogic.gdx.Input.Keys
        keycode match {
          case Keys.Q => {
            log("Q pressed, quitting application")
            Gdx.app.exit()
            true
          }
          case _ => {
            log("Unhandled key, checking entities. KEY=" + keycode)
            entities.iterator().foldLeft(false)((accum, ent) => {
              log("Sending keycode to: " + ent)
              if(ent.keyDown(keycode)) {
                return true
              }
              false
            })
          }
        }
      }

      def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
        false
      }

      def keyUp(keycode: Int): Boolean = {
        keycode match {
          case _ => {
            log("Unhandled key, checking entities. KEY=" + keycode)
            entities.iterator().foldLeft(false)((accum, ent) => {
              log("Sending keycode to: " + ent)
              if(ent.keyUp(keycode)) {
                return true
              }
              false
            })
          }
        }
      }

      def scrolled(amount: Int): Boolean = {
        false
      }

      def touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
        false
      }

      def touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean = {
        false
      }
    })
  }

  def resize(width: Int, height: Int) {
    this.width = width
    this.height = height
    camera.viewportHeight = width
    camera.viewportWidth = height
//    camera.zoom = Main.BOX_TO_WORLD
    camera.update()
  }

  def render() {
    gl.glClear(GL_COLOR_BUFFER_BIT)
    renderer.render(world, camera.combined)
    entities.iterator().foreach((ent) => {
      ent.update(worldStep)
    })
    world.step(worldStep, worldVelocitySteps, worldPositionSteps)
  }

  def pause() {
  }

  def resume() {
  }

  def dispose() {
    world.dispose()
  }
}

trait Entity {
  def update(dt: Float)
  def keyDown(keyCode: Int):Boolean
  def keyUp(keyCode: Int):Boolean
}

class Player(x: Float, y: Float) extends Entity {
  import Utils.log
  private val width = 10.0f
  private val height = 20.0f
  private val force = new Vector2()
  private val FORCE = 100.0f

  private val bodyDef = new BodyDef
  bodyDef.`type` = BodyType.DynamicBody
  bodyDef.position.set(Main.worldToBox(x), Main.worldToBox(y))
  bodyDef.fixedRotation = true
  val body = Main.instance.world.createBody(bodyDef)

  private var box = new PolygonShape()
  box.setAsBox(Main.worldToBox(width), Main.worldToBox(height))
  private val fixtureDef = new FixtureDef
  fixtureDef.shape = box
  fixtureDef.density = 1
  fixtureDef.friction = 0.90f
  fixtureDef.restitution = 0

  private val fixture = body.createFixture(fixtureDef)
  box.dispose()
  box = null

  override def keyDown(keyCode: Int):Boolean = {
    import com.badlogic.gdx.Input.Keys
    keyCode match {
      case Keys.LEFT => {
        log("Applying force left")
        body.applyLinearImpulse(Main.worldToBox(-FORCE / 14.0f), 0, 0, 0)
        force.add(Main.worldToBox(-FORCE), 0)
        true
      }
      case Keys.RIGHT => {
        log("Applying force right")
        body.applyLinearImpulse(Main.worldToBox(FORCE / 14.0f), 0, 0, 0)
        force.add(Main.worldToBox(FORCE), 0)
        true
      }
      case Keys.SPACE => {
        log("Jumping (single impulse up)")
        body.applyLinearImpulse(0, Main.worldToBox(20), 0, 0)
        true
      }
      case _ => false
    }
  }

  override def keyUp(keyCode: Int):Boolean = {
    import com.badlogic.gdx.Input.Keys
    keyCode match {
      case Keys.LEFT => {
        force.add(Main.worldToBox(FORCE), 0)
        body.applyLinearImpulse(Main.worldToBox(FORCE) / 10.0f, 0, 0, 0)
        true
      }
      case Keys.RIGHT => {
        force.add(Main.worldToBox(-FORCE), 0)
        body.applyLinearImpulse(Main.worldToBox(-FORCE) / 10.0f, 0, 0, 0)
        true
      }
      case _ => false
    }
  }

  def update(dt: Float) {
    if(Math.abs(body.getLinearVelocity.x) < Main.worldToBox(200)) {
      body.applyForceToCenter(force)
    }
  }
}

class StaticBox(x: Float, y: Float, width: Float, height: Float) {
  private val bodyDef = new BodyDef()
  bodyDef.`type` = BodyType.StaticBody
  bodyDef.position.set(Main.worldToBox(x), Main.worldToBox(y))

  val body = Main.instance.world.createBody(bodyDef)

  private var box = new PolygonShape()
  box.setAsBox(Main.worldToBox(width / 2.0f), Main.worldToBox(height / 2.0f))

  private val fixture = body.createFixture(box, 0.0f)
  box.dispose()
  box = null

  def position: Vector2 = {
    body.getPosition.cpy().mul(Main.BOX_TO_WORLD)
  }
}

class DynamicCircle(x: Float, y: Float, radius: Float) {
  private val bodyDef = new BodyDef
  bodyDef.`type` = BodyType.DynamicBody
  bodyDef.position.set(Main.worldToBox(x), Main.worldToBox(y))

  val body = Main.instance.world.createBody(bodyDef)

  private var circle = new CircleShape()
  circle.setRadius(Main.worldToBox(radius))

  private val fixtureDef = new FixtureDef
  fixtureDef.shape = circle
  fixtureDef.density = 1
  fixtureDef.friction = 0.5f
  fixtureDef.restitution = 0.7f

  private val fixture = body.createFixture(fixtureDef)
  circle.dispose()
  circle = null
}

class DynamicBox(x: Float, y: Float, width: Float, height: Float) {
  private val bodyDef = new BodyDef
  bodyDef.`type` = BodyType.DynamicBody
  bodyDef.position.set(Main.worldToBox(x), Main.worldToBox(y))
  bodyDef.fixedRotation = true
  val body = Main.instance.world.createBody(bodyDef)

  private var box = new PolygonShape()
  box.setAsBox(Main.worldToBox(width), Main.worldToBox(height))
  private val fixtureDef = new FixtureDef
  fixtureDef.shape = box
  fixtureDef.density = 1
  fixtureDef.friction = 0.90f
  fixtureDef.restitution = 0

  private val fixture = body.createFixture(fixtureDef)
  box.dispose()
  box = null
}