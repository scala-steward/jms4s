package jms4s.jms

import cats.effect.{ Concurrent, ContextShift, Sync }
import cats.implicits._
import javax.jms.MessageConsumer

class JmsMessageConsumer[F[_]: Concurrent: ContextShift] private[jms4s] (
  private[jms4s] val wrapped: MessageConsumer
) {

  val receiveJmsMessage: F[JmsMessage[F]] =
    for {
      recOpt <- Sync[F].delay(Option(wrapped.receiveNoWait()))
      rec <- recOpt match {
              case Some(message) => Sync[F].pure(new JmsMessage(message))
              case None          => ContextShift[F].shift >> receiveJmsMessage
            }
    } yield rec
}
