name := "SBT Release Audit Tool"

organization := "org.musigma"
moduleName := "sbt-rat"
version := "0.1-SNAPSHOT"

licenses := Seq("Apache License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))
developers := List(
  Developer("jvz", "Matt Sicker", "mattsicker@apache.org", url("https://musigma.blog/"))
)
scmInfo := Some(ScmInfo(url("https://github.com/jvz/sbt-rat"), "scm:git:git@github.com:jvz/sbt-rat.git"))

sbtPlugin := true

libraryDependencies += "org.apache.rat" % "apache-rat-core" % "0.12"

bintrayPackageLabels := Seq("sbt", "plugin")
bintrayVcsUrl := Some("git@github.com:jvz/sbt-rat.git")

initialCommands in console := "import org.musigma.sbt.rat._"

// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
