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

import java.io._

import org.apache.rat.analysis.IHeaderMatcher
import org.apache.rat.analysis.util.HeaderMatcherMultiplexer
import org.apache.rat.document.impl.FileDocument
import org.apache.rat.license.ILicenseFamily
import org.apache.rat.report.{IReportable, RatReport}
import org.apache.rat.report.claim.ClaimStatistic
import org.apache.rat.{Report, ReportConfiguration, Defaults => RatDefaults}
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

import scala.collection.JavaConverters._

object SbtRatPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires = JvmPlugin

  object autoImport {
    // FIXME: this should probably be a task, not a configuration (e.g., packageBin)
    val Audit = config("audit")

    val auditCheck = taskKey[Unit]("Performs a release audit check")
    val auditReport = taskKey[AuditReport]("Generates a release audit report")

    val addDefaultLicenseMatchers = settingKey[Boolean]("Adds the default RAT license matchers to allowedLicenses")

    type LicenseFamily = ILicenseFamily
    val allowedLicenseFamilies = settingKey[Seq[LicenseFamily]]("Which licenses families to allow")

    type HeaderMatcher = IHeaderMatcher
    val allowedLicenseHeaders = settingKey[Seq[HeaderMatcher]]("Which licenses to allow in file headers")

    type AuditReport = ClaimStatistic
  }

  import autoImport._

  override def projectConfigurations: Seq[Configuration] = Seq(Audit)

  // TODO: mappings in Audit?
  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    addDefaultLicenseMatchers := true,
    allowedLicenseFamilies := Nil,
    allowedLicenseHeaders := Nil,
    maxErrors in Audit := 0,
    ratReport(Compile),
    ratReport(Test),
    ratCheck(Compile),
    ratCheck(Test)
  )

  def ratReport(cfg: ConfigKey): Setting[Task[AuditReport]] = {
    def go(target: File, inputs: Seq[File], addDefaults: Boolean, families: Seq[LicenseFamily],
           headers: Seq[HeaderMatcher]): AuditReport = {
      if (!target.exists()) target.mkdirs()
      val base = new IReportable {
        override def run(report: RatReport): Unit = {
          report.startReport()
          inputs.map(new FileDocument(_)).foreach(report.report)
          report.endReport()
        }
      }
      val mergedHeaders = if (addDefaults) RatDefaults.DEFAULT_MATCHERS.asScala ++ headers else headers
      val config = new ReportConfiguration
      config.setApproveDefaultLicenses(addDefaults)
      config.setApprovedLicenseNames(families.asJava)
      config.setHeaderMatcher(new HeaderMatcherMultiplexer(mergedHeaders.asJava))
      val stylesheets = Seq(
        "adoc" -> SbtRatPlugin.getClass.getResourceAsStream("/META-INF/asciidoc-rat.xsl"),
        "txt" -> RatDefaults.getPlainStyleSheet
      )
      val results = for ((ext, stylesheet) <- stylesheets) yield {
        Report.report(new FileWriter(target / s"rat.$ext"), base, stylesheet, config)
      }
      results.head
    }

    auditReport in cfg := go(
      (target in cfg).value,
      (unmanagedSources in cfg).value,
      (addDefaultLicenseMatchers in cfg).value,
      (allowedLicenseFamilies in cfg).value,
      (allowedLicenseHeaders in cfg).value
    )
  }

  class UnapprovedLicenseLimitExceededException(max: Int, found: Int)
    extends RuntimeException(s"Max number of unapproved licenses: $max; found: $found. See full report in rat.txt")

  def ratCheck(cfg: ConfigKey): Setting[Task[Unit]] = {
    def go(report: AuditReport, maxErrors: Int): Unit = {
      if (report.getNumUnApproved > maxErrors) {
        throw new UnapprovedLicenseLimitExceededException(maxErrors, report.getNumUnApproved)
      }
    }

    auditCheck in cfg := go(
      (auditReport in cfg).value,
      (maxErrors in Audit).value
    )
  }

  override lazy val buildSettings: Seq[Setting[_]] = Nil

  override lazy val globalSettings: Seq[Setting[_]] = Nil

}

