import org.apache.spark.sql.{SQLContext, SchemaRDD}

object SqlBenchmark {
  def time[A](f: => A): A = {
    val (seconds, res) = bench(f)
    println("That took " + seconds + " secs.")
    res
  }

  /**
   * Runs f and returns (seconds elapsed, result)
   */
  def bench[A](f: => A): (Float, A) = {
    val start = System.currentTimeMillis
    val res = f
    ((System.currentTimeMillis - start) / 1000.0F, res)
  }

  case class BenchResult(avg: Float, max: Float, min: Float, query: String, plan: SchemaRDD)

  /**
   * Benchmarks multiple queries one at a time.
   * @param sqc the SQLContext to execute queries in
   * @param queries the list of queries to execute
   * @param nIterations the number of times to run each query
   */
  def benchSequential(sqc: SQLContext, queries: Seq[String], nIterations: Int = 15): Seq[BenchResult] = {
    queries.map { query =>
      val queryRdd = sqc.sql(query)
      val times = (1 to nIterations).map { n => bench(queryRdd.collect)._1 }
      BenchResult(times.sum * 1.0F / nIterations, times.max, times.min, query, queryRdd)
    }
  }

  def printResults(results: Seq[BenchResult]) {
    for (result <- results) {
      println(s"====\nQuery: ${result.query}\n\nPlan:\n${result.plan}\n")
      println(s"Avg\t${result.avg} secs\nMax\t${result.max} secs\nMin\t${result.min} secs\n")
    }
  }
}