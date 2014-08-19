spark-sql-gdelt
===============

Scripts and code to import the GDELT dataset into Spark SQL for analysis.
Based upon the original [GDELT Spark blog post](http://chrismeserole.com/signals/shark-spark-gdelt-tutorial/).

This experiment is based on Spark 1.0.2.

## Importing Data into Spark SQL

GDELT [raw data](http://data.gdeltproject.org/events/index.html) ([docs](http://gdeltproject.org/data.html#documentation)) is in the form of tab-delimited "CSV" files with no headers.  The header line can be downloaded from the documentation part of their web site.

There are multiple ways of [importing data](http://spark.apache.org/docs/latest/sql-programming-guide.html) into Spark SQL.

* **JSON** - The easiest way to get started is JSON files.  Spark expects not one giant JSON blob, but rather one line per record, with each line being a JSON object, column names and types are inferred from the object keys and values.
    - Converting CSV into JSON with proper types is tedious, the only tools I've found (`pip install dataconverters`) that automatically infer types are pretty slow
    - A JSON file in the format described above is 5x the size of the original CSV.  For a 100GB dataset (based on the CSV) this is too much overhead.

* **Parquet** - being column oriented with compression, Parquet files should be smaller than CSV and load faster into Spark too. Converting CSV to Parquet is nontrivial though.

* **case classes** - This is the approach this tutorial takes, because we could then directly load CSV files into Spark and parse them into case classes.  The disadvantage is that this code only works for the GDELT dataset.

## Walk Through
