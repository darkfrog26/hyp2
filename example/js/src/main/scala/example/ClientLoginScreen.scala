package example

import com.outr.scribe.Logging
import org.hyperscala._
import org.scalajs.dom._

trait ClientLoginScreen extends LoginScreen with Logging with SimpleClientScreen[html.Form] {
  override def main = byId[html.Form]("loginForm")
  def message = byId[html.Div]("message")
  def username = byId[html.Input]("username")
  def password = byId[html.Input]("password")

  override def init(state: InitState): Unit = {
    // Change screen upon successful login
    response.attach { r =>
      r.errorMessage match {
        case Some(msg) => message.innerHTML = msg
        case None => {
          message.innerHTML = ""
          app.connection.replaceWith(ExampleApplication.dashboard)
        }
      }
    }

    // Send authentication request to server
    main.onsubmit = (evt: Event) => {
      authenticate := Authentication(username.value, password.value)
      false
    }
  }
}