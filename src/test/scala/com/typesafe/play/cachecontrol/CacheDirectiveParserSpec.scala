/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
package com.typesafe.play.cachecontrol

import org.joda.time.Seconds
import org.scalatest.WordSpec
import org.scalatest.Matchers._

class CacheDirectiveParserSpec extends WordSpec {
  import CacheDirectives._

  "CacheDirectiveParser" should {

    def parseSingleCacheDirective(cacheHeader: String): CacheDirective = {
      val parsed = CacheDirectiveParser.parse(cacheHeader)
      parsed.length should be(1)
      parsed.head
    }

    "parse a cache directive in upper case" in {
      val cacheDirective = parseSingleCacheDirective("MAX-AGE=3600")
      cacheDirective should be(MaxAge(Seconds.seconds(3600)))
    }

    "parse a cache directive in a quoted string" in {
      val cacheDirective = parseSingleCacheDirective("max-age=\"3600\"")
      cacheDirective should be(MaxAge(Seconds.seconds(3600)))
    }

    // MaxAge
    "parse max-age successfully" in {
      val cacheDirective = parseSingleCacheDirective("max-age=20000")
      cacheDirective should be(MaxAge(Seconds.seconds(20000)))
    }

    "parse max-age successfully with a value of 0" in {
      val cacheDirective = parseSingleCacheDirective("max-age=0")
      cacheDirective should be(MaxAge(Seconds.seconds(0)))
    }

    // MaxStale
    "parse max-stale successfully" in {
      val cacheDirective = parseSingleCacheDirective("max-stale=3600")
      cacheDirective should be(MaxStale(Some(Seconds.seconds(3600))))
    }

    // MinFresh
    "parse min-fresh successfully" in {
      val cacheDirective = parseSingleCacheDirective("min-fresh=3600")
      cacheDirective should be(MinFresh(Seconds.seconds(3600)))
    }

    // NoCache
    "parse no-cache successfully" in {
      val cacheDirective = parseSingleCacheDirective("no-cache")
      cacheDirective should be(NoCache(None))
    }

    "parse no-cache with arguments successfully" in {
      val cacheDirective = parseSingleCacheDirective("no-cache=\"a,b,c,d\"")
      cacheDirective should be(NoCache(Some(List("a", "b", "c", "d"))))
    }

    // NoStore
    "parse no-store successfully" in {
      val cacheDirective = parseSingleCacheDirective("no-store")
      cacheDirective should be(NoStore)
    }

    // NoTransform
    "parse no-transform successfully" in {
      val cacheDirective = parseSingleCacheDirective("no-transform")
      cacheDirective should be(NoTransform)
    }

    // OnlyIfCached
    "parse only-if-cached successfully" in {
      val cacheDirective = parseSingleCacheDirective("only-if-cached")
      cacheDirective should be(OnlyIfCached)
    }

    // must revalidate
    "parse must-revalidate successfully" in {
      val cacheDirective = parseSingleCacheDirective("must-revalidate")
      cacheDirective should be(MustRevalidate)
    }

    // Public
    "parse public successfully" in {
      val cacheDirective = parseSingleCacheDirective("public")
      cacheDirective should be(Public)
    }

    // Private
    "parse private successfully" in {
      val cacheDirective = parseSingleCacheDirective("private")
      cacheDirective should be(Private(None))
    }

    // Private
    "parse private with values successfully" in {
      val fieldNames = "a  , b , c"
      val cacheDirective = parseSingleCacheDirective(s"""private="$fieldNames"""")
      cacheDirective should be(Private(Some(List("a", "b", "c"))))
    }

    // ProxyRevalidate
    "parse proxy-revalidate successfully" in {
      val cacheDirective = parseSingleCacheDirective("proxy-revalidate")
      cacheDirective should be(ProxyRevalidate)
    }

    // SMaxAge
    "parse s-maxage successfully" in {
      val cacheDirective = parseSingleCacheDirective("s-maxage=3600")
      cacheDirective should be(SMaxAge(Seconds.seconds(3600)))
    }

    // CacheDirectiveExtension
    "parse custom cache directive successfully" in {
      val cacheDirective = parseSingleCacheDirective("""community="UCI"""")
      cacheDirective should be(CacheDirectiveExtension(name = "community", value = Some("UCI")))
    }

    "parse private,max-age=0,no-cache successfully" in {
      val directives = CacheDirectiveParser.parse("private, max-age=5, no-cache")

      directives should contain theSameElementsAs Seq(Private(None), MaxAge(Seconds.seconds(5)), NoCache(None))
    }

    "parse duplicated private,private successfully" in {
      val directives = CacheDirectiveParser.parse("private,private")

      directives should contain theSameElementsAs Seq(Private(None), Private(None))
    }

    "parse headers and join them successfully" in {
      val directives = CacheDirectiveParser.parse(Seq("private", "no-cache"))

      directives should contain theSameElementsAs Seq(Private(None), NoCache(None))
    }

    "parse complicated headers and join them with duplicates" in {
      val directives = CacheDirectiveParser.parse(Seq("private, no-cache, max-age=3600", "private, no-transform"))

      directives should contain theSameElementsAs Seq(Private(None), NoCache(None), MaxAge(Seconds.seconds(3600)), Private(None), NoTransform)
    }

    "parse random input as cache directives" in {
      // include embedded commas to make things interesting...
      val directives = CacheDirectiveParser.parse(Seq("""i,  have, no=idea,  what="I'm, doing""""))
      val badSequence = Seq(
        CacheDirectiveExtension("i", None),
        CacheDirectiveExtension("have", None),
        CacheDirectiveExtension("no", Some("idea")),
        CacheDirectiveExtension("what", Some("I'm, doing"))
      )

      directives should contain theSameElementsAs badSequence
    }
  }
}
