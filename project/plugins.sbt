resolvers += "sonatype-releases".at("https://oss.sonatype.org/content/repositories/releases/")

addSbtPlugin("com.typesafe.play" % "interplay" % sys.props.get("interplay.version").getOrElse("3.0.0"))

addSbtPlugin("com.typesafe"      % "sbt-mima-plugin"        % "0.6.1")
addSbtPlugin("org.scalastyle"    %% "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"             % "5.4.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"           % "2.3.1")
addSbtPlugin("com.dwijnand"      % "sbt-dynver"             % "4.0.0")
