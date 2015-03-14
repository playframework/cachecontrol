/*
 *
 *  * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 *
 */
package com.typesafe.cachecontrol

import org.joda.time._
import org.joda.time.format.DateTimeFormat

import scala.util.Try

/**
 * Defines methods for parsing and formatting HTTP dates.
 */
object HttpDate {

  /**
   * The GMT time zone.
   */
  val zone: DateTimeZone = DateTimeZone.forID("GMT")

  // IMF-fixdate
  private val imfFixDateFormat = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
    .withLocale(java.util.Locale.ENGLISH)
    .withZone(DateTimeZone.forID("GMT"))

  // RFC 850 / 1036 format.
  private val rfc850Format = DateTimeFormat.forPattern("EEEE, dd-MMM-yy HH:mm:ss 'GMT'")
    .withLocale(java.util.Locale.ENGLISH)
    .withZone(DateTimeZone.forID("GMT"))

  // asctime format.
  private val asctimeFormat = DateTimeFormat.forPattern("EEE MMM d HH:mm:ss yyyy")
    .withLocale(java.util.Locale.ENGLISH)
    .withZone(DateTimeZone.forID("GMT"))

  def format(dateTime: DateTime): String = {
    // When a sender generates a header field that contains one or more timestamps defined as
    // HTTP-date, the sender MUST generate those timestamps in the IMF-fixdate format.
    imfFixDateFormat.print(dateTime)
  }

  /**
   * Returns the number of seconds between two dates.
   */
  def diff(first: DateTime, second: DateTime): Seconds = {
    Seconds.secondsBetween(first, second)
  }

  /**
   * Returns the current time, in the correct GMT time zone.
   */
  def now: DateTime = DateTime.now(zone)

  /**
   * Parses an HTTP date according to http://tools.ietf.org/html/rfc7231#section-7.1.1.1
   */
  def parse(dateString: String): DateTime = {
    // A recipient that parses a timestamp value in an HTTP header field
    // MUST accept all three HTTP-date formats.
    Try {
      parseIMF(dateString)
    }.recover {
      case _ => parseRFC850(dateString)
    }.recover {
      case _ => parseAscTime(dateString)
    }.get
  }

  /**
   * Produces a DateTime object, given the time since epoch IN SECONDS.  Note that most
   * Java methods return TSE in milliseconds, so be careful.
   */
  def fromEpochSeconds(timeSinceEpochInSeconds: Int): DateTime = {
    new DateTime(timeSinceEpochInSeconds.toLong * 1000).withZone(HttpDate.zone)
  }

  private def parseIMF(dateString: String): DateTime = {
    imfFixDateFormat.parseDateTime(dateString)
  }

  private def parseAscTime(dateString: String): DateTime = {
    // Sun Nov  6 08:49:37 1994         ; ANSI C's asctime() format
    asctimeFormat.parseDateTime(dateString)
  }

  // http://tools.ietf.org/html/rfc850#section-2.1.4
  private def parseRFC850(dateString: String): DateTime = {
    // Sunday, 06-Nov-94 08:49:37 GMT   ; obsolete RFC 850 format

    // Recipients of a timestamp value in rfc850-date format, which uses a
    // two-digit year, MUST interpret a timestamp that appears to be more
    // than 50 years in the future as representing the most recent year in
    // the past that had the same last two digits.
    // -- it turns out that Joda Time handles this automatically, because it can
    // determine

    rfc850Format.parseDateTime(dateString)
  }

}
