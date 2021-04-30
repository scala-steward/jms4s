/*
 * Copyright 2021 Alessandro Zoffoli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import cats.effect.{ ExitCode, IO, IOApp, Resource }
import jms4s.JmsAcknowledgerConsumer.AckAction
import jms4s.JmsClient
import jms4s.config.{ QueueName, TopicName }
import jms4s.jms.MessageFactory

class AckConsumerExample extends IOApp {

  val contextRes: Resource[IO, JmsClient[IO]] = null // see providers section!
  val inputQueue: QueueName                   = QueueName("YUOR.INPUT.QUEUE")
  val outputTopic: TopicName                  = TopicName("YUOR.OUTPUT.TOPIC")

  def yourBusinessLogic(text: String, mf: MessageFactory[IO]): IO[AckAction[IO]] =
    if (text.toInt % 2 == 0)
      mf.makeTextMessage("a brand new message").map(newMsg => AckAction.send(newMsg, outputTopic))
    else if (text.toInt % 3 == 0)
      IO.pure(AckAction.noAck)
    else
      IO.pure(AckAction.ack)

  override def run(args: List[String]): IO[ExitCode] = {
    val consumerRes = for {
      client   <- contextRes
      consumer <- client.createAcknowledgerConsumer(inputQueue, 10)
    } yield consumer

    consumerRes.use(_.handle { (jmsMessage, mf) =>
      for {
        text <- jmsMessage.asTextF[IO]
        res  <- yourBusinessLogic(text, mf)
      } yield res
    }.as(ExitCode.Success))
  }
}
