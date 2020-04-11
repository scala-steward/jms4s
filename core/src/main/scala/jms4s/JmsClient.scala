package jms4s

import cats.effect.{ Concurrent, ContextShift, Resource }
import jms4s.config.DestinationName
import jms4s.jms._

class JmsClient[F[_]: ContextShift: Concurrent] {

  def createTransactedConsumer(
    context: JmsContext[F],
    inputDestinationName: DestinationName,
    concurrencyLevel: Int
  ): Resource[F, JmsTransactedConsumer[F]] =
    JmsTransactedConsumer.make(context, inputDestinationName, concurrencyLevel)

  def createAutoAcknowledgerConsumer(
    context: JmsContext[F],
    inputDestinationName: DestinationName,
    concurrencyLevel: Int
  ): Resource[F, JmsAutoAcknowledgerConsumer[F]] =
    JmsAutoAcknowledgerConsumer.make(context, inputDestinationName, concurrencyLevel)

  def createAcknowledgerConsumer(
    context: JmsContext[F],
    inputDestinationName: DestinationName,
    concurrencyLevel: Int
  ): Resource[F, JmsAcknowledgerConsumer[F]] =
    JmsAcknowledgerConsumer.make(context, inputDestinationName, concurrencyLevel)

  def createProducer(
    context: JmsContext[F],
    concurrencyLevel: Int
  ): Resource[F, JmsProducer[F]] =
    JmsProducer.make(context, concurrencyLevel)

}
