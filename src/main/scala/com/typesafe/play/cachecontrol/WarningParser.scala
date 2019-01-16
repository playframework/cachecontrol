/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.CharSequenceReader

object WarningParser {

  def parse(header: String): Warning = {
    WarningValueParser(new CharSequenceReader(header)) match {
      case WarningValueParser.Success(result, next) =>
        result
      case _: WarningValueParser.NoSuccess =>
        throw new IllegalStateException("Warning parse failure")
    }
  }

  /**
   * Parses out a warning header.
   */
  object WarningValueParser extends JavaTokenParsers {

    override def skipWhitespace = false

    def apply(in: Input): ParseResult[Warning] = {
      warningValue(in)
    }

    val space = regex("[ \\n]+".r)

    val warnCode = regex("""\d{3}""".r) <~ space ^^ { s =>
      Integer.parseInt(s)
    }

    val warnAgent = regex("\\S+".r) <~ space

    val warnText = stringLiteral ^^ { s =>
      s.replaceAllLiterally('"'.toString, "")
    }

    val warnDate = opt(space ~> stringLiteral) ^^ { maybeString =>
      maybeString.map { s =>
        val chomp = s.replaceAllLiterally('"'.toString, "")
        HttpDate.parse(chomp)
      }
    }

    def warningValue: Parser[Warning] = warnCode ~ warnAgent ~ warnText ~ warnDate ^^ {
      case code ~ agent ~ text ~ date =>
        Warning(code, agent, text, date)
    }

  }

}
