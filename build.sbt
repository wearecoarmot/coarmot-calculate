name := """coarmot-calculate"""
organization := "coarmot.calculate"
maintainer := "rnrghks09@gmail.com"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2"
