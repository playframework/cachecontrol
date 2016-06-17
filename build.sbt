import Dependencies._

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform._

name := "cachecontrol"

organization := "com.typesafe.play"

scalaVersion := "2.11.8"

version := "1.0.0-SNAPSHOT"

crossScalaVersions := Seq("2.10.5", "2.11.6")

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomIncludeRepository := { _ => false }

pomExtra :=
  <url>http://github.com/playframework/cachecontrol</url>
    <licenses>
      <license>
        <name>Apache License, Version 2.0</name>
        <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:playframework/cachecontrol.git</url>
      <connection>scm:git:git@github.com:playframework/cachecontrol.git</connection>
    </scm>
    <developers>
      <developer>
        <id>Play Team</id>
        <name>Play Team</name>
        <url>http://playframework.com</url>
      </developer>
    </developers>

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
