spark-sql-gdelt
===============

Scripts and code to import the GDELT dataset into Spark SQL for analysis.
Based upon the original [GDELT Spark blog post](http://chrismeserole.com/signals/shark-spark-gdelt-tutorial/).  Also contains a quick and dirty CSV importer (this will exist in Spark 1.2)

This experiment is based on Spark 1.0.2 and AWS, but can be easily adapted for other environments.  When running in AWS, it is much faster to download and upload the raw GDELT files from AWS itself, don't try it on your own laptop.

## Quick Start

    // convert each CSV line to JSON, use jsonRDD
    val gdelt = sc.textFile(.....).map(GdeltImporter.parseGDeltRow)
    gdelt.registerAsTable("gdelt")

    // Alternative, parses each line as case classes.  Warning: very slow!
    val gdelt = sc.textFile(.....).map(GdeltImporter.parseGDeltAsJson)
    sqlContext.jsonRDD(gdelt).registerAsTable("gdelt")

## Importing Data into Spark SQL

GDELT [raw data](http://data.gdeltproject.org/events/index.html) ([docs](http://gdeltproject.org/data.html#documentation)) is in the form of tab-delimited "CSV" files with no headers.  The header line can be downloaded from the documentation part of their web site.

There are multiple ways of [importing data](http://spark.apache.org/docs/latest/sql-programming-guide.html) into Spark SQL.

* **JSON** - The easiest way to get started is JSON files.  Spark expects not one giant JSON blob, but rather one line per record, with each line being a JSON object, column names and types are inferred from the object keys and values.
    - Converting CSV into JSON with proper types is tedious, the only tools I've found (`pip install dataconverters`) that automatically infer types are pretty slow
    - A JSON file in the format described above is 5x the size of the original CSV.  For a 100GB dataset (based on the CSV) this is too much overhead.

* **Parquet** - being column oriented with compression, Parquet files should be smaller than CSV and load faster into Spark too. Converting CSV to Parquet is nontrivial though.

* **case classes** - This is the approach this tutorial takes, because we could then directly load CSV files into Spark and parse them into case classes.  The disadvantage is that this code only works for the GDELT dataset.
    - UPDATE: Turns out this is *HORRIBLY* slow in Spark 1.0.2.  Thus the tutorial is now taking the approach of reading in each raw CSV line and converting it to JSON using `parseGDeltAsJson`, then using `jsonRDD`.

## Walk Through

1. Run `gdelt_importer.sh` to download and unzip the GDELT raw CSV files

1. Upload the CSV files to S3.  You can use `s3put` as described in the original Chris Meserole blog post.

1. Compile a jar of this repo using `sbt package`, then push the jar to your Spark cluster.
    - For EC2 deployed using Spark's built in scripts, the easiest is to `scp -i <aws_pem_file> target/scala-2.10/spark-sql-gdelt*.jar <your-aws-hostname>:/root/spark/lib`
    - Then, as root in `/root` dir, do `spark-ec2/copy-dir.sh spark/lib`
    - Finally, add this line to `spark/conf/spark-env.sh`: `export SPARK_CLASSPATH="/root/spark/lib/spark-sql-gdelt_2.10-0.1-SNAPSHOT.jar"` (add it above the SPARK_SUBMIT_LIBRARY_PATH export)
    - `spark-ec2/copy-dir.sh spark/conf`

1. Start `spark-shell` pointing to your cluster.  It's easier if you start it from the master node itself.
    - Verify our jar is available by typing `GdeltImporter` and see if the object can be discovered

1. Create a `SQLContext`.

        val sqlContext = new org.apache.spark.sql.SQLContext(sc)

1. Map the input files and transform them.  This is lazy.  It will eventually create a JSON string for every record.

        val gdelt = sc.textFile("s3n://YOUR_BUCKET/gdelt_csv/").map(GdeltImporter.parseGDeltAsJson)

1. Now let's convert the `RDD[String]` to a `SchemaRDD`, register it as a table and run some SQL on it!

        sqlContext.jsonRDD(gdelt).registerAsTable("gdelt")
        sqlContext.sql("SELECT count(*) FROM gdelt").collect

    - This step will take a while.  The first line is when Spark does the actual work of the `parseGDeltAsJson`, CSV reading and converting the JSON lines into `SchemaRDD` records.  Second step does the caching (see below).

The column names used should be the same as GDELT documentation.

### Caching the Data

So far, the data has been read into heap by Spark but it is not cached between queries.  This means that Spark may go all the way back to the data source - say S3 - for each query!   To keep the computation entirely in memory, we need to cache the dataset in memory:

    sqlContext.cacheTable("gdelt")

Now, the table will be cached on the next execution and subsequent queries will run out of the cache.  The cache stores the data in an efficient columnar format and will use compression if enabled.

Compression seems to help significantly (5x savings when I tried it -- at least compared to uncompressed case classes, which might not be a fair comparison).  To enable it, add this line to your `conf/spark-defaults.conf`:

    spark.sql.inMemoryColumnarStorage.compressed true

### Case Class instructions (Very Slow)

NOTE: This may appear to hang, but don't give up.  For GDELT data 1979-1984, it took an HOUR to do the final step on an 8 x c3.xlarge setup in AWS.

1. Do an import so an `RDD[GDeltRow]` can be implicitly converted to an `SchemaRDD`.

        import sqlContext.createSchemaRDD

1. Map the input files and transform them.  This is lazy.  It creates a case class for every record.

        val gdelt = sc.textFile("s3n://YOUR_BUCKET/gdelt_csv/").map(GdeltImporter.parseGDeltRow)

1. Read the first 3 records and verify the records are what we expect.

        gdelt.take(3).foreach(println)

    - You should see something like this:

            GDeltRow(0,19790101,197901,1979,1979.0027,ActorInfo(,,,,,,,,,),ActorInfo(AFR,AFRICA,AFR,,,,,,,),1,040,040,04,1,1.0,9,1,9,5.5263157,GeoInfo(0,,,,0.0,0.0,0),GeoInfo(0,,,,0.0,0.0,0),GeoInfo(0,,,,0.0,0.0,0),20130203)
            GDeltRow(1,19790101,197901,1979,1979.0027,ActorInfo(,,,,,,,,,),ActorInfo(AGR,FARMER,,,,,,AGR,,),1,030,030,03,1,4.0,10,1,10,10.979228,GeoInfo(0,,,,0.0,0.0,0),GeoInfo(1,Nigeria,NI,NI,10.0,8.0,0),GeoInfo(1,Nigeria,NI,NI,10.0,8.0,0),20130203)
            GDeltRow(2,19790101,197901,1979,1979.0027,ActorInfo(,,,,,,,,,),ActorInfo(AGR,FARMER,,,,,,AGR,,),1,100,100,10,3,-5.0,10,1,10,10.979228,GeoInfo(0,,,,0.0,0.0,0),GeoInfo(1,Nigeria,NI,NI,10.0,8.0,0),GeoInfo(1,Nigeria,NI,NI,10.0,8.0,0),20130203)

1. Now let's register the RDD[GdeltRow] as a table and run some SQL on it!

        gdelt.registerAsTable("gdelt")
        sqlContext.sql("SELECT count(*) FROM gdelt").collect

Note that the GDELT event records have 57 fields.  It is really a flattened
hierarchy as fields are repeated for Actor1 and Actor2.  Since Scala case
classes (as of 2.10) only support 22 fields, and we want to represent the
structure, we use nested case classes.

Access nested fields using a dot notation, for example, `Actor2.Name`.  To see a schema of all the fields, do `gdelt.printSchema`.
