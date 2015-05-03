package com.typesafe.play.cachecontrol

import org.joda.time.Seconds

class StubCache(shared: Boolean) extends CacheDefaults {

  override def isShared: Boolean = shared

  override def calculateFreshnessFromHeuristic(request: CacheRequest, response: CacheResponse): Option[Seconds] = None
}
