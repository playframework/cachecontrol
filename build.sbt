import Dependencies._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform._

name := "cachecontrol"

organization := "com.typesafe"

scalaVersion := "2.11.6"

version := "1.0-SNAPSHOT"

crossScalaVersions := Seq("2.10.5", "2.11.6")

val typesafeIvyReleases = Resolver.url("typesafe-ivy-private-releases", new URL("http://private-repo.typesafe.com/typesafe/ivy-releases/"))(Resolver.ivyStylePatterns)

publishTo := Some(typesafeIvyReleases)

publishMavenStyle := false

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

ScalariformKeys.preferences := FormattingPreferences()
  .setPreference(AlignParameters, true)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(DoubleIndentClassDeclaration, true)

scalariformSettings
