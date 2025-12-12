package uz.scala.algebras

import cats.effect.MonadCancelThrow
import cats.implicits._
import doobie.syntax.connectionio._
import org.typelevel.log4cats.Logger

import uz.scala.Language
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
import uz.scala.shared.ResponseMessages._
import uz.scala.utils.ID

trait ContractsAlgebra[F[_]] {
  def generate(listingId: ListingId)(implicit user: AuthedUser, lang: Language): F[String]
}

object ContractsAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID: Logger](
      contractsRepository: ContractsRepository[doobie.ConnectionIO],
      listingsRepository: ListingsRepository[doobie.ConnectionIO],
      usersRepository: UsersRepository[doobie.ConnectionIO],
      // s3Client: S3Client[F], // TODO: Add S3 client for uploading PDF
    )(implicit
      xa: doobie.Transactor[F]
    ): ContractsAlgebra[F] =
    new Impl[F](contractsRepository, listingsRepository, usersRepository)

  private class Impl[F[_]: MonadCancelThrow: GenUUID: Calendar](
      contractsRepository: ContractsRepository[doobie.ConnectionIO],
      listingsRepository: ListingsRepository[doobie.ConnectionIO],
      usersRepository: UsersRepository[doobie.ConnectionIO],
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
          AError.NotFound(LISTING_NOT_FOUND(lang)).raiseError[F, dto.Listing]
        )(_.pure[F])

        _ <- if (listing.status != ListingStatus.Approved) {
          AError.BadRequest(LISTING_NOT_APPROVED(lang)).raiseError[F, Unit]
        } else {
          ().pure[F]
        }

        // Get listing owner info
        ownerOpt <- usersRepository.findById(listing.ownerId).transact(xa)
        owner <- ownerOpt.fold(
          AError.Internal("Owner not found").raiseError[F, dto.User]
        )(_.pure[F])

        // TODO: Generate PDF using Apache PDFBox or external service (Gotenberg)
        // For MVP, we'll create a simple text-based contract
        contractContent = generateContractContent(listing, owner, user)

        // TODO: Upload PDF to S3 and get public URL
        // For now, we'll use a placeholder URL
        contractId <- ID.make[F, ContractId]
        pdfUrl = s"https://nesthub-contracts.s3.amazonaws.com/${contractId.value}.pdf"

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
        _ <- logger.info(s"Contract generated successfully: $contractId")
      } yield pdfUrl

    private def generateContractContent(
        listing: dto.Listing,
        owner: dto.User,
        tenant: AuthedUser,
      ): String = {
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
         |Oylik to'lov: ${listing.price} UZS
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
