ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.13"

lazy val root = (project in file("."))
  .settings(
    name := "scala-pb"
  )

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.1",
  "org.apache.spark" %% "spark-sql" % "3.5.1",
  "org.apache.spark" %%"spark-protobuf"% "3.5.1",
  // Include SparkSQL Protobuf suppor
// Spark Kafka Integration
"org.apache.spark" %% "spark-sql-kafka-0-10" % "3.5.1",
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.15.2",
  "com.thesamet.scalapb" %% "scalapb-runtime" % "0.11.6"

)

