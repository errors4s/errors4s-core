import ReleaseTransformations._
import _root_.io.isomarcte.sbt.version.scheme.enforcer.core._
import _root_.org.errors4s.sbt.GAVs._
import _root_.org.errors4s.sbt._

// Constants //

lazy val org           = "org.errors4s"
lazy val jreVersion    = "16"
lazy val projectName   = "errors4s"
lazy val projectUrl    = url("https://github.com/errors4s/errors4s")
lazy val scala212      = "2.12.13"
lazy val scala213      = "2.13.6"
lazy val scala30       = "3.0.0"
lazy val scalaVersions = Set(scala212, scala213, scala30)

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

// Common Settings //

ThisBuild / crossScalaVersions := scalaVersions.toSeq

ThisBuild / organization := org
ThisBuild / scalaVersion := scala213
ThisBuild / scalafixDependencies ++= List(G.organizeImportsG %% A.organizeImportsA % V.organizeImportsV)
ThisBuild / scalafixScalaBinaryVersion := "2.13"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

// Baseline version for repo split

ThisBuild / versionSchemeEnforcerIntialVersion := Some("1.0.0.0")

// GithubWorkflow
ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowOSes := Set("macos-latest", "ubuntu-latest").toList
ThisBuild / githubWorkflowJavaVersions := Set("adopt@1.15", "adopt@1.11", "adopt@1.8").toList
ThisBuild / githubWorkflowBuildPreamble :=
  List(
    WorkflowStep.Sbt(List("scalafmtSbtCheck", "scalafmtCheckAll")),
    WorkflowStep.Sbt(List("versionSchemeEnforcerCheck")),
    WorkflowStep.Run(List("sbt 'scalafixAll --check'")),
    WorkflowStep.Sbt(List("doc"))
  )
ThisBuild / githubWorkflowBuildPostamble :=
  List(WorkflowStep.Sbt(List("test:doc", "versionSchemeEnforcerCheck", "+core/test")))
ThisBuild / versionScheme := Some("pvp")

lazy val docSettings: List[Def.Setting[_]] = List(
  apiURL := Some(url(s"https://www.javadoc.io/doc/org/errors4s_${scalaBinaryVersion.value}/latest/index.html")),
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
          s"-external-mappings:.*java.*::javadoc::https://docs.oracle.com/en/java/javase/${jreVersion}/docs/api/java.base/",
          "-social-links:github::https://github.com/errors4s/errors4s"
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
    scalaVersion := scala213,
    scalacOptions := {
      scalacOptions.value ++
        (if (isScala3(scalaBinaryVersion.value)) {
           List("-source:3.0-migration") ++
             (if (JREMajorVersion.majorVersion > 8) {
                List("-release:8")
              } else {
                Nil
              })
         } else {
           List("-target:jvm-1.8", "-Wconf:cat=unused-imports:info")
         })
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
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(ScmInfo(projectUrl, "scm:git:git@github.com:errors4s/errors4s.git")),
  developers :=
    List(Developer("isomarcte", "David Strawn", "isomarcte@gmail.com", url("https://github.com/isomarcte"))),
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
)

// Root //

lazy val errors4s = (project in file("."))
  .settings(commonSettings, publishSettings)
  .settings(
    List(
      name := projectName,
      Compile / packageBin / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false
    )
  )
  .aggregate(core)
  .disablePlugins(SbtVersionSchemeEnforcerPlugin)

// Core //

lazy val core = project
  .settings(commonSettings, publishSettings)
  .settings(
    name := s"${projectName}-core",
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

// Docs //

lazy val docs = (project.in(file("errors4s-core-docs")))
  .settings(
    mdocVariables :=
      Map(
        "LATEST_RELEASE" -> versionSchemeEnforcerPreviousVersion.value.getOrElse("latest"),
        "SCALA_VERSION"  -> "2.13"
      ),
    mdocIn := file("docs-src"),
    mdocOut := file("docs")
  )
  .dependsOn(core)
  .enablePlugins(MdocPlugin)
