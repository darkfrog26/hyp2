import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

object HyperscalaBuild extends Build {
  lazy val root = project.in(file("."))
    .aggregate(baseJS, baseJVM, coreJS, coreJVM, exampleJS, exampleJVM)
    .settings(sharedSettings())
    .settings(publishArtifact := false)

  lazy val base = crossProject.crossType(HyperscalaCrossType).in(file("base"))
    .settings(sharedSettings(Some("base")): _*)
    .settings(
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
      libraryDependencies ++= Seq(
        "com.outr" %%% "scribe" % Dependencies.scribe,
        "com.lihaoyi" %%% "upickle" % Dependencies.uPickle,
        "com.outr" %%% "reactify" % Dependencies.reactify,
        "org.scalatest" %%% "scalatest" % Dependencies.scalaTest % "test"
      ),
      autoAPIMappings := true,
      apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/"))
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % Dependencies.scalaJSDOM
      ),
      scalaJSStage in Global := FastOptStage
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "com.outr" %% "scribe-slf4j" % Dependencies.scribe,
        "io.undertow" % "undertow-core" % Dependencies.undertow
      )
    )
  lazy val baseJS = base.js
  lazy val baseJVM = base.jvm

  lazy val core = crossProject.crossType(HyperscalaCrossType).in(file("core"))
    .dependsOn(base)
    .settings(sharedSettings(Some("core")): _*)
    .settings(
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
      libraryDependencies ++= Seq(
        "com.outr" %%% "scribe" % Dependencies.scribe,
        "com.lihaoyi" %%% "upickle" % Dependencies.uPickle,
        "org.scalatest" %%% "scalatest" % Dependencies.scalaTest % "test"
      ),
      autoAPIMappings := true,
      apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/"))
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        "org.scala-js" %%% "scalajs-dom" % Dependencies.scalaJSDOM
      ),
      scalaJSStage in Global := FastOptStage
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        "com.outr" %% "scribe-slf4j" % Dependencies.scribe,
        "io.undertow" % "undertow-core" % Dependencies.undertow
      )
    )
  lazy val coreJS = core.js.dependsOn(baseJS)
  lazy val coreJVM = core.jvm.dependsOn(baseJVM)

  lazy val example = crossProject.crossType(HyperscalaCrossType).in(file("example"))
    .settings(sharedSettings(Some("example")): _*)
    .settings(
      libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
      autoAPIMappings := true,
      apiMappings += (scalaInstance.value.libraryJar -> url(s"http://www.scala-lang.org/api/${scalaVersion.value}/"))
    )
    .jsSettings(
      scalaJSStage in Global := FastOptStage,
      crossTarget in fastOptJS := baseDirectory.value / ".." / "jvm" / "src" / "main" / "web" / "app",
      crossTarget in fullOptJS := baseDirectory.value / ".." / "jvm" / "src" / "main" / "web" / "app"
    )
  lazy val exampleJS = example.js.dependsOn(coreJS)
  lazy val exampleJVM = example.jvm.dependsOn(coreJVM)

  private def sharedSettings(projectName: Option[String] = None) = Seq(
    name := s"${Details.name}${projectName.map(pn => s"-$pn").getOrElse("")}",
    version := Details.version,
    organization := Details.organization,
    scalaVersion := Details.scalaVersion,
    crossScalaVersions := Details.scalaVersions,
    sbtVersion := Details.sbtVersion,
    scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature"),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("snapshots"),
      Resolver.sonatypeRepo("releases"),
      Resolver.typesafeRepo("releases")
    ),
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    publishArtifact in Test := false,
    pomExtra := <url>${Details.url}</url>
      <licenses>
        <license>
          <name>{Details.licenseType}</name>
          <url>{Details.licenseURL}</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <developerConnection>scm:{Details.repoURL}</developerConnection>
        <connection>scm:{Details.repoURL}</connection>
        <url>{Details.projectURL}</url>
      </scm>
      <developers>
        <developer>
          <id>{Details.developerId}</id>
          <name>{Details.developerName}</name>
          <url>{Details.developerURL}</url>
        </developer>
      </developers>
  )
}

object Details {
  val organization = "org.hyperscala"
  val name = "hyperscala"
  val version = "2.1.10"
  val url = "http://hyperscala.org"
  val licenseType = "MIT"
  val licenseURL = "http://opensource.org/licenses/MIT"
  val projectURL = "https://github.com/outr/hyperscala"
  val repoURL = "https://github.com/outr/hyperscala.git"
  val developerId = "darkfrog"
  val developerName = "Matt Hicks"
  val developerURL = "http://matthicks.com"

  val sbtVersion = "0.13.13"
  val scalaVersions = List("2.12.1", "2.11.8")
  val scalaVersion = "2.12.1"
}

object Dependencies {
  val scribe = "1.3.2"
  val undertow = "1.4.8.Final"
  val uPickle = "0.4.4"
  val scalaTest = "3.0.1"
  val scalaJSDOM = "0.9.1"
  val reactify = "1.3.6"
}

object HyperscalaCrossType extends org.scalajs.sbtplugin.cross.CrossType {
  override def projectDir(crossBase: File, projectType: String): File = crossBase / projectType

  override def sharedSrcDir(projectBase: File, conf: String): Option[File] = Some(projectBase.getParentFile / "src" / conf / "scala")
}