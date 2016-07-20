package org.hyperscala.stream

sealed trait Delta {
  def selector: Selector
  def apply(streamer: HTMLStream, tag: OpenTag): Unit
}

class Replace private[hyperscala](val selector: Selector, val content: () => String) extends Delta {
  override def apply(streamer: HTMLStream, tag: OpenTag): Unit = {
    streamer.replace(tag.start, tag.close.map(_.end).getOrElse(tag.end), content())
  }
}
class InsertBefore private[hyperscala](val selector: Selector, val content: () => String) extends Delta {
  override def apply(streamer: HTMLStream, tag: OpenTag): Unit = {
    streamer.insert(tag.start, content())
  }
}
class InsertFirstChild private[hyperscala](val selector: Selector, val content: () => String) extends Delta {
  override def apply(streamer: HTMLStream, tag: OpenTag): Unit = {
    streamer.insert(tag.end, content())
  }
}
class ReplaceContent private[hyperscala](val selector: Selector, val content: () => String) extends Delta {
  override def apply(streamer: HTMLStream, tag: OpenTag): Unit = {
    streamer.replace(tag.end, tag.close.get.start, content())
  }
}
class InsertLastChild private[hyperscala](val selector: Selector, val content: () => String) extends Delta {
  override def apply(streamer: HTMLStream, tag: OpenTag): Unit = {
    streamer.insert(tag.close.get.start, content())
  }
}
class InsertAfter private[hyperscala](val selector: Selector, val content: () => String) extends Delta {
  override def apply(streamer: HTMLStream, tag: OpenTag): Unit = {
    streamer.insert(tag.close.map(_.end).getOrElse(tag.end), content())
  }
}
class Repeat[Data] private[hyperscala](val selector: Selector, data: List[Data], deltas: Data => List[Delta]) extends Delta {
  override def apply(streamer: HTMLStream, tag: OpenTag): Unit = {
    data.zipWithIndex.foreach {
      case (d, index) => {
        streamer.grouped(index) {
          streamer.reposition(tag.start)
          deltas(d).foreach { delta =>
            val tags = delta.selector.lookup(streamer.streamable)
            tags.foreach { tag =>
              delta(streamer, tag)
            }
          }
          streamer.insert(tag.close.map(_.end).getOrElse(tag.end), "")
        }
      }
    }
  }
}

object Delta {
  def Replace(selector: Selector, content: => String): Replace = new Replace(selector, () => content)
  def InsertBefore(selector: Selector, content: => String): InsertBefore = new InsertBefore(selector, () => content)
  def InsertFirstChild(selector: Selector, content: => String): InsertFirstChild = new InsertFirstChild(selector, () => content)
  def ReplaceContent(selector: Selector, content: => String): ReplaceContent = new ReplaceContent(selector, () => content)
  def InsertLastChild(selector: Selector, content: => String): InsertLastChild = new InsertLastChild(selector, () => content)
  def InsertAfter(selector: Selector, content: => String): InsertAfter = new InsertAfter(selector, () => content)
  def Repeat[Data](selector: Selector, data: List[Data], changes: Data => List[Delta]): Repeat[Data] = new Repeat(selector, data, changes)
}