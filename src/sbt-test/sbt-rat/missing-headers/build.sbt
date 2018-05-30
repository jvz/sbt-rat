version := "0.1"
scalaVersion := "2.12.4"

ratExcludes := Seq(
  file(".gitignore"),
  file("project/build.properties")
)

ratReportStyle := "adoc"
