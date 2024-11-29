package example

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.protobuf.functions.from_protobuf

object KafkaProtobufReader {
  def main(args: Array[String]): Unit = {
    // Initialize SparkSession
    val spark = SparkSession.builder
      .appName("KafkaProtobufReader")
      .master("local[*]") // Local mode for testing
      .getOrCreate()

    import spark.implicits._

    // Kafka configuration
    val kafkaBootstrapServers = "localhost:9092"
    val topic = "employee-topic"

    // Path to the Protobuf descriptor file
    val descriptorFile = "/Users/vinodh/protos/Employee.desc"

    // Protobuf message type (fully qualified)
    val messageType = "example.Employee"

    // Read messages from Kafka
    val kafkaDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrapServers)
      .option("subscribe", topic)
      .option("startingOffsets", "earliest")
      .load()

    // Extract Protobuf binary data and convert to DataFrame
    val protoDF = kafkaDF
      .selectExpr("CAST(value AS BINARY) as value") // Extract binary Protobuf data
      .select(from_protobuf($"value", messageType, descriptorFile).alias("employee")) // Deserialize Protobuf
      .select("employee.*") // Flatten the struct for individual fields

    // Print the deserialized messages to the console
    val query = protoDF.writeStream
      .outputMode("append")
      .format("console")
      .start()

    query.awaitTermination() // Keep the query running
  }
}
