/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
package com.typesafe.play.cachecontrol

import java.net.URI

import CacheDirectives.CacheDirectiveExtension
import HeaderNames._
import ResponseSelectionActions.{ GatewayTimeout, SelectedResponse, ForwardToOrigin }
import org.joda.time.Seconds
import org.scalatest.Matchers._
import org.scalatest.WordSpec

/**
 *
 */
class ResponseSelectionCalculatorSpec extends WordSpec {

  val cache = new StubCache(shared = false)

  def defaultHeaders(seconds: Int = 60): Map[HeaderName, Seq[String]] = {
    val age = Seconds.seconds(seconds)
    val date = HttpDate.now.minus(age)
    val headers = Map(
      `Date` -> Seq(HttpDate.format(date)),
      `Age` -> Seq(age.getSeconds.toString)
    )
    headers
  }

  def defaultResponse = {
    val uri = new URI("http://example.com/data")
    val status = 200
    val requestMethod = "GET"

    val nominatedHeaders = Map[HeaderName, Seq[String]]()

    StoredResponse(uri, status, defaultHeaders(), requestMethod, nominatedHeaders)
  }

  def defaultRequest = {
    val uri = new java.net.URI("http://example.com/data")
    val headers = Map[HeaderName, collection.immutable.Seq[String]]()
    CacheRequest(uri, "GET", headers)
  }

  "serveResponse" when {

    "matching URI and no nominated headers" should {

      "return SelectedResponse" in {
        val policy = new ResponseSelectionCalculator(cache)
        val request = defaultRequest
        val response = defaultResponse
        val action = policy.selectResponse(request, Seq(response))

        action should be(SelectedResponse(response, 0))
      }
    }

    "the effective request URI does not match the stored response URI" should {
      "return ForwardToOrigin" in {
        // The presented effective request URI (Section 5.5 of [RFC7230]) and
        // that of the stored response match, and
        val policy = new ResponseSelectionCalculator(cache)
        val request = defaultRequest
        val response = defaultResponse.copy(uri = new URI("http://some.other.example.com/cache"))
        val action = policy.selectResponse(request, Seq(response))

        val reason = "Valid response not found for request"
        action should be(ForwardToOrigin(reason))
      }
    }

    "the request method associated with the stored response does not allow it to be used" should {
      "return ForwardToOrigin" in {
        // the RFC is not very clear on what "allowed" means here.

        val policy = new ResponseSelectionCalculator(cache)
        val request = defaultRequest.copy(method = "POST")
        val response = defaultResponse
        val action = policy.selectResponse(request, Seq(response))

        val reason = "Valid response not found for request"
        action should be(ForwardToOrigin(reason))
      }
    }

    "the selecting header fields do not match" should {

      "return ForwardToOrigin if no presenting header by that name" in {
        // o  selecting header fields nominated by the stored response (if any)
        // match those presented (see Section 4.1), and
        val policy = new ResponseSelectionCalculator(cache)
        val request = defaultRequest
        val nominatedHeaders = Map(HeaderName("X-Custom-Header") -> Seq("value1"))
        val response = defaultResponse.copy(nominatedHeaders = nominatedHeaders)
        val action = policy.selectResponse(request, Seq(response))

        val reason = "Valid response not found for request"
        action should be(ForwardToOrigin(reason))
      }

      "return ForwardToOrigin if a presenting header has the wrong value" in {
        // o  selecting header fields nominated by the stored response (if any)
        // match those presented (see Section 4.1), and
        val policy = new ResponseSelectionCalculator(cache)
        val presentingHeaders = Map(HeaderName("X-Custom-Header") -> Seq("value1"))
        val request = defaultRequest.copy(headers = presentingHeaders)
        val nominatedHeaders = Map(HeaderName("X-Custom-Header") -> Seq("value2"))
        val response = defaultResponse.copy(nominatedHeaders = nominatedHeaders)
        val action = policy.selectResponse(request, Seq(response))

        val reason = "Valid response not found for request"
        action should be(ForwardToOrigin(reason))
      }

      "return ForwardToOrigin if only some of the presenting headers match" in {
        // o  selecting header fields nominated by the stored response (if any)
        // match those presented (see Section 4.1), and
        val policy = new ResponseSelectionCalculator(cache)
        val presentingHeaders = Map(HeaderName("X-Custom-Header") -> Seq("value1"))
        val request = defaultRequest.copy(headers = presentingHeaders)
        val nominatedHeaders = Map(HeaderName("X-Custom-Header") -> Seq("value1"), HeaderName("X-Custom-Header") -> Seq("value2"))
        val response = defaultResponse.copy(nominatedHeaders = nominatedHeaders)
        val action = policy.selectResponse(request, Seq(response))

        val reason = "Valid response not found for request"
        action should be(ForwardToOrigin(reason))
      }

    }

    "matching headers are found" should {

      "return SelectedResponse" in {
        // o  selecting header fields nominated by the stored response (if any)
        // match those presented (see Section 4.1), and
        val policy = new ResponseSelectionCalculator(cache)
        val presentingHeaders = Map(HeaderName("X-Custom-Header") -> Seq("value1"))
        val request = defaultRequest.copy(headers = presentingHeaders)
        val nominatedHeaders = Map(HeaderName("X-Custom-Header") -> Seq("value1"))
        val response = defaultResponse.copy(nominatedHeaders = nominatedHeaders)
        val action = policy.selectResponse(request, Seq(response))

        action should be(SelectedResponse(response, 0))
      }

      "return SelectedResponse with the most recent stored response if multiple responses" in {
        //If multiple selected responses are available (potentially including
        //responses without a Vary header field), the cache will need to choose
        //one to use.  When a selecting header field has a known mechanism for
        //doing so (e.g., qvalues on Accept and similar request header fields),
        //that mechanism MAY be used to select preferred responses; of the
        //remainder, the most recent response (as determined by the Date header
        //field) is used, as per Section 4.

        val policy = new ResponseSelectionCalculator(cache)

        val presentingHeaders = Map(HeaderName("X-Custom-Header") -> Seq("value1"))
        val request = defaultRequest.copy(headers = presentingHeaders)

        val nominatedHeaders = Map(HeaderName("X-Custom-Header") -> Seq("value1"))
        val responseOne = defaultResponse.copy(nominatedHeaders = nominatedHeaders, headers = defaultHeaders(60))
        val responseTwo = defaultResponse.copy(nominatedHeaders = nominatedHeaders, headers = defaultHeaders(120))
        val responseThree = defaultResponse.copy(nominatedHeaders = nominatedHeaders, headers = defaultHeaders(30))
        val responseFour = defaultResponse.copy(nominatedHeaders = nominatedHeaders, headers = defaultHeaders(90))

        val action = policy.selectResponse(request, Seq(responseOne, responseTwo, responseThree, responseFour))

        action should be(SelectedResponse(responseThree, 2))
      }
    }

    "no response is found" should {

      "return ForwardToOrigin" in {
        val policy = new ResponseSelectionCalculator(cache)

        val uri = new java.net.URI("http://localhost/cache")
        val headers = Map[HeaderName, Seq[String]]()
        val request = new CacheRequest(uri, "GET", headers)

        val action = policy.selectResponse(request, Seq())
        action should be(ForwardToOrigin("Valid response not found for request"))
      }

      "return GatewayTimeout if request specifies only-if-cached" in {
        val policy = new ResponseSelectionCalculator(cache)

        val uri = new java.net.URI("http://localhost/cache")
        val headers = Map[HeaderName, Seq[String]](`Cache-Control` -> Seq("only-if-cached"))
        val request = new CacheRequest(uri, "GET", headers)

        val action = policy.selectResponse(request, Seq())
        action should be(GatewayTimeout("Response not found and request contains only-if-cached"))
      }
    }

  }

}
