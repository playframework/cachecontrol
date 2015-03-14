/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.cachecontrol

import com.typesafe.cachecontrol.CacheDirectives.CacheDirectiveExtension
import org.joda.time.Seconds

/**
 * This trait defines methods that are used through the library where core business logic
 * belongs to the cache that is not in scope for RFC 7234 and cannot be predefined.
 */
trait Cache {

  /**
   * Allows the cache to calculate the freshness lifetime of the request using a heuristic.
   *
   * @see https://tools.ietf.org/html/rfc7234#section-4.2.2
   */
  def calculateFreshnessFromHeuristic(request: CacheRequest, response: CacheResponse): Option[Seconds]

  /**
   * Returns true if the cache knows what to do with this cache extension, false otherwise.
   */
  def isCacheableExtension(extension: CacheDirectiveExtension): Boolean

  /**
   * Returns true if this is a shared cache.  False indicates that this is a private cache.
   */
  def isShared: Boolean

  /**
   * In this context, a cache has "understood" a request method or a
   * response status code if it recognizes it and implements all specified
   * caching-related behavior.
   *
   * @see https://tools.ietf.org/html/rfc7234#section-3
   */
  def isUnderstoodStatusCode(statusCode: Int): Boolean

  /**
   * Returns whether a status code is cacheable by default or not.
   *
   * Note that in situations where the cache wants to be more conservative and
   * ONLY cache when there is an explicit caching header, this method should
   * always return false.  See Chris Heald's position on
   * https://blog.phusion.nl/2015/02/09/turbocaching-security-changes/ for
   * more details.
   */
  def isDefaultCacheable(statusCode: Int): Boolean

  /**
   * In this context, a cache has "understood" a request method or a
   * response status code if it recognizes it and implements all specified
   * caching-related behavior.
   *
   * @see https://tools.ietf.org/html/rfc7234#section-3
   */
  def isCacheableMethod(requestMethod: String): Boolean

  /**
   * Returns true if the selecting header fields nominated by the stored response
   * match those presented by the new request.  This method is broken out into the
   * cache functionality as section 4.1 can do transformation of header fields in
   * non-trivial ways that cannot be anticipated by this library in order to find
   * a match.  Given the vagaries of Vary and the overall complexities of fields
   * such as User-Agent, the assumption is that the cache knows best.
   *
   * @see https://tools.ietf.org/html/rfc7234#section-4.1
   *
   * @param presentedHeaders the presented headers by the new request.
   * @param nominatedHeaders the header fields nominated by the stored response
   * @return true if the cache considers it a match, false otherwise.
   */
  def containsMatchingHeaders(presentedHeaders: Map[HeaderName, Seq[String]], nominatedHeaders: Map[HeaderName, Seq[String]]): Boolean

}
