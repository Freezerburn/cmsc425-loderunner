package loderunner

import com.badlogic.gdx._
import com.badlogic.gdx.graphics.{Color, OrthographicCamera}
import com.badlogic.gdx.math.{Vector2, Rectangle}
import com.badlogic.gdx.utils.GdxNativesLoader
import com.badlogic.gdx.utils.{Array => GdxArray}

import scala.collection.JavaConversions._
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.g2d.TextureRegion

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

class Main extends Game with ApplicationListener {

  import Utils._
  import Gdx.gl

  var width = Main.WIDTH
  var height = Main.HEIGHT
  var camera: OrthographicCamera = null

  val worldGravity = new Vector2(0, -10)
  val worldStep = 1 / 60.0f

  var renderer: ShapeRenderer = null
  val entities: GdxArray[Entity] = new GdxArray[Entity]()
  val removeLater: GdxArray[Entity] = new GdxArray[Entity]()
  val addLater: GdxArray[Entity] = new GdxArray[Entity]()

  val levels: GdxArray[Level] = new GdxArray[Level]()
  levels.add(new LevelDebug)
  var currentLevelIndex: Int = 0
  var currentLevel: Level = null

  val clearColor: Array[Float] = Array(0.2f, 0.2f, 0.2f)

  var score: Long = 0L
  var scoreMultiplier: Float = 1.0f

  def create() {
    log("Creating game")
    log("Creating orthographic camera")
    camera = new OrthographicCamera()
    camera.viewportHeight = width
    camera.viewportWidth = height
    camera.update()

    renderer = new ShapeRenderer()

    log("Setting clear color: %.2f %.2f %.2f".format(clearColor(0), clearColor(1), clearColor(2)))
    gl.glClearColor(clearColor(0), clearColor(1), clearColor(2), 1.0f)

    currentLevel = levels.get(currentLevelIndex)
    log("Setting current level to level " + currentLevelIndex + ", " + currentLevel)
    setScreen(currentLevel)
    Gdx.input.setInputProcessor(currentLevel)
  }

  override def resize(width: Int, height: Int) {
    this.width = width
    this.height = height
    camera.viewportHeight = width
    camera.viewportWidth = height
    //    camera.zoom = Main.BOX_TO_WORLD
    camera.update()
  }

  def addScore(score: Long) {
    this.score += Math.round(score * scoreMultiplier)
    log("Added %d (x%.2f) score to get %d total".format(score, this.scoreMultiplier, this.score))
  }
}

object Entity {
  val COLLISION_NONE: Int = 0
  val COLLISION_STATIC: Int = 1
  val COLLISION_PLAYER: Int = 2
  val COLLISION_TREASURE: Int = 3
  val COLLISION_LADDER: Int = 4
  val COLLISION_ENEMY: Int = 5
}

trait Entity {
  var isDestroyed = false

  def update(dt: Float)

  def getSprite: TextureRegion

  def destroy() {
    Main.instance.currentLevel.removeLater.add(this)
    isDestroyed = true
  }

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

  def collisionType: Int = Entity.COLLISION_PLAYER

  def onCollision(collisions: GdxArray[Vector2], collisionTypes: GdxArray[Int]) {
    var x = Float.MaxValue
    var y = Float.MaxValue
    for (i <- 0 to collisions.size - 1) {
      val vec = collisions.get(i)
      val colType = collisionTypes.get(i)
      colType match {
        case Entity.COLLISION_STATIC => {
          if (vec.x != 0 && Math.abs(vec.x) < Math.abs(x)) {
            x = vec.x
          }
          if (vec.y != 0 && Math.abs(vec.y) < Math.abs(y)) {
            y = vec.y
          }
        }
        case Entity.COLLISION_TREASURE => {
          log("Collided with treasure")
          val foo = Main.instance.entities.indexOf(this, true)
          log("Player has index: " + foo)
        }
        case _ => Unit
      }
    }

    if (x == Float.MaxValue) {
      x = 0
    }
    if (y == Float.MaxValue) {
      y = 0
    }
    if (y > 0) {
      if (velocity.y < 0) {
        jumping = false
        velocity.y = 0
      }
    }

    move(x, y)
  }

  def getSprite: TextureRegion = ???
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

  def getSprite: TextureRegion = ???
}

object Treasure {
  val WIDTH = 20.0f
  val HEIGHT = 20.0f
  val SCORE = 100L
}

class Treasure(x: Float, y: Float) extends Entity {
  val pos = new Vector2(x, y)

  def update(dt: Float) {}

  def key(keyCode: Int, pressed: Boolean): Boolean = false

  def position(): Vector2 = new Vector2(pos)

  def size(): Vector2 = new Vector2(Treasure.WIDTH, Treasure.HEIGHT)

  def rectangle(): Rectangle = new Rectangle(pos.x, pos.y, Treasure.WIDTH, Treasure.HEIGHT)

  def collisionType: Int = Entity.COLLISION_TREASURE

  def onCollision(collisions: utils.Array[Vector2], collisionTypes: utils.Array[Int]) {
    for (i <- 0 to collisions.size - 1) {
      val colType = collisionTypes.get(i)
      colType match {
        case Entity.COLLISION_PLAYER => {
          Main.instance.addScore(Treasure.SCORE)
          destroy()
        }
        case _ => Unit
      }
    }
  }

  def getSprite: TextureRegion = ???
}

trait Level extends Screen with InputProcessor {
  val entities = new GdxArray[Entity]()
  val addLater = new GdxArray[Entity]()
  val removeLater = new GdxArray[Entity]()

  def load()

  def isGoalMet: Boolean

  def doCollision() {
    // Well this is kind of a big fustercluck, but whatever. If it works, I couldn't care less.
    // Also: this has worst-case performance of O(n^2) if all entities are COLLISION_DYNAMIC/etc. A lot of
    //  collisions should be culled though due to not doing static-to-static collision, ignoring NONE, etc..
    val colliding = new GdxArray[Vector2]
    val collidingType = new GdxArray[Int]
    for (i <- 0 to entities.size - 1) {
      val ent = entities.get(i)
      colliding.clear()
      if (ent.collisionType != Entity.COLLISION_STATIC && ent.collisionType != Entity.COLLISION_NONE) {
        for (j <- 0 to entities.size - 1) {
          val ent2 = entities.get(j)
          if (ent2 != ent && ent2.collisionType != Entity.COLLISION_NONE) {
            val rect = ent.rectangle()
            val rect2 = ent2.rectangle()
            if (rect.overlaps(rect2)) {
              val bottom1 = rect.getY
              val top1 = bottom1 + rect.getHeight
              val left1 = rect.getX
              val right1 = left1 + rect.getWidth

              val bottom2 = rect2.getY
              val top2 = bottom2 + rect2.getHeight
              val left2 = rect2.getX
              val right2 = left2 + rect2.getWidth

              val topBottom = top2 - bottom1
              val bottomTop = bottom2 - top1
              val rightLeft = left2 - right1
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
            }
          }
        }
      }
      if (colliding.size > 0) {
        ent.onCollision(colliding, collidingType)
      }
    }

    for (i <- 0 to removeLater.size - 1) {
      entities.removeValue(removeLater.get(i), true)
    }
    removeLater.clear()
    for (i <- 0 to addLater.size - 1) {
      entities.add(addLater.get(i))
    }
    addLater.clear()
  }

  def render(delta: Float) {
    import Gdx.gl
    import com.badlogic.gdx.graphics.GL10.GL_COLOR_BUFFER_BIT
    gl.glClear(GL_COLOR_BUFFER_BIT)
    entities.iterator().foreach((ent) => {
      ent.update(Main.instance.worldStep)
    })

    doCollision()

    val renderer = Main.instance.renderer
    if (Main.DEBUG) {
      renderer.begin(ShapeRenderer.ShapeType.Rectangle)
      entities.iterator().foreach((ent) => {
        val pos = ent.position()
        val size = ent.size()
        ent.collisionType match {
          case Entity.COLLISION_PLAYER => {
            renderer.setColor(Color.RED)
          }
          case Entity.COLLISION_TREASURE => {
            renderer.setColor(Color.YELLOW)
          }
          case _ => {
            renderer.setColor(Color.WHITE)
          }
        }
        renderer.rect(pos.x, pos.y, size.x, size.y)
      })
      renderer.end()
    }
  }

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
        Utils.log("Q pressed, quitting application")
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
}

class LevelDebug extends Level {

  import Utils.log
  import Main.{instance => game}

  def load() {
    log("Creating some static blocks")
    for (i <- 0 to (game.width / 10).asInstanceOf[Int]) {
      entities.add(new StaticBlock(game.width / 10.0f * i, 20, game.width / 10.0f, 20))
    }
    entities.add(new StaticBlock(game.width / 10.0f * 5, 50, game.width / 10.0f, 20))
    entities.add(new StaticBlock(game.width / 10.0f * 4, 90, game.width / 10.0f, 20))

    log("Creating treasure")
    entities.add(new Treasure(150, 100))

    log("Making the player")
    entities.add(new Player(0, 80))
  }

  def isGoalMet(): Boolean = true

  def resize(width: Int, height: Int) {
    log("Resized")
  }

  def show() {
    log("Shown")
    load()
  }

  def hide() {
    log("Hidden")
  }

  def pause() {
    log("Paused")
  }

  def resume() {
    log("Resumed")
  }

  def dispose() {
    log("Disposed")
  }
}