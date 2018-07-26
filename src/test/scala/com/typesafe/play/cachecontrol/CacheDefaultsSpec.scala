package com.typesafe.play.cachecontrol

import org.scalatest.{ MustMatchers, WordSpec }

class CacheDefaultsSpec extends WordSpec with MustMatchers {

  "isCacheableMethod" should {

    "return true for GET" in {
      val defaults = new StubCache(false)
      defaults.isCacheableMethod("GET") must be(true)
    }

    "return true for HEAD" in {
      val defaults = new StubCache(false)
      defaults.isCacheableMethod("HEAD") must be(true)
    }

    "return false for POST" in {
      val defaults = new StubCache(false)
      defaults.isCacheableMethod("POST") must be(false)
    }

  }

}
