/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
package com.typesafe.play.cachecontrol

import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder, ResolverStyle, TextStyle }
import java.time.temporal.ChronoField
import java.time._

import scala.util.Try
import scala.util.control.NonFatal

/**
 * Defines methods for parsing and formatting HTTP dates.
 */
object HttpDate {

  /**
   * The GMT time zone.
   */
  val zone: ZoneId = ZoneId.of("GMT")

  private val EMPTY = ' '

  // IMF-fixdate
  private val imfFixDateFormat = DateTimeFormatter
    .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
    .withLocale(java.util.Locale.ENGLISH)
    .withZone(zone)

  // RFC 850 / 1036 format.
  private def rfc850Format(pattern: String) = {
    new DateTimeFormatterBuilder()
      .appendPattern(pattern)
      .appendLiteral(',')
      .appendLiteral(EMPTY)
      .appendPattern("dd-MMM-")
      // Pivot Year:
      // https://tools.ietf.org/html/rfc6265#section-5.1.1
      .appendValueReduced(ChronoField.YEAR, 2, 2, 1970)
      .appendLiteral(EMPTY)
      .appendPattern("HH:mm:ss")
      .appendLiteral(EMPTY)
      .appendLiteral("GMT")
      .parseLenient()
      .toFormatter(java.util.Locale.ENGLISH)
      .withZone(zone)
      .withResolverStyle(ResolverStyle.LENIENT)
  }

  // asctime format.
  private val asctimeFormat = DateTimeFormatter
    .ofPattern("EEE MMM d HH:mm:ss yyyy")
    .withLocale(java.util.Locale.ENGLISH)
    .withZone(zone)

  def format(dateTime: ZonedDateTime): String = {
    // When a sender generates a header field that contains one or more timestamps defined as
    // HTTP-date, the sender MUST generate those timestamps in the IMF-fixdate format.
    imfFixDateFormat.format(dateTime)
  }

  /**
   * Returns the number of seconds between two dates.
   */
  def diff(start: ZonedDateTime, end: ZonedDateTime): Seconds = {
    // we only need the diff of the seconds, so we actually use between
    // and then remove the seconds part
    Seconds.between(start, end)
  }

  /**
   * Returns the current time, in the correct GMT time zone.
   */
  def now: ZonedDateTime = ZonedDateTime.now(zone)

  /**
   * Parses an HTTP date according to http://tools.ietf.org/html/rfc7231#section-7.1.1.1
   */
  def parse(dateString: String): ZonedDateTime = {
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
  private def parseIMF(dateString: String): ZonedDateTime = {
    ZonedDateTime.parse(dateString, imfFixDateFormat)
  }
  private def parseAscTime(dateString: String): ZonedDateTime = {
    // Sun Nov 6 08:49:37 1994         ; ANSI C's asctime() format
    ZonedDateTime.parse(dateString, asctimeFormat)
  }
  // http://tools.ietf.org/html/rfc850#section-2.1.4
  private def parseRFC850(dateString: String): ZonedDateTime = {
    // Sunday, 06-Nov-94 08:49:37 GMT   ; obsolete RFC 850 format

    // Recipients of a timestamp value in rfc850-date format, which uses a
    // two-digit year, MUST interpret a timestamp that appears to be more
    // than 50 years in the future as representing the most recent year in
    // the past that had the same last two digits.
    // -- java.time needs to handle 'Sunday' and 'Sun' with different date formatters and
    // we need to provide a pivot year like explained in the following RFC:
    // https://tools.ietf.org/html/rfc6265#section-5.1.1

    try {
      ZonedDateTime.parse(dateString, rfc850Format("EEE"))
    } catch {
      case NonFatal(_) => ZonedDateTime.parse(dateString, rfc850Format("EEEE"))
    }
  }

  /**
   * Produces a DateTime object, given the time since epoch IN SECONDS.  Note that most
   * Java methods return TSE in milliseconds, so be careful.
   */
  def fromEpochSeconds(timeSinceEpochInSeconds: Int): ZonedDateTime = {
    ZonedDateTime.ofInstant(
      Instant.ofEpochSecond(timeSinceEpochInSeconds),
      HttpDate.zone)
  }

}
