/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import java.net.URI

import org.scalatest.WordSpec
import org.scalatest.Matchers._
import HeaderNames._

class CurrentAgeCalculatorSpec extends WordSpec {

  val ageCalculator = new CurrentAgeCalculator()
  val zeroSeconds = Seconds.seconds(0)

  def defaultResponse(age: Int) = {
    val uri = new URI("http://example.com/data")
    val status = 200
    val requestMethod = "GET"
    val now = HttpDate.now
    val ageSeconds = Seconds.seconds(60)
    val headers = Map(
      `Date` -> Seq(HttpDate.format(now)),
      `Age` -> Seq(ageSeconds.seconds.toString))
    StoredResponse(uri, status, headers, requestMethod, Map())
  }

  "calculateAgeValue" should {

    "calculate age from Age header" in {
      val now = HttpDate.now
      val ageSeconds = Seconds.seconds(60)
      val headers = Map(
        `Date` -> Seq(HttpDate.format(now)),
        `Age` -> Seq(ageSeconds.seconds.toString))

      val seconds = ageCalculator.calculateAgeValue(headers)
      seconds should be(ageSeconds)
    }

    "calculate age as 0 if Age header is missing" in {
      val now = HttpDate.now
      val headers = Map(
        `Date` -> Seq(HttpDate.format(now)))
      val seconds = ageCalculator.calculateAgeValue(headers)
      seconds should be(Seconds.seconds(0))
    }
  }

  "calculateDateValue" should {

    "calculate age from Date header" in {
      val ageSeconds = Seconds.seconds(60)
      val date = HttpDate.now.minus(ageSeconds)
      val headers = Map(
        `Date` -> Seq(HttpDate.format(date)))

      val calculatedDate = ageCalculator.calculateDateValue(headers)
      HttpDate.diff(calculatedDate, date) should be(zeroSeconds)
    }

    "throw exception if Date header is missing" in {
      val headers = Map(
        `Age` -> Seq("60"))
      a[CacheControlException] should be thrownBy { ageCalculator.calculateDateValue(headers) }
    }
  }

  "calculateAge" should {

    "work with a Date header" in {
      val now = HttpDate.now
      // The origin server generated this content 60 seconds ago.
      val originDate = now.minusSeconds(60)

      val requestTime = now
      val responseTime = now

      val headers = Map(
        `Date` -> Seq(HttpDate.format(originDate)))
      val seconds = ageCalculator.calculateCurrentAge(headers, now, requestTime, responseTime)
      seconds should be(Seconds.seconds(60))
    }

    "work with a Date header and a response delay" in {
      val now = HttpDate.now
      // The origin server generated this content 60 seconds ago.
      val originDate = now.minusSeconds(60)

      // The response came back two seconds after the request.
      val requestTime = now.minusSeconds(2)
      val responseTime = now

      val headers = Map(
        `Date` -> Seq(HttpDate.format(originDate)))
      // Because there's no age header, it comes back with
      // apparent_age = responseTime (now) - dateValue (now - 60) = 60 seconds
      // corrected_age_value = age_value (0) + response_delay (2 seconds) = 2 seconds
      // corrected_initial_age = max(apprent_age, corrected_age_value) = 60 seconds
      // resident_time = now - response_time (now) = 0 seconds
      // current_age = corrected_initial_age (60 seconds) + resident_time (0 seconds) = 60 seconds
      val seconds = ageCalculator.calculateCurrentAge(headers, now, requestTime, responseTime)
      seconds should be(Seconds.seconds(60))
    }

    "work with an Age header" in {
      val now = HttpDate.now
      // The origin server generated this content 60 seconds ago.
      val originAge = Seconds.seconds(60)
      val originDate = now.minus(originAge)

      val requestTime = now
      val responseTime = now

      val headers = Map(
        `Age` -> Seq(originAge.seconds.toString),
        `Date` -> Seq(HttpDate.format(originDate)))
      val seconds = ageCalculator.calculateCurrentAge(headers, now, requestTime, responseTime)
      seconds should be(Seconds.seconds(60))
    }

    "work with an Age header and a response delay of two seconds" in {
      val now = HttpDate.now
      // The origin server generated this content 60 seconds ago.
      val originAge = Seconds.seconds(60)
      val originDate = now.minus(originAge)

      // The response came back two seconds after the request.
      val requestTime = now.minusSeconds(2)
      val responseTime = now

      val headers = Map(
        `Age` -> Seq(originAge.seconds.toString),
        `Date` -> Seq(HttpDate.format(originDate)))
      val seconds = ageCalculator.calculateCurrentAge(headers, now, requestTime, responseTime)
      // The final content should be 62 seconds.
      seconds should be(Seconds.seconds(62))
    }

  }

}
