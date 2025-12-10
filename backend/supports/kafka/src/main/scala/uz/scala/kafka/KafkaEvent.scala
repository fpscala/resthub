package uz.scala.kafka

import io.circe.Decoder
import io.circe.Encoder

/** Base trait for all Kafka events
  */
trait KafkaEvent {
  def eventType: String
}

object KafkaEvent {
  /** Event with JSON serialization support
    */
  trait JsonEvent[A <: KafkaEvent] {
    implicit def encoder: Encoder[A]
    implicit def decoder: Decoder[A]
  }
}
