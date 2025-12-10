package uz.scala.kafka

import scala.concurrent.duration._

import pureconfig.ConfigReader
import pureconfig.generic.semiauto._

/** Kafka configuration
  *
  * @param bootstrapServers Kafka bootstrap servers (e.g., "localhost:9092")
  * @param topics Topic names configuration
  * @param consumer Consumer settings
  * @param producer Producer settings
  */
case class KafkaConfig(
    bootstrapServers: String,
    topics: KafkaConfig.Topics,
    consumer: KafkaConfig.ConsumerConfig,
    producer: KafkaConfig.ProducerConfig,
  )

object KafkaConfig {
  implicit val reader: ConfigReader[KafkaConfig] = deriveReader

  /** Topic names configuration
    */
  case class Topics(
      draftApproved: String
    )

  object Topics {
    implicit val reader: ConfigReader[Topics] = deriveReader
  }

  /** Consumer configuration
    */
  case class ConsumerConfig(
      groupIdPrefix: String = "uzbek-jobs",
      maxPollRecords: Int = 100,
      sessionTimeout: FiniteDuration = 30.seconds,
      heartbeatInterval: FiniteDuration = 3.seconds,
      enableAutoCommit: Boolean = false,
    )

  object ConsumerConfig {
    implicit val reader: ConfigReader[ConsumerConfig] = deriveReader
  }

  /** Producer configuration
    */
  case class ProducerConfig(
      acks: String = "all",
      retries: Int = 3,
      batchSize: Int = 16384,
      lingerMs: Int = 10,
      bufferMemory: Long = 33554432L,
      compressionType: String = "snappy",
    )

  object ProducerConfig {
    implicit val reader: ConfigReader[ProducerConfig] = deriveReader
  }
}
