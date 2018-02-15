import Dependencies._

name := "cachecontrol"

organization := "com.typesafe.play"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.12.4", "2.11.12", "2.10.7", "2.13.0-M2")

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ parserCombinators
    case _ =>
      // Earlier than 2.11, and parser combinators are included automatically.
      libraryDependencies.value
  }
}

libraryDependencies ++= scalaTest ++ jodaTime ++ slf4j

libraryDependencies += "org.slf4j" % "slf4j-simple" % slf4jVersion % Test

//---------------------------------------------------------------
// Release
//---------------------------------------------------------------
import ReleaseTransformations._

releaseCrossBuild := true

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
  releaseStepCommand("+sonatypeReleaseAll"),
  pushChanges
)
