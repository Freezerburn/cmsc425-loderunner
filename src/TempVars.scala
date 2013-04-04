/**
 * Created with IntelliJ IDEA.
 * User: freezerburn
 * Date: 4/4/13
 * Time: 9:58 AM
 */

package com.unlockeddoors.stuff {

import com.badlogic.gdx.math.Vector3
import scala.concurrent.Lock

object TempVars {
  private val theLock = new Lock

  val vect1 = new Vector3
  val vect2 = new Vector3
  val vect3 = new Vector3
  val vect4 = new Vector3
  val vect5 = new Vector3
  val vect6 = new Vector3

  def use(f: (TempVars.type) => Unit) = {
    theLock.synchronized {
      f(TempVars.this)
    }
  }

  def lock = theLock.acquire()
  def unlock = theLock.release()
}

}
