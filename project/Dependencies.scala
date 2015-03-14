/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
import sbt._

object Dependencies {

  val scalaTestVersion = "2.2.4"

  val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
  )

  val parserCombinatorVersion = "1.0.3"
  val parserCombinators = Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % parserCombinatorVersion
  )

  val slf4jVersion = "1.7.10"
  val slf4j = Seq(
    "org.slf4j" % "slf4j-api" % slf4jVersion,
    "org.slf4j" % "slf4j-nop" % slf4jVersion
  )

  val jodaTime = Seq(
    "joda-time" % "joda-time" % "2.7",
    "org.joda" % "joda-convert" % "1.7"
  )

}