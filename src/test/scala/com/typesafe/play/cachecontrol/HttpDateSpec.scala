/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
package com.typesafe.play.cachecontrol

import org.joda.time.format.DateTimeFormat
import org.joda.time._
import org.scalatest.{ TryValues, WordSpec }

import org.scalatest.MustMatchers

/**
 *
 */
class HttpDateSpec extends WordSpec with TryValues with MustMatchers {

  "parse a date in IMF-fixdate format" in {
    val expectedDate = new DateTime(1994, 11, 6, 8, 49, 37, DateTimeZone.forID("GMT"))
    val dateString = "Sun, 06 Nov 1994 08:49:37 GMT"
    (HttpDate.parse(dateString)) must ===(expectedDate)
  }

  "parse a date in RFC 850 format" in {
    val expectedDate = new DateTime(1994, 11, 6, 8, 49, 37, DateTimeZone.forID("GMT"))
    val dateString = "Sunday, 06-Nov-94 08:49:37 GMT"
    HttpDate.parse(dateString) must ===(expectedDate)

  }

  "parse a date that is > 50 years in the future correctly" in {
    pendingUntilFixed {
      val rfc850Format = DateTimeFormat.forPattern("EEE, dd-MMM-yy HH:mm:ss 'GMT'")
        .withLocale(java.util.Locale.ENGLISH)
        .withZone(DateTimeZone.forID("GMT"))

      // 2166-11-06T08:49:37.000Z
      val actualDate = new DateTime(2166, 11, 6, 8, 49, 37, DateTimeZone.forID("GMT"))

      // Thu, 06-Nov-66 08:49:37 GMT
      val futureDateString = rfc850Format.print(actualDate)

      // 1966-11-06T08:49:37.000Z
      val expectedDate = new DateTime(1966, 11, 6, 8, 49, 37, DateTimeZone.forID("GMT"))

      HttpDate.parse(futureDateString) must ===(expectedDate)
    }
  }

  "parse a date in asctime format" in {
    val expectedDate = new DateTime(1994, 11, 6, 8, 49, 37, DateTimeZone.forID("GMT"))
    val dateString = "Sun Nov 6 08:49:37 1994"
    HttpDate.parse(dateString) must ===(expectedDate)
  }

  "format a date in IMF format" in {
    val expectedString = "Sun, 06 Nov 1994 08:49:37 GMT"
    val date = new DateTime(1994, 11, 6, 8, 49, 37, DateTimeZone.forID("GMT"))
    HttpDate.format(date) must ===(expectedString)
  }
}

