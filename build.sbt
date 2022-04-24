import Dependencies._

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}

lazy val cachecontrol = (project in file("."))
  .enablePlugins(Common)
  .settings(
    libraryDependencies ++= Seq(
      parserCombinators(scalaVersion.value),
      scalaTest,
      slf4j,
      slf4jSimple % Test
    ),
    // On the main branch we don't check for incompatible changes,
    // because it's ok to introduce breaking changes between minor version bumps
    mimaPreviousArtifacts := Set.empty,
    sonatypeProfileName := "com.typesafe.play",
    headerLicense := {
      Some(
        HeaderLicense.Custom(
          s"Copyright (C) Lightbend Inc. <https://www.lightbend.com>"
        )
      )
    }
  )
