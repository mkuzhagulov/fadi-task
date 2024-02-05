package models.exception

case class IncorrectFileFormatException(ex: Throwable)
  extends RuntimeException(s"Error occured while parsing csv file: '${ex.getMessage}''")
