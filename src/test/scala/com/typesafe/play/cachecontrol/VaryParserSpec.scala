/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.play.cachecontrol

import org.scalatest.WordSpec
import org.scalatest.Matchers._

class VaryParserSpec extends WordSpec {

  "Vary parser" should {

    "parse * correctly" in {
      VaryParser.parse("*") should contain theSameElementsInOrderAs List(HeaderName("*"))
    }

    "parse a list of header names correctly" in {
      val list = List(HeaderName("accept-encoding"), HeaderName("Accept-Language"))
      VaryParser.parse("accept-encoding, accept-language") should contain theSameElementsInOrderAs (list)
    }

  }

}
