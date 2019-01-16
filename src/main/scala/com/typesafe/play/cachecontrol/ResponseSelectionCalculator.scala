/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import java.time.ZonedDateTime

import CacheDirectives.OnlyIfCached
import HeaderNames._
import org.slf4j.LoggerFactory

sealed trait ResponseSelectionAction

/**
 * Predefined actions to take for a selected response.
 */
object ResponseSelectionActions {

  /**
   * No stored response exists.
   *
   * The cache must forward the request to the origin server.
   */
  case class ForwardToOrigin(reason: String) extends ResponseSelectionAction

  /**
   * No stored response exists.
   *
   * The cache must serve a gateway timeout response.
   */
  case class GatewayTimeout(reason: String) extends ResponseSelectionAction

  /**
   * A stored response has been selected.
   */
  case class SelectedResponse(response: StoredResponse, index: Int) extends ResponseSelectionAction
}

/**
 * This class looks through the available responses, and provides an action -- either selecting
 * a response, or rejecting with either a timeout or forward to origin.  The cache's
 * containsMatchingHeaders method is called in the case where the response has secondary keys
 * (defined with Vary) that must be matched.
 */
class ResponseSelectionCalculator(cache: Cache) {
  import ResponseSelectionActions._
  import ResponseSelectionCalculator._

  /**
   * Returns the action to take based off the available stored responses.
   */
  def selectResponse(request: CacheRequest, responses: Seq[StoredResponse]): ResponseSelectionAction = {
    findMatchingResponse(request, responses).getOrElse {
      noValidResponseFound(request)
    }
  }

  protected def uriAndMethodMatch(request: CacheRequest, response: StoredResponse): Boolean = {
    request.uri.equals(response.uri) && request.method.equals(response.requestMethod)
  }

  val mostRecentOrdering: Ordering[StoredResponse] = new Ordering[StoredResponse] {
    override def compare(r1: StoredResponse, r2: StoredResponse): Int = toDate(r2).compareTo(toDate(r1))
  }

  protected def findMatchingResponse(request: CacheRequest, responses: Seq[StoredResponse]): Option[SelectedResponse] = {
    logger.trace(s"findMatchingResponse: request = $request, responses = $responses")

    val matchingResponses: Seq[StoredResponse] = responses.filter {
      uriAndMethodMatch(request, _)
    }.filter { r =>
      cache.containsMatchingHeaders(request.headers, r.nominatedHeaders)
    }

    if (matchingResponses.isEmpty) {
      None
    } else {
      // When more than one suitable response is stored, a cache MUST use the
      // most recent response (as determined by the Date header field).  It
      // can also forward the request with "Cache-Control: max-age=0" or
      // "Cache-Control: no-cache" to disambiguate which response to use
      // https://tools.ietf.org/html/rfc7234#section-4

      // Order the matching response by the most recent and find the most recent one...
      val mostRecentResponse = matchingResponses.sorted(mostRecentOrdering).head
      val mostRecentIndex = responses.indexOf(mostRecentResponse)
      Some(SelectedResponse(mostRecentResponse, mostRecentIndex))
    }
  }

  def toDate(r: StoredResponse): ZonedDateTime = {
    HttpDate.parse(r.headers(`Date`).head)
  }

  protected def noValidResponseFound(request: CacheRequest): ResponseSelectionAction = {
    logger.trace(s"noValidResponseFound: request = $request")

    if (request.directives.contains(OnlyIfCached)) {
      GatewayTimeout("Response not found and request contains only-if-cached")
    } else {
      ForwardToOrigin("Valid response not found for request")
    }
  }

}

object ResponseSelectionCalculator {
  private val logger = LoggerFactory.getLogger("com.typesafe.cachecontrol.ResponseSelectionCalculator")
}
