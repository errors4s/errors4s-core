import ReleaseTransformations._

// Constants //

lazy val scala212      = "2.12.12"
lazy val scala213      = "2.13.3"
lazy val scalaVersions = Set(scala212, scala213)
lazy val projectName   = "errors4s"
lazy val projectUrl    = url("https://github.com/isomarcte/errors4s")

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
lazy val organizeImportsA = "organize-imports"
lazy val refinedA         = "refined"
lazy val refinedCatsA     = "refined-cats"
lazy val scalacheckA      = "scalacheck"
lazy val scalatestA       = "scalatest"
lazy val scodecBitsA      = "scodec-bits"
lazy val shapelessA       = "shapeless"
lazy val vaultA           = "vault"

// Versions //

lazy val catsEffectV      = "2.2.0"
lazy val catsV            = "2.2.0"
lazy val circeV           = "0.13.0"
lazy val fs2V             = "2.4.4"
lazy val http4sV          = "0.21.8"
lazy val ip4sV            = "1.4.0"
lazy val organizeImportsV = "0.4.0"
lazy val refinedV         = "0.9.17"
lazy val scalacheckV      = "1.15.1"
lazy val scalatestV       = "3.2.2"
lazy val scodecBitsV      = "1.1.21"
lazy val shapelessV       = "2.3.3"
lazy val vaultV           = "2.0.0"

// Common Settings

inThisBuild(
  List(
    organization := "io.isomarcte",
    scalaVersion := scala213,
    scalafixScalaBinaryVersion := "2.13",
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalafixDependencies ++= List(organizeImportsG %% organizeImportsA % organizeImportsV),
    doc / scalacOptions --= List("-Werror", "-Xfatal-warnings"),
    scalacOptions ++= List("-target:jvm-1.8")
  )
)

lazy val commonSettings = List(
  scalaVersion := scala213,
  crossScalaVersions := scalaVersions.toSeq,
  addCompilerPlugin(typelevelG    % "kind-projector"     % "0.11.0" cross CrossVersion.full),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  autoAPIMappings := true,
  apiURL := Some(url("https://isomarcte.github.io/errors4s/api"))
)

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
  .settings(List(name := projectName))
  .aggregate(core, http, http4s, `http-circe`, `http4s-circe`)
  .enablePlugins(ScalaUnidocPlugin)

// Core //

lazy val core = project
  .settings(commonSettings, publishSettings)
  .settings(name := s"${projectName}-core", libraryDependencies ++= List(refinedG %% refinedA % refinedV))

// http //

lazy val http = project
  .settings(commonSettings, publishSettings)
  .settings(
    name := s"${projectName}-http",
    libraryDependencies ++= List(refinedG %% refinedA % refinedV, shapelessG %% shapelessA % shapelessV)
  )
  .dependsOn(core)

// http4s //

lazy val http4s = project
  .settings(commonSettings, publishSettings)
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

// circe //

lazy val `http-circe` = project
  .settings(commonSettings, publishSettings)
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

// http4s //

lazy val `http4s-circe` = project
  .settings(commonSettings, publishSettings)
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

// MDoc //

lazy val docs = project
  .in(file(s"${projectName}-docs"))
  .settings(commonSettings)
  .settings(List(skip in publish := true, name := s"${projectName}-docs"))
  .dependsOn(core, http, `http-circe`, `http4s-circe`)
  .enablePlugins(MdocPlugin)
