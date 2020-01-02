/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers._

class VaryParserSpec extends AnyWordSpec {
  "Vary parser" should {
    "parse * correctly" in {
      (VaryParser.parse("*") should contain).theSameElementsInOrderAs(List(HeaderName("*")))
    }

    "parse a list of header names correctly" in {
      val list = List(HeaderName("accept-encoding"), HeaderName("Accept-Language"))
      (VaryParser.parse("accept-encoding, accept-language") should contain).theSameElementsInOrderAs(list)
    }
  }
}
