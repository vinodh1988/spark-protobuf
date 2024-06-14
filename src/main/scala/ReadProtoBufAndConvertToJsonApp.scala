import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.protobuf.functions._
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.io.IOUtils
import org.apache.spark.input.PortableDataStream

object ReadProtoBufAndConvertToJsonApp {
  def main(args: Array[String]): Unit = {
    // Create Spark session
    val spark = SparkSession.builder()
      .appName("ReadProtobufAndConvertToJson")
      .master("local[*]") // Use local mode
      .getOrCreate()

    import spark.implicits._

    // Path to your descriptor file
    val descriptorFile = "/Users/vinodh/datasets/messages.desc"

    // Path to your binary files directory
    val binaryFilesPath = "file:///Users/vinodh/datasets/protooutput/*"

    // Read binary files into RDD[PortableDataStream]
    val binaryFilesRDD = spark.sparkContext.binaryFiles(binaryFilesPath)

    // Convert each PortableDataStream into byte array
    val binaryDataRDD = binaryFilesRDD.mapValues(_.toArray())

    // Create DataFrame from RDD
    val binaryDF = binaryDataRDD.map(_._2).toDF("content")

    // Define the schema of the Protobuf data
    val messageSchema = "person"  // Assuming the Protobuf message type is 'Person'

    // Convert from Protobuf binary to DataFrame using from_protobuf
    val protoDF = binaryDF.select(
      from_protobuf($"content", messageSchema, descriptorFile).alias("parsed")
    )

    // Explode the struct to flatten the data
    val flattenedDF = protoDF.selectExpr("parsed.*")

    // Convert DataFrame to JSON
    val jsonDF = flattenedDF.toJSON

    // Show JSON results
    jsonDF.show(false)

    // Optionally, write JSON to a file
    jsonDF.write.text("file:///Users/vinodh/datasets/proto_processed_output")

    // Stop Spark session
    spark.stop()
  }
}
