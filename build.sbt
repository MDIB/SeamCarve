enablePlugins(ScalaJSPlugin)

name := "SeamCarve"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.8"

persistLauncher in Compile := true

persistLauncher in Test := false

testFrameworks += new TestFramework("utest.runner.Framework")

unmanagedSourceDirectories in Compile <<= baseDirectory(base =>
  (base / "src" / "main" / "scala" / "com" / "gdicristofaro" / "seamcarve" / "js") ::
  (base / "src" / "main" / "scala" / "com" / "gdicristofaro" / "seamcarve" / "core") ::
  Nil
)

resolvers += "xuggle repo" at "http://xuggle.googlecode.com/svn/trunk/repo/share/java/"

libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.0",
    "com.lihaoyi" %%% "utest" % "0.3.0" % "test",
    "commons-cli" % "commons-cli" % "1.3.1",
    "xuggle" % "xuggle-xuggler" % "5.2"
)
