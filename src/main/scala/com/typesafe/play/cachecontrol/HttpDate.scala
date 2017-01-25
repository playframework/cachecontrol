/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
package com.typesafe.play.cachecontrol

import java.time.format.DateTimeFormatter
import java.time.temporal.{ Temporal, TemporalAccessor, TemporalQuery }
import java.time.{ Duration, ZoneId, ZonedDateTime }

import scala.util.Try

/**
 * Defines methods for parsing and formatting HTTP dates.
 */
object HttpDate {

  /**
   * The GMT time zone.
   */
  val zoneId: ZoneId = ZoneId.of("GMT")
  def zone: java.util.TimeZone = java.util.TimeZone.getTimeZone(zoneId)

  // IMF-fixdate
  private val imfFixDateFormat: DateTimeFormatter = {
    // RFC 7231
    DateTimeFormatter.RFC_1123_DATE_TIME
  }

  // RFC 850 / 1036 format.
  private def rfc850Format: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss 'GMT'").withLocale(java.util.Locale.ENGLISH).withZone(zoneId)

  // asctime format.
  private def asctimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy").withLocale(java.util.Locale.ENGLISH).withZone(zoneId)

  def format(dateTime: TemporalAccessor): String = {
    // When a sender generates a header field that contains one or more timestamps defined as
    // HTTP-date, the sender MUST generate those timestamps in the IMF-fixdate format.
    imfFixDateFormat.format(dateTime)
  }

  /**
   * Returns the duration between two dates.
   */
  def diff(start: Temporal, end: Temporal): Duration = {
    Duration.between(start, end)
  }

  /**
   * Returns the current time, in the correct GMT time zone.
   */
  def now: ZonedDateTime = ZonedDateTime.now(zoneId)

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

  /**
   * Produces a DateTime object, given the time since epoch IN SECONDS.  Note that most
   * Java methods return TSE in milliseconds, so be careful.
   */
  def fromEpochSeconds(timeSinceEpochInSeconds: Int): ZonedDateTime = {
    new ZonedDateTime(timeSinceEpochInSeconds.toLong * 1000).withZoneSameLocal(zoneId)
  }

  private def parseIMF(dateString: String): ZonedDateTime = {
    val query: TemporalQuery[ZonedDateTime] = ZonedDateTime.from _
    imfFixDateFormat.parse(dateString, query)
  }

  private def parseAscTime(dateString: String): ZonedDateTime = {
    // Sun Nov  6 08:49:37 1994         ; ANSI C's asctime() format
    val query: TemporalQuery[ZonedDateTime] = ZonedDateTime.from _
    asctimeFormat.parse(dateString, query)
  }

  // http://tools.ietf.org/html/rfc850#section-2.1.4
  private def parseRFC850(dateString: String): ZonedDateTime = {
    // Sunday, 06-Nov-94 08:49:37 GMT   ; obsolete RFC 850 format

    // Recipients of a timestamp value in rfc850-date format, which uses a
    // two-digit year, MUST interpret a timestamp that appears to be more
    // than 50 years in the future as representing the most recent year in
    // the past that had the same last two digits.
    // -- it turns out that Joda Time handles this automatically, because it can
    // determine

    val query: TemporalQuery[ZonedDateTime] = ZonedDateTime.from _
    rfc850Format.parse(dateString, query)
  }

}
