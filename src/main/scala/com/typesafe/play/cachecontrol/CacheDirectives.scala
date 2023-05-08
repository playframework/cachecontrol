/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

/**
 * A trait that marks the cache directives generated
 * from parsing a cache-control header.
 */
sealed trait CacheDirective

// https://tools.ietf.org/html/rfc7234
// https://tools.ietf.org/html/rfc7234#section-5.2
object CacheDirectives {
  // https://tools.ietf.org/html/rfc7234#section-5.2.2.8
  // https://tools.ietf.org/html/rfc7234#section-5.2.1.1
  case class MaxAge(delta: Seconds) extends CacheDirective {
    override def toString: String = s"max-age=${delta.seconds}"
  }

  /**
   * Finds the instance of MaxAge.
   */
  def maxAge(directives: Seq[CacheDirective]): Option[MaxAge] = {
    // We could use a TypeTag here, but until we know that 2.10 code is no
    // longer being used, it's probably safer just to provide the methods
    directives.collectFirst { case directive: MaxAge =>
      directive
    }
  }

  /**
   *    The "max-stale" request directive indicates that the client is
   * willing to accept a response that has exceeded its freshness
   * lifetime.  If max-stale is assigned a value, then the client is
   * willing to accept a response that has exceeded its freshness lifetime
   * by no more than the specified number of seconds.  If no value is
   * assigned to max-stale, then the client is willing to accept a stale
   * response of any age.
   *
   * This directive uses the token form of the argument syntax: e.g.,
   * 'max-stale=10' not 'max-stale="10"'.  A sender SHOULD NOT generate
   * the quoted-string form.
   *
   * https://tools.ietf.org/html/rfc7234#section-5.2.1.2
   */
  case class MaxStale(delta: Option[Seconds]) extends CacheDirective {
    override def toString: String = {
      delta match {
        case Some(d) =>
          s"max-stale=${d.seconds}"
        case None =>
          "max-stale"
      }
    }
  }

  def maxStale(directives: scala.collection.immutable.Seq[CacheDirective]): Option[MaxStale] = {
    // We could use a TypeTag here, but until we know that 2.10 code is no
    // longer being used, it's probably safer just to provide the methods
    directives.collectFirst { case directive: MaxStale =>
      directive
    }
  }

  // https://tools.ietf.org/html/rfc7234#section-5.2.1.3
  case class MinFresh(delta: Seconds) extends CacheDirective {
    override def toString: String = s"min-fresh=${delta.seconds}"
  }

  def minFresh(directives: scala.collection.immutable.Seq[CacheDirective]): Option[MinFresh] = {
    // We could use a TypeTag here, but until we know that 2.10 code is no
    // longer being used, it's probably safer just to provide the methods
    directives.collectFirst { case directive: MinFresh =>
      directive
    }
  }

  // https://tools.ietf.org/html/rfc7234#section-5.2.1.4
  // https://tools.ietf.org/html/rfc7234#section-5.2.2.2
  case class NoCache(headerNames: Option[scala.collection.immutable.Seq[String]]) extends CacheDirective {
    override def toString: String = {
      headerNames match {
        case Some(names) =>
          //This directive uses the quoted-string form of the argument syntax.  A
          //sender SHOULD NOT generate the token form (even if quoting appears
          //not to be needed for single-entry lists).
          s"""no-cache="${names.mkString(",")}""""
        case None =>
          s"no-cache"
      }
    }
  }

  def noCache(directives: scala.collection.immutable.Seq[CacheDirective]): Option[NoCache] = {
    // We could use a TypeTag here, but until we know that 2.10 code is no
    // longer being used, it's probably safer just to provide the methods
    directives.collectFirst { case directive: NoCache =>
      directive
    }
  }

  // https://tools.ietf.org/html/rfc7234#section-5.2.1.5
  // https://tools.ietf.org/html/rfc7234#section-5.2.2.3
  case object NoStore extends CacheDirective {
    override def toString: String = "no-store"
  }

  /**
   * The "no-transform" request directive indicates that an intermediary
   * (whether or not it implements a cache) MUST NOT transform the
   * payload, as defined in Section 5.7.2 of [RFC7230].
   *
   * // https://tools.ietf.org/html/rfc7234#section-5.2.1.6
   *
   * The "no-transform" response directive indicates that an intermediary
   * (regardless of whether it implements a cache) MUST NOT transform the
   * payload, as defined in Section 5.7.2 of [RFC7230].
   *
   * // https://tools.ietf.org/html/rfc7234#section-5.2.2.4
   */
  case object NoTransform extends CacheDirective {
    override def toString: String = "no-transform"
  }

  /**
   * The "only-if-cached" request directive indicates that the client only
   * wishes to obtain a stored response.  If it receives this directive, a
   * cache SHOULD either respond using a stored response that is
   * consistent with the other constraints of the request, or respond with
   * a 504 (Gateway Timeout) status code.  If a group of caches is being
   * operated as a unified system with good internal connectivity, a
   * member cache MAY forward such a request within that group of caches.
   *
   * https://tools.ietf.org/html/rfc7234#section-5.2.1.7
   */
  case object OnlyIfCached extends CacheDirective {
    override def toString: String = "only-if-cached"
  }

  // https://tools.ietf.org/html/rfc7234#section-5.2.2.1
  case object MustRevalidate extends CacheDirective {
    override def toString: String = "must-revalidate"
  }

  // https://tools.ietf.org/html/rfc7234#section-5.2.2.5
  case object Public extends CacheDirective {
    override def toString: String = "public"
  }

  // https://tools.ietf.org/html/rfc7234#section-5.2.2.6
  case class Private(headerNames: Option[scala.collection.immutable.Seq[String]]) extends CacheDirective {
    override def toString: String = {
      headerNames match {
        case Some(names) =>
          //This directive uses the quoted-string form of the argument syntax.  A
          //sender SHOULD NOT generate the token form (even if quoting appears
          //not to be needed for single-entry lists).
          s"""private="${names.mkString(",")}""""
        case None =>
          "private"
      }
    }
  }

  def `private`(directives: scala.collection.immutable.Seq[CacheDirective]): Option[Private] = {
    directives.collectFirst { case directive: Private =>
      directive
    }
  }

  // https://tools.ietf.org/html/rfc7234#section-5.2.2.7
  case object ProxyRevalidate extends CacheDirective {
    override def toString: String = s"proxy-revalidate"
  }

  // https://tools.ietf.org/html/rfc7234#section-5.2.2.9
  case class SMaxAge(delta: Seconds) extends CacheDirective {
    override def toString: String = s"s-maxage=${delta.seconds}"
  }

  def sMaxAge(directives: scala.collection.immutable.Seq[CacheDirective]): Option[SMaxAge] = {
    // We could use a TypeTag here, but until we know that 2.10 code is no
    // longer being used, it's probably safer just to provide the methods
    directives.collectFirst { case directive: SMaxAge =>
      directive
    }
  }

  // https://tools.ietf.org/html/rfc5861#section-3
  case class StaleWhileRevalidate(delta: Seconds) extends CacheDirective {
    override def toString: String = s"stale-while-revalidate=${delta.seconds}"
  }

  def staleWhileRevalidate(directives: scala.collection.immutable.Seq[CacheDirective]): Option[StaleWhileRevalidate] = {
    // We could use a TypeTag here, but until we know that 2.10 code is no
    // longer being used, it's probably safer just to provide the methods
    directives.collectFirst { case directive: StaleWhileRevalidate =>
      directive
    }
  }

  // https://tools.ietf.org/html/rfc5861#section-4
  case class StaleIfError(delta: Seconds) extends CacheDirective {
    override def toString: String = s"stale-if-error=${delta.seconds}"
  }

  def staleIfError(directives: scala.collection.immutable.Seq[CacheDirective]): Option[StaleIfError] = {
    // We could use a TypeTag here, but until we know that 2.10 code is no
    // longer being used, it's probably safer just to provide the methods
    directives.collectFirst { case directive: StaleIfError =>
      directive
    }
  }

  // https://tools.ietf.org/html/rfc7234#section-5.2.3
  case class CacheDirectiveExtension(name: String, value: Option[String]) extends CacheDirective {
    override def toString: String = value.map(v => s"""$name="$v""").getOrElse(name)
  }

  def extensions(
      directives: scala.collection.immutable.Seq[CacheDirective]
  ): scala.collection.immutable.Seq[CacheDirectiveExtension] = {
    // We could use a TypeTag here, but until we know that 2.10 code is no
    // longer being used, it's probably safer just to provide the methods
    directives.collect { case directive: CacheDirectiveExtension =>
      directive
    }
  }
}
