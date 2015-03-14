/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */

package com.typesafe.cachecontrol

import org.slf4j.LoggerFactory

/**
 * Parses the Vary header in to a list of field names.
 */
object VaryParser {

  private val logger = LoggerFactory.getLogger("com.typesafe.cachecontrol.VaryParser")

  def parse(fieldValue: String): collection.immutable.Seq[HeaderName] = {
    if (fieldValue.startsWith("*")) {
      List(HeaderName("*"))
    } else {
      val headerNames = fieldValue.split(",\\s*").map(HeaderName)
      headerNames.toList
    }
  }

}
