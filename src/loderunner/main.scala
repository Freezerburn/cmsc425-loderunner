package loderunner

import com.badlogic.gdx._
import com.badlogic.gdx.graphics.{FPSLogger, Color, OrthographicCamera}
import com.badlogic.gdx.math.{Vector3, Vector2, Rectangle}
import com.badlogic.gdx.utils.GdxNativesLoader
import com.badlogic.gdx.utils.{Array => GdxArray}

import scala.collection.JavaConversions._
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.g2d.{SpriteBatch, BitmapFont, TextureRegion}
import java.util

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

  val DEBUG = true

  val BLOCK_SIZE = Player.HEIGHT

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
  var spriteRenderer: SpriteBatch = null
  val entities: GdxArray[Entity] = new GdxArray[Entity]()
  val removeLater: GdxArray[Entity] = new GdxArray[Entity]()
  val addLater: GdxArray[Entity] = new GdxArray[Entity]()

  val gameOverLevel = new GameOver
  val levels: GdxArray[Level] = new GdxArray[Level]()
  levels.add(new LevelDebug)
  levels.add(new LevelOne)
  var currentLevelIndex: Int = 0
  var currentLevel: Level = null

  val clearColor: Array[Float] = Array(0.2f, 0.2f, 0.2f)

  var score: Long = 0L
  var scoreMultiplier: Float = 1.0f

  def create() {
    log("Creating game")
    resize(Main.WIDTH, Main.HEIGHT)
//    log("Creating orthographic camera")
//    camera = new OrthographicCamera(2f * (Main.WIDTH / Main.HEIGHT), 2f)
//    camera.viewportHeight = width
//    camera.viewportWidth = height
//    camera.tick()

    renderer = new ShapeRenderer()
    spriteRenderer = new SpriteBatch()

    log("Setting clear color: %.2f %.2f %.2f".format(clearColor(0), clearColor(1), clearColor(2)))
    import com.badlogic.gdx.graphics.GL10
    gl.glClearColor(clearColor(0), clearColor(1), clearColor(2), 1.0f)
    gl.glEnable(GL10.GL_BLEND)
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)

    currentLevel = levels.get(currentLevelIndex)
    log("Setting current level to level " + currentLevelIndex + ", " + currentLevel)
    //    setScreen(gameOverLevel)
    //    Gdx.input.setInputProcessor(gameOverLevel)
    setScreen(currentLevel)
    Gdx.input.setInputProcessor(currentLevel)
  }

  override def resize(width: Int, height: Int) {
    this.width = width
    this.height = height
    camera = new OrthographicCamera(width * (width.asInstanceOf[Float] / height.asInstanceOf[Float]), width)
//    camera.viewportHeight = width
//    camera.viewportWidth = height
    //    camera.zoom = Main.BOX_TO_WORLD
    camera.update()
    camera.apply(Gdx.graphics.getGL10)
  }

  def addScore(score: Long) {
    this.score += Math.round(score * scoreMultiplier)
    log("Added %d (x%.2f) score to get %d total".format(score, this.scoreMultiplier, this.score))
  }

  def nextLevel() {
    currentLevelIndex += 1
    if (currentLevelIndex == levels.size) {
      setScreen(gameOverLevel)
      Gdx.input.setInputProcessor(gameOverLevel)
    }
    else {
      currentLevel = levels.get(currentLevelIndex)
      setScreen(currentLevel)
      Gdx.input.setInputProcessor(currentLevel)
    }
  }
}

object Entity {
  val COLLISION_NONE: Int = 0
  val COLLISION_STATIC: Int = 1
  val COLLISION_PLAYER: Int = 2
  val COLLISION_TREASURE: Int = 4
  val COLLISION_LADDER: Int = 8
  val COLLISION_ENEMY: Int = 16
  val COLLISION_DOOR: Int = 32
  val COLLISION_STATIC_FLOOR = 64

  val COLLISION_LEFT: Int = 0
  val COLLISION_RIGHT: Int = 1
  val COLLISION_UP: Int = 2
  val COLLISION_DOWN: Int = 3
}

trait Entity {
  var isDestroyed = false

  def doMove(dt: Float)
  def tick(dt: Float)

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

  def onCollision(collisions: GdxArray[Vector2], collisionTypes: GdxArray[Int], directions: GdxArray[(Int, Int)])
  def lostCollision(collisionType: Int)
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

  def doMove(dt: Float) {
    velocity.add(acceleration.x * dt * accelerationScale, acceleration.y * dt * accelerationScale)
    move(velocity.x * dt, velocity.y * dt)
  }
}

object Player {
  val WIDTH = 20.0f
  val HEIGHT = 40.0f
  val IGNORE_DELTA = 1.55f
  val GRAVITY = -9.81f * 70.0f
}

class Player(x: Float, y: Float) extends Entity with MovingEntity {

  import Utils.log

  val VEL: Float = 140.0f
  val JUMP_VEL: Float = 330.0f
  var jumping = false
  val rect = new Rectangle(x, y, Player.WIDTH, Player.HEIGHT)
  acceleration.y = Player.GRAVITY
  val accelerationScale: Float = 1
  val doorDelta = new Vector2

  val ladderDeltaTrigger = Player.WIDTH / 3.0f
  val ladderDelta = new Vector2
  var onLadder = false

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
            acceleration.y = Player.GRAVITY
          }
          true
        }
        else {
          false
        }
      }
      case Keys.UP => {
        if (pressed) {
          if (Math.abs(doorDelta.x) > Player.WIDTH / 2.0f) {
            log("Up pressed with door delta: " + doorDelta.x)
            if (Main.instance.currentLevel.isGoalMet) {
              Main.instance.nextLevel()
            }
          }
          if(Math.abs(ladderDelta.x) > ladderDeltaTrigger || ladderDelta.y != 0) {
            log("Up pressed with ladder delta: " + ladderDelta.x)
            onLadder = true
            acceleration.y = 0
            velocity.y = VEL
          }
        }
        else {
          if(onLadder) {
            velocity.y = 0
          }
        }
        true
      }
      case Keys.DOWN => {
        if(pressed) {
          if(onLadder || ladderDelta.y != 0) {
            onLadder = true
            velocity.y = -VEL
            acceleration.y = 0
          }
        }
        else {
          if(onLadder) {
            velocity.y = 0
          }
        }
        true
      }
      case _ => false
    }
  }

  def tick(dt: Float) { }

  def position(): Vector2 = new Vector2(rect.getX, rect.getY)

  def size(): Vector2 = new Vector2(rect.getWidth, rect.getHeight)

  def rectangle(): Rectangle = new Rectangle(rect)

  def collisionType: Int = Entity.COLLISION_PLAYER

  def onCollision(collisions: GdxArray[Vector2], collisionTypes: GdxArray[Int], directions: GdxArray[(Int, Int)]) {
    var x = Float.MaxValue
    var y = Float.MaxValue
    var numStatics = 0
    val statics = Array[Int](0, 0)
    for (i <- 0 to collisions.size - 1) {
      val vec = collisions.get(i)
      val colType = collisionTypes.get(i)
      colType match {
        case Entity.COLLISION_STATIC | Entity.COLLISION_STATIC_FLOOR => {
          if(onLadder && colType == Entity.COLLISION_STATIC) {
          }
          else {
            if (vec.x != 0 && Math.abs(vec.x) < Math.abs(x)) {
              x = vec.x
            }
            if (vec.y != 0 && Math.abs(vec.y) < Math.abs(y)) {
              y = vec.y
            }
            numStatics += 1
            if(numStatics < 3) {
              statics(numStatics - 1) = i
            }
          }
        }
        case Entity.COLLISION_TREASURE => {
          log("Collided with treasure")
        }
        case Entity.COLLISION_DOOR => {
          doorDelta.set(vec)
        }
        case Entity.COLLISION_LADDER => {
//          ladderDelta.x = if(vec.x == 0) vec.y else vec.x
          ladderDelta.set(vec)
        }
        case _ => Unit
      }
    }

    if (y == Float.MaxValue) {
      y = 0
    }
    else if(numStatics == 2 && y > 0 && x != Float.MaxValue) {
      if(directions.get(0)._1 == directions.get(1)._1) {
        y = 0
      }
    }
    if (x == Float.MaxValue) {
      x = 0
    }
    if (y > 0) {
      if (velocity.y < 0) {
        jumping = false
        velocity.y = 0
      }
    }

    if(onLadder && (Math.abs(ladderDelta.x) < ladderDeltaTrigger && ladderDelta.y == 0)) {
      onLadder = false
      acceleration.y = Player.GRAVITY
    }

    move(x, y)
  }

  def lostCollision(collisionType: Int) {
    collisionType match {
      case Entity.COLLISION_LADDER => {
        if(onLadder) {
          log("Lost")
          ladderDelta.set(0, 0)
          onLadder = false
          velocity.y = 0
          acceleration.y = Player.GRAVITY
        }
      }
      case _ => {
      }
    }
  }

  def getSprite: TextureRegion = ???
}

class StaticBlock(x: Float, y: Float, width: Float, height: Float, floor: Boolean) extends Entity with MovingEntity {
  val rect: Rectangle = new Rectangle(x, y, width, height)
  val accelerationScale: Float = 0

  def tick(dt: Float) {}

  def key(keyCode: Int, pressed: Boolean): Boolean = false

  def position(): Vector2 = new Vector2(rect.getX, rect.getY)

  def size(): Vector2 = new Vector2(rect.getWidth, rect.getHeight)

  def rectangle(): Rectangle = new Rectangle(rect)

  def collisionType: Int = if(floor) Entity.COLLISION_STATIC_FLOOR else Entity.COLLISION_STATIC

  def onCollision(collisions: GdxArray[Vector2], collisionTypes: GdxArray[Int], directions: GdxArray[(Int, Int)]) {}
  def lostCollision(collisionType: Int) {}

  def getSprite: TextureRegion = ???
}

object Treasure {
  val WIDTH = 20.0f
  val HEIGHT = 20.0f
  val SCORE = 100L
}

class Treasure(x: Float, y: Float) extends Entity {
  val pos = new Vector2(x, y)

  def doMove(dt: Float) {}
  def tick(dt: Float) {}

  def key(keyCode: Int, pressed: Boolean): Boolean = false

  def position(): Vector2 = new Vector2(pos)

  def size(): Vector2 = new Vector2(Treasure.WIDTH, Treasure.HEIGHT)

  def rectangle(): Rectangle = new Rectangle(pos.x, pos.y, Treasure.WIDTH, Treasure.HEIGHT)

  def collisionType: Int = Entity.COLLISION_TREASURE

  def onCollision(collisions: utils.Array[Vector2], collisionTypes: utils.Array[Int], directions: GdxArray[(Int, Int)]) {
    for (i <- 0 to collisions.size - 1) {
      val colType = collisionTypes.get(i)
      colType match {
        case Entity.COLLISION_PLAYER => {
          Utils.log("Player collided with treasure, adding score")
          Utils.log(collisions.toString())
          Utils.log(collisionTypes.toString())
          Utils.log(directions.toString())
          Main.instance.addScore(Treasure.SCORE)
          destroy()
        }
        case _ => Unit
      }
    }
  }

  def lostCollision(collisionType: Int) {
  }

  def getSprite: TextureRegion = ???
}

object Door {
  val WIDTH = 35.0f
  val HEIGHT = 35.0f
}
class Door(x: Float, y: Float) extends Entity {
  val pos = new Vector2(x, y)

  def doMove(dt: Float) {}
  def tick(dt: Float) {}

  def getSprite: TextureRegion = ???

  def key(keyCode: Int, pressed: Boolean): Boolean = false

  def position(): Vector2 = new Vector2(pos)

  def size(): Vector2 = new Vector2(Door.WIDTH, Door.HEIGHT)

  def rectangle(): Rectangle = new Rectangle(pos.x, pos.y, Door.WIDTH, Door.HEIGHT)

  def collisionType: Int = Entity.COLLISION_DOOR

  def onCollision(collisions: utils.Array[Vector2], collisionTypes: utils.Array[Int], directions: GdxArray[(Int, Int)]) {
  }

  def lostCollision(collisionType: Int) {}
}

object Ladder {
  val WIDTH = Main.BLOCK_SIZE / 2.0f
  val HEIGHT = Main.BLOCK_SIZE * 2.0f + Main.BLOCK_SIZE / 10.0f
}
class Ladder(x: Float, y: Float) extends Entity {
  val pos = new Vector2(x + Ladder.WIDTH / 2.0f, y)

  def doMove(dt: Float) {}
  def tick(dt: Float) {}

  def getSprite: TextureRegion = ???

  def key(keyCode: Int, pressed: Boolean): Boolean = {
    false
  }

  def position(): Vector2 = new Vector2(pos)
  def size(): Vector2 = new Vector2(Ladder.WIDTH, Ladder.HEIGHT)
  def rectangle(): Rectangle = new Rectangle(pos.x, pos.y, Ladder.WIDTH, Ladder.HEIGHT)

  def collisionType: Int = Entity.COLLISION_LADDER

  def onCollision(collisions: utils.Array[Vector2], collisionTypes: utils.Array[Int], directions: utils.Array[(Int, Int)]) {}

  def lostCollision(collisionType: Int) {}
}

trait Level extends Screen with InputProcessor {
  val entities = new GdxArray[Entity]()
  val addLater = new GdxArray[Entity]()
  val removeLater = new GdxArray[Entity]()
  var fpslogger: FPSLogger = null

  def isGoalMet: Boolean

  def moveCamera()

  def collisionBetween(ent: Entity, ent2: Entity, delta: Vector2, directions: Array[Int]):Boolean = {
    delta.set(0, 0)
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
              delta.set(0, bottomTop)
              directions(0) = Entity.COLLISION_LEFT
              directions(1) = Entity.COLLISION_UP
            }
            else {
              delta.set(leftRight, 0)
              directions(0) = Entity.COLLISION_LEFT
              directions(1) = Entity.COLLISION_UP
            }
          }
          else {
            if (bottomTopAbs < rightLeftAbs) {
              delta.set(0, bottomTop)
              directions(0) = Entity.COLLISION_RIGHT
              directions(1) = Entity.COLLISION_UP
            }
            else {
              delta.set(rightLeft, 0)
              directions(0) = Entity.COLLISION_RIGHT
              directions(1) = Entity.COLLISION_UP
            }
          }
        }
        else {
          if (leftRightAbs < rightLeftAbs) {
            if (topBottomAbs < leftRightAbs) {
              delta.set(0, topBottom)
              directions(0) = Entity.COLLISION_LEFT
              directions(1) = Entity.COLLISION_DOWN
            }
            else {
              delta.set(leftRight, 0)
              directions(0) = Entity.COLLISION_LEFT
              directions(1) = Entity.COLLISION_DOWN
            }
          }
          else {
            if (topBottomAbs < rightLeftAbs) {
              delta.set(0, topBottom)
              directions(0) = Entity.COLLISION_RIGHT
              directions(1) = Entity.COLLISION_DOWN
            }
            else {
              delta.set(rightLeft, 0)
              directions(0) = Entity.COLLISION_RIGHT
              directions(1) = Entity.COLLISION_DOWN
            }
          }
        }
//        Utils.log("Collision between: " + ent + ", " + ent2)
        true
      }
      else {
        false
      }
    }
    else {
      false
    }
  }

  def doCollision() {
    // Well this is kind of a big fustercluck, but whatever. If it works, I couldn't care less.
    // Also: this has worst-case performance of O(n^2) if all entities are COLLISION_DYNAMIC/etc. A lot of
    //  collisions should be culled though due to not doing static-to-static collision, ignoring NONE, etc..
    val colliding = new GdxArray[Vector2]
    val collidingType = new GdxArray[Int]
    val directions = new GdxArray[(Int, Int)]
    val collisionIndices = new GdxArray[Int]
    for (i <- 0 to entities.size - 1) {
      val ent = entities.get(i)
      colliding.clear()
      collidingType.clear()
      collisionIndices.clear()
      if (ent.collisionType != Entity.COLLISION_STATIC && ent.collisionType != Entity.COLLISION_NONE) {
        for (j <- 0 to entities.size - 1) {
          val ent2 = entities.get(j)
          if (ent2 != ent && ent2.collisionType != Entity.COLLISION_NONE) {
            val rect = ent.rectangle()
            val rect2 = ent2.rectangle()
            if (rect.overlaps(rect2)) {
              collisionIndices.add(j)
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
                    directions.add((Entity.COLLISION_LEFT, Entity.COLLISION_UP))
                  }
                  else {
                    colliding.add(new Vector2(leftRight, 0))
                    directions.add((Entity.COLLISION_LEFT, Entity.COLLISION_UP))
                  }
                }
                else {
                  if (bottomTopAbs < rightLeftAbs) {
                    colliding.add(new Vector2(0, bottomTop))
                    directions.add((Entity.COLLISION_RIGHT, Entity.COLLISION_UP))
                  }
                  else {
                    colliding.add(new Vector2(rightLeft, 0))
                    directions.add((Entity.COLLISION_RIGHT, Entity.COLLISION_UP))
                  }
                }
              }
              else {
                if (leftRightAbs < rightLeftAbs) {
                  if (topBottomAbs < leftRightAbs) {
                    colliding.add(new Vector2(0, topBottom))
                    directions.add((Entity.COLLISION_LEFT, Entity.COLLISION_DOWN))
                  }
                  else {
                    colliding.add(new Vector2(leftRight, 0))
                    directions.add((Entity.COLLISION_LEFT, Entity.COLLISION_DOWN))
                  }
                }
                else {
                  if (topBottomAbs < rightLeftAbs) {
                    colliding.add(new Vector2(0, topBottom))
                    directions.add((Entity.COLLISION_RIGHT, Entity.COLLISION_DOWN))
                  }
                  else {
                    colliding.add(new Vector2(rightLeft, 0))
                    directions.add((Entity.COLLISION_RIGHT, Entity.COLLISION_DOWN))
                  }
                }
              }
              collidingType.add(ent2.collisionType)
            }
          }
        }
      }
      if (colliding.size > 0) {
        ent.onCollision(colliding, collidingType, directions)
        val rect = ent.rectangle()
        if(ent.collisionType == Entity.COLLISION_PLAYER) {
          Utils.log(collisionIndices.toString())
        }
        for(j <- 0 to colliding.size - 1) {
          if(!rect.overlaps(entities.get(collisionIndices.get(j)).rectangle())) {
            ent.lostCollision(collidingType.get(j))
          }
        }
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

    // Get the stuff each entity collides with...
    val collisions = new util.HashMap[Entity, GdxArray[Entity]]()
    val delta = new Vector2
    var curArr: GdxArray[Entity] = null
    val colliding = new GdxArray[Vector2]
    val collidingType = new GdxArray[Int]
    val directions = new GdxArray[(Int, Int)]
    val directionsArr = Array(0, 0)
    for(i <- 0 to entities.size - 1) {
      val ent = entities.get(i)
      for(j <- 0 to entities.size - 1) {
        val ent2 = entities.get(j)
        if(!(ent.collisionType == Entity.COLLISION_STATIC && ent2.collisionType == Entity.COLLISION_STATIC) &&
           !(ent.collisionType == Entity.COLLISION_STATIC && ent2.collisionType == Entity.COLLISION_STATIC_FLOOR) &&
           !(ent.collisionType == Entity.COLLISION_STATIC_FLOOR && ent.collisionType == Entity.COLLISION_STATIC) &&
           !(ent.collisionType == Entity.COLLISION_STATIC_FLOOR && ent2.collisionType == Entity.COLLISION_STATIC_FLOOR)) {
          if(collisionBetween(ent, ent2, delta, directionsArr)) {
            colliding.add(new Vector2(delta))
            collidingType.add(ent2.collisionType)
            directions.add((directionsArr(0), directionsArr(1)))
            if(curArr == null) {
              curArr = new GdxArray[Entity]
              curArr.add(ent2)
              collisions.put(ent, curArr)
            }
            else {
              curArr.add(ent2)
            }
          }
        }
      }
      if(colliding.size > 0) {
        ent.onCollision(colliding, collidingType, directions)
      }
      colliding.clear()
      collidingType.clear()
      directions.clear()
      curArr = null
    }

    for (i <- 0 to removeLater.size - 1) {
      entities.removeValue(removeLater.get(i), true)
    }
    removeLater.clear()
    for (i <- 0 to addLater.size - 1) {
      entities.add(addLater.get(i))
    }
    addLater.clear()

    // THEN move everything...
    for(i <- 0 to entities.size - 1) {
      entities.get(i).doMove(Main.instance.worldStep)
    }

    // Finally check for anything that each entity is not colliding with anymore after moving
    collisions.entrySet().iterator().foreach((entry) => {
      val key = entry.getKey
      val value = entry.getValue
      val keyRect = key.rectangle()
      for(i <- 0 to value.size - 1) {
        if(!keyRect.overlaps(value.get(i).rectangle())) {
          key.lostCollision(value.get(i).collisionType)
        }
      }
    })

    for(i <- 0 to entities.size - 1) {
      entities.get(i).tick(Main.instance.worldStep)
    }

    moveCamera()

    gl.glClear(GL_COLOR_BUFFER_BIT)
    Main.instance.camera.update()
    Main.instance.camera.apply(Gdx.graphics.getGL10)

    if (Main.DEBUG) {
      val renderer = Main.instance.renderer
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
          case Entity.COLLISION_DOOR => {
            renderer.setColor(Color.GREEN)
          }
          case Entity.COLLISION_LADDER => {
            renderer.setColor(Color.PINK)
          }
          case _ => {
            renderer.setColor(Color.WHITE)
          }
        }
        renderer.rect(pos.x, pos.y, size.x, size.y)
      })
      renderer.end()

      if (fpslogger != null) {
        fpslogger.log()
      }
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
      case Keys.W => {
        Utils.log("W pressed, skipping to next level")
        Main.instance.nextLevel()
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

  var isLoaded = false

  def load() {
    isLoaded = true
    log("Creating some static blocks")
    for (i <- 0 to (game.width / 10)) {
      entities.add(new StaticBlock(game.width / 10.0f * i, 20, game.width / 10.0f, 20, true))
    }
    entities.add(new StaticBlock(game.width / 10.0f * 5, 50, game.width / 10.0f, 20, false))
    entities.add(new StaticBlock(game.width / 10.0f * 4 - 17, 90, game.width / 10.0f, 20, false))

    log("Creating treasure")
    entities.add(new Treasure(150, 100))

    log("Creating door")
    entities.add(new Door(game.width / 10.0f * 7, 40))

    log("Creating ladder")
    entities.add(new Ladder(game.width / 10.0f * 4, game.width / 10.0f * 2))

    log("Making the player")
    entities.add(new Player(0, 80))

    log("Moving camera by: " + (Main.instance.width / 2 - 100) + "x" + Main.instance.height / 2)
    Main.instance.camera.translate(Main.instance.width / 2 - 100, Main.instance.height / 2)
    Main.instance.camera.update()
//    Main.instance.camera.apply(Gdx.graphics.getGL10)
  }

  override def keyDown(keycode: Int): Boolean = {
    keycode match {
      case com.badlogic.gdx.Input.Keys.W => {
        Main.instance.nextLevel()
        true
      }
      case _ => {
        super.keyDown(keycode)
      }
    }
  }

  def isGoalMet: Boolean = true

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

  def moveCamera() {}
}

class LevelOne extends Level {
  import Main.BLOCK_SIZE
  var player: Player = null

  def resize(width: Int, height: Int) {}

  def show() {
    fpslogger = new FPSLogger

    player = new Player(BLOCK_SIZE * 2, BLOCK_SIZE)
    entities.add(player)

    for (i <- 0 to 100) {
      entities.add(new StaticBlock(BLOCK_SIZE * i, 0, BLOCK_SIZE, BLOCK_SIZE, true))
      entities.add(new StaticBlock(0, BLOCK_SIZE * i, BLOCK_SIZE, BLOCK_SIZE, false))
    }
    entities.add(new StaticBlock(BLOCK_SIZE * 4, BLOCK_SIZE * 2, BLOCK_SIZE, BLOCK_SIZE, false))
    entities.add(new Ladder(BLOCK_SIZE * 4, BLOCK_SIZE))
    entities.add(new Door(BLOCK_SIZE * 10, BLOCK_SIZE))
  }

  def hide() {}

  def pause() {}

  def resume() {}

  def dispose() {}

  def isGoalMet: Boolean = true

  def moveCamera() {
    val cam = Main.instance.camera
    val pos = player.position()
    val campos = new Vector3(cam.position)
    //    Utils.log("Camera position: " + campos)
    //    Utils.log("Player position: " + pos)
    cam.translate(pos.x - campos.x, pos.y - campos.y)
    if (cam.position.x - cam.viewportWidth / 2.0f < 0) {
      cam.translate(-(cam.position.x - cam.viewportWidth / 2.0f) - 5, 0)
    }
    if (cam.position.y - cam.viewportHeight / 2.0f < 0) {
      cam.translate(0, -(cam.position.y - cam.viewportHeight / 2.0f) - 5)
    }
    cam.update()
  }
}

class GameOver extends Level {
  val QUIT_AFTER = 2.0f
  var font: BitmapFont = null
  var time: Float = 0

  def resize(width: Int, height: Int) {}

  def show() {
    font = new BitmapFont()
  }

  override def render(delta: Float) {
    import Gdx.gl
    import com.badlogic.gdx.graphics.GL10.GL_COLOR_BUFFER_BIT
    time += delta
    if (time > QUIT_AFTER) {
      Utils.log("Spent >2s on game over screen, quitting")
      Gdx.app.exit()
    }

    gl.glClear(GL_COLOR_BUFFER_BIT)

    val renderer = Main.instance.spriteRenderer
    renderer.begin()
    font.draw(renderer, "Game over, man", Main.instance.width / 2.0f - 70, Main.instance.height / 2.0f)
    renderer.end()
  }

  def hide() {}

  def pause() {}

  def resume() {}

  def dispose() {}

  def isGoalMet: Boolean = true

  def moveCamera() {}
}