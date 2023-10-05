/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.playframework.cachecontrol

import org.scalatest.Inside._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers._

class WarningParserSpec extends AnyWordSpec {
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
