/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

object Dependencies {
  // Sync versions in .travis.yml
  val Scala212 = "2.12.11"
  val Scala213 = "2.13.2"

  val ScalaVersions = Seq(Scala212, Scala213)

  def scalaTest = "org.scalatest" %% "scalatest" % "3.2.3" % Test

  val parserCombinators =
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"

  val slf4jVersion = "1.7.30"
  val slf4j        = "org.slf4j" % "slf4j-api"    % slf4jVersion
  val slf4jSimple  = "org.slf4j" % "slf4j-simple" % slf4jVersion
}
