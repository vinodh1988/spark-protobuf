package example

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger
import employee.Employee // Import the ScalaPB-generated Employee class
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper

// Define a custom case class with Spark-compatible types
case class EmployeeData(
                         id: String,
                         name: String,
                         department: String,
                         salary: Float
                       )

object KafkaToJsonJob {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession
    val spark = SparkSession.builder
      .appName("KafkaToJsonJob")
      .master("local[*]") // Local mode for testing
      .getOrCreate()

    import spark.implicits._

    // Kafka configuration
    val kafkaBootstrapServers = "localhost:9092"
    val topic = "employee-topic"

    // Initialize Jackson ObjectMapper
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    // Read messages from Kafka
    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("subscribe", topic)
      .load()

    // Extract and transform Protobuf messages into Spark-compatible case class
    val employeeDS = kafkaDF
      .selectExpr("CAST(value AS BINARY) as value") // Extract binary Protobuf data
      .as[Array[Byte]]                              // Convert to Dataset of byte arrays
      .map { bytes =>
        val employee = Employee.parseFrom(bytes) // Deserialize Protobuf to Employee case class
        // Convert to custom case class
        EmployeeData(
          id = employee.id,
          name = employee.name,
          department = employee.department,
          salary = employee.salary
        )
      }

    // Convert EmployeeData objects to JSON using Jackson
    val jsonDS = employeeDS.map { employeeData =>
      mapper.writeValueAsString(employeeData) // Serialize EmployeeData to JSON
    }

    // Write JSON output to a directory
    val query = jsonDS.writeStream
      .format("json")
      .option("path", "output/json")         // Directory to write JSON files
      .option("checkpointLocation", "output/checkpoint") // Required for streaming
      .trigger(Trigger.ProcessingTime("10 seconds"))
      .start()

    query.awaitTermination() // Keep the query running
  }
}
