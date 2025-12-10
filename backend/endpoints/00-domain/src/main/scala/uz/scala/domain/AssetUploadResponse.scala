package uz.scala.domain

import java.util.UUID

import io.circe.generic.JsonCodec

@JsonCodec
case class AssetUploadResponse(
    id: UUID,
    publicUrl: String,
  )
