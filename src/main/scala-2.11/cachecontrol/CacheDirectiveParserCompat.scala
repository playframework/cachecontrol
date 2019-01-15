/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package com.typesafe.play.cachecontrol

object CacheDirectiveParserCompat {
  def toImmutableSeq[T](seq: Seq[T]): scala.collection.immutable.Seq[T] = {
    seq.to[scala.collection.immutable.Seq]
  }
}