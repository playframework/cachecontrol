/*
 * Copyright (C) 2015-2017 Lightbend, Inc. All rights reserved.
 */
import sbt._

object Dependencies {

  def scalaTest = Seq("org.scalatest" %% "scalatest" % "3.0.8-RC4" % "test")

  val parserCombinators = Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
  )

  val parserCombinators211 = Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1"
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
