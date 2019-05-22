/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
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
