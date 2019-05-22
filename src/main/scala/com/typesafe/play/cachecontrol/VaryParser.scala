/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package com.typesafe.play.cachecontrol

/**
 * Parses the Vary header in to a list of field names.
 */
object VaryParser {

  def parse(fieldValue: String): collection.immutable.Seq[HeaderName] = {
    if (fieldValue.startsWith("*")) {
      List(HeaderName("*"))
    } else {
      val headerNames = fieldValue.split(",\\s*").map(HeaderName)
      headerNames.toList
    }
  }

}
