package loderunner

import com.badlogic.gdx.{InputProcessor, Gdx, ApplicationListener}
import com.badlogic.gdx.graphics.{GL10, OrthographicCamera}
import com.badlogic.gdx.math.{Vector2, Rectangle}
import com.badlogic.gdx.utils.GdxNativesLoader
import com.badlogic.gdx.utils.{Array => GdxArray}

import scala.collection.JavaConversions._
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

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

  val DEBUG = true

  def boxToWorld(value: Float): Float = {
    value * BOX_TO_WORLD
  }

  def worldToBox(value: Float): Float = {
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

  var renderer: ShapeRenderer = null
  val entities: GdxArray[Entity] = new GdxArray[Entity]()

  val clearColor: Array[Float] = Array(0.2f, 0.2f, 0.2f)

  def create() {
    log("Creating game")
    log("Creating orthographic camera")
    camera = new OrthographicCamera()
    camera.viewportHeight = width
    camera.viewportWidth = height
    //    camera.zoom = Main.WORLD_TO_BOX
    //    camera.zoom = 0.5f
    camera.update()

    renderer = new ShapeRenderer()

    log("Setting clear color: %.2f %.2f %.2f".format(clearColor(0), clearColor(1), clearColor(2)))
    gl.glClearColor(clearColor(0), clearColor(1), clearColor(2), 1.0f)

    log("Creating some static blocks")
    for (i <- 0 to (width / 10).asInstanceOf[Int]) {
      entities.add(new StaticBlock(width / 10.0f * i, 20, width / 10.0f, 20))
    }

    log("Making the player")
    entities.add(new Player(0, 80))

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
            //            log("Unhandled key, checking entities. KEY=" + keycode)
            entities.iterator().foldLeft(false)((accum, ent) => {
              //              log("Sending keycode to: " + ent)
              if (ent.key(keycode, true)) {
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
            //            log("Unhandled key, checking entities. KEY=" + keycode)
            entities.iterator().foldLeft(false)((accum, ent) => {
              //              log("Sending keycode to: " + ent)
              if (ent.key(keycode, false)) {
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
    entities.iterator().foreach((ent) => {
      ent.update(worldStep)
    })

    // Well this is kind of a big fustercluck, but whatever. If it works, I couldn't care less.
    // Also: this has worst-case performance of O(n^2) if all entities are COLLISION_DYNAMIC/etc. A lot of
    //  collisions should be culled though due to not doing static-to-static collision, ignoring NONE, etc..
    val colliding = new GdxArray[Vector2]
    val collidingType = new GdxArray[Int]
    for (i <- 0 to entities.size - 1) {
      val ent = entities.get(i)
      colliding.clear()
      if (ent.collisionType == Entity.COLLISION_DYNAMIC) {
        for (j <- 0 to entities.size - 1) {
          val ent2 = entities.get(j)
          if (ent2 != ent && ent2.collisionType != Entity.COLLISION_NONE) {
            val rect = ent.rectangle()
            val rect2 = ent2.rectangle()
            if (rect.overlaps(rect2)) {
              //              log("Overlaps passed: " + ent + ", " + ent2)
              val bottom1 = rect.getY
              val top1 = bottom1 + rect.getHeight
              val left1 = rect.getX
              val right1 = left1 + rect.getWidth

              val bottom2 = rect2.getY
              val top2 = bottom2 + rect2.getHeight
              val left2 = rect2.getX
              val right2 = rect2.getWidth

              val topBottom = top2 - bottom1
              val bottomTop = top1 - bottom2
              val rightLeft = right1 - left2
              val leftRight = right2 - left1
              val topBottomAbs = Math.abs(topBottom)
              val bottomTopAbs = Math.abs(bottomTop)
              val rightLeftAbs = Math.abs(rightLeft)
              val leftRightAbs = Math.abs(leftRight)

              if (bottomTopAbs < topBottomAbs) {
                if (leftRightAbs < rightLeftAbs) {
                  if (bottomTopAbs < leftRightAbs) {
                    colliding.add(new Vector2(0, bottomTop))
                  }
                  else {
                    colliding.add(new Vector2(leftRight, 0))
                  }
                }
                else {
                  if (bottomTopAbs < rightLeftAbs) {
                    colliding.add(new Vector2(0, bottomTop))
                  }
                  else {
                    colliding.add(new Vector2(rightLeft, 0))
                  }
                }
              }
              else {
                if (leftRightAbs < rightLeftAbs) {
                  if (topBottomAbs < leftRightAbs) {
                    colliding.add(new Vector2(0, topBottom))
                  }
                  else {
                    colliding.add(new Vector2(leftRight, 0))
                  }
                }
                else {
                  if (topBottomAbs < rightLeftAbs) {
                    colliding.add(new Vector2(0, topBottom))
                  }
                  else {
                    colliding.add(new Vector2(rightLeft, 0))
                  }
                }
              }
              collidingType.add(ent2.collisionType)

              //              if (bottom1 < top2) {
              //                if (right1 > left2) {
              //                  if (Math.abs(topBottom) < Math.abs(rightLeft)) {
              //                    colliding.add(new Vector2(0, topBottom))
              //                    collidingType.add(ent2.collisionType)
              //                  }
              //                  else {
              //                    colliding.add(new Vector2(rightLeft, 0))
              //                    collidingType.add(ent2.collisionType)
              //                  }
              //                }
              //              }
            }
          }
        }
      }
      if (colliding.size > 0) {
        ent.onCollision(colliding, collidingType)
      }
    }

    if (Main.DEBUG) {
      renderer.begin(ShapeRenderer.ShapeType.Rectangle)
      entities.iterator().foreach((ent) => {
        val pos = ent.position()
        val size = ent.size()
        renderer.rect(pos.x, pos.y, size.x, size.y)
      })
      renderer.end()
    }
  }

  def pause() {
  }

  def resume() {
  }

  def dispose() {
  }
}

object Entity {
  val COLLISION_NONE: Int = 0
  val COLLISION_STATIC: Int = 1
  val COLLISION_DYNAMIC: Int = 2
  val COLLISION_TREASURE: Int = 3
  val COLLISION_LADDER: Int = 4
  val COLLISION_ENEMY: Int = 5
}

trait Entity {
  def update(dt: Float)

  def key(keyCode: Int, pressed: Boolean): Boolean

  def position(): Vector2

  def size(): Vector2

  def rectangle(): Rectangle

  def collisionType: Int

  def onCollision(collisions: GdxArray[Vector2], collisionTypes: GdxArray[Int])
}

trait MovingEntity {
  val rect: Rectangle
  val accelerationScale: Float
  val velocity: Vector2 = new Vector2
  val acceleration: Vector2 = new Vector2

  def move(x: Float, y: Float) {
    rect.setX(rect.getX + x)
    rect.setY(rect.getY + y)
  }

  def updateMove(dt: Float) {
    velocity.add(acceleration.x * dt * accelerationScale, acceleration.y * dt * accelerationScale)
    move(velocity.x * dt, velocity.y * dt)
  }
}

class Player(x: Float, y: Float) extends Entity with MovingEntity {

  import Utils.log

  val VEL: Float = 100.0f
  val JUMP_VEL: Float = 290.0f
  var jumping = false
  val rect = new Rectangle(x, y, 20.0f, 40.0f)
  acceleration.y = -9.81f * 70.0f
  val accelerationScale: Float = 1

  override def key(keyCode: Int, pressed: Boolean): Boolean = {
    import com.badlogic.gdx.Input.Keys
    keyCode match {
      case Keys.LEFT => {
        pressed match {
          case true => {
            log("Moving left")
            velocity.x -= VEL
          }
          case false => {
            log("Finished moving left")
            velocity.x += VEL
          }
        }
        true
      }
      case Keys.RIGHT => {
        pressed match {
          case true => {
            log("Moving right")
            velocity.x += VEL
          }
          case false => {
            log("Finished moving right")
            velocity.x -= VEL
          }
        }
        true
      }
      case Keys.SPACE => {
        if (pressed) {
          if (!jumping) {
            log("Jumping")
            jumping = true
            velocity.y = JUMP_VEL
          }
          true
        }
        else {
          false
        }
      }
      case _ => false
    }
  }

  override def update(dt: Float) {
    updateMove(dt)
  }

  def position(): Vector2 = new Vector2(rect.getX, rect.getY)

  def size(): Vector2 = new Vector2(rect.getWidth, rect.getHeight)

  def rectangle(): Rectangle = new Rectangle(rect)

  def collisionType: Int = Entity.COLLISION_DYNAMIC

  def onCollision(collisions: GdxArray[Vector2], collisionTypes: GdxArray[Int]) {
    //    log("Collision: " + collisions)
    var x = Float.MaxValue
    var y = Float.MaxValue
    for (i <- 0 to collisions.size - 1) {
      val vec = collisions.get(i)
      if (Math.abs(vec.x) < Math.abs(x)) {
        x = vec.x
      }
      if (Math.abs(vec.y) < Math.abs(y)) {
        y = vec.y
      }
    }
    if (y > 0) {
      jumping = false
      velocity.y = 0
    }
    move(x, y)
  }
}

class StaticBlock(x: Float, y: Float, width: Float, height: Float) extends Entity with MovingEntity {
  val rect: Rectangle = new Rectangle(x, y, width, height)
  val accelerationScale: Float = 0

  def update(dt: Float) {}

  def key(keyCode: Int, pressed: Boolean): Boolean = false

  def position(): Vector2 = new Vector2(rect.getX, rect.getY)

  def size(): Vector2 = new Vector2(rect.getWidth, rect.getHeight)

  def rectangle(): Rectangle = new Rectangle(rect)

  def collisionType: Int = Entity.COLLISION_STATIC

  def onCollision(collisions: GdxArray[Vector2], collisionTypes: GdxArray[Int]) {}
}
