package models

import play.api.libs.json.{Json, Format}

case class ResultResponse(mostSpeeches: String, mostSecurity: String, leastSecurity: String)

object ResultResponse {
  implicit val jsonFormat: Format[ResultResponse] = Json.format
}
