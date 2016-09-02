package example

import com.outr.scribe.Logging
import org.hyperscala._
import org.scalajs.dom._

trait ClientDashboardScreen extends DashboardScreen with SimpleClientScreen[html.Div] with Logging {
  def main = byId[html.Div]("example")

  override def init(isPage: Boolean): Unit = {
    logger.info(s"Dashboard init! (isPage: $isPage)")
  }
}