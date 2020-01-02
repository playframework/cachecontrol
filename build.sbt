import Dependencies._
import interplay.ScalaVersions._

playBuildRepoName in ThisBuild := "cachecontrol"

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
dynverVTagPrefix in ThisBuild := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  val v = version.value
  if (dynverGitDescribeOutput.value.hasNoTags)
    throw new MessageOnlyException(
      s"Failed to derive version from git tags. Maybe run `git fetch --unshallow`? Version: $v"
    )
  s
}

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
      Some(
        HeaderLicense.Custom(
          s"Copyright (C) Lightbend Inc. <https://www.lightbend.com>"
        )
      )
    },
  )
  .settings(
    Seq(
      releaseProcess := {
        import ReleaseTransformations._
        Seq[ReleaseStep](
          checkSnapshotDependencies,
          runClean,
          releaseStepCommandAndRemaining("+test"),
          releaseStepCommandAndRemaining("+publishSigned"),
          releaseStepCommand("sonatypeBundleRelease"),
          pushChanges // <- this needs to be removed when releasing from tag
        )
      }
    )
  )
