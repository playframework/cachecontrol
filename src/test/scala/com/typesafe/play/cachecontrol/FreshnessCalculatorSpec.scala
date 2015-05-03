package com.typesafe.play.cachecontrol

import java.net.URI

import HeaderNames._
import org.joda.time.Seconds
import org.scalatest.Matchers._
import org.scalatest._

class FreshnessCalculatorSpec extends WordSpec {

  val cache = new StubCache(shared = false)

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

  "calculateFreshnessLifetime" should {

    "return stale on conflicting information" in {
      //    When there is more than one value present for a given directive
      //    (e.g., two Expires header fields, multiple Cache-Control: max-age
      //      directives), the directive's value is considered invalid.  Caches are
      //      encouraged to consider responses that have invalid freshness
      //      information to be stale.
      val calculator = new FreshnessCalculator(cache)

      val request = defaultRequest
      val headers = defaultHeaders ++ Map(HeaderName("Expires") -> Seq(
        HttpDate.format(HttpDate.now.plusSeconds(134)),
        HttpDate.format(HttpDate.now.plusSeconds(11))
      ))
      val response: OriginResponse = defaultResponse.copy(headers = headers)

      val seconds = calculator.calculateFreshnessLifetime(request, response)
      seconds should be(Seconds.seconds(0))
    }

    "use the Expires headers" in {
      val calculator = new FreshnessCalculator(cache)

      val request = defaultRequest
      val headers = defaultHeaders ++ Map(HeaderName("Expires") -> Seq(HttpDate.format(HttpDate.now.plusSeconds(134))))
      val response: OriginResponse = defaultResponse.copy(headers = headers)

      val seconds = calculator.calculateFreshnessLifetime(request, response)
      seconds should be(Seconds.seconds(134))
    }

    "use the max-age directive" in {
      val calculator = new FreshnessCalculator(cache)

      val request = defaultRequest
      val headers = defaultHeaders ++ Map(HeaderName("Cache-Control") -> Seq("max-age=113"))
      val response: OriginResponse = defaultResponse.copy(headers = headers)

      val seconds = calculator.calculateFreshnessLifetime(request, response)
      seconds should be(Seconds.seconds(113))
    }

    "use the max-age directive over the expires header" in {
      val calculator = new FreshnessCalculator(cache)

      //If a response includes a Cache-Control field with the max-age
      //directive (Section 5.2.2.8), a recipient MUST ignore the Expires field.

      val request = defaultRequest
      val headers = defaultHeaders ++ Map(
        HeaderName("Expires") -> Seq(HttpDate.format(HttpDate.now.plusSeconds(134))),
        HeaderName("Cache-Control") -> Seq("max-age=113")
      )
      val response: OriginResponse = defaultResponse.copy(headers = headers)

      val seconds = calculator.calculateFreshnessLifetime(request, response)
      seconds should be(Seconds.seconds(113))
    }

    "use s-maxage directive" in {
      val sharedCache = new StubCache(shared = true)
      val calculator = new FreshnessCalculator(sharedCache)

      //The "s-maxage" response directive indicates that, in shared caches,
      //the maximum age specified by this directive overrides the maximum age
      //specified by either the max-age directive or the Expires header
      //field.  The s-maxage directive also implies the semantics of the
      //proxy-revalidate response directive.

      // Likewise, if a response includes the s-maxage directive
      // (Section 5.2.2.9), a shared cache recipient MUST ignore the Expires field.
      val request = defaultRequest
      val headers = defaultHeaders ++ Map(
        HeaderName("Cache-Control") -> Seq("s-maxage=113")
      )
      val response: OriginResponse = defaultResponse.copy(headers = headers)

      val seconds = calculator.calculateFreshnessLifetime(request, response)
      seconds should be(Seconds.seconds(113))
    }

    "use the s-maxage directive over the expires header if shared" in {
      val sharedCache = new StubCache(shared = true)
      val calculator = new FreshnessCalculator(sharedCache)

      // Likewise, if a response includes the s-maxage directive
      // (Section 5.2.2.9), a shared cache recipient MUST ignore the Expires field.
      val request = defaultRequest
      val headers = defaultHeaders ++ Map(
        HeaderName("Expires") -> Seq(HttpDate.format(HttpDate.now.plusSeconds(134))),
        HeaderName("Cache-Control") -> Seq("s-maxage=113")
      )
      val response: OriginResponse = defaultResponse.copy(headers = headers)

      val seconds = calculator.calculateFreshnessLifetime(request, response)
      seconds should be(Seconds.seconds(113))
    }

    "use the s-maxage directive over the max-age header if shared" in {
      val sharedCache = new StubCache(shared = true)
      val calculator = new FreshnessCalculator(sharedCache)

      val request = defaultRequest
      val headers = defaultHeaders ++ Map(
        HeaderName("Expires") -> Seq(HttpDate.format(HttpDate.now.plusSeconds(134))),
        HeaderName("Cache-Control") -> Seq("s-maxage=113,max-age=311")
      )
      val response: OriginResponse = defaultResponse.copy(headers = headers)

      val seconds = calculator.calculateFreshnessLifetime(request, response)
      seconds should be(Seconds.seconds(113))
    }

    "ignore the s-maxage header if private" in {
      val sharedCache = new StubCache(shared = false)
      val calculator = new FreshnessCalculator(sharedCache)

      val request = defaultRequest
      val headers = defaultHeaders ++ Map(
        HeaderName("Expires") -> Seq(HttpDate.format(HttpDate.now.plusSeconds(134))),
        HeaderName("Cache-Control") -> Seq("s-maxage=113,max-age=311")
      )
      val response: OriginResponse = defaultResponse.copy(headers = headers)

      val seconds = calculator.calculateFreshnessLifetime(request, response)
      seconds should be(Seconds.seconds(311))
    }

    "use a heuristic if defined" in {
      val sharedCache = new StubCache(shared = false) {
        override def calculateFreshnessFromHeuristic(request: CacheRequest, response: CacheResponse): Option[Seconds] = {
          Some(Seconds.seconds(999))
        }
      }
      val calculator = new FreshnessCalculator(sharedCache)

      val request = defaultRequest
      val headers = defaultHeaders ++ Map()
      val response: OriginResponse = defaultResponse.copy(headers = headers)

      val seconds = calculator.calculateFreshnessLifetime(request, response)
      seconds should be(Seconds.seconds(999))
    }

    "return stale if there is no heuristic" in {
      val sharedCache = new StubCache(shared = false)
      val calculator = new FreshnessCalculator(sharedCache)

      val request = defaultRequest
      val headers = defaultHeaders ++ Map()
      val response: OriginResponse = defaultResponse.copy(headers = headers)

      val seconds = calculator.calculateFreshnessLifetime(request, response)
      seconds should be(Seconds.seconds(0))
    }
  }

}
