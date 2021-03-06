import cats.effect.{ ExitCode, IO, IOApp, Resource }
import jms4s.JmsAutoAcknowledgerConsumer.AutoAckAction
import jms4s.JmsClient
import jms4s.config.{ QueueName, TopicName }
import jms4s.jms.MessageFactory

class AutoAckConsumerExample extends IOApp {

  val jmsClient: Resource[IO, JmsClient[IO]] = null // see providers section!
  val inputQueue: QueueName                  = QueueName("YUOR.INPUT.QUEUE")
  val outputTopic: TopicName                 = TopicName("YUOR.OUTPUT.TOPIC")

  def yourBusinessLogic(text: String, mf: MessageFactory[IO]): IO[AutoAckAction[IO]] =
    if (text.toInt % 2 == 0) {
      mf.makeTextMessage("a brand new message").map(newMsg => AutoAckAction.send[IO](newMsg, outputTopic))
    } else {
      IO.pure(AutoAckAction.noOp)
    }

  override def run(args: List[String]): IO[ExitCode] = {
    val consumerRes = for {
      client   <- jmsClient
      consumer <- client.createAutoAcknowledgerConsumer(inputQueue, 10)
    } yield consumer

    consumerRes.use(_.handle { (jmsMessage, mf) =>
      for {
        text <- jmsMessage.asTextF[IO]
        res  <- yourBusinessLogic(text, mf)
      } yield res
    }.as(ExitCode.Success))
  }
}
