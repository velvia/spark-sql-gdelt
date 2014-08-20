/**
 * Note: There is a Spark ticket to add CSV import to Spark, slated for 1.2.   So this is just a
 * quick hack.  For one, it only handles tab-delimited lines, and you must supply a schema.
 */
object CsvImporter {
  sealed trait CsvType
  case object CsvString extends CsvType
  case object CsvNumber extends CsvType

  private def quotify(s: String) = "\"" + s + "\""

  /**
   * Convert a single CSV tab-delimited line to a JSON object string.
   * WARNING: NO checking of number types is done!!  Except for empty number
   */
  def csvLineToJson(line: String, schema: Seq[(String, CsvType)]): String = {
    val parts = line.split("\t")
    val fields = parts.zip(schema).map {
      case (part, (fieldKey, CsvString)) => quotify(fieldKey) + ":" + quotify(part)
      case (part, (fieldKey, CsvNumber)) => quotify(fieldKey) + ":" + (if (part.isEmpty) "null" else part)
    }
    "{" + fields.mkString(",") + "}"
  }
}