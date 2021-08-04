import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._
import _root_.org.errors4s.sbt.GAVs._
import _root_.org.errors4s.sbt._

// Constants //

lazy val org           = "org.errors4s"
lazy val jreVersion    = "16"
lazy val projectName   = "errors4s-core"
lazy val projectUrl    = url(s"https://github.com/errors4s/${projectName}")
lazy val scala212      = "2.12.14"
lazy val scala213      = "2.13.6"
lazy val scala30       = "3.0.1"
lazy val scalaVersions = Set(scala212, scala213, scala30)

// SBT Command Aliases //

// Usually run before making a PR
addCommandAlias(
  "full_build",
  s";+clean;githubWorkflowGenerate;+test;+test:doc;+versionSchemeEnforcerCheck;++${scala213};scalafmtAll;scalafmtSbt;scalafixAll;++${scala30};docs/mdoc"
)

// Functions //

def isScala3(version: String): Boolean = version.startsWith("3")

def initialImports(packages: List[String], isScala3: Boolean): String = {
  val wildcard: Char =
    if (isScala3) {
      '*'
    } else {
      '_'
    }

  packages.map(value => s"import ${value}.${wildcard}").mkString("\n")
}

// Dependency Overrides //

ThisBuild / dependencyOverrides += G.scalametaG % A.semanticdbA % V.semanticdbV cross CrossVersion.full

// Common Settings //

ThisBuild / crossScalaVersions := scalaVersions.toSeq

ThisBuild / organization := org
ThisBuild / scalaVersion := scala30
ThisBuild / scalafixDependencies ++= List(G.organizeImportsG %% A.organizeImportsA % V.organizeImportsV)
ThisBuild / scalafixScalaBinaryVersion := "2.13"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

// Baseline version for repo split

ThisBuild / versionSchemeEnforcerInitialVersion := Some("1.0.0.0")
ThisBuild / versionScheme := Some("pvp")

// GithubWorkflow
ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowOSes := Set("macos-latest", "ubuntu-latest").toList
ThisBuild / githubWorkflowJavaVersions := Set("adopt@1.16", "adopt@1.11", "adopt@1.8").toList
ThisBuild / githubWorkflowBuild := List(WorkflowStep.Sbt(List("versionSchemeEnforcerCheck", "Test / doc")))

// Doc Settings

def scaladocLink(scalaBinaryVersion: String, version: String): String =
  s"https://www.javadoc.io/doc/${org}/${projectName}_${scalaBinaryVersion}/${version}/index.html"

def javadocIoLink(groupId: String, artifactId: String, depVersion: String, scalaBinaryVersion: Option[String]): String =
  scalaBinaryVersion
    .fold(s"https://www.javadoc.io/doc/${groupId}/${artifactId}/${depVersion}/api/")(scalaBinaryVersion =>
      s"https://www.javadoc.io/doc/${groupId}/${artifactId}_${scalaBinaryVersion}/${depVersion}/api/"
    )

lazy val docSettings: List[Def.Setting[_]] = List(
  apiURL := Some(url(scaladocLink(scalaBinaryVersion.value, version.value))),
  autoAPIMappings := true,
  Compile / doc / apiMappings := {
    if (isScala3(scalaBinaryVersion.value)) {
      Map.empty[File, URL]
    } else {
      val moduleLink: String => (java.io.File, java.net.URL) = module => ScalaApiDoc.jreModuleLink(jreVersion)(module)
      Map(moduleLink("java.base"))
    }
  },
  Compile / doc / scalacOptions := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        List(
          "-external-mappings:" +
            List(
              s".*java.*::javadoc::https://docs.oracle.com/en/java/javase/${jreVersion}/docs/api/java.base/",
              """.*scala/.*::scaladoc3::http://dotty.epfl.ch/api/""",
              s""".*org/scalacheck/.*::scaladoc3::${javadocIoLink(
                G.scalacheckG,
                A.scalacheckA,
                V.scalacheckV,
                Some("3")
              )}"""
            ).mkString(","),
          s"-social-links:github::https://github.com/errors4s/${projectName}",
          "-verbose"
        )
      case Some((2, n)) =>
        List("-language:experimental.macros") ++
          (if (n <= 12) {
             List("-no-link-warnings")
           } else {
             List("-jdk-api-doc-base", s"https://docs.oracle.com/en/java/javase/${jreVersion}/docs/api")
           })
      case otherwise =>
        throw new AssertionError(s"Unhandled Scala version in Compile / doc/ scalacOptions: ${otherwise}")
    }
  }
)

lazy val commonSettings: List[Def.Setting[_]] =
  List(
    scalaVersion := scala30,
    scalacOptions := {
      val currentOptions: Seq[String] = scalacOptions.value
      (
        if (isScala3(scalaBinaryVersion.value)) {
          // Remove -source since as of 0.1.19 of sbt-tpolecat it sets -source
          // to be `-source:future`, but we only want that on sources which are
          // _strictly_ Scala 3, we want `-source:3.0-migration` from cross
          // compiled sources.
          currentOptions.filterNot(_.startsWith("-source")) ++ List("-source:3.0-migration") ++
            (if (JREMajorVersion.majorVersion > 8) {
               List("-release:8")
             } else {
               Nil
             })
        } else {
          currentOptions ++ List("-target:jvm-1.8", "-Wconf:cat=unused-imports:info")
        }
      )
    },
    libraryDependencies ++= {
      if (isScala3(scalaBinaryVersion.value)) {
        Nil
      } else {
        List(
          compilerPlugin(G.betterMonadicForG %% A.betterMonadicForA % V.betterMonadicForV),
          compilerPlugin(G.typelevelG         % A.kindProjectorA    % V.kindProjectorV cross CrossVersion.full)
        )
      }
    },
    crossScalaVersions := scalaVersions.toSeq
  ) ++ docSettings

// Publish Settings //

lazy val publishSettings = List(
  homepage := Some(projectUrl),
  licenses := Seq("BSD3" -> url("https://opensource.org/licenses/BSD-3-Clause")),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://s01.oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(ScmInfo(projectUrl, s"scm:git:git@github.com:errors4s/${projectName}.git")),
  developers :=
    List(Developer("isomarcte", "David Strawn", "isomarcte@gmail.com", url("https://github.com/isomarcte"))),
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
)

// Root //

lazy val root = (project in file("."))
  .settings(commonSettings, publishSettings)
  .settings(
    List(
      name := s"${projectName}-root",
      Compile / packageBin / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false
    )
  )
  .aggregate(core, scalacheck)
  .disablePlugins(SbtVersionSchemeEnforcerPlugin)

// Core //

lazy val core = project
  .settings(commonSettings, publishSettings)
  .settings(
    name := s"${projectName}",
    console / initialCommands :=
      initialImports(List("org.errors4s.core", "org.errors4s.core.syntax.all"), isScala3(scalaBinaryVersion.value)),
    libraryDependencies ++= {
      if (isScala3(scalaBinaryVersion.value)) {
        Nil
      } else {
        List(G.scalaLangG % A.scalaReflectA % scalaVersion.value % Provided)
      }
    },
    libraryDependencies ++= List(G.scalametaG %% A.munitA % V.munitV % Test)
  )

lazy val scalacheck = project
  .settings(commonSettings, publishSettings)
  .settings(
    name := s"${projectName}",
    console / initialCommands :=
      List(
        "org.errors4s.core._",
        "org.errors4s.core.syntax.all._",
        "org.errors4s.core.scalacheck.instances._",
        "org.scalacheck._"
      ).map(value => s"import $value").mkString("\n"),
    libraryDependencies ++=
      List(G.scalacheckG %% A.scalacheckA % V.scalacheckV)
  ).dependsOn(core)

// Docs //

lazy val docs = (project.in(file("errors4s-core-docs")))
  .settings(commonSettings)
  .settings(
    name := s"${projectName}-docs",
    mdocVariables := {
      val latestRelease: String =
        if (isSnapshot.value) {
          versionSchemeEnforcerPreviousVersion.value.getOrElse("latest")
        } else {
          version.value
        }
      val scalaBinVer: String = scalaBinaryVersion.value

      Map(
        "LATEST_RELEASE"       -> latestRelease,
        "SCALA_BINARY_VERSION" -> scalaBinVer,
        "SCALADOC_LINK"        -> scaladocLink(scalaBinVer, latestRelease),
        "ORG"                  -> org,
        "PROJECT_NAME"         -> projectName
      )
    },
    mdocIn := file("docs-src"),
    mdocOut := file("docs")
  )
  .dependsOn(core, scalacheck)
  .enablePlugins(MdocPlugin)
