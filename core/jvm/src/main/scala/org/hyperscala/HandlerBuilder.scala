package org.hyperscala

import io.undertow.server.{HttpHandler, HttpServerExchange}
import io.undertow.util.Headers

class HandlerBuilder(matcher: PartialFunction[URL, Boolean] = Map.empty,
                     handler: (URL, HttpServerExchange) => Unit = (url, exchange) => Unit,
                     priority: Priority = Priority.Normal) {
  def withMatcher(matcher: PartialFunction[URL, Boolean]): HandlerBuilder = {
    val m = this.matcher.orElse(matcher)
    new HandlerBuilder(m, handler, priority)
  }
  def pathMatch(path: String): HandlerBuilder = withMatcher({ case url if url.path == path => true })
  def hostMatch(host: String): HandlerBuilder = withMatcher({ case url if url.host.equalsIgnoreCase(host) => true })

  def withHandler(handler: HttpHandler): HandlerBuilder = {
    val h = (url: URL, exchange: HttpServerExchange) => handler.handleRequest(exchange)
    new HandlerBuilder(matcher, h, priority)
  }

  def withHandler(contentType: String)(handler: HttpServerExchange => String): HandlerBuilder = {
    val h = (url: URL, exchange: HttpServerExchange) => {
      val content = handler(exchange)
      exchange.getResponseHeaders.put(Headers.CONTENT_LENGTH, content.length)
      exchange.getResponseHeaders.put(Headers.CONTENT_TYPE, contentType)
      val sender = exchange.getResponseSender
      sender.send(content)
    }
    new HandlerBuilder(matcher, h, priority)
  }

  def withPriority(priority: Priority): HandlerBuilder = {
    new HandlerBuilder(matcher, handler, priority)
  }

  def build(): Handler = {
    val p = priority
    new Handler {
      override def isURLMatch(url: URL): Boolean = matcher.lift(url).getOrElse(false)

      override def priority: Priority = p

      override def handleRequest(url: URL, exchange: HttpServerExchange): Unit = handler(url, exchange)
    }
  }

  def register(server: Server): Unit = server.register(build())
}