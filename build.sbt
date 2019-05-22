import Dependencies._

name := "cachecontrol"

organization := "com.typesafe.play"

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.12.8", "2.11.12", "2.13.0-RC2")

scalacOptions ++= Seq("-encoding", "utf8")
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint")

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 11)) =>
      libraryDependencies.value ++ parserCombinators211
    case Some((2, scalaMajor)) if scalaMajor >= 12 =>
      libraryDependencies.value ++ parserCombinators
    case _ =>
      // Earlier than 2.11, and parser combinators are included automatically.
      libraryDependencies.value
  }
}

libraryDependencies ++= scalaTest ++ jodaTime ++ slf4j

libraryDependencies += "org.slf4j" % "slf4j-simple" % slf4jVersion % Test

mimaPreviousArtifacts := {
  if (scalaVersion.value.startsWith("2.13.0-")) Set.empty
  else Set("com.typesafe.play" %% "cachecontrol" % "1.1.5")
}

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
