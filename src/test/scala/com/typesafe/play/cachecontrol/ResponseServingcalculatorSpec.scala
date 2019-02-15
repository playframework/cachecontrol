/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
package com.typesafe.play.cachecontrol

import java.net.URI

import HeaderNames._
import org.joda.time.Seconds
import org.scalactic.ConversionCheckedTripleEquals
import org.scalatest.Matchers._
import org.scalatest.WordSpec

class ResponseServingCalculatorSpec extends WordSpec with ConversionCheckedTripleEquals {
  import ResponseServeActions._

  val privateCache = new StubCache(shared = false)

  val sharedCache = new StubCache(shared = true)

  def defaultResponse = {
    val uri = new URI("http://example.com/data")
    val status = 200
    val requestMethod = "GET"
    val now = HttpDate.now
    val age = Seconds.seconds(60)
    val headers = Map(
      `Date` -> Seq(HttpDate.format(now)),
      `Age` -> Seq(age.getSeconds.toString))
    val nominatedHeaders = Map[HeaderName, Seq[String]]()
    StoredResponse(uri, status, headers, requestMethod, nominatedHeaders)
  }

  def defaultRequest = {
    val uri = new java.net.URI("http://example.com/data")
    val headers = Map[HeaderName, collection.immutable.Seq[String]]()
    CacheRequest(uri, "GET", headers)
  }

  "serviceResponse" when {

    //--------------------------------------------------------------------------------
    // Request Directives
    //--------------------------------------------------------------------------------

    "no-cache request directive" should {

      "return Validate" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest.copy(headers = defaultRequest.headers ++ Map(`Cache-Control` -> Seq("no-cache")))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ Map(`Cache-Control` -> Seq("max-age=60")))

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(5))
        val msg = "Request contains no-cache directive, validation required"
        action should be(Validate(msg))
      }

    }

    "no-cache pragma" should {
      "return Validate" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest.copy(headers = defaultRequest.headers ++ Map(`Pragma` -> Seq("no-cache")))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ Map(`Cache-Control` -> Seq("max-age=60")))

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(65))
        val msg = "Request does not contain Cache-Control header found, but does contains no-cache Pragma header, validation required"
        action should be(Validate(msg))
      }
    }

    "no-store request directive" should {

      "return ServeFresh when fresh" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest.copy(headers = defaultRequest.headers ++ Map(`Cache-Control` -> Seq("no-store")))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ Map(`Cache-Control` -> Seq("publish,max-age=60")))

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(5))
        val msg = "Fresh response: lifetime = PT60S, PT55S seconds left"
        action should be(ServeFresh(msg))
      }

      "return Validate when stale" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest.copy(headers = defaultRequest.headers ++ Map(`Cache-Control` -> Seq("no-store")))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ Map(`Cache-Control` -> Seq("publish,max-age=60")))

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        val msg = "Response is stale, and stale response is not allowed"
        action should be(Validate(msg))
      }
    }

    "max-stale request directive" should {

      "return ServeFresh when fresh" in {
        val policy = new ResponseServingCalculator(privateCache)

        val requestHeaders = Map(`Cache-Control` -> Seq("max-stale"))
        val request = defaultRequest.copy(headers = defaultRequest.headers ++ requestHeaders)

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=10"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(5))
        action should be(ServeFresh("Fresh response: lifetime = PT10S, PT5S seconds left"))
      }

      "return ServeStale when stale but there is max-age with no delta" in {
        val policy = new ResponseServingCalculator(privateCache)

        val requestHeaders = Map(`Cache-Control` -> Seq("max-stale"))
        val request = defaultRequest.copy(headers = defaultRequest.headers ++ requestHeaders)

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=10"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(500))
        action should be(ServeStale(s"Request contains no-args max-stale directive"))
      }

      "return ServeStale when stale but within range" in {
        val policy = new ResponseServingCalculator(privateCache)

        val requestHeaders = Map(`Cache-Control` -> Seq("max-stale=60"))
        val request = defaultRequest.copy(headers = defaultRequest.headers ++ requestHeaders)

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=10"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(30))
        action should be(ServeStale(s"Request contains max-stale=60, current age = 30 which is inside range"))
      }

      "return Validate when stale outside of max-age" in {
        val policy = new ResponseServingCalculator(privateCache)

        val requestHeaders = Map(`Cache-Control` -> Seq("max-stale=60"))
        val request = defaultRequest.copy(headers = defaultRequest.headers ++ requestHeaders)

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=10"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(120))
        action should be(Validate(s"Response is stale, and stale response is not allowed"))
      }
    }

    "max-age request directive" should {

      "return Validate when max-age=0" in {
        val policy = new ResponseServingCalculator(privateCache)

        val requestHeaders = Map(`Cache-Control` -> Seq("max-age=0"))
        val request = defaultRequest.copy(headers = defaultRequest.headers ++ requestHeaders)

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(Validate(s"Response is stale, and stale response is not allowed"))
      }

      "return ServeFresh when in range" in {
        val policy = new ResponseServingCalculator(privateCache)

        val requestHeaders = Map(`Cache-Control` -> Seq("max-age=60"))
        val request = defaultRequest.copy(headers = defaultRequest.headers ++ requestHeaders)

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(ServeFresh(s"Fresh response: lifetime = PT120S, PT60S seconds left"))
      }

      "return Validate when not in range" in {
        val policy = new ResponseServingCalculator(privateCache)

        val requestHeaders = Map(`Cache-Control` -> Seq("max-age=60"))
        val request = defaultRequest.copy(headers = defaultRequest.headers ++ requestHeaders)

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(120))
        action should be(Validate(s"Response is stale, and stale response is not allowed"))
      }

    }

    "min-fresh request directive" should {

      "return ServeFresh when in range" in {
        val policy = new ResponseServingCalculator(privateCache)

        val requestHeaders = Map(`Cache-Control` -> Seq("min-fresh=60"))
        val request = defaultRequest.copy(headers = defaultRequest.headers ++ requestHeaders)

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(ServeFresh(s"Fresh response: minFresh = PT60S, freshnessLifetime = PT120S, currentAge = PT60S"))
      }

      "return Validate when out of range" in {
        val policy = new ResponseServingCalculator(privateCache)

        val requestHeaders = Map(`Cache-Control` -> Seq("min-fresh=60"))
        val request = defaultRequest.copy(headers = defaultRequest.headers ++ requestHeaders)

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(120))
        action should be(Validate(s"Response is stale, and stale response is not allowed"))
      }

      "return Validate when in range but combined with max-age" in {
        val policy = new ResponseServingCalculator(privateCache)

        val requestHeaders = Map(`Cache-Control` -> Seq("min-fresh=60, max-age=30"))
        val request = defaultRequest.copy(headers = defaultRequest.headers ++ requestHeaders)

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(Validate(s"Response is stale, and stale response is not allowed"))
      }
    }

    "stale-if-error request directive" should {
      // https://tools.ietf.org/html/rfc5861#section-4
      "return Validate with stale-if-error = true when in range" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest
        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=10,stale-if-error=30"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(20))
        action should be(Validate(s"Response is stale, and stale response is not allowed", staleIfError = true))
      }

      "return Validate with stale-if-error = false when out of range" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest
        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=10,stale-if-error=30"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(Validate(s"Response is stale, and stale response is not allowed", staleIfError = false))
      }
    }

    //--------------------------------------------------------------------------------
    // Response Directives
    //--------------------------------------------------------------------------------

    "no-cache response directive" should {

      "always return Validate, even when fresh" in {
        val policy = new ResponseServingCalculator(privateCache)

        val headers = Map(`Cache-Control` -> Seq("max-age=600, no-cache"))
        val request = defaultRequest
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ headers)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(Validate("Response contains no-args no-cache directive"))
      }

    }

    "no-store response directive" should {

      "return ServeFresh when fresh" in {
        val policy = new ResponseServingCalculator(privateCache)

        val headers = Map(`Cache-Control` -> Seq("max-age=600, no-store"))
        val request = defaultRequest
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ headers)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(ServeFresh("Fresh response: lifetime = PT600S, PT540S seconds left"))
      }

      "return Validate when stale" in {
        //A cache MUST NOT generate a stale response if it is prohibited by an
        //explicit in-protocol directive (e.g., by a "no-store" or "no-cache"
        //cache directive, a "must-revalidate" cache-response-directive, or an
        //applicable "s-maxage" or "proxy-revalidate" cache-response-directive;
        //see Section 5.2.2).
        val policy = new ResponseServingCalculator(privateCache)

        val headers = Map(`Cache-Control` -> Seq("max-age=10, no-store"))
        val request = defaultRequest
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ headers)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(Validate("Response is stale, and stale response is not allowed"))
      }

    }

    "must-revalidate response directive" should {

      "return ServeFresh when fresh" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120, must-revalidate"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(ServeFresh("Fresh response: lifetime = PT120S, PT60S seconds left"))
      }

      "return ValidateOrTimeout when stale" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120, must-revalidate"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(300))
        action should be(ValidateOrTimeout(s"Response is stale, response contains must-revalidate directive"))
      }

    }

    "max-age response directive" should {
      //The "max-age" response directive indicates that the response is to be
      //considered stale after its age is greater than the specified number
      //of seconds.

      "return ServeFresh when fresh" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=120"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(ServeFresh("Fresh response: lifetime = PT120S, PT60S seconds left"))
      }

      "return Validate when stale" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=120"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(300))
        action should be(Validate(s"Response is stale, and stale response is not allowed"))
      }

      "return Validate when max-age=0" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=0"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(0))
        action should be(Validate(s"Response is stale, and stale response is not allowed"))
      }
    }

    "s-maxage response directive" when {

      "private cache" should {

        "return ServeFresh when fresh (s-maxage is ignored)" in {
          val policy = new ResponseServingCalculator(privateCache)

          val request = defaultRequest

          val responseHeaders = Map(`Cache-Control` -> Seq("max-age=300,s-maxage=0"))
          val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

          val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(0))
          action should be(ServeFresh(s"Fresh response: lifetime = PT300S, PT300S seconds left"))
        }

        "return Validate when stale (s-maxage is ignored)" in {
          val policy = new ResponseServingCalculator(privateCache)

          val request = defaultRequest

          val responseHeaders = Map(`Cache-Control` -> Seq("max-age=0,s-maxage=120"))
          val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

          val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
          action should be(Validate(s"Response is stale, and stale response is not allowed"))
        }

      }

      "shared cache" should {

        //The "s-maxage" response directive indicates that, in shared caches,
        //the maximum age specified by this directive overrides the maximum age
        //specified by either the max-age directive or the Expires header
        //field.

        "return ServeFresh when fresh, overriding max-age" in {
          val policy = new ResponseServingCalculator(sharedCache)

          val request = defaultRequest

          val responseHeaders = Map(`Cache-Control` -> Seq("max-age=0,s-maxage=120"))
          val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

          val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
          action should be(ServeFresh("Fresh response: lifetime = PT120S, PT60S seconds left"))
        }

        // The s-maxage directive also implies the semantics of the proxy-revalidate response directive.
        "return ValidateOrTimeout when stale" in {
          val policy = new ResponseServingCalculator(sharedCache)

          val request = defaultRequest

          // should override max-age
          val responseHeaders = Map(`Cache-Control` -> Seq("max-age=600, s-maxage=120"))
          val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

          val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(300))
          action should be(ValidateOrTimeout(s"Response is stale, response contains s-maxage directive and cache is shared"))
        }

        // Double check that s-maxage=0 works as defined
        "return ValidateOrTimeout when s-maxage=0" in {
          //In particular, a response with
          //either "max-age=0, must-revalidate" or "s-maxage=0" cannot be used to
          //satisfy a subsequent request without revalidating it on the origin
          //server.
          //
          // In other words, "s-maxage=0" === "max-age=0, must-revalidate"

          val policy = new ResponseServingCalculator(sharedCache)

          val request = defaultRequest

          val responseHeaders = Map(`Cache-Control` -> Seq("s-maxage=0"))
          val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

          val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(0))
          action should be(ValidateOrTimeout(s"Response is stale, response contains s-maxage directive and cache is shared"))
        }
      }

    }

    //The "proxy-revalidate" response directive has the same meaning as the
    //must-revalidate response directive, except that it does not apply to
    //private caches.
    "proxy-revalidate response directive" should {

      "return ServeFresh when fresh" in {
        val policy = new ResponseServingCalculator(sharedCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120, proxy-revalidate"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(60))
        action should be(ServeFresh("Fresh response: lifetime = PT120S, PT60S seconds left"))
      }

      "return ValidateOrTimeout when stale" in {
        val policy = new ResponseServingCalculator(sharedCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120, proxy-revalidate"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        //The "must-revalidate" response directive indicates that once it has
        //become stale, a cache MUST NOT use the response to satisfy subsequent
        //requests without successful validation on the origin server.
        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(300))
        action should be(ValidateOrTimeout(s"Response is stale, response contains proxy-revalidate directive and cache is shared"))
      }

      "return Validate when stale and private cache" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("public, max-age=120, proxy-revalidate"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        //The "must-revalidate" response directive indicates that once it has
        //become stale, a cache MUST NOT use the response to satisfy subsequent
        //requests without successful validation on the origin server.
        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(300))
        action should be(Validate(s"Response is stale, and stale response is not allowed"))
      }
    }

    "stale-while-revalidate response directive" should {
      // https://tools.ietf.org/html/rfc5861#section-3
      "return ServeFresh when fresh" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=120, stale-while-revalidate=30"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(30))
        action should be(ServeFresh(s"Fresh response: lifetime = PT120S, PT90S seconds left"))
      }

      "return ServeStaleAndValidate when stale but in range" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=120, stale-while-revalidate=30"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(130))
        action should be(ServeStaleAndValidate(s"Response contains stale-while-revalidate and is within delta range PT30S"))
      }

      "return Validate when stale" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=120, stale-while-revalidate=30"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(300))
        action should be(Validate(s"Response is stale, and stale response is not allowed"))
      }

      "return Validate when stale and combined with stale-if-error" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest
        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=120, stale-while-revalidate=30, stale-if-error=600"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(300))
        action should be(Validate(s"Response is stale, and stale response is not allowed", staleIfError = true))
      }
    }

    "stale-if-error response directive" should {

      "return ServeFresh when fresh" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=120, stale-if-error=30"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(30))
        action should be(ServeFresh(s"Fresh response: lifetime = PT120S, PT90S seconds left"))
      }

      "return Validate with stale-if-error=true when stale but in range" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=120, stale-if-error=30"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(130))
        action should be(Validate(s"Response is stale, and stale response is not allowed", staleIfError = true))
      }

      // https://tools.ietf.org/html/rfc5861#section-4
      "return Validate with stale-if-error=false when not in range" in {
        val policy = new ResponseServingCalculator(privateCache)

        val request = defaultRequest

        val responseHeaders = Map(`Cache-Control` -> Seq("max-age=120, stale-if-error=30"))
        val response = defaultResponse.copy(headers = defaultResponse.headers ++ responseHeaders)

        val action: ResponseServeAction = policy.serveResponse(request, response, Seconds.seconds(200))
        action should be(Validate(s"Response is stale, and stale response is not allowed", staleIfError = false))
      }

    }

  }

}
