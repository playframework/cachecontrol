/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */

package com.typesafe.play.cachecontrol

class CacheControlException(message: String, ex: Throwable) extends RuntimeException(message, ex) {

  def this(message: String) = this(message, null)

  def this(ex: Throwable) = this(null, ex)
}
