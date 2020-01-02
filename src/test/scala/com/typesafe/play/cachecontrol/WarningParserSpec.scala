/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import org.scalatest.WordSpec
import org.scalatest.Matchers._
import org.scalatest.Inside._

/**
 *
 */
class WarningParserSpec extends WordSpec {
  "Parse a warning spec correctly with a date" in {
    val warning  = WarningParser.parse("""112 - "network down" "Sat, 25 Aug 2012 23:34:45 GMT"""")
    val dateTime = HttpDate.parse("Sat, 25 Aug 2012 23:34:45 GMT")
    warning should ===(Warning(112, "-", "network down", Some(dateTime)))
  }

  "Parse a warning spec correctly" in {
    val warning = WarningParser.parse("""112 - "network down"""")
    warning should ===(Warning(112, "-", "network down", None))
  }

  "Parse a warning spec correctly with uri-host" in {
    val warning = WarningParser.parse("""112 example.com:9000 "network down"""")
    inside(warning) {
      case Warning(112, "example.com:9000", "network down", time) =>
        time should ===(None)
      case Warning(code, agent, text, date) =>
        code should ===(112)
    }
  }
}
