package org.hyperscala

import com.outr.reactify.Channel

import scala.language.experimental.macros

trait BaseScreen {
  protected def register[T]: Channel[T] = macro BaseMacros.screenPickler[T]
}