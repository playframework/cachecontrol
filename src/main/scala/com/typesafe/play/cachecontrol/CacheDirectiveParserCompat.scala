/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

object CacheDirectiveParserCompat {
  def toImmutableSeq[T](seq: Seq[T]): scala.collection.immutable.Seq[T] = {
    seq match {
      case xs: scala.collection.immutable.Seq[T @unchecked] => xs
      case _                                                =>
        val b = scala.collection.immutable.Seq.newBuilder[T]
        seq.iterator.foreach(b += _)
        b.result()
    }
  }
}
