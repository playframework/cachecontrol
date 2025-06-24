/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

trait CacheDefaults extends Cache {

  /**
   * Cache understands the response status code behavior for caching purposes.
   */
  override def isUnderstoodStatusCode(statusCode: Int): Boolean = {
    statusCode match {
      case 100 | 101 =>
        // https://tools.ietf.org/html/rfc7231#section-6.2.1
        true
      case success if 200 until 206 contains success =>
        // https://tools.ietf.org/html/rfc7231#section-6.3.1
        true
      case redirection if 300 until 308 contains redirection =>
        // https://tools.ietf.org/html/rfc7231#section-6.4
        // https://tools.ietf.org/html/rfc7238 defines 308 and it should be cacheable.
        true
      case clientError if (400 until 417) contains clientError =>
        // https://tools.ietf.org/html/rfc7231#section-6.5
        // http://tools.ietf.org/html/rfc6585 defines 428 & 429 but
        // they shouldn't be cacheable in any event.
        true
      case serverError if (500 until 505) contains serverError =>
        // https://tools.ietf.org/html/rfc7231#section-6.6
        true
      case other =>
        // recipient MUST NOT cache a response with an unrecognized status code.
        // https://tools.ietf.org/html/rfc7231#section-6
        false
    }
  }

  /**
   * Returns true if the method is cacheable, true for GET and HEAD by default.
   */
  override def isCacheableMethod(requestMethod: String): Boolean = {
    //    In general, safe methods that
    //    do not depend on a current or authoritative response are defined as
    //    cacheable; this specification defines GET, HEAD, and POST as
    //    cacheable, although the overwhelming majority of cache
    //      implementations only support GET and HEAD.
    //
    // https://tools.ietf.org/html/rfc7231#section-4.2.3

    // In this case, we only specify "GET" and "HEAD as "POST" is a
    // cacheable method only if the request contains explicit freshness
    // headers, and even then it's not terribly useful.
    // https://www.mnot.net/blog/2012/09/24/caching_POST
    // https://tools.ietf.org/html/draft-ietf-httpbis-p2-semantics-20#section-2.3.4
    requestMethod match {
      case "GET" | "HEAD" =>
        true
      case _ =>
        false
    }
  }

  /**
   * Returns true if the response code is cacheable by default.
   */
  override def isDefaultCacheable(statusCode: Int): Boolean = {
    // 200, 203, 204, 206, 300, 301, 404, 405, 410, 414, and 501 are all cacheable by default.
    // 308 is also cacheable.
    // http://tools.ietf.org/html/rfc7231#section-6.1
    statusCode match {
      case 200 =>
        true
      case 203 =>
        true
      case 204 =>
        true
      case 206 =>
        true
      case 300 =>
        true
      case 301 =>
        true
      case 308 =>
        true
      case 404 =>
        true
      case 405 =>
        true
      case 410 =>
        true
      case 414 =>
        true
      case 501 =>
        true
      case default =>
        false
    }
  }

  override def containsMatchingHeaders(
      presentedHeaders: Map[HeaderName, Seq[String]],
      nominatedHeaders: Map[HeaderName, Seq[String]]
  ): Boolean = {
    if (nominatedHeaders.isEmpty) {
      return true
    }

    // A Vary header field value of "*" always fails to match.
    if (nominatedHeaders.contains(HeaderName("*"))) {
      return false
    }

    // ALL of the selecting header fields nominated by the Vary header field match.
    val nominatedHeadersIter = nominatedHeaders.keys.iterator
    while (nominatedHeadersIter.hasNext) {
      val nominatedHeaderName = nominatedHeadersIter.next()
      presentedHeaders.get(nominatedHeaderName) match {
        case None                        => return false
        case Some(presentedHeaderValues) =>
          val nominatedHeaderValues = nominatedHeaders(nominatedHeaderName)
          // There's a list of header values, so we need a way to normalize it.
          //
          // This is much more complicated than it looks, because individual headers have their own semantics,
          // for example the Cache-Control header's directives are case insensitive.  In others, the order
          // is not significant, i.e.

          // Accept-Encoding: gzip,deflate
          // and
          // Accept-Encoding: deflate,gzip
          //
          // have the same semantic meaning, but may be seen differently.
          //
          // http://coad.measurement-factory.com/cgi-bin/coad2/GraseInfoCgi?info_id=test_clause/rfc2616/varyMismatch&proto=http
          //
          // Properly implementing this feature means correctly parsing every single defined HTTP header.  Out of scope.
          // So, we punt, and just start looking for straight values.
          val nominatedHeaderValuesIter = nominatedHeaderValues.iterator
          while (nominatedHeaderValuesIter.hasNext) {
            val nominatedHeaderValue = nominatedHeaderValuesIter.next()
            if (!presentedHeaderValues.exists(value => value.equals(nominatedHeaderValue))) {
              return false
            }
          }
      }
    }
    true
  }

  override def isCacheableExtension(extension: CacheDirectives.CacheDirectiveExtension): Boolean = {
    false
  }
}
