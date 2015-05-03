/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.play.cachecontrol

import org.joda.time.DateTime

/**
 * A parsed warning.
 */
case class Warning(code: Int, agent: String, text: String, date: Option[DateTime] = None) {
  override def toString: String = {
    date.map { d =>
      val httpDate = HttpDate.format(d)
      s"""$code $agent "$text" "$httpDate""""
    }.getOrElse {
      s"""$code $agent "$text""""
    }
  }
}
