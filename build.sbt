import Dependencies._

name := "cachecontrol"

organization := "com.typesafe.play"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.12.8", "2.11.12", "2.13.0-M5")

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ parserCombinators
    case _ =>
      // Earlier than 2.11, and parser combinators are included automatically.
      libraryDependencies.value
  }
}

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