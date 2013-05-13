package loderunner

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.{Rectangle, Vector2}
import com.badlogic.gdx.utils
import com.badlogic.gdx.utils.{Array => GdxArray}

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
  val HIT_TIME = 0.13f
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
  var hit = false
  var endHit = -1.0f

  override def key(keyCode: Int, pressed: Boolean): Boolean = {
    import com.badlogic.gdx.Input.Keys
    if(!hit) {
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
    false
  }

  def tick(dt: Float) {
    if(hit) {
      endHit -= dt
      if(endHit < 0) {
        hit = false
        velocity.set(0, 0)
      }
    }
  }

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
          ladderDelta.set(vec)
        }
        case Entity.COLLISION_ENEMY => {
          log(collisions.get(i) + ", " + collisionTypes.get(i) + ", " + directions.get(i))
          if(collisions.get(i).x != 0) {
            hit = true
            endHit = Player.HIT_TIME
            if(directions.get(i)._1 == Entity.COLLISION_LEFT) {
              velocity.set(200f, 0)
            }
            else {
              velocity.set(-200f, 0)
            }
          }
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
  val WIDTH = 32.0f
  val HEIGHT = 32.0f
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

object Enemy {
  val WIDTH = 40.0f
  val HEIGHT = 20.0f
  val VELOCITY = 100.0f
  val LADDER_COOLDOWN = 3.0f
}
class Enemy(x: Float, y: Float) extends Entity with MovingEntity {
  val rect: Rectangle = new Rectangle(x, y, Enemy.WIDTH, Enemy.HEIGHT)
  val accelerationScale: Float = 1.0f

  var onLadder = false
  var ladderCollision = false
  var madeLadderDecision = false
  var startLadderCollision = 0.0f
  var ladderCollisionCooldown = 0.0f
  var numStatics = 0

  if(Math.abs(Utils.rand.nextInt(100)) < 49) {
    velocity.set(Enemy.VELOCITY, 0)
  }
  else {
    velocity.set(-Enemy.VELOCITY, 0)
  }

  def tick(dt: Float) {
    ladderCollisionCooldown -= dt
    if(!madeLadderDecision && ladderCollision && !onLadder && Math.abs(rect.x - startLadderCollision) > rect.width / 2.0f + 10.0f) {
      madeLadderDecision = true
      if(Math.abs(Utils.rand.nextInt(100)) < 49) {
        onLadder = true
        velocity.set(0, Enemy.VELOCITY)
      }
    }
  }

  def getSprite: TextureRegion = ???

  def key(keyCode: Int, pressed: Boolean): Boolean = false

  def position(): Vector2 = new Vector2(rect.x, rect.y)

  def size(): Vector2 = new Vector2(rect.width, rect.height)

  def rectangle(): Rectangle = new Rectangle(rect)

  def collisionType: Int = Entity.COLLISION_ENEMY

  def onCollision(collisions: utils.Array[Vector2], collisionTypes: utils.Array[Int], directions: utils.Array[(Int, Int)]) {
    var curStatics = 0
    for(i <- 0 to collisions.size - 1) {
      collisionTypes.get(i) match {
        case Entity.COLLISION_PLAYER => {
          if(directions.get(i)._2 == Entity.COLLISION_UP && collisions.get(i).y < 0) {
            Utils.log(collisions + ", " + collisionTypes + ", " + directions)
            destroy()
          }
        }
        case Entity.COLLISION_LADDER => {
          if(!ladderCollision && ladderCollisionCooldown < 0) {
            ladderCollision = true
            startLadderCollision = rect.x
          }
        }
        case Entity.COLLISION_STATIC | Entity.COLLISION_STATIC_FLOOR => {
          if(collisions.get(i).x != 0) {
            velocity.set(-velocity.x, 0)
          }
          else if(!onLadder) {
            if(numStatics == 0) {
              Utils.log("foo?")
              numStatics = 1
              move(collisions.get(i).x, collisions.get(i).y)
              acceleration.y = 0
              velocity.y = 0

              if(Math.abs(Utils.rand.nextInt(100)) < 49) {
                velocity.set(Enemy.VELOCITY, 0)
              }
              else {
                velocity.set(-Enemy.VELOCITY, 0)
              }
            }
          }
          curStatics += 1
        }
        case _ => {
        }
      }
    }
    numStatics = curStatics
  }

  def lostCollision(collisionType: Int) {
    collisionType match {
      case Entity.COLLISION_LADDER => {
        if(ladderCollision) {
          ladderCollision = false
          madeLadderDecision = false
        }
        if(onLadder) {
          onLadder = false
          ladderCollisionCooldown = Enemy.LADDER_COOLDOWN
          move(0, -Main.BLOCK_SIZE / 10.0f - Enemy.VELOCITY * Main.instance.worldStep)
          if(Math.abs(Utils.rand.nextInt(100)) < 49) {
            velocity.set(Enemy.VELOCITY, 0)
          }
          else {
            velocity.set(-Enemy.VELOCITY, 0)
          }
        }
      }
      case Entity.COLLISION_STATIC | Entity.COLLISION_STATIC_FLOOR => {
        numStatics -= 1
        if(numStatics == 0 && !onLadder) {
          acceleration.y = Player.GRAVITY
        }
      }
      case _ => {
      }
    }
  }
}
