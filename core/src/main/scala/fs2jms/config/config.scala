package fs2jms.config

case class Config(
  qm: QueueManager,
  endpoints: List[Endpoint],
  channel: Channel,
  username: Option[Username] = None,
  password: Option[Password] = None
)

case class Username(value: String) extends AnyVal

case class Password(value: String) extends AnyVal

case class Endpoint(host: String, port: Int)

case class QueueName(value: String) extends AnyVal

case class QueueManager(value: String) extends AnyVal

case class Channel(value: String) extends AnyVal