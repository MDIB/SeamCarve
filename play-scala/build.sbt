name := """play-scala"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "commons-cli" % "commons-cli" % "1.3.1",
  "xuggle" % "xuggle-xuggler" % "5.2",
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)


unmanagedSourceDirectories in Compile <<= baseDirectory(base =>
  (base / "app" / "com" / "gdicristofaro" / "seamcarve" / "jvm") ::
  (base / "app" / "com" / "gdicristofaro" / "seamcarve" / "core") ::
  (base / "app" / "controllers") ::
  Nil
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "xuggle repo" at "http://xuggle.googlecode.com/svn/trunk/repo/share/java/"

fork in run := true
