/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
package com.typesafe.play.cachecontrol

/**
 * Tells the cache what headers should be stripped before they are cached.
 */
class StripHeaderCalculator(cache: Cache) {

  def stripHeaders(response: CacheResponse): Set[HeaderName] = {
    val directives = response.directives
    val buffer = scala.collection.mutable.ListBuffer[HeaderName]()

    if (cache.isShared) {
      CacheDirectives.`private`(directives).foreach {
        _.headerNames.foreach { headers =>
          headers.foreach((name) => buffer += HeaderName(name))
        }
      }
    }

    CacheDirectives.noCache(directives).foreach {
      _.headerNames.foreach { headers =>
        headers.foreach(name => buffer += HeaderName(name))
      }
    }
    buffer.toSet
  }

}
