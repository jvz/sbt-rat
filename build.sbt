lazy val metadataSettings = Seq(
  name := "SBT Release Audit Tool",
  organization := "org.musigma",
  moduleName := "sbt-rat",
  version := "0.1-SNAPSHOT",
  licenses := Seq("Apache License, Version 2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")),
  developers := List(
    Developer("jvz", "Matt Sicker", "mattsicker@apache.org", url("https://musigma.blog/"))
  ),
  scmInfo := Some(ScmInfo(url("https://github.com/jvz/sbt-rat"), "scm:git:git@github.com:jvz/sbt-rat.git")),
  homepage := Some(url("https://github.com/jvz/sbt-rat"))
)

lazy val buildSettings = Seq(
  sbtPlugin := true,
  libraryDependencies += "org.apache.rat" % "apache-rat-core" % "0.12",
  scriptedLaunchOpts ++= Seq("-Xmx1024M", "-Dplugin.version=" + version.value),
  initialCommands in console := "import org.musigma.sbt.rat._"
)

//lazy val releaseSettings = Seq(
//  pgpSecretRing := file(".travis-secring.gpg"),
//  usePgpKeyHex("E102 55CA ADF8 274A 7146"),
//  releaseEarlyWith := SonatypePublisher
//)

lazy val root = (project in file("."))
  .settings(metadataSettings: _*)
  .settings(buildSettings: _*)
//  .settings(releaseSettings: _*)
