package doodlebot.validation

import cats.data.{Validated, ValidatedNel}

object ValidationHelpers {

  def lengthAtLeast(str: String, minLen: Int): ValidatedNel[String, Unit] =
    Validated.condNel(str.length >= minLen, (), s"Must be at least $minLen characters.")

  def onlyLettersOrDigits(str: String): ValidatedNel[String, Unit] =
    Validated.condNel(str.forall(_.isLetterOrDigit), (), "Input can only contain letters or digits")

}
