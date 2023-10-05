/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.playframework.cachecontrol

import org.slf4j.LoggerFactory

sealed trait ResponseCachingAction

/**
 * Case classes used by ResponseCachingPolicy.
 */
object ResponseCachingActions {

  /**
   * The client should store the origin response in the cache.
   *
   * Depending on the qualified "no-cache" response directive and/or the
   * qualified "private" response directive (if the cache is shared), then
   * headers may be stripped from the response before it is cached.
   *
   * @see https://tools.ietf.org/html/rfc7234#section-5.2.2.6
   * @see https://tools.ietf.org/html/rfc7234#section-5.2.2.2
   */
  case class DoCacheResponse(reason: String) extends ResponseCachingAction

  /**
   * The client should not store the origin response in the cache.
   */
  case class DoNotCacheResponse(reason: String) extends ResponseCachingAction
}

/**
 * Decides whether this response from an origin server should be stored in cached or not.
 *
 * https://tools.ietf.org/html/rfc7234#section-3
 */
class ResponseCachingCalculator(cache: Cache) {
  import CacheDirectives._
  import ResponseCachingActions._
  import ResponseCachingCalculator._

  /**
   * Returns an action indicating whether the request should be cached or not.
   */
  def isCacheable(request: CacheRequest, response: OriginResponse): ResponseCachingAction = {
    if (logger.isTraceEnabled) {
      logger.trace(s"isCacheable: request: $request, response = $response")
    }

    // XXX Don't know how to implement unknown cache control extensions.
    // Note that any of the requirements listed above can be overridden by a
    // cache-control extension; see Section 5.2.3.

    val result: ResponseCachingAction = notCacheableMethod(request, response)
      .orElse {
        ineligibleResponseCode(request, response)
      }
      .orElse {
        containsNoStoreDirective(request, response)
      }
      .orElse {
        containsPrivateResponseDirectiveInSharedCache(request, response)
      }
      .orElse {
        containsIncompatibleAuthorizationInSharedCache(request, response)
      }
      .getOrElse {
        responseIsCacheable(request, response)
      }

    logger.trace(s"isCacheable: result = $result")
    result
  }

  /**
   * Returns true if the response is cacheable.
   *
   * https://tools.ietf.org/html/rfc7234#section-3
   */
  protected def responseIsCacheable(request: CacheRequest, response: OriginResponse): ResponseCachingAction = {
    if (logger.isTraceEnabled) {
      logger.trace(s"responseIsCacheable: response = $response")
    }

    //    o  the response either:
    // *  contains an Expires header field (see Section 5.3), or
    if (containsExpiresHeader(response)) {
      DoCacheResponse("Response contains expires header")
    } else if (containsMaxAgeDirective(response)) {
      // *  contains a max-age response directive (see Section 5.2.2.8), or
      DoCacheResponse("Response contains max-age response directive")
    } else if (cache.isShared && containsSMaxAgeDirective(response)) {
      // *  contains a s-maxage response directive (see Section 5.2.2.9)
      // and the cache is shared, or
      DoCacheResponse("Response contains s-maxage and the cache is shared")
    } else if (containsCachableExtension(response)) {
      // *  contains a Cache Control Extension (see Section 5.2.3) that
      // allows it to be cached, or
      DoCacheResponse("Response contains a cache control extension that allows it to be cached")
    } else if (cache.isDefaultCacheable(response.status)) {
      // *  has a status code that is defined as cacheable by default (see
      // Section 4.2.2), or
      DoCacheResponse(s"Response status code ${response.status} is cacheable by default")
    } else if (containsPublicDirective(response)) {
      // *  contains a public response directive (see Section 5.2.2.5).
      DoCacheResponse("Response contains public response directive")
    } else {
      // If none of the above... then no.
      DoNotCacheResponse("Response is not cacheable by default, and there are no explicit overrides")
    }
  }

  /**
   * Returns DoNotCacheResponse if request method is not cacheable, according to cache.isCacheableMethod.
   */
  protected def notCacheableMethod(request: CacheRequest, response: OriginResponse): Option[DoNotCacheResponse] = {
    if (cache.isCacheableMethod(request.method)) {
      None
    } else {
      Some(DoNotCacheResponse(s"Request method ${request.method} is not cacheable"))
    }
  }

  /**
   * Returns DoNotCacheResponse if this is not an "understood" response code, according to cache.isUnderstoodStatusCode.
   */
  protected def ineligibleResponseCode(request: CacheRequest, response: OriginResponse): Option[DoNotCacheResponse] = {
    val statusCode = response.status
    if (cache.isUnderstoodStatusCode(statusCode)) {
      None
    } else {
      Some(DoNotCacheResponse(s"Response code $statusCode is not understood by the cache"))
    }
  }

  /**
   * "the "no-store" cache directive (see Section 5.2) does not appear
   * in request or response header fields"
   */
  protected def containsNoStoreDirective(
      request: CacheRequest,
      response: OriginResponse
  ): Option[DoNotCacheResponse] = {
    if (request.directives.contains(NoStore)) {
      Some(DoNotCacheResponse("Request Cache-Control header contains no-store cache directive"))
    } else if (response.directives.contains(NoStore)) {
      Some(DoNotCacheResponse("Response Cache-Control header contains no-store cache directive"))
    } else {
      None
    }
  }

  /**
   * "the "private" response directive (see Section 5.2.2.6) does not
   * appear in the response, if the cache is shared, and"
   */
  protected def containsPrivateResponseDirectiveInSharedCache(
      request: CacheRequest,
      response: OriginResponse
  ): Option[DoNotCacheResponse] = {
    if (cache.isShared) {
      CacheDirectives.`private`(response.directives).flatMap { privateDirective =>
        if (privateDirective.headerNames.isDefined) {
          // If the private response directive specifies one or more field-names,
          // this requirement is limited to the field-values associated with the
          // listed response header fields.  That is, a shared cache MUST NOT
          // store the specified field-names(s), whereas it MAY store the
          // remainder of the response message.
          // https://tools.ietf.org/html/rfc7234#section-5.2.2.6
          logger.debug("qualified private response directive found, returning None")
          None
        } else {
          Some(DoNotCacheResponse("Cache is shared, and private directive found in response"))
        }
      }
    } else {
      None
    }
  }

  /**
   * "the Authorization header field (see Section 4.2 of [Part7]) does
   * not appear in the request, if the cache is shared, unless the
   * response explicitly allows it (see Section 3.2)"
   */
  protected def containsIncompatibleAuthorizationInSharedCache(
      request: CacheRequest,
      response: OriginResponse
  ): Option[DoNotCacheResponse] = {
    if (cache.isShared) {
      if (containsAuthorizationHeader(request) && !directiveAllowsAuthorization(response)) {
        val msg = s"Cache is shared, authorization header found, no cache directives allow authorization"
        Some(DoNotCacheResponse(msg))
      } else {
        None
      }
    } else {
      None
    }
  }

  /**
   * Returns true if this is an extension that allows it to be cached.
   *
   * https://tools.ietf.org/html/rfc7234#section-5.2.3
   */
  protected def isCacheableExtension(extension: CacheDirectiveExtension): Boolean = {
    cache.isCacheableExtension(extension)
  }

  /**
   * https://tools.ietf.org/html/rfc7234#section-3.2
   *
   * "must-revalidate, public, and s-maxage" allow an authorization header.
   */
  protected def directiveAllowsAuthorization(response: OriginResponse): Boolean = {
    containsMustRevalidateDirective(response) ||
    containsPublicDirective(response) ||
    containsSMaxAgeDirective(response)
  }

  /**
   * Returns true if the request contains an Authorization header, false otherwise.
   */
  protected def containsAuthorizationHeader(request: CacheRequest): Boolean = {
    request.headers.contains(HeaderNames.`Authorization`)
  }

  /**
   * Returns true if the response contains an expires header, false otherwise.
   */
  protected def containsExpiresHeader(response: OriginResponse): Boolean = {
    response.headers.contains(HeaderNames.`Expires`)
  }

  protected def containsPublicDirective(response: OriginResponse): Boolean = {
    response.directives.contains(Public)
  }

  protected def containsMaxAgeDirective(response: OriginResponse): Boolean = {
    CacheDirectives.maxAge(response.directives).isDefined
  }

  protected def containsSMaxAgeDirective(response: OriginResponse): Boolean = {
    CacheDirectives.sMaxAge(response.directives).isDefined
  }

  protected def containsMustRevalidateDirective(response: OriginResponse) = {
    response.directives.contains(MustRevalidate)
  }

  protected def containsCachableExtension(response: OriginResponse): Boolean = {
    CacheDirectives.extensions(response.directives).exists { extension => isCacheableExtension(extension) }
  }
}

object ResponseCachingCalculator {
  private val logger = LoggerFactory.getLogger("org.playframework.cachecontrol.ResponseCachingCalculator")
}
