/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import org.slf4j.LoggerFactory

/**
 * This class produces secondary keys for a response from an origin server that contains a Vary header.
 *
 * The cache must store the response with the requested header from the original request, so that
 * subsequent requests match.  This is a little confusing, so an example follows:
 *
 * A request comes in with:
 *
 * <pre>
 * GET /some/path HTTP/1.1
 * Host: example.com
 * Accept-Encoding: gzip
 * </pre>
 *
 * The response comes back with a Vary header on the Accept-Encoding:
 *
 * <pre>
 * HTTP/1.1 200 OK
 * Vary: Accept-Encoding
 * Content-Encoding: gzip
 * Content-Type: application/json
 * Content-Length: 230
 * Cache-Control: max-age=10000000
 * </pre>
 *
 * The cache has to look at the Vary header, see that "Accept-Encoding" is a header on the request,
 * and then store "Accept-Encoding: gzip" as a secondary key on the cache entry.
 *
 * The implication is that the primary key entry of "GET http://example.com/some/path" is insufficient,
 * and in order to get that specific response from cache, any subsequent requests should have a
 * "Accept-Encoding: gzip" header in the same way.
 *
 * @see https://tools.ietf.org/html/rfc7234#section-4.1
 * @see https://tools.ietf.org/html/rfc7231#section-7.1.4
 */
class SecondaryKeyCalculator {
  import HeaderNames._
  import SecondaryKeyCalculator._

  /**
   * Finds the selecting header fields, based off the response's Vary header and the original
   * request.
   *
   * @param request the original request
   * @param responseHeaders the origin response
   * @return the selecting header fields, or None if there are no keys.
   */
  def calculate(
      request: CacheRequest,
      responseHeaders: Map[HeaderName, Seq[String]]
  ): Option[Map[HeaderName, Seq[String]]] = {
    logger.trace(s"calculate: request = $request, responseHeaders = $responseHeaders")

    responseHeaders.get(`Vary`).map { varyHeaders =>
      logger.debug(s"calculate: varyHeaders = $varyHeaders")
      // The origin server sent a Vary header saying that the content will only match
      // if the given headers in the response match what's in the request.
      val varyHeaderSet = varyHeaders.foldLeft(Set[HeaderName]()) { (acc, line) => acc ++ VaryParser.parse(line) }

      if (varyHeaderSet.contains(HeaderName("*"))) {
        logger.debug(s"calculate: returning wildcard keys *")
        Map(`Vary` -> Seq("*")) // If there's a *, then it is the only thing that matters.
      } else {
        // We have a set of header names.  Get the request headers corresponding to the vary response.
        val secondaryKeys = request.headers.filter { case (name, values) => varyHeaderSet.contains(name) }
        logger.debug(s"calculate: returning secondaryKeys = $secondaryKeys")
        secondaryKeys
      }
    }
  }
}

object SecondaryKeyCalculator {
  private val logger = LoggerFactory.getLogger("com.typesafe.cachecontrol.SecondaryKeyCalculator")
}
