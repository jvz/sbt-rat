libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.10")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.5")
