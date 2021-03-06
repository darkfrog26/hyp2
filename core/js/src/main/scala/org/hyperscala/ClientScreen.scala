package org.hyperscala

import com.outr.reactify.{Channel, Var}
import org.hyperscala.manager.ClientConnection
import org.scalajs.dom._

trait ClientScreen extends Screen {
  private[hyperscala] var _loaded = false
  def loaded: Boolean = _loaded

  protected[hyperscala] var activated = false

  val title: Var[Option[String]] = Var(None)
  val stateChange: Channel[StateChange] = Channel[StateChange]

  def urlChanged(url: URL): Unit = {}

  def onStateChange(stateChangeType: StateChange)(f: => Unit): Unit = {
    stateChange.attach { evt =>
      if (evt eq stateChangeType) {
        f
      }
    }
  }

  final def show(): Unit = if (loaded) {
    title.get match {
      case Some(t) => document.title = t
      case None => // No title set
    }
    doActivate()
  }

  def requestReloadContent(replace: Boolean = false): Unit = {
    app.screenContentRequest := ScreenContentRequest(screenName, app.connection.url.get, replace)
  }

  private[hyperscala] def load(content: Option[ScreenContentResponse]): Unit = {
    val isPage = content match {
      case Some(c) => {
        title := Option(c.title)
        val parent = document.getElementById(c.parentId)
        val temp = document.createElement("div")
        temp.innerHTML = c.content
        val child = temp.firstChild.asInstanceOf[html.Element]
        logger.debug(s"Loading content: ${c.title}, ${c.screenName}, ${c.parentId}")
        Option(document.getElementById(child.id)) match {
          case Some(existing) => parent.replaceChild(child, existing)
          case None => parent.appendChild(child)
        }
        false
      }
      case None => {
        // First load
        title := Option(document.title)
        true
      }
    }
    val state = isPage match {
      case _ if loaded => InitState.ScreenReload
      case true => InitState.PageLoad
      case false => InitState.ScreenLoad
    }
    _loaded = true
    title.attachAndFire {
      case Some(t) => if (app.connection.screen.get == this) {
        document.title = t
      }
      case None => // No title
    }
    init(state)
    stateChange := StateChange.Initialized
    if (!isPage) {
      if (app.connection.screen.get == this) {
        if (state == InitState.ScreenReload && activated) {
          // Reactivate
          doDeactivate()
          stateChange := StateChange.Deactivated
        }
        doActivate()
        stateChange := StateChange.Activated
      } else {
        deactivate()
        stateChange := StateChange.Deactivated
      }
    }
  }
  private def doActivate(): Unit = if (!activated) {
    activated = true
    val currentURL = app.connection.url.get
    activate(currentURL) match {
      case Some(urlChange) => {
        if (urlChange.url != currentURL || urlChange.force) {
          val c = app.connection.asInstanceOf[ClientConnection]
          logger.info(s"URL changing to ${urlChange.url}")
          if (urlChange.replace || app.connection.replace) {
            c.replaceURL(urlChange.url)
          } else {
            c.pushURL(urlChange.url)
          }
          c.updateState()
        }
      }
      case None => // No path change requested
    }
  }
  private[hyperscala] def doDeactivate(): Unit = {
    activated = false
    deactivate()
  }

  /**
    * Initializes this screen. Called after the content of the screen has been properly loaded and injected into the
    * page.
    *
    * @param state defines the state in which this init method is invoked.
    */
  protected def init(state: InitState): Unit

  /**
    * Called after init() when this Screen should be displayed.
    *
    * @return url change for this Screen if there is an explicit url. Will only apply if the url is different or if
    *         force is set to true.
    */
  protected def activate(url: URL): Option[URLChange]

  /**
    * Deactivates the screen. Guaranteed to only be called after init and activate have been called. Called immediately
    * before the new screen is activated.
    */
  protected def deactivate(): Unit
}

/**
  * PathChange represents a path change request that returns from a ClientScreen.activate.
  *
  * @param url the new url to set.
  * @param replace replaces the current path in the browser history if true or pushes a new state if false. Defaults to
  *                false.
  * @param force forces the state change even if the path is the same as the current path. Defaults to false.
  */
case class URLChange(url: URL, replace: Boolean = false, force: Boolean = false)

sealed trait StateChange

object StateChange {
  case object Initialized extends StateChange
  case object Activated extends StateChange
  case object Deactivated extends StateChange
}