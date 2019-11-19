/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

class CacheControlException(message: String, ex: Throwable) extends RuntimeException(message, ex) {
  def this(message: String) = this(message, null)

  def this(ex: Throwable) = this(null, ex)
}
