name := """plant_shop_play_framework"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.13.18"

libraryDependencies += guice

Compile / run / fork := true
Compile / run / connectInput := true
Compile / run / outputStrategy := Some(StdoutOutput)
libraryDependencies += "at.favre.lib" % "bcrypt" % "0.10.2"
libraryDependencies += "org.postgresql" % "postgresql" % "42.7.4"
libraryDependencies += "io.jsonwebtoken" % "jjwt-api" % "0.12.6"
libraryDependencies += "io.jsonwebtoken" % "jjwt-impl" % "0.12.6" % Runtime
libraryDependencies += "io.jsonwebtoken" % "jjwt-jackson" % "0.12.6" % Runtime
libraryDependencies += "org.json" % "json" % "20240303" % Test
libraryDependencies += "org.jsoup" % "jsoup" % "1.18.3" % Test
libraryDependencies += "org.seleniumhq.selenium" % "selenium-java" % "4.27.0" % Test
