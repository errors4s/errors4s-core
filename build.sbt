import ReleaseTransformations._
import sbt.librarymanagement.VersionNumber
import _root_.io.isomarcte.errors4s.sbt.ScalaApiDoc

// Constants //

lazy val isomarcteOrg  = "io.isomarcte"
lazy val jreVersion    = "15"
lazy val projectName   = "errors4s"
lazy val projectUrl    = url("https://github.com/isomarcte/errors4s")
lazy val scala212      = "2.12.12"
lazy val scala213      = "2.13.4"
lazy val scalaVersions = Set(scala212, scala213)

// Groups //

lazy val chrisDavenportG  = "io.chrisdavenport"
lazy val circeG           = "io.circe"
lazy val comcastG         = "com.comcast"
lazy val fs2G             = "co.fs2"
lazy val http4sG          = "org.http4s"
lazy val organizeImportsG = "com.github.liancheng"
lazy val refinedG         = "eu.timepit"
lazy val scalacheckG      = "org.scalacheck"
lazy val scalatestG       = "org.scalatest"
lazy val scodecG          = "org.scodec"
lazy val shapelessG       = "com.chuusai"
lazy val slf4jG           = "org.slf4j"
lazy val typelevelG       = "org.typelevel"

// Artifacts //

lazy val catsCoreA        = "cats-core"
lazy val catsEffectA      = "cats-effect"
lazy val catsKernelA      = "cats-kernel"
lazy val circeCoreA       = "circe-core"
lazy val circeGenericA    = "circe-generic"
lazy val circeRefinedA    = "circe-refined"
lazy val fs2CoreA         = "fs2-core"
lazy val http4sCirceA     = "http4s-circe"
lazy val http4sClientA    = "http4s-client"
lazy val http4sCoreA      = "http4s-core"
lazy val http4sLawsA      = "http4s-laws"
lazy val http4sServerA    = "http4s-server"
lazy val ip4sCoreA        = "ip4s-core"
lazy val jawnParserA      = "jawn-parser"
lazy val organizeImportsA = "organize-imports"
lazy val refinedA         = "refined"
lazy val refinedCatsA     = "refined-cats"
lazy val scalacheckA      = "scalacheck"
lazy val scalatestA       = "scalatest"
lazy val scodecBitsA      = "scodec-bits"
lazy val shapelessA       = "shapeless"
lazy val slf4jApiA        = "slf4j-api"
lazy val vaultA           = "vault"

// Versions //

lazy val catsEffectV      = "2.3.1"
lazy val catsV            = "2.3.1"
lazy val circeV           = "0.13.0"
lazy val fs2V             = "2.5.0"
lazy val http4sV          = "0.21.15"
lazy val ip4sV            = "1.4.0"
lazy val jawnParserV      = "1.0.1"
lazy val organizeImportsV = "0.4.4"
lazy val refinedV         = "0.9.20"
lazy val scalacheckV      = "1.15.2"
lazy val scalatestV       = "3.2.3"
lazy val scodecBitsV      = "1.1.23"
lazy val shapelessV       = "2.3.3"
lazy val slf4jApiV        = "1.7.30"
lazy val vaultV           = "2.0.0"

// Common Settings

ThisBuild / crossScalaVersions := scalaVersions.toSeq

ThisBuild / organization := isomarcteOrg
ThisBuild / scalaVersion := scala213
ThisBuild / scalacOptions ++= List("-target:jvm-1.8")
ThisBuild / scalafixDependencies ++= List(organizeImportsG %% organizeImportsA % organizeImportsV)
ThisBuild / scalafixScalaBinaryVersion := "2.13"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

// GithubWorkflow
ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowOSes := Set("macos-latest", "windows-latest", "ubuntu-latest").toList
ThisBuild / githubWorkflowJavaVersions := Set("adopt@1.15", "adopt@1.11", "adopt@1.8").toList
ThisBuild / githubWorkflowBuildPreamble :=
  List(
    WorkflowStep.Sbt(List("scalafmtSbtCheck", "scalafmtCheckAll")),
    WorkflowStep.Run(List("sbt 'scalafixAll --check'")),
    WorkflowStep.Sbt(List("doc", "unidoc"))
  )
ThisBuild / githubWorkflowBuildPostamble := List(WorkflowStep.Sbt(List("test:doc", "versionPolicyCheck")))
ThisBuild / githubWorkflowBuildMatrixExclusions :=
  List(
    // For some reason the `githubWorkflowCheck` step gets stuck with this
    // particular combination.
    MatrixExclude(Map("os" -> "windows-latest", "scala" -> "2.13.4", "java" -> "adopt@1.15"))
  )

lazy val docSettings: List[Def.Setting[_]] = List(
  apiURL :=
    Some(url(s"https://www.javadoc.io/doc/io.isomarcte/errors4s_${scalaBinaryVersion.value}/latest/index.html")),
  autoAPIMappings := true,
  Compile / doc / apiMappings ++= {
    val moduleLink: String => (java.io.File, java.net.URL) = module => ScalaApiDoc.jreModuleLink(jreVersion)(module)
    Map(moduleLink("java.base"))
  },
  Compile / doc / scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n <= 12 =>
        List("-no-link-warnings")
      case _ =>
        List("-jdk-api-doc-base", s"https://docs.oracle.com/en/java/javase/${jreVersion}/docs/api")
    }
  }
)

lazy val commonSettings: List[Def.Setting[_]] =
  List(
    scalaVersion := scala213,
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    addCompilerPlugin(typelevelG    % "kind-projector"     % "0.11.2" cross CrossVersion.full),
    crossScalaVersions := scalaVersions.toSeq,
    // sbt-version-policy
    versionPolicyIntention := Compatibility.BinaryAndSourceCompatible,
    versionPolicyDefaultReconciliation := Some(VersionCompatibility.Strict),
    versionPolicyDependencyRules ++=
      List("core", "http", "http4s", "http-circe", "http4s-circe")
        .map(artifact => isomarcteOrg % s"errors4s-${artifact}_${scalaBinaryVersion.value}" % "pvp")
  ) ++ docSettings

// Mima //

lazy val mimaCommonSettings: Seq[Def.Setting[_]] = List(
  mimaFailOnProblem := true,
  mimaReportSignatureProblems := true,
  mimaCheckDirection := "both"
)

lazy val mimaSettings: Seq[Def.Setting[_]] = mimaCommonSettings

// Publish Settings //

lazy val publishSettings = List(
  homepage := Some(projectUrl),
  licenses := Seq("BSD3" -> url("https://opensource.org/licenses/BSD-3-Clause")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
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
  scmInfo := Some(ScmInfo(projectUrl, "scm:git:git@github.com:isomarcte/errors4s.git")),
  developers :=
    List(Developer("isomarcte", "David Strawn", "isomarcte@gmail.com", url("https://github.com/isomarcte"))),
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value
)

// Release Process //

releaseProcess :=
  Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    releaseStepCommandAndRemaining("+versionCheck"),
    releaseStepCommandAndRemaining("+publishSigned"),
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )

// Root //

lazy val errors4s = (project in file("."))
  .settings(commonSettings, publishSettings)
  .settings(
    List(
      name := projectName,
      mimaFailOnNoPrevious := false,
      Compile / packageBin / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false,
      Compile / packageDoc / mappings :=
        (ScalaUnidoc / packageDoc / mappings).value
    )
  )
  .aggregate(core, http, http4s, `http-circe`, `http4s-circe`)
  .enablePlugins(ScalaUnidocPlugin)

// Core //

lazy val core = project
  .settings(commonSettings, publishSettings, mimaSettings)
  .settings(name := s"${projectName}-core", libraryDependencies ++= List(refinedG %% refinedA % refinedV))
  .enablePlugins(MimaPlugin)

// http //

lazy val http = project
  .settings(commonSettings, publishSettings, mimaSettings)
  .settings(
    name := s"${projectName}-http",
    libraryDependencies ++= List(refinedG %% refinedA % refinedV, shapelessG %% shapelessA % shapelessV)
  )
  .dependsOn(core)
  .enablePlugins(MimaPlugin)

// http4s //

lazy val http4s = project
  .settings(commonSettings, publishSettings, mimaSettings)
  .settings(
    name := s"${projectName}-http4s",
    libraryDependencies ++=
      List(
        fs2G        %% fs2CoreA      % fs2V,
        http4sG     %% http4sCoreA   % http4sV,
        refinedG    %% refinedA      % refinedV,
        scodecG     %% scodecBitsA   % scodecBitsV,
        typelevelG  %% catsCoreA     % catsV,
        typelevelG  %% catsKernelA   % catsV,
        http4sG     %% http4sClientA % http4sV     % Test,
        http4sG     %% http4sLawsA   % http4sV     % Test,
        scalacheckG %% scalacheckA   % scalacheckV % Test
      )
  )
  .dependsOn(core)
  .enablePlugins(MimaPlugin)

// circe //

lazy val `http-circe` = project
  .settings(commonSettings, publishSettings, mimaSettings)
  .settings(
    name := s"${projectName}-http-circe",
    libraryDependencies ++=
      List(
        circeG     %% circeCoreA    % circeV,
        circeG     %% circeGenericA % circeV,
        circeG     %% circeRefinedA % circeV,
        refinedG   %% refinedA      % refinedV,
        shapelessG %% shapelessA    % shapelessV,
        typelevelG %% catsCoreA     % catsV
      )
  )
  .dependsOn(http)
  .enablePlugins(MimaPlugin)

// http4s //

lazy val `http4s-circe` = project
  .settings(commonSettings, publishSettings, mimaSettings)
  .settings(
    name := s"${projectName}-http4s-circe",
    libraryDependencies ++=
      List(
        chrisDavenportG %% vaultA        % vaultV,
        circeG          %% circeCoreA    % circeV,
        fs2G            %% fs2CoreA      % fs2V,
        http4sG         %% http4sCirceA  % http4sV,
        http4sG         %% http4sClientA % http4sV,
        http4sG         %% http4sCoreA   % http4sV,
        http4sG         %% http4sServerA % http4sV,
        refinedG        %% refinedA      % refinedV,
        typelevelG      %% catsCoreA     % catsV,
        typelevelG      %% catsKernelA   % catsV,
        typelevelG      %% catsEffectA   % catsEffectV,
        scalatestG      %% scalatestA    % scalatestV % Test
      )
  )
  .dependsOn(`http-circe`)
  .enablePlugins(MimaPlugin)

// MDoc //

lazy val docs = project
  .in(file(s"${projectName}-docs"))
  .settings(commonSettings)
  .settings(List(skip in publish := true, name := s"${projectName}-docs"))
  .dependsOn(core, http, `http-circe`, `http4s-circe`)
  .enablePlugins(MdocPlugin)
