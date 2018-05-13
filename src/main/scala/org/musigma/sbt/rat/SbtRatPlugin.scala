/*
   Copyright 2018 Matt Sicker

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.musigma.sbt.rat

import org.apache.rat.analysis.IHeaderMatcher
import org.apache.rat.analysis.license.FullTextMatchingLicense
import org.apache.rat.analysis.util.HeaderMatcherMultiplexer
import org.apache.rat.document.impl.FileDocument
import org.apache.rat.license.ILicenseFamily
import org.apache.rat.license.SimpleLicenseFamily
import org.apache.rat.mp.util.ScmIgnoreParser
import org.apache.rat.report.claim.ClaimStatistic
import org.apache.rat.report.{ IReportable, RatReport }
import org.apache.rat.{ Report, ReportConfiguration, Defaults => RatDefaults }

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import scala.collection.JavaConverters._

object SbtRatPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires = JvmPlugin

  object autoImport {
    val ratCheck = taskKey[Unit]("Performs a release audit check")

    type AuditReport = ClaimStatistic
    val ratReport = taskKey[AuditReport]("Generates a release audit report")

    val ratAddDefaultLicenseMatchers = settingKey[Boolean]("Whether to add the default list of license matchers.")

    val ratLicenseFamilies = settingKey[Seq[String]]("Specifies the license families to accept.")

    val ratLicenses = settingKey[Seq[(String, String, String)]]("Extra license to match--each Seq item is a tuple of (category, name, license text)")

    val ratExcludes = settingKey[Seq[File]]("Files to exlucde from audit checks, relative to the baseDirectory")

    val ratParseSCMIgnoresAsExcludes = settingKey[Boolean]("Whether to parse source code management system (SCM) ignore files and use their contents as excludes.")

    val ratReportStyle = settingKey[String]("Which style of rat report to generate, either 'txt', or 'adoc'")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    makeRatCheckSetting(),
    makeRatReportSetting(),
    ratAddDefaultLicenseMatchers := true,
    ratLicenseFamilies := Nil,
    ratLicenses := Nil,
    ratExcludes := Nil,
    ratParseSCMIgnoresAsExcludes := true,
    ratReportStyle := "txt"
  )

  def makeRatReportSetting(): Setting[Task[AuditReport]] = {

    def getExclusionsFilter(baseDir: File, customExclusions: Seq[File], parseSCMIgnores: Boolean) = {
      val scmExclusionsFilter =
        if (parseSCMIgnores) {
          val scmExclusions = ScmIgnoreParser.getExclusionsFromSCM(baseDir).asScala
          scmExclusions.foldLeft(NothingFilter: sbt.FileFilter) {
            case (filter: FileFilter, exclusion) => filter || new ExactFilter(exclusion)
          }
        } else {
          NothingFilter
        }

      val customExclusionsFilter = customExclusions.foldLeft(NothingFilter: sbt.FileFilter) {
        case (filter: FileFilter, exclusion) => filter || new SimpleFileFilter(file => {
          val rel = baseDir.relativize(file)
          rel.map { _.compareTo(exclusion) == 0 }.getOrElse(false)
        })
      }

      scmExclusionsFilter || customExclusionsFilter
    }

    def makeHeaderMatcher(licenses: Seq[(String, String, String)], addDefaultMatchers: Boolean): IHeaderMatcher = {
      val customHeaders = licenses.map { case (category, family, text) =>
        val license = new FullTextMatchingLicense()
        license.setLicenseFamilyCategory(category)
        license.setLicenseFamilyName(family)
        license.setFullText(text)
        license
      }

      val defaultHeaders = if (addDefaultMatchers) RatDefaults.DEFAULT_MATCHERS.asScala else Nil

      val headerMatcher = new HeaderMatcherMultiplexer((customHeaders ++ defaultHeaders).asJava)
      headerMatcher
    }

    def makeLicenseFamilies(families: Seq[String]): Seq[ILicenseFamily] = {
      families.map { new SimpleLicenseFamily(_) }
    }

    def go(target: File, baseDir: File,
      addDefaultMatchers: Boolean, families: Seq[String], licenses: Seq[(String, String, String)],
      excludes: Seq[File],  parseSCMIgnores: Boolean,
      ratReportStyle: String): AuditReport = {

      if (!target.exists()) target.mkdirs()

      val exclusionsFilter = getExclusionsFilter(baseDir, excludes, parseSCMIgnores)
      val inputs: Seq[File] =
        PathFinder(baseDir).descendantsExcept(AllPassFilter, exclusionsFilter)
          .get
          .filter { _.isFile }
          .map { baseDir.relativize(_).get }

      val base = new IReportable {
        override def run(report: RatReport): Unit = {
          report.startReport()
          inputs.map(new FileDocument(_)).foreach(report.report)
          report.endReport()
        }
      }

      val headerMatcher = makeHeaderMatcher(licenses, addDefaultMatchers)
      val licenseFamilies = makeLicenseFamilies(families).asJava

      val config = new ReportConfiguration
      config.setApproveDefaultLicenses(addDefaultMatchers)
      config.setApprovedLicenseNames(licenseFamilies)
      config.setHeaderMatcher(headerMatcher)

      val stylesheet = ratReportStyle match {
        case "adoc" => SbtRatPlugin.getClass.getResourceAsStream("/META-INF/asciidoc-rat.xsl")
        case "txt" => RatDefaults.getPlainStyleSheet
        case _ => RatDefaults.getPlainStyleSheet
      }

      val writer = new java.io.FileWriter(target / s"rat.$ratReportStyle")
      val results = Report.report(writer, base, stylesheet, config)
      results
    }

    ratReport := go(
      target.value,
      baseDirectory.value,
      ratAddDefaultLicenseMatchers.value,
      ratLicenseFamilies.value,
      ratLicenses.value,
      ratExcludes.value,
      ratParseSCMIgnoresAsExcludes.value,
      ratReportStyle.value
    )
  }

  class UnapprovedLicenseException(found: Int)
    extends RuntimeException(s"Unapproved licenses found: $found. See full report in rat.txt")

  def makeRatCheckSetting(): Setting[Task[Unit]] = {
    def go(report: AuditReport): Unit = {
      if (report.getNumUnApproved > 0) {
        throw new UnapprovedLicenseException(report.getNumUnApproved)
      }
    }

    ratCheck := go(
      (ratReport).value
    )
  }

  override lazy val buildSettings: Seq[Setting[_]] = Nil

  override lazy val globalSettings: Seq[Setting[_]] = Nil

}
