/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

class CacheControlException(message: String, ex: Throwable) extends RuntimeException(message, ex) {
  def this(message: String) = this(message, null)

  def this(ex: Throwable) = this(null, ex)
}
