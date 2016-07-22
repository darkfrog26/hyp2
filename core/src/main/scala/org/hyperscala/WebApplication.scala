package org.hyperscala

import scala.language.experimental.macros

abstract class WebApplication(val host: String, val port: Int) extends BaseApplication {
  override protected[hyperscala] var picklers = Vector.empty[Pickler[_]]
  override protected[hyperscala] var _screens = Vector.empty[BaseScreen]
  def screens: Vector[Screen] = _screens.asInstanceOf[Vector[Screen]]

  lazy val manager: ApplicationManager = createApplicationManager()
  def connection: Connection = manager.connection

  def create[S <: Screen]: S = macro Macros.screen[S]
  def communicationPath: String = "/communication"

  protected[hyperscala] def add[T](pickler: Pickler[T]): Unit = synchronized {
    val position = picklers.length
    picklers = picklers :+ pickler
    pickler.channel.attach { t =>
      if (!pickler.receiving.get()) {
        val json = pickler.write(t)
        manager.connection.send(position, json)
      }
    }
  }

  def init(): Unit = manager.init()
}