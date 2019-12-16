import Dependencies._
import interplay.ScalaVersions._

playBuildRepoName in ThisBuild := "cachecontrol"

val previousVersion = Some("2.0.0")

lazy val cachecontrol = (project in file("."))
  .enablePlugins(PlayReleaseBase, PlayLibrary)
  .settings(
    name := "cachecontrol",
    organization := "com.typesafe.play",
    scalaVersion := scala212,
    crossScalaVersions := Seq(scala212, scala213),
    scalacOptions ++= {
      Seq(
        "-target:jvm-1.8",
        "-encoding",
        "utf8",
        "-deprecation",
        "-feature",
        "-unchecked",
        "-Xlint",
        "-Ywarn-unused:imports",
        "-Xlint:nullary-unit",
        "-Ywarn-dead-code",
      )
    },
    javacOptions ++= Seq(
      "-source",
      "1.8",
      "-target",
      "1.8",
      "-Xlint:deprecation",
      "-Xlint:unchecked",
    ),
    unmanagedSourceDirectories in Compile += {
      val sourceDir = (sourceDirectory in Compile).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 13)) => sourceDir / "scala-2.13+"
        case _             => sourceDir / "scala-2.13-"
      }
    },
    libraryDependencies ++= parserCombinators ++ scalaTest ++ slf4j,
    libraryDependencies += "org.slf4j" % "slf4j-simple" % slf4jVersion % Test,
    headerLicense := {
      val currentYear = java.time.Year.now(java.time.Clock.systemUTC).getValue
      Some(
        HeaderLicense.Custom(
          s"Copyright (C) 2009-$currentYear Lightbend Inc. <https://www.lightbend.com>"
        )
      )
    },
    mimaFailOnNoPrevious := false,
    mimaPreviousArtifacts := previousVersion.map(version => organization.value %% name.value % version).toSet
  )
