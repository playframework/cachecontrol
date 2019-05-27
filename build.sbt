import Dependencies._

name := "cachecontrol"

organization := "com.typesafe.play"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.12.8", "2.11.12", "2.13.0-RC2")

scalacOptions ++= {
  if (scalaVersion.value.startsWith("2.11")) {
    Seq("-target:jvm-1.8")
  } else {
    Seq(
      "-target:jvm-1.8",
      "-encoding", "utf8",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Ywarn-unused:imports",
      "-Xlint:nullary-unit",
      "-Ywarn-dead-code",
    )
  }
}

javacOptions ++= Seq(
  "-source", "1.8",
  "-target", "1.8",
  "-Xlint:deprecation",
  "-Xlint:unchecked",
)

unmanagedSourceDirectories in Compile += {
  val sourceDir = (sourceDirectory in Compile).value
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 13)) => sourceDir / "scala-2.13+"
    case _             => sourceDir / "scala-2.13-"
  }
}

libraryDependencies ++= parserCombinators

fork in Test := scalaVersion.value.startsWith("2.11.") // https://github.com/sbt/sbt/issues/4609

libraryDependencies ++= scalaTest ++ slf4j

libraryDependencies += "org.slf4j" % "slf4j-simple" % slf4jVersion % Test

//---------------------------------------------------------------
// Release
//---------------------------------------------------------------
import ReleaseTransformations._

releaseCrossBuild := true

// This automatically selects the snapshots or staging repository
// according to the version value.
publishTo in ThisBuild := Some(sonatypeDefaultResolver.value)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("+publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeRelease"),
  pushChanges
)

headerLicense := {
  val currentYear = java.time.Year.now(java.time.Clock.systemUTC).getValue
  Some(HeaderLicense.Custom(
    s"Copyright (C) 2009-$currentYear Lightbend Inc. <https://www.lightbend.com>"
  ))
}

lazy val checkCodeFormat = taskKey[Unit]("Check that code format is following Scalariform rules")

checkCodeFormat := {
  import scala.sys.process._
  val exitCode = "git diff --exit-code".!
  if (exitCode != 0) {
    sys.error(
      """
        |ERROR: Scalariform check failed, see differences above.
        |To fix, format your sources using sbt scalariformFormat test:scalariformFormat before submitting a pull request.
        |Additionally, please squash your commits (eg, use git commit --amend) if you're going to update this pull request.
      """.stripMargin)
  }
}

addCommandAlias("validateCode", ";scalariformFormat;test:scalariformFormat;headerCheck;test:headerCheck;checkCodeFormat")
