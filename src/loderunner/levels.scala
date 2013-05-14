package loderunner

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.{Color, FPSLogger}
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.{Vector3, Vector2}
import com.badlogic.gdx.utils.{Array => GdxArray}
import com.badlogic.gdx.{Gdx, InputProcessor, Screen}
import java.util
import scala.Array
import scala.collection.JavaConversions._

trait Level extends Screen with InputProcessor {
  val entities = new GdxArray[Entity]()
  val addLater = new GdxArray[Entity]()
  val removeLater = new GdxArray[Entity]()
  var fpslogger: FPSLogger = null

  def isGoalMet: Boolean

  def moveCamera()
  def levelSize():Vector2

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
      if(ent.collisionType == Entity.COLLISION_PLAYER) {
        if(ent.position().y < 0)  {
          Main.instance.nextLevel()
        }
      }
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
    //    Main.instance.camera.apply(Gdx.graphics.getGL10)

    val spriteRenderer = Main.instance.spriteRenderer
    val backgroundTexture = Main.instance.backgroundTexture
    spriteRenderer.begin()
    spriteRenderer.draw(backgroundTexture, 0, 0)
    spriteRenderer.end()

    val boxTexture = Main.instance.boxTexture
    val treasureTexture = Main.instance.treasureTexture
    val doorTexture = Main.instance.doorTexture
    val ladderTexture = Main.instance.ladderTexture

    spriteRenderer.setProjectionMatrix(Main.instance.camera.combined)
    spriteRenderer.begin()
    entities.iterator().foreach((ent) => {
      val pos = ent.position()
      val size = ent.size()
      ent.collisionType match {
        case Entity.COLLISION_STATIC_FLOOR => {
          spriteRenderer.draw(boxTexture, pos.x, pos.y)
        }
        case Entity.COLLISION_STATIC => {
          spriteRenderer.draw(boxTexture, pos.x, pos.y)
        }
        case Entity.COLLISION_TREASURE => {
          spriteRenderer.draw(treasureTexture, pos.x, pos.y)
        }
        case Entity.COLLISION_DOOR => {
          spriteRenderer.draw(doorTexture, pos.x, pos.y)
        }
        case Entity.COLLISION_LADDER => {
          var i = 0
          for (i <- 0 to (Ladder.HEIGHT/20.0).toInt) {
            spriteRenderer.draw(ladderTexture, pos.x, pos.y+20*i)
          }
        }
        case Entity.COLLISION_PLAYER => {
          val player:Player = ent.asInstanceOf[Player]
          var frame = if(player.facingLeft) Main.instance.playerIdleLeftRegion else Main.instance.playerIdleRightRegion
          if(player.state == (Player.State.WALKING)) {
            frame = if(player.facingLeft) Main.instance.playerWalkLeftAnimation.getKeyFrame(player.stateTime, true) else Main.instance.playerWalkRightAnimation.getKeyFrame(player.stateTime, true)
          }
          spriteRenderer.draw(frame, pos.x, pos.y)
        }
        case _ => {
          //            renderer.setColor(Color.WHITE)
        }
      }
    })
    spriteRenderer.end()

    if (Main.DEBUG) {
      val renderer = Main.instance.renderer
      renderer.setProjectionMatrix(Main.instance.camera.combined)
      renderer.begin(ShapeRenderer.ShapeType.Rectangle)
      entities.iterator().foreach((ent) => {
        val pos = ent.position()
        val size = ent.size()
        ent.collisionType match {
          case Entity.COLLISION_PLAYER => {
            renderer.setColor(Color.RED)
          }
          case Entity.COLLISION_TREASURE => {
            renderer.setColor(Color.CLEAR)
          }
          case Entity.COLLISION_DOOR => {
            renderer.setColor(Color.CLEAR)
          }
          case Entity.COLLISION_LADDER => {
            renderer.setColor(Color.PINK)
          }
          case Entity.COLLISION_ENEMY => {
            renderer.setColor(Color.GREEN)
          }
          case _ => {
            renderer.setColor(Color.BLACK)
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

  def levelSize():Vector2 = {
    new Vector2
  }

  def load() {
    isLoaded = true
    log("Creating some static blocks")
    for (i <- 0 to (game.width / 10)) {
      entities.add(new StaticBlock(game.width / 10.0f * i, 20, game.width / 10.0f, 20, true))
    }
    entities.add(new StaticBlock(game.width / 10.0f * 5, 50, game.width / 10.0f, 20, false))
    entities.add(new StaticBlock(game.width / 10.0f * 4 - 17, 90, game.width / 10.0f, 20, false))

    log("Creating treasure")
    entities.add(new Treasure(150, 150))

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

trait CameraFollowsPlayer {
  var player: Player
  def levelSize():Vector2

  def moveCamera() {
    val cam = Main.instance.camera
    val pos = player.position()
    val campos = new Vector3(cam.position)
    val bounds = levelSize()
    //    Utils.log("Camera position: " + campos)
    //    Utils.log("Player position: " + pos)
    cam.translate(pos.x - campos.x, pos.y - campos.y)
    if (cam.position.x - cam.viewportWidth / 2.0f < 0) {
      cam.translate(-(cam.position.x - cam.viewportWidth / 2.0f) - 5, 0)
    }
    else if(cam.position.x + cam.viewportWidth / 2.0f > bounds.x) {
      cam.translate(-(cam.position.x + cam.viewportWidth / 2.0f - bounds.x), 0)
    }
    if (cam.position.y - cam.viewportHeight / 2.0f < 0) {
      cam.translate(0, -(cam.position.y - cam.viewportHeight / 2.0f) - 5)
    }
    else if(cam.position.y + cam.viewportHeight / 2.0f > bounds.y) {
      cam.translate(0, -(cam.position.x + cam.viewportWidth / 2.0f - bounds.x))
    }
    cam.update()
  }
}

class LevelOne extends Level with CameraFollowsPlayer {
  import Main.BLOCK_SIZE
  var player: Player = null
  val WIDTH = 880f
  val HEIGHT = 480f

  def levelSize():Vector2 = {
    new Vector2(WIDTH, HEIGHT)
  }

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

    entities.add(new Treasure(150,150))

    entities.add(new Enemy(BLOCK_SIZE * 6, BLOCK_SIZE))
  }

  def hide() {}

  def pause() {}

  def resume() {}

  def dispose() {}

  def isGoalMet: Boolean = true
}

class LevelTwo extends Level with CameraFollowsPlayer {
  import Main.BLOCK_SIZE
  var player: Player = null
  val WIDTH = 880f
  val HEIGHT = 640f

  def levelSize():Vector2 = {
    new Vector2(WIDTH, HEIGHT)
  }

  def resize(width: Int, height: Int) {}

  def show() {
    fpslogger = new FPSLogger

    player = new Player(BLOCK_SIZE * 2, BLOCK_SIZE)
    entities.add(player)

    for (i <- 0 to 100) {
      entities.add(new StaticBlock(0, BLOCK_SIZE * i, BLOCK_SIZE, BLOCK_SIZE, false))
    }

    for (i <- 0 to 10) {
      entities.add(new StaticBlock(BLOCK_SIZE * i, 0, BLOCK_SIZE, BLOCK_SIZE, true))
    }
    for (i <- 14 to 100) {
      entities.add(new StaticBlock(BLOCK_SIZE * i, 40, BLOCK_SIZE, BLOCK_SIZE, true))
    }

    for (i <- 10 to 14) {
      entities.add(new StaticBlock(BLOCK_SIZE * i, BLOCK_SIZE * 2, BLOCK_SIZE, BLOCK_SIZE, false))
    }

    entities.add(new Ladder(BLOCK_SIZE * 10, BLOCK_SIZE))
    entities.add(new Door(BLOCK_SIZE * 16, BLOCK_SIZE*2))
  }

  def hide() {}

  def pause() {}

  def resume() {}

  def dispose() {}

  def isGoalMet: Boolean = true
}

class GameOver extends Level {
  val QUIT_AFTER = 2.0f
  var font: BitmapFont = null
  var time: Float = 0

  def levelSize():Vector2 = {
    new Vector2
  }

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