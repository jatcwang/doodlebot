package doodlebot
package action

import cats.data.ValidatedNel
import cats.std.list._
import cats.syntax.validated._
import cats.syntax.cartesian._

object Store {
  import doodlebot.model._
  import scala.collection.mutable

  sealed abstract class SignupError extends Product with Serializable
  final case class EmailAlreadyExists(email: Email) extends SignupError
  final case class NameAlreadyExists(name: Name) extends SignupError

  sealed abstract class LoginError extends Product with Serializable
  final case class NameDoesNotExist(name: Name) extends LoginError
  final case object PasswordIncorrect extends LoginError

  def emailAlreadyExists(email: Email): SignupError =
    EmailAlreadyExists(email)

  def nameAlreadyExists(name: Name): SignupError =
    NameAlreadyExists(name)

  def nameDoesNotExist(name: Name): LoginError =
    NameDoesNotExist(name)

  val passwordIncorrect: LoginError =
    PasswordIncorrect

  private var emails: mutable.Set[Email] = mutable.Set.empty
  private var names: mutable.Set[Name] = mutable.Set.empty
  private var accounts: mutable.Map[Name, User] = mutable.Map.empty
  private var sessionsBySession: mutable.Map[Session, Name] = mutable.Map.empty
  private var sessionsByName: mutable.Map[Name, Session] = mutable.Map.empty

  def signup(user: User): ValidatedNel[SignupError,Session] = {
    Store.synchronized {
      val emailCheck: ValidatedNel[SignupError,Unit] =
        if(emails(user.email))
          emailAlreadyExists(user.email).invalidNel[Unit]
        else
          ().validNel[SignupError]

      val nameCheck: ValidatedNel[SignupError,Unit] =
        if(names(user.name))
          nameAlreadyExists(user.name).invalidNel[Unit]
        else
          ().validNel

      (emailCheck |@| nameCheck).map { (_,_) =>
        emails += user.email
        names += user.name
        accounts += (user.name -> user)
        makeSession(user.name)
      }
    }
  }

  def login(login: Login): ValidatedNel[LoginError,Session] = {
    Store.synchronized {
      accounts.get(login.name).fold(nameDoesNotExist(login.name).invalidNel[Session]){ user =>
        if(user.password == login.password)
          makeSession(login.name).validNel
        else
          passwordIncorrect.invalidNel
      }
    }
  }

  def makeSession(name: Name): Session = {
    Store.synchronized {
      sessionsByName.get(name).getOrElse {
        val session = Session()
        sessionsByName += (name -> session)
        sessionsBySession += (session -> name)
        session
      }
    }
  }
}
