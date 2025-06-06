resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)


addSbtPlugin("uk.gov.hmrc"       %  "sbt-auto-build"        % "3.24.0")
addSbtPlugin("com.github.sbt"    %  "sbt-release"           % "1.0.15")
addSbtPlugin("org.playframework" %  "sbt-plugin"            % "3.0.6")
addSbtPlugin("uk.gov.hmrc"       %  "sbt-distributables"    % "2.6.0")
addSbtPlugin("net.virtual-void"  %  "sbt-dependency-graph"  % "0.10.0-RC1")
addSbtPlugin("org.scoverage"     %  "sbt-scoverage"         % "2.3.0")
addSbtPlugin("org.scalastyle"    %% "scalastyle-sbt-plugin" % "1.0.0" exclude("org.scala-lang.modules", "scala-xml_2.12"))
addSbtPlugin("com.timushev.sbt"  %  "sbt-updates"           % "0.6.3")


// To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
// Try to remove when sbt 1.8.0+ and scoverage is 2.0.7+
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
