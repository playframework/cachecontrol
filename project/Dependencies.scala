/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._

object Dependencies {
  val Scala212 = "2.12.20"
  val Scala213 = "2.13.14"
  val Scala3   = "3.3.3"

  val ScalaVersions = Seq(Scala212, Scala213, Scala3)

  def scalaTest = "org.scalatest" %% "scalatest" % "3.2.19" % Test

  def parserCombinators(scalaVersion: String) =
    "org.scala-lang.modules" %% "scala-parser-combinators" % {
      CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, _)) => "1.1.2"
        case _            => "2.4.0"
      }
    }

  val slf4jVersion = "2.0.16"
  val slf4j        = "org.slf4j" % "slf4j-api"    % slf4jVersion
  val slf4jSimple  = "org.slf4j" % "slf4j-simple" % slf4jVersion
}
