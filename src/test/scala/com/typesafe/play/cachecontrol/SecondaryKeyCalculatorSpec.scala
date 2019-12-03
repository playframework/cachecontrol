/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import java.time.Duration

import HeaderNames._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class SecondaryKeyCalculatorSpec extends AnyWordSpec {
  def responseHeaders = {
    val now = HttpDate.now
    val age = Duration.ofSeconds(60)
    val headers = Map(
      `Date`                         -> Seq(HttpDate.format(now)),
      `Age`                          -> Seq(age.getSeconds.toString),
      HeaderName("Content-Encoding") -> Seq("gzip")
    )
    headers
  }

  def defaultRequest = {
    val uri     = new java.net.URI("http://example.com/data")
    val headers = Map(HeaderName("Accept-Encoding") -> Seq("gzip"))
    CacheRequest(uri, "GET", headers)
  }

  "Secondary Calculators" should {
    val calculator = new SecondaryKeyCalculator()

    "be None with no Vary header" in {
      val request = defaultRequest
      val matches = calculator.calculate(request, responseHeaders)

      matches should be(None)
    }

    "return the wildcard itself with a Vary header of *" in {
      val request = defaultRequest
      val headers = responseHeaders ++ Map(HeaderName("Vary") -> List("Some-Value", "*"))
      val matches = calculator.calculate(request, headers)

      matches should be(Some(Map(HeaderName("Vary") -> List("*"))))
    }

    "be Some with the request's Accept-Encoding value with a Vary header of Accept-Encoding" in {
      val request = defaultRequest
      val headers = responseHeaders ++ Map(`Vary` -> Seq("Accept-Encoding"))
      val matches = calculator.calculate(request, headers)

      matches should be(Some(Map(HeaderName("Accept-Encoding") -> Seq("gzip"))))
    }
  }
}
