/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
package com.typesafe.play.cachecontrol

import CacheDirectives.SMaxAge
import org.slf4j.LoggerFactory

/**
 * Calculates freshness lifetime for a request.
 *
 */
class FreshnessCalculator(cache: Cache) {

  import HeaderNames._

  private val logger = LoggerFactory.getLogger("com.typesafe.cachecontrol.FreshnessCalculator")

  /**
   * Returns the freshness lifetime in seconds.
   *
   * https://tools.ietf.org/html/rfc7234#section-4.2.1
   */
  def calculateFreshnessLifetime(request: CacheRequest, response: CacheResponse): Seconds = {
    if (logger.isTraceEnabled) {
      logger.trace(s"calculateFreshnessLifetime: ")
    }

    val maybeResult = if (isFreshnessInformationInvalid(request, response)) {
      //    When there is more than one value present for a given directive
      //    (e.g., two Expires header fields, multiple Cache-Control: max-age
      //      directives), the directive's value is considered invalid.  Caches are
      //      encouraged to consider responses that have invalid freshness
      //      information to be stale.
      None
    } else {
      // A cache can calculate the freshness lifetime (denoted as
      //  freshness_lifetime) of a response by using the first match of the
      //  following:
      val freshnessLifetime: Option[Seconds] = {
        // o  If the cache is shared and the s-maxage response directive
        // (Section 5.2.2.9) is present, use its value, or
        calculateFreshnessFromSMaxAge(request, response).orElse {

          //    o  If the max-age response directive (Section 5.2.2.8) is present,
          // use its value, or
          calculateFreshnessFromMaxAge(request, response)
        }.orElse {
          //  o  If the Expires response header field (Section 5.3) is present, use
          //its value minus the value of the Date response header field, or
          calculateFreshnessFromExpires(request, response)
        }.orElse {
          // o  Otherwise, no explicit expiration time is present in the response.
          //  A heuristic freshness lifetime might be applicable; see
          // Section 4.2.2.
          calculateFreshnessFromHeuristic(request, response)
        }
      }
      logger.debug(s"calculateFreshnessLifetime: freshnessLifetime = $freshnessLifetime")
      freshnessLifetime
    }

    val result = maybeResult.getOrElse {
      Seconds.ZERO
    }
    logger.debug(s"calculateFreshnessLifetime: result = $result")
    result
  }

  /**
   * Returns true if the freshness information is invalid, false otherwise.
   */
  def isFreshnessInformationInvalid(request: CacheRequest, response: CacheResponse): Boolean = {
    val responseHeaders = response.headers
    val directives = response.directives
    if (responseHeaders.get(`Expires`).exists(_.size > 1)) {
      logger.debug("isFreshnessInformationInvalid: duplicate Expires headers found, returning true")
      true
    } else if (containsDuplicates(directives)) {
      logger.debug(s"isFreshnessInformationInvalid: duplicate directives found in $directives, returning true")
      true
    } else {
      false
    }
  }

  /**
   * Returns true if instances of the same type were found in the directives seq, false otherwise.
   */
  @annotation.tailrec
  private def containsDuplicates(directives: Seq[CacheDirective], seen: Set[Class[_]] = Set[Class[_]]()): Boolean = {
    directives match {
      case x :: xs =>
        // We don't care about the value -- if the type is the same, then they conflict.
        if (seen.contains(x.getClass)) {
          true
        } else {
          containsDuplicates(xs, seen + x.getClass)
        }
      case _ =>
        false
    }
  }

  /**
   * Calculates the freshness lifetime from the s-maxage cache directive, if the cache is shared.
   */
  def calculateFreshnessFromSMaxAge(request: CacheRequest, response: CacheResponse): Option[Seconds] = {
    if (cache.isShared) {
      CacheDirectives.sMaxAge(response.directives).map(_.delta)
    } else {
      None
    }
  }

  /**
   * Returns the freshness duration as calculated from the Max-Age cache directive.
   *
   * https://tools.ietf.org/html/rfc7234#section-4.2.1
   */
  def calculateFreshnessFromMaxAge(request: CacheRequest, response: CacheResponse): Option[Seconds] = {
    CacheDirectives.maxAge(response.directives).map(_.delta)
  }

  /**
   * Returns the freshness duration as calculated from the Expires header.
   *
   * https://tools.ietf.org/html/rfc7234#section-4.2.1
   */
  def calculateFreshnessFromExpires(request: CacheRequest, response: CacheResponse): Option[Seconds] = {
    // o  If the Expires response header field (Section 5.3) is present, use
    // its value minus the value of the Date response header field, or
    val headers = response.headers

    headers.get(`Expires`).flatMap { expiresList =>
      val dateString = headers.getOrElse(`Date`, throw new RuntimeException("No Date header found!")).head
      try {
        val expires = HttpDate.parse(expiresList.head)
        val date = HttpDate.parse(dateString)

        val expiresDuration = HttpDate.diff(start = date, end = expires)
        logger.debug(s"calculateFreshnessFromExpires: expiresDuration = $expiresDuration")
        Some(expiresDuration)
      } catch {
        case e: Exception =>
          logger.error("calculateFreshnessFromExpires: HTTP date parsing failed", e)
          None
      }
    }
  }

  /**
   * Returns the freshness lifetime from heuristics, or None if the response is stale.
   *
   * https://tools.ietf.org/html/rfc7234#section-4.2.2
   */
  def calculateFreshnessFromHeuristic(request: CacheRequest, response: CacheResponse): Option[Seconds] = {
    cache.calculateFreshnessFromHeuristic(request, response)
  }

  def unapplySeq(directives: Seq[CacheDirective]): Option[Seconds] = {
    directives match {
      case SMaxAge(delta) :: _ => Some(delta)
      case _ => None
    }
  }

}
