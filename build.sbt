name := """plant_shop_play_framework"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.18"

libraryDependencies += guice

Compile / run / fork := true
Compile / run / connectInput := true
Compile / run / outputStrategy := Some(StdoutOutput)
libraryDependencies += "at.favre.lib" % "bcrypt" % "0.10.2"
libraryDependencies += "org.postgresql" % "postgresql" % "42.7.4"
libraryDependencies += "org.json" % "json" % "20240303" % Test
