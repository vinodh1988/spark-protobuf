import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.protobuf.functions._
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.hadoop.fs.{FileSystem, Path, FSDataOutputStream}

object LocalProtoBufConversionApp {
  def main(args: Array[String]): Unit = {
    // Create Spark session
    val spark = SparkSession.builder()
      .appName("LocalProtoBufConversion")
      .master("local[*]") // Use local mode
      .getOrCreate()

    import spark.implicits._
    // Path to your descriptor file in local system
    val descriptorFile = "/Users/vinodh/datasets/person.desc"

    // Path to your JSON file in local system
    val jsonFilePath = "file:///Users/vinodh/datasets/person.json"
    // Define the schema of the JSON file explicitly
    val schema = StructType(Array(
      StructField("name", StringType, true),
      StructField("id", IntegerType, true),  // Ensuring 'id' is treated as Integer
      StructField("email", StringType, true)
    ))


    // Read JSON file into DataFrame
    val inputDF = spark.read.schema(schema).json(jsonFilePath)

    // Convert DataFrame to Protobuf format using the descriptor file
    val protoBytesDF = inputDF.select(
      to_protobuf(struct($"name", $"id", $"email"),"person",descriptorFile) as ("result")
    )

    val resultrdd=protoBytesDF.rdd.map(r => r.getAs[Array[Byte]]("result"))
    val basePath = "file:///Users/vinodh/datasets/protooutput/"

    // Write each byte array to a separate binary file
    resultrdd.zipWithIndex().foreachPartition { partition =>
      val configuration = new Configuration()
      val fs = FileSystem.get(configuration)

      partition.foreach { case (bytes, index) =>
        val path = new Path(basePath + s"output_$index.bin")
        val output: FSDataOutputStream = fs.create(path, true)
        output.write(bytes)
        output.close()
      }
    }
    // Show the results
    protoBytesDF.show(false)



  }
}


