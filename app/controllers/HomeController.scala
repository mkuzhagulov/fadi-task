package controllers

import javax.inject._
import play.api._
import play.api.inject.Injector
import play.api.mvc._
import services.PoliticalSpeechesService
import models.ResultResponse._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(politicalSpeechesService: PoliticalSpeechesService,
                               val controllerComponents: ControllerComponents)
                              (implicit ec: ExecutionContext)
  extends BaseController {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def evaluate(urls: List[String]) = Action.async { _ =>
    politicalSpeechesService
      .evaluate(urls)
      .map(result => Ok(Json.toJson(result)))
  }
}
