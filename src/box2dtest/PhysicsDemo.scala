package box2dtest

import com.badlogic.gdx.{InputProcessor, Gdx, ApplicationListener}
import com.badlogic.gdx.backends.lwjgl.{LwjglApplicationConfiguration, LwjglApplication}
import com.badlogic.gdx.physics.box2d._
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.graphics.{GL10, OrthographicCamera}
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import com.badlogic.gdx.utils.GdxNativesLoader

/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 4/14/13
 * Time: 5:26 PM
 */
object PhysicsDemo {
  val WIDTH = 640
  val HEIGHT = 480

  val BOX_STEP = 1 / 60.0f
  val BOX_VELOCITY_ITERATIONS = 6
  val BOX_POSITION_ITERATIONS = 2
  val WORLD_TO_BOX = 0.01f
  val BOX_WORLD_TO = 100f

  def main(args: Array[String]) {
    GdxNativesLoader.load()
    val config = new LwjglApplicationConfiguration
    config.title = "Game Test"
    config.useGL20 = true
    config.width = PhysicsDemo.WIDTH
    config.height = PhysicsDemo.HEIGHT
    config.resizable = false
    config.vSyncEnabled = true
    new LwjglApplication(new PhysicsDemo, config)
  }
}
class PhysicsDemo extends ApplicationListener {
  var debugRenderer: Box2DDebugRenderer = null
  var camera: OrthographicCamera = null
  var circle: Body = null

  val world = new World(new Vector2(0, -100), true)

  def create() {
    // Input
    import com.badlogic.gdx.Input.Keys
    Gdx.input.setInputProcessor(new InputProcessor {
      def keyTyped(character: Char): Boolean = {
        false
      }

      def mouseMoved(screenX: Int, screenY: Int): Boolean = {
        false
      }

      def keyDown(keycode: Int): Boolean = {
        keycode match {
          case Keys.Q => Gdx.app.exit(); true
          case Keys.LEFT => {
            val center = circle.getWorldCenter
            circle.applyLinearImpulse(-500, 0, center.x, center.y)
            println("Apply force")
            true
          }
          case Keys.RIGHT => circle.applyForceToCenter(5, 0); true
          case _ => false
        }
      }

      def touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean = {
        false
      }

      def keyUp(keycode: Int): Boolean = {
        keycode match {
          case Keys.LEFT => circle.applyForceToCenter(5, 0); true
          case Keys.RIGHT => circle.applyForceToCenter(-5, 0); true
          case _ => false
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

    // Camera
    camera = new OrthographicCamera()
    camera.viewportHeight = PhysicsDemo.HEIGHT
    camera.viewportWidth = PhysicsDemo.WIDTH
    camera.position.set(PhysicsDemo.WIDTH * 0.5f, PhysicsDemo.HEIGHT * 0.5f, 0)
    camera.update()

    // Ground body
    val groundBodyDef = new BodyDef()
    groundBodyDef.`type` = BodyType.StaticBody
    groundBodyDef.position.set(new Vector2(0, 10))
    val groundBody = world.createBody(groundBodyDef)

    val groundBox = new PolygonShape()
    groundBox.setAsBox(camera.viewportWidth * 2.0f, 10.0f)
    groundBody.createFixture(groundBox, 0.0f)

    // Dynamic body
    val bodyDef = new BodyDef()
    bodyDef.`type` = BodyType.DynamicBody
    bodyDef.position.set(camera.viewportWidth * 0.5f, camera.viewportHeight * 0.5f)
    circle = world.createBody(bodyDef)
    val dynamicCircle = new CircleShape()
    dynamicCircle.setRadius(50f)
    val fixtureDef = new FixtureDef
    fixtureDef.shape = dynamicCircle
    fixtureDef.density = 0.001f
    fixtureDef.friction = 0.0f
    fixtureDef.restitution = 1.0f
    circle.createFixture(fixtureDef)

    debugRenderer = new Box2DDebugRenderer()
  }

  def resize(width: Int, height: Int) {
  }

  def render() {
    Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT)
    debugRenderer.render(world, camera.combined)
    println(circle.getPosition)
    world.step(PhysicsDemo.BOX_STEP, PhysicsDemo.BOX_VELOCITY_ITERATIONS, PhysicsDemo.BOX_POSITION_ITERATIONS)
  }

  def pause() {
  }

  def resume() {
  }

  def dispose() {
  }
}
