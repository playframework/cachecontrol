import Dependencies._

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}

val previousVersion: Option[String] = Some("2.3.0")

lazy val cachecontrol = (project in file("."))
  .enablePlugins(Common)
  .settings(
    libraryDependencies ++= Seq(
      parserCombinators(scalaVersion.value),
      scalaTest,
      slf4j,
      slf4jSimple % Test
    ),
    mimaPreviousArtifacts := previousVersion.map(organization.value %% moduleName.value % _).toSet,
    headerLicense := {
      Some(
        HeaderLicense.Custom(
          s"Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>"
        )
      )
    }
  )
