/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import CacheDirectives._
import org.slf4j.LoggerFactory

sealed trait ResponseServeAction

/**
 * The possible actions a client can execute when determining to serve a stored response.
 */
object ResponseServeActions {
  /**
   * The stored response is fresh.
   *
   * The cache must serve the stored response without validation.
   */
  case class ServeFresh(reason: String) extends ResponseServeAction

  /**
   * A stored response was found, but is stale.
   *
   * The cache must serve the stored response without validation.
   */
  case class ServeStale(reason: String) extends ResponseServeAction

  /**
   * A stored response was found, but is stale.
   *
   * The cache must serve the stored response immediately, and validate in the background.
   *
   * If the validation has not happened after now + delta seconds, then the resource should NOT
   * be served stale.
   */
  case class ServeStaleAndValidate(reason: String) extends ResponseServeAction

  /**
   * A stored response was found, but is stale.
   *
   * The cache must validate the request with the origin server.
   *
   * If the origin server cannot be reached, then the cache MAY serve the
   * stale response.  In addition, if staleOnError is true, then the cache can serve
   * the stale response when the origin server returns a 5xx response.
   */
  case class Validate(reason: String, staleIfError: Boolean = false) extends ResponseServeAction

  /**
   * A stored response was found, but is stale.
   *
   * The cache must validate the request with the origin server.
   *
   * If the origin server cannot be reached, then the cache MUST serve a gateway timeout response.
   */
  case class ValidateOrTimeout(reason: String) extends ResponseServeAction
}

/**
 * This class determines whether a cache can serve a stored response from request.  It
 * is "Constructing Responses from Caches" section in RFC 7234.
 *
 * If there are multiple matches, the most recent response must be sent for
 * evaluation, as determined by the Date header field.
 *
 * https://tools.ietf.org/html/rfc7234#section-4
 */
class ResponseServingCalculator(cache: Cache) {
  import HeaderNames._
  import ResponseServeActions._
  import ResponseServingCalculator._

  private val freshnessCalculator: FreshnessCalculator = new FreshnessCalculator(cache)

  def serveResponse(request: CacheRequest, response: StoredResponse, currentAge: Seconds): ResponseServeAction = {
    logger.debug(s"serveResponse: response found for '${request.method} ${request.uri}', age = ${currentAge.seconds}")

    implicit val req = request
    implicit val res = response

    // o  the presented request does not contain the no-cache pragma
    //  (Section 5.4), nor the no-cache cache directive (Section 5.2.1),
    //  unless the stored response is successfully validated
    //  (Section 4.3), and
    // o  the stored response does not contain the no-cache cache directive
    //  (Section 5.2.2.2), unless it is successfully validated
    //  (Section 4.3), and
    val explicitValidate: Option[Validate] = noCacheFound

    explicitValidate
      .orElse {
        //o  the stored response is either:
        //
        //   *  fresh (see Section 4.2), or
        val serveFresh: Option[ServeFresh] = isCachedResponseFresh(currentAge)
        serveFresh
      }
      .orElse {
        // "must-revalidate" / "proxy-revalidate" mean timeout on disconnect
        // rather than serve stale
        val notAllowedStale: Option[ValidateOrTimeout] = isStaleResponseExplicitlyProhibited
        notAllowedStale
      }
      .orElse {
        //   *  allowed to be served stale (see Section 4.2.4), or
        val serveStale: Option[ServeStale] = isStaleResponseAllowed(currentAge)
        serveStale
      }
      .orElse {
        val serveStaleAndRevalidate: Option[ServeStaleAndValidate] = canServeStaleAndRevalidate(currentAge)
        serveStaleAndRevalidate
      }
      .getOrElse {
        // "is successfully validated" -- which means validate but serve stale on disconnect.
        val defaultValidation: Validate = allowStaleIfError(currentAge)
        defaultValidation
      }
  }

  protected def allowStaleIfError(age: Seconds)(implicit request: CacheRequest, response: StoredResponse): Validate = {
    val v = Validate("Response is stale, and stale response is not allowed")

    val freshnessLifetime = freshnessCalculator.calculateFreshnessLifetime(request, response)
    CacheDirectives
      .staleIfError(response.directives)
      .map { staleIfError =>
        val delta = staleIfError.delta
        //Its value indicates the upper limit to staleness; when the cached
        //response is more stale than the indicated amount, the cached response
        //SHOULD NOT be used to satisfy the request, absent other information.
        // https://tools.ietf.org/html/rfc5861#section-4

        val serveStale = age.isLessThan(freshnessLifetime.plus(delta))
        logger.debug(s"allowStaleIfError: delta = ${delta}, staleIfError = $serveStale")

        v.copy(staleIfError = serveStale)
      }
      .getOrElse {
        v
      }
  }

  protected def noCacheFound(implicit request: CacheRequest, response: StoredResponse): Option[Validate] = {
    if (logger.isTraceEnabled) {
      logger.trace(s"noCacheFound: request = $request, response = $response")
    }
    // o  the presented request does not contain the no-cache pragma
    //  (Section 5.4), nor the no-cache cache directive (Section 5.2.1),
    //  unless the stored response is successfully validated
    //  (Section 4.3), and
    val requestContainsNoCache: Option[Validate] = {
      if (request.directives.exists(_.isInstanceOf[NoCache])) {
        //    The "no-cache" request directive indicates that a cache MUST NOT use
        //      a stored response to satisfy the request without successful
        //    validation on the origin server.
        // https://tools.ietf.org/html/rfc7234#section-5.2.1.4
        logger.trace(s"noCacheFound: no-cache directive found!")
        val msg = "Request contains no-cache directive, validation required"
        Some(Validate(msg))
      } else {
        // When the Cache-Control header field is not present in a request,
        // caches MUST consider the no-cache request pragma-directive as having
        // the same effect as if "Cache-Control: no-cache" were present (see
        // Section 5.2.1).
        // https://tools.ietf.org/html/rfc7234#section-5.4
        if (request.directives.isEmpty) {
          if (containsPragmaNoCache) {
            val msg =
              "Request does not contain Cache-Control header found, but does contains no-cache Pragma header, validation required"
            Some(Validate(msg))
          } else {
            None
          }
        } else {
          None
        }
      }
    }

    val result: Option[Validate] = requestContainsNoCache.orElse {
      CacheDirectives.noCache(response.directives).flatMap { noCache =>
        noCache.headerNames match {
          case Some(headers) =>
            //  If the no-cache response directive specifies one or more field-names,
            //  then a cache MAY use the response to satisfy a subsequent request,
            //  subject to any other restrictions on caching.  However, any header
            //  fields in the response that have the field-name(s) listed MUST NOT be
            //  sent in the response to a subsequent request without successful
            //  revalidation with the origin server.  This allows an origin server to
            //  prevent the re-use of certain header fields in a response, while
            //  still allowing caching of the rest of the response.
            //
            // https://tools.ietf.org/html/rfc7234#section-5.2.2.2
            //
            // The above text doesn't make much sense -- if no-cache means "revalidate-always"
            // then revalidation preventing reuse of header fields is already a given, so the
            // "without successful revalidation" doesn't apply.
            //
            // It looks like others have had the same question about the spec:
            //
            // http://www.mobify.com/blog/beginners-guide-to-http-cache-headers/
            // http://www.squid-cache.org/mail-archive/squid-dev/199704/0023.html
            //
            // So, no-cache="Set-Cookie" means that you CAN cache the response, but you
            // have to strip out the Set-Cookie first before you cache it.  That's
            // handled in ResponseCachingPolicy, so we just pass back None here to
            // indicate caching it is fine.  This makes it the Anti Vary.
            logger.debug(s"noCacheFound: no-cache response directive qualified with ${noCache.headerNames}")
            None
          case None =>
            //The "no-cache" response directive indicates that the response MUST
            //NOT be used to satisfy a subsequent request without successful
            //validation on the origin server.
            val msg = "Response contains no-args no-cache directive"
            Some(Validate(msg))
        }
      }
    }

    result
  }

  protected def containsPragmaNoCache(implicit request: CacheRequest): Boolean = {
    request.headers.get(`Pragma`).exists(_.exists(_.contains("no-cache")))
  }

  protected def headersFound(noCacheHeaders: Seq[HeaderName], response: StoredResponse): Boolean = {
    val keySet = response.headers.keySet
    keySet.exists(elem => noCacheHeaders.contains(elem))
  }

  /**
   * Returns a ServeFresh if the cached response is fresh enough.
   *
   * Clients can send the max-age or min-fresh cache directives in a
   * request to constrain or relax freshness calculations for the
   * corresponding response (Section 5.2.1).
   *
   * https://tools.ietf.org/html/rfc7234#section-4.2
   */
  protected def isCachedResponseFresh(
      currentAge: Seconds
  )(implicit request: CacheRequest, response: StoredResponse): Option[ServeFresh] = {
    if (logger.isTraceEnabled) {
      logger.trace(s"isCachedResponseFresh: request = $request, response = $response")
    }

    val freshnessLifetime = freshnessCalculator.calculateFreshnessLifetime(request, response)
    logger.debug(s"""isCachedResponseFresh: freshnessLifetime = $freshnessLifetime, currentAge = $currentAge""")

    // The "max-age" request directive indicates that the client is
    // unwilling to accept a response whose age is greater than the
    // specified number of seconds.
    // https://tools.ietf.org/html/rfc7234#section-5.2.1
    CacheDirectives.maxAge(request.directives).map(_.delta) match {
      case Some(maxAge) if currentAge.isGreaterThan(maxAge) =>
        logger.debug(s"isCachedResponseFresh: maxAge = $maxAge, currentAge = $currentAge, not fresh enough.")
        None

      case other =>
        //    The "min-fresh" request directive indicates that the client is
        //      willing to accept a response whose freshness lifetime is no less than
        //      its current age plus the specified time in seconds.  That is, the
        //    client wants a response that will still be fresh for at least the
        //    specified number of seconds.
        // https://tools.ietf.org/html/rfc7234#section-5.2.1.3
        CacheDirectives.minFresh(request.directives).map(_.delta) match {
          case Some(minFresh) if !freshnessLifetime.isLessThan(currentAge.plus(minFresh)) =>
            logger.debug(
              s"isCachedResponseFresh: freshnessLifetime = $freshnessLifetime, currentAge = $currentAge, minFresh = $minFresh"
            )
            Some(
              ServeFresh(
                s"Fresh response: minFresh = $minFresh, freshnessLifetime = $freshnessLifetime, currentAge = $currentAge"
              )
            )

          case noMinFresh =>
            val responseIsFresh = freshnessLifetime.isGreaterThan(currentAge)
            logger.debug(s"isCachedResponseFresh: freshnessLifetime = $freshnessLifetime, currentAge = $currentAge")
            if (responseIsFresh) {
              val secondsLeft = freshnessLifetime.minus(currentAge)
              val reason      = s"Fresh response: lifetime = $freshnessLifetime, $secondsLeft seconds left"

              Some(ServeFresh(reason))
            } else {
              None
            }
        }
    }
  }

  /**
   * Determines if a stale response without validation is appropriate.
   *
   * https://tools.ietf.org/html/rfc7234#section-4.2.4
   */
  protected def isStaleResponseAllowed(
      currentAge: Seconds
  )(implicit request: CacheRequest, response: StoredResponse): Option[ServeStale] = {
    if (logger.isTraceEnabled) {
      logger.trace(s"isStaleResponseAllowed: $currentAge, request = $request, response = $response")
    }

    //The "max-stale" request directive indicates that the client is
    //willing to accept a response that has exceeded its freshness
    //lifetime.
    val result = CacheDirectives
      .maxStale(request.directives)
      .flatMap { maxStale =>
        logger.debug(s"isStaleResponseAllowed: maxStale = $maxStale")

        maxStale.delta match {
          case Some(maxStaleDelta) =>
            // If max-stale is assigned a value, then the client is
            // willing to accept a response that has exceeded its freshness lifetime
            // by no more than the specified number of seconds.
            val freshnessLifetime = freshnessCalculator.calculateFreshnessLifetime(request, response)
            val totalLifetime     = freshnessLifetime.plus(maxStaleDelta)
            logger.debug(
              s"isStaleResponseAllowed: freshnessLifetime = $freshnessLifetime, maxAge = $maxStaleDelta, totalLifetime = $totalLifetime, currentAge = $currentAge"
            )

            if (totalLifetime.isGreaterThan(currentAge)) {
              logger.debug(
                s"isStaleResponseAllowed: ($freshnessLifetime + $maxStaleDelta) > = $currentAge, allowing serve stale"
              )
              val msg = s"Request contains ${maxStale}, current age = ${currentAge.seconds} which is inside range"
              Some(ServeStale(msg))
            } else {
              logger.debug(s"isStaleResponseAllowed: stale response outside of max-stale $maxStale")
              None
            }
          case None =>
            logger.debug(s"isStaleResponseAllowed: maxStale has no delta, stale response allowed")

            // If no value is assigned to max-stale, then the client is willing
            // to accept a stale response of any age.
            val msg = "Request contains no-args max-stale directive"
            Some(ServeStale(msg))
        }
      }
      .orElse {
        logger.debug(s"isStaleResponseAllowed: stale response not allowed")
        None
      }
    result
  }

  protected def isStaleResponseExplicitlyProhibited(
      implicit request: CacheRequest,
      response: StoredResponse
  ): Option[ValidateOrTimeout] = {
    if (logger.isTraceEnabled) {
      logger.trace(s"isStaleResponseProhibited: request = $request, response = $response")
    }

    // We do NOT check for "no-store" here on the request with a stored response:
    //    Note that if a request containing this directive is satisfied from a
    //      cache, the no-store request directive does not apply to the already
    //    stored response.
    // https://tools.ietf.org/html/rfc7234#section-5.2.1.5
    //
    // Noted explicitly in https://tools.ietf.org/html/rfc7234#appendix-A
    //      The "no-store" request directive doesn't apply to responses; i.e., a
    //      cache can satisfy a request with no-store on it and does not
    //        invalidate it.  (Section 5.2.1.5)

    //The "must-revalidate" response directive indicates that once it has
    //become stale, a cache MUST NOT use the response to satisfy subsequent
    //requests without successful validation on the origin server.
    // In all circumstances a cache MUST obey the must-revalidate directive;
    // in particular, if a cache cannot reach the origin server for any reason,
    // it MUST generate a 504 (Gateway Timeout) response.
    // https://tools.ietf.org/html/rfc7234#section-5.2.2.1
    if (response.directives.contains(MustRevalidate)) {
      Some(ValidateOrTimeout("Response is stale, response contains must-revalidate directive"))
    } else if (cache.isShared) {
      //The "proxy-revalidate" response directive has the same meaning as the
      //must-revalidate response directive, except that it does not apply to
      //private caches.
      // https://tools.ietf.org/html/rfc7234#section-5.2.2.7
      if (response.directives.contains(ProxyRevalidate)) {
        val msg = "Response is stale, response contains proxy-revalidate directive and cache is shared"
        Some(ValidateOrTimeout(msg))
      } else if (CacheDirectives.sMaxAge(response.directives).isDefined) {
        // Note that s-maxage REQUIRES revalidation or timeout, and never serves stale!
        //
        //Note that cached responses that contain the "must-revalidate" and/or
        //"s-maxage" response directives are not allowed to be served stale
        //(Section 4.2.4) by shared caches.  In particular, a response with
        //either "max-age=0, must-revalidate" or "s-maxage=0" cannot be used to
        //satisfy a subsequent request without revalidating it on the origin
        //server.
        // https://tools.ietf.org/html/rfc7234#section-3.2

        // The s-maxage directive also implies the semantics of the proxy-revalidate response directive.
        // https://tools.ietf.org/html/rfc7234#section-5.2.2.9
        val msg = "Response is stale, response contains s-maxage directive and cache is shared"
        Some(ValidateOrTimeout(msg))
      } else {
        None
      }
    } else {
      None
    }
  }

  protected def canServeStaleAndRevalidate(
      age: Seconds
  )(implicit request: CacheRequest, response: StoredResponse): Option[ServeStaleAndValidate] = {
    if (logger.isTraceEnabled) {
      logger.trace(s"canServeStaleAndRevalidate: response = $response")
    }

    // https://tools.ietf.org/html/rfc5861#section-3
    val freshnessLifetime = freshnessCalculator.calculateFreshnessLifetime(request, response)

    CacheDirectives.staleWhileRevalidate(response.directives).iterator.map(_.delta).collectFirst {
      case delta if age.isLessThan(freshnessLifetime.plus(delta)) =>
        logger.debug(s"canServeStaleAndRevalidate: age = $age, delta = $delta")
        val reason = s"Response contains stale-while-revalidate and is within delta range $delta"
        ServeStaleAndValidate(reason)
    }
  }
}

object ResponseServingCalculator {
  private val logger = LoggerFactory.getLogger("com.typesafe.cachecontrol.ResponseServingCalculator")
}
