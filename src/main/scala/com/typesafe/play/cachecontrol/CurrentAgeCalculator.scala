/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
package com.typesafe.play.cachecontrol

import org.joda.time._
import org.slf4j.LoggerFactory

/**
 * Calculates the current age of a response.
 *
 * https://tools.ietf.org/html/rfc7234#section-4.2.3
 */
class CurrentAgeCalculator {
  import HeaderNames._
  import CurrentAgeCalculator._

  def calculateCurrentAge(request: CacheRequest, response: StoredResponse, requestTime: DateTime, responseTime: DateTime): Seconds = {
    calculateCurrentAge(response.headers, HttpDate.now, requestTime, responseTime)
  }

  def calculateCurrentAge(
    headers: Map[HeaderName, Seq[String]],
    now: DateTime,
    requestTime: DateTime,
    responseTime: DateTime): Seconds = {
    if (logger.isTraceEnabled) {
      logger.trace(s"calculateCurrentAge(headers: $headers, now: $now, requestTime: $requestTime, responseTime: $responseTime)")
    }
    val ageValue = calculateAgeValue(headers)
    val dateValue = calculateDateValue(headers)

    //  apparent_age = max(0, response_time - date_value);
    val apparentAge = {
      if (responseTime.isAfter(dateValue)) {
        Seconds.secondsBetween(dateValue, responseTime)
      } else {
        Seconds.seconds(0)
      }
    }
    // response_delay = response_time - request_time;
    val responseDelay = Seconds.secondsBetween(requestTime, responseTime)

    //  corrected_age_value = age_value + response_delay;
    val correctedAgeValue = ageValue.plus(responseDelay)

    // corrected_initial_age = max(apparent_age, corrected_age_value);
    val correctedInitialAge = {
      val a = Math.max(apparentAge.getSeconds, correctedAgeValue.getSeconds)
      Seconds.seconds(a)
    }

    // resident_time = now - response_time;
    val residentTime = Seconds.secondsBetween(responseTime, now)

    // current_age = corrected_initial_age + resident_time;
    val currentAge = correctedInitialAge.plus(residentTime)
    if (logger.isTraceEnabled) {
      logger.trace(s"calculateCurrentAge: currentAge = $currentAge")
    }
    currentAge
  }

  // value of the Age header field in seconds, or 0 if not available.
  def calculateAgeValue(headers: Map[HeaderName, Seq[String]]): Seconds = {
    // https://tools.ietf.org/html/rfc7234#section-5.1
    // Age is delta-seconds since generated or last validated.
    headers.get(`Age`).flatMap(_.headOption).map { age =>
      Seconds.seconds(age.toInt)
    }.getOrElse {
      Seconds.seconds(0)
    }
  }

  def calculateDateValue(headers: Map[HeaderName, Seq[String]]): DateTime = {
    val result = for {
      dateValues <- headers.get(`Date`)
      firstDateValue <- dateValues.headOption
    } yield {
      HttpDate.parse(firstDateValue)
    }
    result.getOrElse {
      val msg = "Date header is required for age calculation! (see RFC7231, 7.1.1.2)"
      throw new CacheControlException(msg)
    }
  }

}

object CurrentAgeCalculator {
  private val logger = LoggerFactory.getLogger("com.typesafe.cachecontrol.CurrentAgeCalculator")

}
