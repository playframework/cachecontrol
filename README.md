# CacheControl

[![Build Status](https://travis-ci.org/playframework/cachecontrol.svg?branch=master)](https://travis-ci.org/playframework/cachecontrol) [![Maven](https://img.shields.io/maven-central/v/com.typesafe.play/cachecontrol_2.12.svg)](http://mvnrepository.com/artifact/com.typesafe.play/cachecontrol_2.12)

This is a minimal library that provides a set of utility calculators that abstract away much of the fiddly complexities involved in adhering to the HTTP caching model, aka [RFC 7234](https://tools.ietf.org/html/rfc7234).  The core idea is abstracting the decision about what to cache and when, following the example of [Caching is Hard, Draw Me A Picture](http://www.bizcoder.com/caching-is-hard-draw-me-a-picture).

It does not implement caching itself, and there are some aspects of RFC 7234 which must be implemented directly, such as stripping headers, invalidating unsafe methods, and implementing the Vary header functionality correctly.

## Deployment

To add this project to SBT, use:

```
libraryDependencies += "com.typesafe.play" %% "cachecontrol" % "1.1.1"
```

## License 

This software is licensed under the Apache 2 license, quoted below.

Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
