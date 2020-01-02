/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import java.net.URI

import CacheDirectives.NoCache
import CacheDirectives.Private
import HeaderNames._
import ResponseCachingActions.DoCacheResponse
import ResponseCachingActions.DoNotCacheResponse
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class ResponseCachingCalculatorSpec extends AnyWordSpec {
  val privateCache = new StubCache(shared = false)

  val sharedCache = new StubCache(shared = true)

  def defaultHeaders = {
    Map(`Date` -> Seq(HttpDate.format(HttpDate.now)))
  }

  def defaultRequest = {
    val uri     = new java.net.URI("http://example.com/data")
    val headers = Map[HeaderName, collection.immutable.Seq[String]]()
    CacheRequest(uri, "GET", headers)
  }

  def defaultResponse = {
    val uri    = new URI("http://example.com/data")
    val status = 200

    OriginResponse(uri, status, defaultHeaders)
  }

  def h(value: String) = {
    defaultHeaders ++ Map(HeaderNames.`Cache-Control` -> Seq(value))
  }

  "isCacheable" when {
    "with non cacheable method" should {
      "return DoNotCacheResponse" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest.copy(method = "OPTIONS")
        val response: OriginResponse = defaultResponse
        val result                   = policy.isCacheable(request, response)

        result should be(DoNotCacheResponse("Request method OPTIONS is not cacheable"))
      }
    }

    "with ineligible response code" should {
      "return DoNotCacheResponse" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(status = 600)
        val result                   = policy.isCacheable(request, response)

        result should be(DoNotCacheResponse("Response code 600 is not understood by the cache"))
      }
    }

    "with Authorized Header in request" should {
      val authHeader = Map(HeaderNames.`Authorization` -> Seq("*"))

      "return DoNotCacheResponse if a shared cache" in {
        val policy                   = new ResponseCachingCalculator(sharedCache)
        val request: CacheRequest    = defaultRequest.copy(headers = authHeader)
        val response: OriginResponse = defaultResponse
        val result                   = policy.isCacheable(request, response)

        result should be(
          DoNotCacheResponse("Cache is shared, authorization header found, no cache directives allow authorization")
        )
      }

      "return DoCacheResponse if a shared cache and explicitly allowed" in {
        val policy                   = new ResponseCachingCalculator(sharedCache)
        val request: CacheRequest    = defaultRequest.copy(headers = authHeader)
        val response: OriginResponse = defaultResponse.copy(headers = h("public"))
        val result                   = policy.isCacheable(request, response)

        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }

      "return DoCacheResponse if a private cache" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest.copy(headers = authHeader)
        val response: OriginResponse = defaultResponse
        val result                   = policy.isCacheable(request, response)

        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }
    }

    "with no-store request directive" should {
      "return DoNotCacheResponse" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest.copy(headers = h("no-store"))
        val response: OriginResponse = defaultResponse
        val result                   = policy.isCacheable(request, response)
        result should be(DoNotCacheResponse("Request Cache-Control header contains no-store cache directive"))
      }
    }

    "with no-store response directive" should {
      "return DoNotCacheResponse" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("no-store"))
        val result                   = policy.isCacheable(request, response)
        result should be(DoNotCacheResponse("Response Cache-Control header contains no-store cache directive"))
      }
    }

    "with no-cache response directive" should {
      "return DoCacheResponse" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("no-cache"))
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }

      "return DoCacheResponse when qualified" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("no-cache=Set-Cookie"))
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }
    }

    "with private response directive" should {
      "return DoCacheResponse when cache is private" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("private"))
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }

      "return DoNotCacheResponse when cache is shared" in {
        val policy                   = new ResponseCachingCalculator(sharedCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("private"))
        val result                   = policy.isCacheable(request, response)
        result should be(DoNotCacheResponse("Cache is shared, and private directive found in response"))
      }

      "return DoCacheResponse with no stripped headers when qualified and cache is private" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest
        val privDirective            = Private(Some(List("Some-Header")))
        val response: OriginResponse = defaultResponse.copy(headers = h(privDirective.toString))
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }

      "return DoCacheResponse with stripped headers when qualified and cache is shared" in {
        val policy                   = new ResponseCachingCalculator(sharedCache)
        val request: CacheRequest    = defaultRequest
        val priv                     = Private(Some(List("Set-Cookie")))
        val response: OriginResponse = defaultResponse.copy(headers = h(priv.toString))
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }
    }

    "with qualified private and qualified no-cache response directives" should {
      "return DoCache with only no-cache stripped headers when private" in {
        import CacheDirectives._
        val policy                = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest = defaultRequest
        val noCache               = NoCache(Some(List("Set-Cookie")))
        val privDirective         = Private(Some(List("Some-Header")))
        val response: OriginResponse =
          defaultResponse.copy(headers = h(privDirective.toString + "," + noCache.toString))
        val result = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }

      "return DoCache with both stripped headers when shared cache" in {
        import CacheDirectives._
        val policy                = new ResponseCachingCalculator(sharedCache)
        val request: CacheRequest = defaultRequest
        val noCache               = NoCache(Some(List("Set-Cookie")))
        val privDirective         = Private(Some(List("Some-Header")))
        val response: OriginResponse =
          defaultResponse.copy(headers = h(privDirective.toString + "," + noCache.toString))
        val result = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }
    }

    "with Expires header in response" should {
      "return DoCacheResponse" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val expiresHeader            = Map(HeaderNames.`Expires` -> Seq(HttpDate.format(HttpDate.now)))
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = defaultHeaders ++ expiresHeader)
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response contains expires header"))
      }
    }

    "with max-age response directive" should {
      "return DoCacheResponse" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("max-age=600"))
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response contains max-age response directive"))
      }
    }

    "with s-maxage response directive" should {
      "return DoCacheResponse in a shared cache" in {
        val policy                   = new ResponseCachingCalculator(sharedCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("s-maxage=600"))
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response contains s-maxage and the cache is shared"))
      }

      "have no effect in a private cache" in {
        val policy                   = new ResponseCachingCalculator(privateCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("s-maxage=600"))
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }

      "overrides max-age directive" in {
        pending
      }
    }

    "with a containsCachableExtension" should {
      "return DoCacheResponse" in {
        var called = false
        val cacheWithExtensions = new StubCache(shared = false) {
          override def isCacheableExtension(extension: CacheDirectives.CacheDirectiveExtension): Boolean = {
            called = (extension.name == "public-on-tuesday")
            true
          }
        }
        val policy                   = new ResponseCachingCalculator(cacheWithExtensions)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("public-on-tuesday"))
        val result                   = policy.isCacheable(request, response)
        called should be(true)
        result should be(DoCacheResponse("Response contains a cache control extension that allows it to be cached"))
      }
    }

    "with an unrecognized cache extension" should {
      "return DoCacheResponse and be ignored" in {
        val cacheWithExtensions      = new StubCache(shared = false)
        val policy                   = new ResponseCachingCalculator(cacheWithExtensions)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("omg-never-cache"))
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response status code 200 is cacheable by default"))
      }
    }

    "with a status code that is not cacheable by default" should {
      "return DoNotCacheResponse" in {
        val policy                   = new ResponseCachingCalculator(sharedCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(status = 400)
        val result                   = policy.isCacheable(request, response)
        result should be(
          DoNotCacheResponse("Response is not cacheable by default, and there are no explicit overrides")
        )
      }
    }

    "with a public directive" should {
      "return DoCacheResponse even when not cacheable by default" in {
        val policy                   = new ResponseCachingCalculator(sharedCache)
        val request: CacheRequest    = defaultRequest
        val response: OriginResponse = defaultResponse.copy(headers = h("public"), status = 400)
        val result                   = policy.isCacheable(request, response)
        result should be(DoCacheResponse("Response contains public response directive"))
      }
    }
  }
}
