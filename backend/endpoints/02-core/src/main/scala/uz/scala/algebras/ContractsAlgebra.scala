package uz.scala.algebras

import cats.effect.MonadCancelThrow
import cats.implicits._
import doobie.syntax.connectionio._
import fs2.Stream
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.aws.s3.S3Client
import uz.scala.domain.AuthedUser
import uz.scala.domain.ContractId
import uz.scala.domain.ListingId
import uz.scala.domain.enums.ListingStatus
import uz.scala.effects.Calendar
import uz.scala.effects.GenUUID
import uz.scala.exception.AError
import uz.scala.repos.ContractsRepository
import uz.scala.repos.ListingsRepository
import uz.scala.repos.UsersRepository
import uz.scala.repos.dto
import uz.scala.services.PdfService
import uz.scala.shared.ResponseMessages._
import uz.scala.utils.ID

trait ContractsAlgebra[F[_]] {
  def generate(listingId: ListingId)(implicit user: AuthedUser, lang: Language): F[String]
}

object ContractsAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID: Logger: fs2.Compiler.Target](
      contractsRepository: ContractsRepository[doobie.ConnectionIO],
      listingsRepository: ListingsRepository[doobie.ConnectionIO],
      usersRepository: UsersRepository[doobie.ConnectionIO],
      s3Client: S3Client[F],
      pdfService: PdfService[F],
    )(implicit
      xa: doobie.Transactor[F]
    ): ContractsAlgebra[F] =
    new Impl[F](contractsRepository, listingsRepository, usersRepository, s3Client, pdfService)

  private class Impl[F[_]: MonadCancelThrow: GenUUID: Calendar: fs2.Compiler.Target](
      contractsRepository: ContractsRepository[doobie.ConnectionIO],
      listingsRepository: ListingsRepository[doobie.ConnectionIO],
      usersRepository: UsersRepository[doobie.ConnectionIO],
      s3Client: S3Client[F],
      pdfService: PdfService[F],
    )(implicit
      logger: Logger[F],
      xa: doobie.Transactor[F],
    ) extends ContractsAlgebra[F] {
    override def generate(
        listingId: ListingId
      )(implicit
        user: AuthedUser,
        lang: Language,
      ): F[String] =
      for {
        _ <- logger.info(s"Generating contract for listing: $listingId by user ${user.id}")

        // Check listing exists and is APPROVED
        listingOpt <- listingsRepository.findById(listingId).transact(xa)
        listing <- listingOpt.fold(
          AError.BadRequest(LISTING_NOT_FOUND(lang)).raiseError[F, dto.Listing]
        )(_.pure[F])

        _ <-
          if (listing.status != ListingStatus.Approved)
            AError.BadRequest(LISTING_NOT_APPROVED(lang)).raiseError[F, Unit]
          else
            ().pure[F]

        // Get listing owner info
        ownerOpt <- usersRepository.findById(listing.ownerId).transact(xa)
        owner <- ownerOpt.fold(
          AError.Internal("Owner not found").raiseError[F, dto.User]
        )(_.pure[F])

        // Generate contract content
        contractContent = generateContractContent(listing, owner, user)
        _ <- logger.info("Contract content generated")

        // Generate PDF bytes
        pdfBytes <- pdfService.generateContractPdf(contractContent)
        _ <- logger.info(s"PDF generated, size: ${pdfBytes.length} bytes")

        // Generate contract ID and S3 key
        contractId <- ID.make[F, ContractId]
        s3Key = s"contracts/${contractId.value}.pdf"

        // Upload to S3 with public read access
        _ <- Stream
          .emits(pdfBytes)
          .covary[F]
          .through(s3Client.putObjectPublic(s3Key, pdfBytes.length.toLong))
          .compile
          .drain
        _ <- logger.info(s"PDF uploaded to S3: $s3Key")

        // Get public URL
        pdfUrl <- s3Client.generatePublicUrl(s3Key)
        _ <- logger.info(s"Public URL generated: $pdfUrl")

        // Save contract metadata to database
        now <- Calendar[F].currentZonedDateTime
        contract = dto.Contract(
          id = contractId,
          listingId = listingId,
          pdfUrl = pdfUrl,
          generatedBy = user.id,
          createdAt = now,
        )

        _ <- contractsRepository.create(contract).transact(xa)
        _ <- logger.info(s"Contract saved to database: $contractId")
      } yield pdfUrl

    private def generateContractContent(
        listing: dto.Listing,
        owner: dto.User,
        tenant: AuthedUser,
      ): String = {
      // Format price as "1,000,000 UZS"
      val priceFormatted = f"${listing.price.amount}%,.0f ${listing.price.currency.code}"

      s"""
         |UY-JOY IJARASI SHARTNOMASI
         |
         |Shartnoma raqami: ${java.util.UUID.randomUUID()}
         |Sana: ${java.time.LocalDate.now()}
         |
         |1. TOMONLAR
         |
         |Ijara beruvchi (Egasi):
         |F.I.O: ${owner.firstName.value} ${owner.lastName.value}
         |Email: ${owner.email.value}
         |Telefon: ${owner.phone.value}
         |
         |Ijara oluvchi:
         |F.I.O: ${tenant.name.value}
         |Email: ${tenant.email.value}
         |
         |2. IJARAGA BERILADIGAN MUL'K
         |
         |Manzil: ${listing.city.value}
         |Tavsif: ${listing.title.value}
         |
         |${listing.description.value}
         |
         |3. IJARA HAQI
         |
         |Oylik to'lov: $priceFormatted
         |
         |4. SHARTNOMA MUDDATI
         |
         |Shartnoma 1 yil muddatga tuziladi.
         |
         |5. IMZOLAR
         |
         |Ijara beruvchi: _____________
         |Ijara oluvchi: _____________
         |
         |""".stripMargin
    }
  }
}
