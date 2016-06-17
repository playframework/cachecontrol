import Dependencies._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform._

name := "cachecontrol"

organization := "com.typesafe.play"

scalaVersion := "2.11.8"

version := "1.1.0-SNAPSHOT"

crossScalaVersions := Seq("2.10.5", "2.11.6")

publishMavenStyle := true

// Set "set isSnapshot := false" when releasing a new version to production.
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) {
    Some("snapshots" at nexus + "content/repositories/snapshots")
  } else {
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
}

pomIncludeRepository := { _ => false }

// Good practice options
scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-Xlint",
  "-deprecation",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Xfatal-warnings"
)


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

scalariformSettings
