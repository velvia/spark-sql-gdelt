import scala.util.Try

case class ActorInfo(Code: String,
                     Name: String,
                     CountryCode: String,
                     KnownGroupCode: String,
                     EthnicCode: String, Religion1Code: String, Religion2Code: String,
                     Type1Code: String, Type2Code: String, Type3Code: String)

case class GeoInfo(`Type`: Int, FullName: String, CountryCode: String, ADM1Code: String, Lat: Float,
                   `Long`: Float, FeatureID: Int)

case class GDeltRow(EventId: Int, Day: Int, MonthYear: Int, Year: Int, FractionDate: Float,
                    Actor1: ActorInfo, Actor2: ActorInfo,
                    IsRootEvent: Byte, EventCode: String, EventBaseCode: String,
                    EventRootCode: String, QuadClass: Int, GoldsteinScale: Float,
                    NumMentions: Int, NumSources: Int, NumArticles: Int,
                    AvgTone: Float,
                    Actor1Geo: GeoInfo, Actor2Geo: GeoInfo, ActionGeo: GeoInfo, DateAdded: String)

object GdeltImporter {
  def time[A](f: => A): A = {
    val start = System.currentTimeMillis
    val res = f
    println("That took " + (System.currentTimeMillis - start) / 1000.0 + " secs.")
    res
  }

  private def str(row: Array[String], i: Int): String = Try(row(i)).getOrElse("")
  private def int(row: Array[String], i: Int): Int = Try(row(i).toInt).getOrElse(0)
  private def float(row: Array[String], i: Int): Float = Try(row(i).toFloat).getOrElse(0.0F)

  def makeActorInfo(row: Array[String], startIndex: Int): ActorInfo =
    ActorInfo(str(row, startIndex),
              str(row, startIndex + 1),
              str(row, startIndex + 2),
              str(row, startIndex + 3),
              str(row, startIndex + 4),
              str(row, startIndex + 5),
              str(row, startIndex + 6),
              str(row, startIndex + 7),
              str(row, startIndex + 8),
              str(row, startIndex + 9))

  def makeGeoInfo(row: Array[String], startIndex: Int): GeoInfo =
    GeoInfo(int(row, startIndex),       // Type
            str(row, startIndex + 1),   // FullName
            str(row, startIndex + 2),   // CountryCode
            str(row, startIndex + 3),   // ADM1Code
            float(row, startIndex + 4),  // Lat
            float(row, startIndex + 5),  // Long
            int(row, startIndex + 6))    // FeatureID

  /**
   * Parses a single line from a raw GDELT events CSV data file, found at
   * http://data.gdeltproject.org/events/index.html
   * @param row a tab-delimited raw CSV input line from a GDELT data file
   * @return a populated GDeltRow case class
   */
  def parseGDeltRow(row: String): GDeltRow = {
    val parts = row.split("\t")
    GDeltRow(int(parts, 0), int(parts, 1), int(parts, 2), int(parts, 3), float(parts, 4),
             makeActorInfo(parts, 5), makeActorInfo(parts, 15),
             // IsRootEvent, EventCode, EventBaseCode etc.
             int(parts, 25).toByte, str(parts, 26), str(parts, 27),
             str(parts, 28), int(parts, 29), float(parts, 30),
             int(parts, 31), int(parts, 32), int(parts, 33),   // NumMentions/Sources/Articles
             float(parts, 34),    // AvgTone
             // Actor1Geo*, Actor2Geo*, ActionGeo*
             makeGeoInfo(parts, 35), makeGeoInfo(parts, 42), makeGeoInfo(parts, 49),
             str(parts, 56)
             )
  }
}