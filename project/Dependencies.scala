/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

object Dependencies {

  def scalaTest = Seq("org.scalatest" %% "scalatest" % "3.0.8-RC1" % "test")

  val parserCombinatorVersion = "1.1.1"
  val parserCombinators = Seq(
    "org.scala-lang.modules" %% "scala-parser-combinators" % parserCombinatorVersion
  )

  val slf4jVersion = "1.7.26"
  val slf4j = Seq(
    "org.slf4j" % "slf4j-api" % slf4jVersion
  )

}
