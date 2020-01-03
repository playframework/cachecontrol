/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

class StubCache(shared: Boolean) extends CacheDefaults {
  override def isShared: Boolean = shared

  override def calculateFreshnessFromHeuristic(request: CacheRequest, response: CacheResponse): Option[Seconds] = None
}
