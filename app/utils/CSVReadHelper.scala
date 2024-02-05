package utils

import com.github.tototoshi.csv._
import models.PoliticalSpeeches
import models.exception.IncorrectFileFormatException

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

object CSVReadHelper {
  private val DateTimePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def parseCSVFile(file: File): List[PoliticalSpeeches] = {
    val reader = CSVReader.open(file)
    val result = reader.all().tail.map { row =>
      Try {
        row match {
          case List(name, theme, date, counter) =>
            val localDate = LocalDate.parse(date.trim, DateTimePattern)

            PoliticalSpeeches(name.trim, theme.trim, localDate, counter.trim.toInt)
        }
      } match {
        case Success(value) => value
        case Failure(ex) => throw IncorrectFileFormatException(ex)
      }
    }

    reader.close()

    result
  }
}
