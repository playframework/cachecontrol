package com.typesafe.cachecontrol

import java.net.URI

import com.typesafe.cachecontrol.HeaderNames._
import org.scalatest.Matchers._
import org.scalatest.WordSpec

/**
 *
 */
class StripHeaderCalculatorSpec extends WordSpec {

  val privateCache = new StubCache(shared = false)
  val sharedCache = new StubCache(shared = true)

  def defaultHeaders = {
    Map(
      `Date` -> Seq(HttpDate.format(HttpDate.now))
    )
  }

  def defaultRequest = {
    val uri = new java.net.URI("http://example.com/data")
    val headers = Map[HeaderName, collection.immutable.Seq[String]]()
    CacheRequest(uri, "GET", headers)
  }

  def defaultResponse = {
    val uri = new URI("http://example.com/data")
    val status = 200

    OriginResponse(uri, status, defaultHeaders)
  }

  def h(value: String) = {
    defaultHeaders ++ Map(HeaderNames.`Cache-Control` -> Seq(value))
  }

  "stripHeaders" should {

    "contain no-cache results" in {
      val stripper = new StripHeaderCalculator(privateCache)
      val response: OriginResponse = defaultResponse.copy(headers = h("no-cache=Set-Cookie"))

      val result = stripper.stripHeaders(response)

      result should be(Set(HeaderName("Set-Cookie")))
    }

    "contain private results" in {
      val stripper = new StripHeaderCalculator(sharedCache)
      val response: OriginResponse = defaultResponse.copy(headers = h("private=Set-Cookie"))

      val result = stripper.stripHeaders(response)

      result should be(Set(HeaderName("Set-Cookie")))
    }

    "contain no results when cache is private" in {
      val stripper = new StripHeaderCalculator(privateCache)
      val response: OriginResponse = defaultResponse.copy(headers = h("private=Set-Cookie"))

      val result = stripper.stripHeaders(response)

      result should be(Set())
    }

  }
}
