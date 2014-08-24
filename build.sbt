name := "spark-sql-gdelt"

organization := "org.velvia"

scalaVersion := "2.10.4"

libraryDependencies +=  "org.apache.spark" %% "spark-sql" % "1.0.2" % "provided" exclude("io.netty", "netty-all")
