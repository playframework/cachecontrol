/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.ZonedDateTime

import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/**
 *
 */
class HttpDateSpec extends AnyWordSpec with TryValues with Matchers {
  "parse a date in IMF-fixdate format" in {
    val expectedDate = ZonedDateTime.of(1994, 11, 6, 8, 49, 37, 0, HttpDate.zone)
    val dateString   = "Sun, 06 Nov 1994 08:49:37 GMT"
    (HttpDate.parse(dateString)) must ===(expectedDate)
  }

  "parse a date in RFC 850 format" in {
    val expectedDate = ZonedDateTime.of(1994, 11, 6, 8, 49, 37, 0, HttpDate.zone)
    val dateString   = "Sunday, 06-Nov-94 08:49:37 GMT"
    HttpDate.parse(dateString) must ===(expectedDate)
  }

  "parse a date that is > 50 years in the future correctly" in {
    pendingUntilFixed {
      val rfc850Format = DateTimeFormatter
        .ofPattern("EEE, dd-MMM-yy HH:mm:ss 'GMT'")
        .withLocale(java.util.Locale.ENGLISH)
        .withZone(ZoneOffset.UTC)

      // 2166-11-06T08:49:37.000Z
      val actualDate = ZonedDateTime.of(2166, 11, 6, 8, 49, 37, 0, HttpDate.zone)

      // Thu, 06-Nov-66 08:49:37 GMT
      val futureDateString = rfc850Format.format(actualDate)

      // 1966-11-06T08:49:37.000Z
      val expectedDate = ZonedDateTime.of(1966, 11, 6, 8, 49, 37, 0, HttpDate.zone)

      HttpDate.parse(futureDateString) must ===(expectedDate)
    }
  }

  "parse a date in asctime format" in {
    val expectedDate = ZonedDateTime.of(1994, 11, 6, 8, 49, 37, 0, HttpDate.zone)
    val dateString   = "Sun Nov 6 08:49:37 1994"
    HttpDate.parse(dateString) must ===(expectedDate)
  }

  "format a date in IMF format" in {
    val expectedString = "Sun, 06 Nov 1994 08:49:37 GMT"
    val date           = ZonedDateTime.of(1994, 11, 6, 8, 49, 37, 0, HttpDate.zone)
    HttpDate.format(date) must ===(expectedString)
  }
}
