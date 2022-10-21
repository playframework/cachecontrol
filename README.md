# Cachecontrol - Minimal HTTP cache management library in Scala

[![Twitter Follow](https://img.shields.io/twitter/follow/playframework?label=follow&style=flat&logo=twitter&color=brightgreen)](https://twitter.com/playframework)
[![Discord](https://img.shields.io/discord/931647755942776882?logo=discord&logoColor=white)](https://discord.gg/g5s2vtZ4Fa)
[![GitHub Discussions](https://img.shields.io/github/discussions/playframework/playframework?&logo=github&color=brightgreen)](https://github.com/playframework/playframework/discussions)
[![StackOverflow](https://img.shields.io/static/v1?label=stackoverflow&logo=stackoverflow&logoColor=fe7a16&color=brightgreen&message=playframework)](https://stackoverflow.com/tags/playframework)
[![YouTube](https://img.shields.io/youtube/channel/views/UCRp6QDm5SDjbIuisUpxV9cg?label=watch&logo=youtube&style=flat&color=brightgreen&logoColor=ff0000)](https://www.youtube.com/channel/UCRp6QDm5SDjbIuisUpxV9cg)
[![Twitch Status](https://img.shields.io/twitch/status/playframework?logo=twitch&logoColor=white&color=brightgreen&label=live%20stream)](https://www.twitch.tv/playframework)
[![OpenCollective](https://img.shields.io/opencollective/all/playframework?label=financial%20contributors&logo=open-collective)](https://opencollective.com/playframework)

[![Build Status](https://github.com/playframework/cachecontrol/actions/workflows/build-test.yml/badge.svg)](https://github.com/playframework/cachecontrol/actions/workflows/build-test.yml)
[![Maven](https://img.shields.io/maven-central/v/com.typesafe.play/cachecontrol_2.13.svg?logo=apache-maven)](https://mvnrepository.com/artifact/com.typesafe.play/cachecontrol_2.13)
[![Repository size](https://img.shields.io/github/repo-size/playframework/cachecontrol.svg?logo=git)](https://github.com/playframework/cachecontrol)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![Mergify Status](https://img.shields.io/endpoint.svg?url=https://api.mergify.com/v1/badges/playframework/cachecontrol&style=flat)](https://mergify.com)

This is a minimal library that provides a set of utility calculators that abstract away much of the fiddly complexities involved in adhering to the HTTP caching model, aka [RFC 7234](https://tools.ietf.org/html/rfc7234).  The core idea is abstracting the decision about what to cache and when, following the example of [Caching is Hard, Draw Me A Picture](http://www.bizcoder.com/caching-is-hard-draw-me-a-picture).

It does not implement caching itself, and there are some aspects of RFC 7234 which must be implemented directly, such as stripping headers, invalidating unsafe methods, and implementing the Vary header functionality correctly.

## Usage

To add this project to sbt, use:

```
libraryDependencies += "com.typesafe.play" %% "cachecontrol" % -latest version-
```

## Releasing a new version

See https://github.com/playframework/.github/blob/main/RELEASING.md
