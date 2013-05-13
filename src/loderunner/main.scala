package loderunner

import com.badlogic.gdx._
import com.badlogic.gdx.graphics.{Texture, FPSLogger, Color, OrthographicCamera}
import com.badlogic.gdx.math.{Vector3, Vector2, Rectangle}
import com.badlogic.gdx.utils.GdxNativesLoader
import com.badlogic.gdx.utils.{Array => GdxArray}

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.g2d.{SpriteBatch, BitmapFont, TextureRegion}

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
  levels.add(new LevelTwo)
  var currentLevelIndex: Int = 1
  var currentLevel: Level = null

  val clearColor: Array[Float] = Array(0.2f, 0.2f, 0.2f)

  var boxTexture: Texture = null
  var treasureTexture: Texture = null
  var doorTexture: Texture = null
  var ladderTexture: Texture = null
  var backgroundTexture: Texture = null

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

    boxTexture = new Texture(Gdx.files.internal("assets/box.png"))
    treasureTexture = new Texture(Gdx.files.internal("assets/coin.png"))
    doorTexture = new Texture(Gdx.files.internal("assets/door.png"))
    ladderTexture = new Texture(Gdx.files.internal("assets/ladder.png"))
    backgroundTexture = new Texture(Gdx.files.internal("assets/background.png"))

    log("Setting clear color: %.2f %.2f %.2f".format(clearColor(0), clearColor(1), clearColor(2)))
    import com.badlogic.gdx.graphics.GL10
    gl.glClearColor(clearColor(0), clearColor(1), clearColor(2), 1.0f)
    gl.glEnable(GL10.GL_BLEND)
    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
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
//    camera.apply(Gdx.graphics.getGL10)
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

  def gameOver() {
    setScreen(gameOverLevel)
    Gdx.input.setInputProcessor(gameOverLevel)
  }
}
