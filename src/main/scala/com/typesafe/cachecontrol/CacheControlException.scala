/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.cachecontrol

class CacheControlException(message: String, ex: Throwable) extends RuntimeException(message, ex) {

  def this(message: String) = this(message, null)

  def this(ex: Throwable) = this(null, ex)
}
