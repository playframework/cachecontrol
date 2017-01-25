/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
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
