import org.apache.spark.sql.{SQLContext, SchemaRDD}
import org.apache.spark.rdd.RDD

object GdeltBenchmark {
  /**
   * Loads and caches a given subset of the GDELT dataset from raw CSV files.
   * {{{
   *   val sqc = new org.apache.spark.sql.SQLContext(sc)
   *   val schemaRdd = GdeltBenchmark.gdeltCsvFile(sqc, "s3n://my-bucket/gdelt_csv/", 1979, 2005)
   * }}}
   *
   * The files are all expected to be uploaded to a single directory in any supported textFile storage,
   * and must begin with YYYY, but may be compressed.
   *
   * @param sqc the SQLContext to use for caching the gdelt table and running queries against it
   * @param prefix the first part of the hdfs/s3n/local path, before the actual GDELT .csv files
   * @param startYear the year to start loading data
   * @param endYear the last year to load data for
   * @param cache if true, calls cacheTable() and also invokes an initial SQL query to make the caching happen
   * @param tableName the name of the table to cache
   */
  def gdeltCsvFile(sqc: SQLContext, prefix: String, startYear: Int, endYear: Int,
                   cache: Boolean = true, tableName: String = "gdelt"): SchemaRDD = {
    require(prefix.endsWith("/"), "Prefix must end with a /")
    require(endYear >= startYear)
    require(startYear >= 1979, "First eligible year of GDELT dataset is 1979")

    val csvRdds = (startYear to endYear).map { year => sqc.sparkContext.textFile(prefix + year + "*") }
    val jsonRdd = sqc.sparkContext.union(csvRdds).map(GdeltImporter.parseGDeltAsJson)
    val schemaRdd = sqc.jsonRDD(jsonRdd)
    schemaRdd.registerAsTable(tableName)
    if (!cache) return schemaRdd

    sqc.cacheTable(tableName)
    // Run one query through, force the caching to happen
    println("Cached " + sqc.sql("SELECT count(*) from " + tableName).collect + " rows")
    schemaRdd
  }
}