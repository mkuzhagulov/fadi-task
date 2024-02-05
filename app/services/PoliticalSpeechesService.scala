package services

import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.google.inject.ImplementedBy
import models.{PoliticalSpeeches, ResultResponse}
import play.api.libs.ws.WSClient
import play.api.inject.Injector
import utils.CSVReadHelper

import java.io.File
import java.nio.file.{Files, Paths}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[PoliticalSpeechesServiceImpl])
trait PoliticalSpeechesService {
  def evaluate(urls: List[String]): Future[ResultResponse]
}

@Singleton
class PoliticalSpeechesServiceImpl @Inject()(ws: WSClient)
                                            (implicit mat: Materializer,
                                             ec: ExecutionContext)
  extends PoliticalSpeechesService {
  override def evaluate(urls: List[String]): Future[ResultResponse] = {
    val urlsWithIndex = urls.zipWithIndex
    Source(urlsWithIndex)
      .mapAsync(2)(ui => processUrl(ui._1, ui._2))
      .runWith(Sink.fold(List.empty[PoliticalSpeeches])((acc, elem: List[PoliticalSpeeches]) => acc ++ elem))
      .map(x => calculateStatistics(x.toSet))
  }

  private def processUrl(url: String, index: Int): Future[List[PoliticalSpeeches]] = {
    val futureResponse = ws.url(url).stream()

    val downloadedFile: Future[File] = futureResponse.flatMap { res =>
      val file = Files.createFile(Paths.get(s"tmp/$index.csv")).toFile
      val outputStream = java.nio.file.Files.newOutputStream(file.toPath)
      val sink = Sink.foreach[ByteString] { bytes => outputStream.write(bytes.toArray) }

      res.bodyAsSource
        .runWith(sink)
        .andThen {
          case result =>
            outputStream.close()
            result.get
        }
        .map(_ => file)

    }

    downloadedFile.map { file =>
      val res = CSVReadHelper.parseCSVFile(file)
      file.delete()
      res
    }
  }

  private def calculateStatistics(stats: Set[PoliticalSpeeches]): ResultResponse = {
    val SecurityTheme = "Innere Sicherheit"
    val Year = 2013

    val mostSpeeches = {
      val mostSpeechesMap = stats
        .filter(row => row.date.getYear == Year)
        .foldLeft(Map[String, Int]().withDefaultValue(0)) { case (acc, x) =>
          acc.updated(x.name, acc(x.name) + x.wordsCounter)
        }
        if (mostSpeechesMap.isEmpty) null
        else mostSpeechesMap.maxBy(_._2)._1
    }

    val mostSecurity = {
      val mostSecurityMap = stats
        .filter(row => row.theme == SecurityTheme)
        .foldLeft(Map[String, Int]().withDefaultValue(0)) { case (acc, x) =>
          acc.updated(x.name, acc(x.name) + x.wordsCounter)
        }
      if (mostSecurityMap.isEmpty) null
      else mostSecurityMap.maxBy(_._2)._1
    }

    val fewestWords = {
      val fewestWordsMap = stats
        .foldLeft(Map[String, Int]().withDefaultValue(0)) { case (acc, x) =>
          acc.updated(x.name, acc(x.name) + x.wordsCounter)
        }

      if (fewestWordsMap.isEmpty) null
      else fewestWordsMap.minBy(_._2)._1
    }

    ResultResponse(mostSpeeches, mostSecurity, fewestWords)
  }
}
