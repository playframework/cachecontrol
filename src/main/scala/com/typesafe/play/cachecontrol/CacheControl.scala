/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import java.net.URI

case class HeaderName(private val raw: String) extends Ordered[HeaderName] {
  require(raw != null, "Null header name!")
  require(raw.nonEmpty, "Empty header name!")

  override def toString: String = raw

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case headerName: HeaderName =>
        headerName.raw.equalsIgnoreCase(this.raw)
      case other =>
        false
    }
  }

  // Don't need lexical ordering
  override def compare(that: HeaderName): Int = {
    val xl = this.raw.length
    val yl = that.raw.length
    if (xl < yl) -1 else if (xl > yl) 1 else this.raw.compareToIgnoreCase(that.raw)
  }
}

object HeaderNames {
  val `Cache-Control`: HeaderName = HeaderName("Cache-Control")

  val `Vary`: HeaderName = HeaderName("Vary")

  val `Age`: HeaderName = HeaderName("Age")

  val `Date`: HeaderName = HeaderName("Date")

  val `Pragma`: HeaderName = HeaderName("Pragma")

  val `If-None-Modified`: HeaderName = HeaderName("If-None-Modified")

  val `Expires`: HeaderName = HeaderName("Expires")

  val `Authorization`: HeaderName = HeaderName("Authorization")
}

/**
 * An incoming request which may be served from cache.
 */
case class CacheRequest(uri: URI, method: String, headers: Map[HeaderName, Seq[String]]) {
  import HeaderNames._

  lazy val directives: collection.immutable.Seq[CacheDirective] = CacheDirectiveParser.parse(headers.getOrElse(`Cache-Control`, Nil))
}

trait CacheResponse {
  def uri: URI
  def status: Int
  def headers: Map[HeaderName, Seq[String]]
  def directives: collection.immutable.Seq[CacheDirective]
}

/**
 * A response from an origin server.
 */
case class OriginResponse(uri: URI, status: Int, headers: Map[HeaderName, Seq[String]]) extends CacheResponse {
  import HeaderNames._

  lazy val directives: collection.immutable.Seq[CacheDirective] = CacheDirectiveParser.parse(headers.getOrElse(`Cache-Control`, Nil))
}

/**
 * A response that comes from cache.
 *
 * @param uri the <a href="https://tools.ietf.org/html/rfc7230#section-5.5">effective request URI</a>.  This
 *                     is part of the primary cache lookup key.
 * @param status the numeric cached response status code.
 * @param headers the headers on the stored response.
 * @param requestMethod the original request method that was used to generate the stored response.
 * @param nominatedHeaders the request headers that were nominated by the response's Vary header.
 */
case class StoredResponse(
  uri: URI,
  status: Int,
  headers: Map[HeaderName, Seq[String]],
  requestMethod: String,
  nominatedHeaders: Map[HeaderName, Seq[String]]) extends CacheResponse {
  import HeaderNames._

  lazy val directives: collection.immutable.Seq[CacheDirective] = CacheDirectiveParser.parse(headers.getOrElse(`Cache-Control`, Nil))
}
