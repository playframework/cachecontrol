/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.playframework.cachecontrol

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
