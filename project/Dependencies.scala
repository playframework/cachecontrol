/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

object Dependencies {
  val Scala212 = "2.12.14"
  val Scala213 = "2.13.6"
  val Scala3   = "3.0.2"

  val ScalaVersions = Seq(Scala212, Scala213, Scala3)

  def scalaTest = "org.scalatest" %% "scalatest" % "3.2.9" % Test

  val parserCombinators =
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.0.0"

  val slf4jVersion = "1.7.32"
  val slf4j        = "org.slf4j" % "slf4j-api"    % slf4jVersion
  val slf4jSimple  = "org.slf4j" % "slf4j-simple" % slf4jVersion
}
