val catsV                = "2.0.0"
val jmsV                 = "2.0.1"
val ibmMQV               = "9.1.4.0"
val catsEffectV          = "2.0.0"
val catsEffectScalaTestV = "0.4.0"
val fs2V                 = "2.0.0"
val log4catsV            = "1.0.1"
val log4jSlf4jImplV      = "2.13.1"

val kindProjectorV    = "0.11.0"
val betterMonadicForV = "0.3.1"

// Projects
lazy val `fs2-jms` = project
  .in(file("."))
  .enablePlugins(NoPublishPlugin)
  .aggregate(core, ibmMQ, tests, examples, site)

lazy val core = project
  .in(file("core"))
  .settings(commonSettings)
  .settings(name := "fs2-jms")
  .settings(parallelExecution in Test := false)

lazy val ibmMQ = project
  .in(file("ibm-mq"))
  .settings(commonSettings)
  .settings(name := "fs2-jms-ibm-mq")
  .settings(libraryDependencies += "com.ibm.mq" % "com.ibm.mq.allclient" % ibmMQV)
  .settings(parallelExecution in Test := false)
  .dependsOn(core)

lazy val tests = project
  .in(file("tests"))
  .settings(commonSettings: _*)
  .enablePlugins(NoPublishPlugin)
  .settings(libraryDependencies += "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jSlf4jImplV % Runtime)
  .settings(parallelExecution in Test := false)
  .dependsOn(core, ibmMQ)

lazy val examples = project
  .in(file("examples"))
  .settings(commonSettings: _*)
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core, ibmMQ)

lazy val site = project
  .in(file("site"))
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(MdocPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(commonSettings)
  .dependsOn(core)
  .settings {
    import microsites._
    Seq(
      micrositeName := "fs2-jms",
      micrositeDescription := "fs2/cats-effects wrapper for jms",
      micrositeAuthor := "Alessandro Zoffoli",
      micrositeGithubOwner := "al333z",
      micrositeGithubRepo := "fs2-jms",
      micrositeBaseUrl := "/fs2-jms",
      micrositeFooterText := None,
      micrositeHighlightTheme := "atom-one-light",
      micrositePalette := Map(
        "brand-primary"   -> "#3e5b95",
        "brand-secondary" -> "#294066",
        "brand-tertiary"  -> "#2d5799",
        "gray-dark"       -> "#49494B",
        "gray"            -> "#7B7B7E",
        "gray-light"      -> "#E5E5E6",
        "gray-lighter"    -> "#F4F3F4",
        "white-color"     -> "#FFFFFF"
      ),
      micrositeCompilingDocsTool := WithMdoc,
      scalacOptions in Tut --= Seq(
        "-Xfatal-warnings",
        "-Ywarn-unused-import",
        "-Ywarn-numeric-widen",
        "-Ywarn-dead-code",
        "-Ywarn-unused:imports",
        "-Xlint:-missing-interpolator,_"
      ),
      micrositePushSiteWith := GitHub4s,
      micrositeGithubToken := sys.env.get("GITHUB_TOKEN"),
      micrositeExtraMdFiles := Map(
        file("CODE_OF_CONDUCT.md") -> ExtraMdFileConfig(
          "code-of-conduct.md",
          "page",
          Map("title" -> "code of conduct", "section" -> "code of conduct", "position" -> "100")
        ),
        file("LICENSE") -> ExtraMdFileConfig(
          "license.md",
          "page",
          Map("title" -> "license", "section" -> "license", "position" -> "101")
        )
      )
    )
  }

// General Settings
lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  crossScalaVersions := Seq(scalaVersion.value, "2.12.10"),
  scalafmtOnCompile := true,
  addCompilerPlugin("org.typelevel" %% "kind-projector"     % kindProjectorV cross CrossVersion.full),
  addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % betterMonadicForV),
  libraryDependencies ++= Seq(
    "javax.jms"         % "javax.jms-api"                  % jmsV,
    "org.typelevel"     %% "cats-core"                     % catsV,
    "org.typelevel"     %% "cats-effect"                   % catsEffectV,
    "co.fs2"            %% "fs2-core"                      % fs2V,
    "co.fs2"            %% "fs2-io"                        % fs2V,
    "io.chrisdavenport" %% "log4cats-slf4j"                % log4catsV,
    "com.codecommit"    %% "cats-effect-testing-scalatest" % catsEffectScalaTestV % Test
  )
)

// General Settings
inThisBuild(
  List(
    organization := "com.al333z",
    developers := List(
      Developer("al333z", "Alessandro Zoffoli", "alessandro.zoffoli@gmail.com", url("https://github.com/al333z"))
    ),
    homepage := Some(url("https://github.com/al333z/fs2-jms")),
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    pomIncludeRepository := { _ =>
      false
    },
    scalacOptions in (Compile, doc) ++= Seq(
      "-groups",
      "-sourcepath",
      (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url",
      "https://github.com/al333z/fs2-jms/blob/v" + version.value + "€{FILE_PATH}.scala"
    )
  )
)

addCommandAlias("buildAll", ";clean;scalafmtAll;+test;mdoc")