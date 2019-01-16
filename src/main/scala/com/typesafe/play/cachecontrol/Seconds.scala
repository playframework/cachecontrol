/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

import java.time.{ DateTimeException, Duration }
import java.time.temporal.{ Temporal, TemporalAmount, TemporalUnit, UnsupportedTemporalTypeException }
import java.util

import scala.language.implicitConversions

/**
 * An immutable time period representing a number of seconds.
 */
case class Seconds private (duration: Duration) extends TemporalAmount {

  /**
   * Gets the number of seconds that this period represents.
   *
   * @return the number of seconds in the period
   */
  def seconds: Long = duration.getSeconds

  /**
   * Returns a new instance with the specified number of seconds added.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param other  the amount of seconds to add, may be negative, null means zero
   * @return the new period plus the specified number of seconds
   * @throws ArithmeticException if the result overflows an int
   */
  def plus(other: Seconds): Seconds =
    Seconds.ofDuration(duration.plus(other.duration))

  /**
   * Returns a new instance with the specified number of seconds taken away.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param other  the amount of seconds to take away, may be negative, null means zero
   * @return the new period minus the specified number of seconds
   * @throws ArithmeticException if the result overflows an int
   */
  def minus(other: Seconds): Seconds =
    Seconds.ofDuration(duration.minus(other.duration))

  /**
   * Is this seconds instance less than the specified number of seconds.
   *
   * @param other  the other period, null means zero
   * @return true if this seconds instance is less than the specified one
   */
  def isLessThan(other: Seconds): Boolean =
    duration.compareTo(other.duration) < 0

  /**
   * Is this seconds instance greater than the specified number of seconds.
   *
   * @param other  the other period, null means zero
   * @return true if this seconds instance is greater than the specified one
   */
  def isGreaterThan(other: Seconds): Boolean =
    duration.compareTo(other.duration) > 0

  /**
   * Gets this instance as a String in the ISO8601 duration format.
   * <p>
   * For example, "PT4S" represents 4 seconds.
   *
   * @return the value as an ISO8601 string
   */
  override def toString: String = s"PT${seconds}S"

  // shims to calculate with other java8 apis

  /**
   * Gets the value of the requested unit.
   * <p>
   * This returns a value for each of the two supported units,
   * {@link ChronoUnit#SECONDS SECONDS} and {@link ChronoUnit#NANOS NANOS}.
   * All other units throw an exception.
   *
   * @param unit the { @code TemporalUnit} for which to return the value
   * @return the long value of the unit
   * @throws DateTimeException if the unit is not supported
   * @throws UnsupportedTemporalTypeException if the unit is not supported
   */
  override def get(unit: TemporalUnit): Long = duration.get(unit)
  /**
   * Subtracts this duration from the specified temporal object.
   * <p>
   * This returns a temporal object of the same observable type as the input
   * with this duration subtracted.
   * <p>
   * In most cases, it is clearer to reverse the calling pattern by using
   * {@link Temporal#minus(TemporalAmount)}.
   * <pre>
   *   // these two lines are equivalent, but the second approach is recommended
   *   dateTime = thisDuration.subtractFrom(dateTime);
   *   dateTime = dateTime.minus(thisDuration);
   * </pre>
   * <p>
   * The calculation will subtract the seconds, then nanos.
   * Only non-zero amounts will be added.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param temporal  the temporal object to adjust, not null
   * @return an object of the same type with the adjustment made, not null
   * @throws DateTimeException if unable to subtract
   * @throws ArithmeticException if numeric overflow occurs
   */
  override def subtractFrom(temporal: Temporal): Temporal = duration.subtractFrom(temporal)

  /**
   * Gets the set of units supported by this duration.
   * <p>
   * The supported units are {@link ChronoUnit#SECONDS SECONDS},
   * and {@link ChronoUnit#NANOS NANOS}.
   * They are returned in the order seconds, nanos.
   * <p>
   * This set can be used in conjunction with {@link #get(TemporalUnit)}
   * to access the entire state of the duration.
   *
   * @return a list containing the seconds and nanos units, not null
   */
  override def getUnits: util.List[TemporalUnit] = duration.getUnits

  /**
   * Adds this duration to the specified temporal object.
   * <p>
   * This returns a temporal object of the same observable type as the input
   * with this duration added.
   * <p>
   * In most cases, it is clearer to reverse the calling pattern by using
   * {@link Temporal#plus(TemporalAmount)}.
   * <pre>
   *   // these two lines are equivalent, but the second approach is recommended
   *   dateTime = thisDuration.addTo(dateTime);
   *   dateTime = dateTime.plus(thisDuration);
   * </pre>
   * <p>
   * The calculation will add the seconds, then nanos.
   * Only non-zero amounts will be added.
   * <p>
   * This instance is immutable and unaffected by this method call.
   *
   * @param temporal  the temporal object to adjust, not null
   * @return an object of the same type with the adjustment made, not null
   * @throws DateTimeException if unable to add
   * @throws ArithmeticException if numeric overflow occurs
   */
  override def addTo(temporal: Temporal): Temporal = duration.addTo(temporal)

}

object Seconds {

  /** implicit that will only use the seconds part of a duration */
  implicit def fromDurationToSecond(duration: Duration): Seconds = {
    Seconds.seconds(duration.getSeconds)
  }

  /** Constant representing zero seconds. */
  val ZERO: Seconds = Seconds.ofDuration(Duration.ZERO)

  /**
   * Creates a [[Seconds]] representing the number of whole seconds
   * between the two specified datetimes.
   *
   * @param startInclusive  the start instant, must not be null
   * @param endInclusive  the end instant, must not be null
   * @return the period in seconds
   * @throws IllegalArgumentException if the instants are null or invalid
   */
  def between(startInclusive: Temporal, endInclusive: Temporal): Seconds = {
    Seconds.seconds(Duration.between(startInclusive, endInclusive).getSeconds)
  }

  /**
   * Obtains an instance of [[Seconds]].
   * This will actually only use the seconds of a [[java.time.Duration]].
   *
   * @param duration the duration
   * @return the instance of Seconds
   */
  def ofDuration(duration: Duration): Seconds = {
    new Seconds(Duration.ofSeconds(duration.getSeconds))
  }

  /**
   * Obtains an instance of [[Seconds]].
   *
   * @param seconds  the number of seconds to obtain an instance for
   * @return the instance of Seconds
   */
  def seconds(seconds: Long): Seconds = {
    new Seconds(Duration.ofSeconds(seconds))
  }

  /**
   * Creates a new [[Seconds]] by parsing a string in the ISO8601 format 'PTnS'.
   * <p>
   * The parse will accept the full ISO syntax of PnYnMnWnDTnHnMnS however only the
   * seconds component may be non-zero. If any other component is non-zero, an exception
   * will be thrown.
   *
   * @param periodStr  the period string, null returns zero
   * @return the period in seconds
   * @throws IllegalArgumentException if the string format is invalid
   */
  def parse(periodStr: String): Seconds = {
    new Seconds(Duration.ofSeconds(Duration.parse(periodStr).getSeconds))
  }

}
