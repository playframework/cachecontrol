resolvers += "sonatype-releases".at("https://oss.sonatype.org/content/repositories/releases/")

addSbtPlugin("com.typesafe.play" % "interplay" % sys.props.get("interplay.version").getOrElse("2.1.3"))

addSbtPlugin("com.typesafe"      % "sbt-mima-plugin"        % "0.6.1")
addSbtPlugin("org.scalastyle"    %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"             % "5.3.1")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"           % "2.2.1")
