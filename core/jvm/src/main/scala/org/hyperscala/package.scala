package org

import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.AttachmentKey
import org.hyperscala.manager.ServerConnection

import scala.language.implicitConversions

package object hyperscala {
  implicit def screen2ServerScreen(screen: BaseScreen): ServerScreen = screen.asInstanceOf[ServerScreen]

  private val attachmentKey = AttachmentKey.create[ExtendedExchange](classOf[ExtendedExchange])

  implicit def connection2ServerConnection(connection: Connection): ServerConnection = connection.asInstanceOf[ServerConnection]

  // Cache so we don't create multiple for performance reasons
  implicit def exchange2Extended(exchange: HttpServerExchange): ExtendedExchange = Option(exchange.getAttachment[ExtendedExchange](attachmentKey)) match {
    case Some(ee) => ee
    case None => {
      val ee = new ExtendedExchange(exchange)
      exchange.putAttachment(attachmentKey, ee)
      ee
    }
  }

  implicit def handler2HttpHandler(handler: Handler): HttpHandler = new HttpHandler {
    override def handleRequest(exchange: HttpServerExchange): Unit = handler.handleRequest(exchange.url, exchange)
  }

  class ExtendedExchange(exchange: HttpServerExchange) {
    lazy val query: Option[String] = exchange.getQueryString match {
      case null | "" => None
      case q => Some(q)
    }
    lazy val url: URL = {
      val s = query match {
        case Some(q) => s"${exchange.getRequestURL}?$q"
        case None => exchange.getRequestURL
      }
      URL(s)
    }
  }
}