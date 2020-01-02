/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

object CacheDirectiveParserCompat {
  def toImmutableSeq[T](seq: Seq[T]): scala.collection.immutable.Seq[T] = {
    seq match {
      case xs: scala.collection.immutable.Seq[T @unchecked] => xs
      case _ =>
        val b = scala.collection.immutable.Seq.newBuilder[T]
        seq.iterator.foreach(b += _)
        b.result()
    }
  }
}
