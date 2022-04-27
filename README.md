[![Build Status](https://travis-ci.org/playframework/cachecontrol.svg?branch=main)](https://travis-ci.org/playframework/cachecontrol) [![Maven](https://img.shields.io/maven-central/v/com.typesafe.play/cachecontrol_2.12.svg)](http://mvnrepository.com/artifact/com.typesafe.play/cachecontrol_2.12)

This is a minimal library that provides a set of utility calculators that abstract away much of the fiddly complexities involved in adhering to the HTTP caching model, aka [RFC 7234](https://tools.ietf.org/html/rfc7234).  The core idea is abstracting the decision about what to cache and when, following the example of [Caching is Hard, Draw Me A Picture](http://www.bizcoder.com/caching-is-hard-draw-me-a-picture).

It does not implement caching itself, and there are some aspects of RFC 7234 which must be implemented directly, such as stripping headers, invalidating unsafe methods, and implementing the Vary header functionality correctly.

## Usage

To add this project to sbt, use:

```
libraryDependencies += "com.typesafe.play" %% "cachecontrol" % -latest version-
``` 
