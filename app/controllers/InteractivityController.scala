package controllers

import javax.inject._
import objects.SlackAction
import play.api._
import play.api.mvc._
import play.api.libs.json._
import scalaj.http.Http

@Singleton
class InteractivityController @Inject()
  (
    val controllerComponents: ControllerComponents,
    config: Configuration
  ) extends BaseController {

  private val LOGGER = Logger(getClass)
  private val SLACK_API_URI = config.get[String]("slack.api")
  private val TOKEN = config.get[String]("slack.token")

  def open() = Action { implicit request: Request[AnyContent] =>
    val body: Option[Map[String, Seq[String]]] = request.body.asFormUrlEncoded
    val triggerId = body.get("trigger_id")(0)
    val text = body.get("text")(0)

    if (triggerId.strip().isEmpty || text.strip().isEmpty) {
      BadRequest("trigger_id or text is empty")
    } else {
      val payload = Json.obj(
        "trigger_id" -> triggerId,
        "dialog" -> Json.obj(
          "callback_id" -> "calc_action",
          "title" -> "코알못의 정산봇",
          "submit_label" -> "정산하기",
          "state" -> text,
          "elements" -> Json.arr(
            Json.obj(
              "type" -> "text",
              "label" -> "계좌번호를 입력해 주세요!",
              "name" -> "account"
            ),
            Json.obj(
              "type" -> "text",
              "subtype" -> "number",
              "label" -> "금액을 입력해 주세요!",
              "name" -> "price"
            ),
            Json.obj(
              "type" -> "text",
              "subtype" -> "number",
              "label" -> "인원을 입력해 주세요!",
              "name" -> "count"
            ),
            Json.obj(
              "label" -> "정산요정",
              "name" -> "calc_fairy",
              "type" -> "select",
              "data_source" -> "users"
            )
          )
        )
      )
      send(SlackAction.DIALOG_OPEN, payload.toString)
      Ok("")
    }
  }

  def calc() = Action { implicit request: Request[AnyContent] =>
    val slackPayload = request.body.asFormUrlEncoded.get("payload").map {
      m => Json.parse(m)
    }(0)

    val submission = slackPayload \ "submission"
    val state = (slackPayload \ "state").as[String].split(" ").toList

    val account = (submission \ "account").as[String]
    val calcFairy = (submission \ "calc_fairy").as[String]
    val count = (submission \ "count").as[String].toInt
    val price = (submission \ "price").as[String].toInt

    val text = s"<@$calcFairy> 님께 `${price / count}원`을 보내주시면 됩니다!\n계좌번호는 `$account` 입니다."

    state.foreach(user => {
      val payload = Json.obj(
        "channel" -> user,
        "text" -> text
      )
      send(SlackAction.SEND_MESSAGE, payload.toString)
    })

    Ok("")
  }

  private def send(action: String, payload: String): Unit = {
    val res = Http(s"$SLACK_API_URI/$action")
      .header("Content-Type", "application/json")
      .header("Authorization", s"Bearer $TOKEN")
      .postData(payload)
      .asString

    LOGGER.info(res.body)
  }
}
