package jms4s

import cats.data.NonEmptyList
import cats.effect.{ Concurrent, ContextShift, Resource, Sync }
import cats.implicits._
import fs2.Stream
import fs2.concurrent.Queue
import jms4s.JmsAcknowledgerConsumer.AckResult
import jms4s.JmsAcknowledgerConsumer.AckResult.Destination
import jms4s.JmsAcknowledgerConsumer.JmsAcknowledgerConsumerPool.{ JmsResource, Received }
import jms4s.config.{ DestinationName, QueueName }
import jms4s.jms.{ JmsConnection, JmsMessage, JmsMessageConsumer }
import jms4s.model.SessionType.ClientAcknowledge

import scala.concurrent.duration.FiniteDuration

trait JmsAcknowledgerConsumer[F[_]] {
  def handle(f: JmsMessage[F] => F[AckResult]): F[Unit]
}

object JmsAcknowledgerConsumer {

  private[jms4s] def make[F[_]: ContextShift: Concurrent](
    connection: JmsConnection[F],
    inputDestinationName: DestinationName,
    concurrencyLevel: Int
  ): Resource[F, JmsAcknowledgerConsumer[F]] =
    for {
      input <- Resource.liftF(
                connection.createSession(ClientAcknowledge).use(_.createDestination(inputDestinationName))
              )
      queue <- Resource.liftF(Queue.bounded[F, JmsResource[F]](concurrencyLevel))
      _ <- (0 until concurrencyLevel).toList.traverse_ { _ =>
            for {
              session  <- connection.createSession(ClientAcknowledge)
              consumer <- session.createConsumer(input)
              _        <- Resource.liftF(queue.enqueue1(JmsResource(consumer, Map.empty)))
            } yield ()
          }
      pool = new JmsAcknowledgerConsumerPool(queue)
    } yield build(pool, concurrencyLevel)

  private def build[F[_]: ContextShift: Concurrent](
    pool: JmsAcknowledgerConsumerPool[F],
    concurrencyLevel: Int
  ): JmsAcknowledgerConsumer[F] =
    (f: JmsMessage[F] => F[AckResult]) =>
      Stream
        .emits(0 until concurrencyLevel)
        .as(
          Stream.eval(
            for {
              received <- pool.receive
              res      <- f(received.message)
              _ <- res match {
                    case AckResult.Ack   => Sync[F].delay(received.message.wrapped.acknowledge())
                    case AckResult.NoAck => Sync[F].unit
                    case AckResult.Send(destinations) =>
                      destinations.traverse_ {
                        case Destination(name, delay) =>
                          delay.fold(
                            ifEmpty = received.resource
                              .producers(name)
                              .publish(received.message)
                          )(
                            f = d =>
                              received.resource
                                .producers(name)
                                .publish(received.message, d)
                          )
                      }
                  }
              _ <- pool.restore(received.resource)
            } yield ()
          )
        )
        .parJoin(concurrencyLevel)
        .repeat
        .compile
        .drain

  private[jms4s] class JmsAcknowledgerConsumerPool[F[_]: Concurrent: ContextShift](pool: Queue[F, JmsResource[F]]) {

    val receive: F[Received[F]] =
      for {
        resource <- pool.dequeue1
        msg      <- resource.consumer.receiveJmsMessage
      } yield Received(msg, resource)

    def restore(resource: JmsResource[F]): F[Unit] =
      pool.enqueue1(resource)
  }

  object JmsAcknowledgerConsumerPool {

    private[jms4s] case class JmsResource[F[_]] private[jms4s] (
      consumer: JmsMessageConsumer[F],
      producers: Map[DestinationName, JmsProducer[F]]
    )

    private[jms4s] case class Received[F[_]] private (message: JmsMessage[F], resource: JmsResource[F])
  }

  sealed abstract class AckResult extends Product with Serializable

  object AckResult {

    private[jms4s] case object Ack extends AckResult

    // if the client want's to ack groups of messages, it'll pass a sequence of NoAck and then a cumulative ack
    private[jms4s] case object NoAck extends AckResult

    private[jms4s] case class Send(destinations: NonEmptyList[Destination]) extends AckResult

    private[jms4s] case class Destination(queueName: DestinationName, delay: Option[FiniteDuration])

    val ack: AckResult   = Ack
    val noAck: AckResult = NoAck

    def sendTo(queueNames: QueueName*): Send =
      Send(NonEmptyList.fromListUnsafe(queueNames.toList.map(name => Destination(name, None))))

    def sendWithDelayTo(queueNames: (QueueName, FiniteDuration)*): Send = Send(
      NonEmptyList.fromListUnsafe(
        queueNames.toList.map(x => Destination(x._1, Some(x._2)))
      )
    )
  }
}
