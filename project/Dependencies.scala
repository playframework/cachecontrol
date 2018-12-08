/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
import sbt._

object Dependencies {

  def scalaTest = Seq("org.scalatest" %% "scalatest" % "3.0.6-SNAP5" % "test")

  val parserCombinatorVersion = "1.1.1"
  val parserCombinators = Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % parserCombinatorVersion
  )

  val slf4jVersion = "1.7.25"
  val slf4j = Seq(
    "org.slf4j" % "slf4j-api" % slf4jVersion
  )

  val jodaTime = Seq(
    "joda-time" % "joda-time" % "2.9.9",
    "org.joda" % "joda-convert" % "1.9.2"
  )

}
