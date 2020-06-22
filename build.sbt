import Dependencies._

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
  .enablePlugins(Common, Publish)
  .settings(
    libraryDependencies ++= Seq(
      parserCombinators,
      scalaTest,
      slf4j,
      slf4jSimple % Test
    ),
    mimaPreviousArtifacts := Set(
      organization.value %% name.value % previousStableVersion.value
        .getOrElse(throw new Error("Unable to determine previous version"))
    ),
    headerLicense := {
      Some(
        HeaderLicense.Custom(
          s"Copyright (C) Lightbend Inc. <https://www.lightbend.com>"
        )
      )
    }
  )
