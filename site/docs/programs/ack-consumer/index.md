---
layout: docs
title:  "Acknowledger Consumer"
---

# Acknowledger Consumer

A `JmsAcknowledgerConsumer` is a consumer which let the client decide whether confirm (a.k.a. ack) or reject (a.k.a. nack) a message after its reception.
Its only operation is:

```scala
def handle(f: JmsMessage => F[AckAction[F]]): F[Unit]
```

This is where the user of the API can specify its business logic, which can be any effectful operation.

What `handle` expects is an `AckAction[F]`, which can be either:
- an `AckAction.ack`, which will confirm the message
- an `AckAction.noAck`, which will do nothing
- an `AckAction.send` in all its forms, which can be used to send 1 or multiple messages to 1 or multiple destinations

The consumer can be configured specifying a `concurrencyLevel`, which is used internally to scale the operations (receive and then process up to `concurrencyLevel`).

## A complete example

```scala mdoc
import cats.effect.{ ExitCode, IO, IOApp, Resource }
import jms4s.JmsAcknowledgerConsumer.AckAction
import jms4s.JmsClient
import jms4s.config.{ QueueName, TopicName }
import jms4s.jms.{ JmsContext, MessageFactory }

class AckConsumerExample extends IOApp {

  val contextRes: Resource[IO, JmsContext[IO]] = null // see providers section!
  val jmsClient: JmsClient[IO]                 = new JmsClient[IO]
  val inputQueue: QueueName                    = QueueName("YUOR.INPUT.QUEUE")
  val outputTopic: TopicName                   = TopicName("YUOR.OUTPUT.TOPIC")

  def yourBusinessLogic(text: String, mf: MessageFactory[IO]): IO[AckAction[IO]] =
    if (text.toInt % 2 == 0)
      mf.makeTextMessage("a brand new message").map(newMsg => AckAction.send(newMsg, outputTopic))
    else if (text.toInt % 3 == 0)
      IO.pure(AckAction.noAck)
    else
      IO.pure(AckAction.ack)

  override def run(args: List[String]): IO[ExitCode] = {
    val consumerRes = for {
      jmsContext <- contextRes
      consumer   <- jmsClient.createAcknowledgerConsumer(jmsContext, inputQueue, 10)
    } yield consumer

    consumerRes.use(_.handle { (jmsMessage, mf) =>
      for {
        text <- jmsMessage.asTextF[IO]
        res  <- yourBusinessLogic(text, mf)
      } yield res
    }.as(ExitCode.Success))
  }
}
```