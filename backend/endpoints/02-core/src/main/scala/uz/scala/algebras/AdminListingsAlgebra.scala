package uz.scala.algebras

import cats.effect.MonadCancelThrow
import cats.implicits._
import doobie.syntax.connectionio._
import io.scalaland.chimney.dsl._
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.domain.AuthedUser
import uz.scala.domain.ListingId
import uz.scala.domain.enums.{ListingStatus, Privilege}
import uz.scala.domain.listings._
import uz.scala.effects.Calendar
import uz.scala.exception.AError
import uz.scala.repos.ListingsRepository
import uz.scala.repos.UsersRepository
import uz.scala.repos.dto
import uz.scala.shared.ResponseMessages._

trait AdminListingsAlgebra[F[_]] {
  def getAllListings(
      filters: ListingFilters
    )(implicit
      user: AuthedUser,
      lang: Language,
    ): F[PaginatedResponse[ListingOutput]]

  def approve(id: ListingId)(implicit user: AuthedUser, lang: Language): F[Unit]

  def reject(
      id: ListingId,
      reason: String,
    )(implicit
      user: AuthedUser,
      lang: Language,
    ): F[Unit]
}

object AdminListingsAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: Logger](
      listingsRepository: ListingsRepository[doobie.ConnectionIO],
      usersRepository: UsersRepository[doobie.ConnectionIO],
    )(implicit
      xa: doobie.Transactor[F]
    ): AdminListingsAlgebra[F] =
    new Impl[F](listingsRepository, usersRepository)

  private class Impl[F[_]: MonadCancelThrow: Calendar](
      listingsRepository: ListingsRepository[doobie.ConnectionIO],
      usersRepository: UsersRepository[doobie.ConnectionIO],
    )(implicit
      logger: Logger[F],
      xa: doobie.Transactor[F],
    ) extends AdminListingsAlgebra[F] {

    private def checkAdminPrivilege(
        privilege: Privilege
      )(implicit
        user: AuthedUser,
        lang: Language,
      ): F[Unit] =
      if (user.role.privileges.contains(privilege)) {
        ().pure[F]
      } else {
        AError.NotAllowed(INSUFFICIENT_PRIVILEGES_ADMIN(lang)).raiseError[F, Unit]
      }

    override def getAllListings(
        filters: ListingFilters
      )(implicit
        user: AuthedUser,
        lang: Language,
      ): F[PaginatedResponse[ListingOutput]] =
      for {
        _ <- logger.info(s"Admin getting all listings with filters: $filters")

        // Check admin privilege
        _ <- checkAdminPrivilege(Privilege.AdminListingsViewAll)

        // Get listings (with status filter if provided)
        listings <- listingsRepository.findByFilters(filters).transact(xa)
        total <- listingsRepository.count(filters).transact(xa)

        // Get all unique owner IDs
        ownerIds = listings.map(_.ownerId).distinct

        // Fetch all owners
        owners <- ownerIds.traverse { ownerId =>
          usersRepository.findById(ownerId).transact(xa).map(owner => ownerId -> owner)
        }.map(_.toMap)

        // Convert to ListingOutput
        outputs = listings.flatMap { listing =>
          owners.get(listing.ownerId).flatten.map { owner =>
            val ownerDomain = owner.into[uz.scala.domain.users.User]
              .withFieldComputed(_.role, _ => user.role) // TODO: Load actual role
              .transform

            listing
              .into[ListingOutput]
              .withFieldConst(_.owner, ownerDomain)
              .withFieldComputed(_.price, _.price.amount) // Convert Money to BigDecimal
              .transform
          }
        }

        page = filters.page.getOrElse(1)
        size = filters.size.getOrElse(20)
      } yield PaginatedResponse(outputs, total, page, size)

    override def approve(
        id: ListingId
      )(implicit
        user: AuthedUser,
        lang: Language,
      ): F[Unit] =
      for {
        _ <- logger.info(s"Admin ${user.id} approving listing: $id")

        // Check admin privilege
        _ <- checkAdminPrivilege(Privilege.AdminListingsApprove)

        // Check listing exists
        listingOpt <- listingsRepository.findById(id).transact(xa)
        listing <- listingOpt.fold(
          AError.BadRequest(LISTING_NOT_FOUND(lang)).raiseError[F, dto.Listing]
        )(_.pure[F])

        // Check listing is PENDING
        _ <- if (listing.status != ListingStatus.Pending) {
          AError.BadRequest(LISTING_NOT_PENDING(lang)).raiseError[F, Unit]
        } else {
          ().pure[F]
        }

        // Update status to APPROVED
        _ <- listingsRepository
          .updateStatus(id, ListingStatus.Approved, Some(user.id))
          .transact(xa)

        _ <- logger.info(s"Listing approved successfully: $id")
      } yield ()

    override def reject(
        id: ListingId,
        reason: String,
      )(implicit
        user: AuthedUser,
        lang: Language,
      ): F[Unit] =
      for {
        _ <- logger.info(s"Admin ${user.id} rejecting listing: $id with reason: $reason")

        // Check admin privilege
        _ <- checkAdminPrivilege(Privilege.AdminListingsReject)

        // Check listing exists
        listingOpt <- listingsRepository.findById(id).transact(xa)
        listing <- listingOpt.fold(
          AError.BadRequest(LISTING_NOT_FOUND(lang)).raiseError[F, dto.Listing]
        )(_.pure[F])

        // Check listing is PENDING
        _ <- if (listing.status != ListingStatus.Pending) {
          AError.BadRequest(LISTING_NOT_PENDING(lang)).raiseError[F, Unit]
        } else {
          ().pure[F]
        }

        // Update status to REJECTED with reason
        _ <- listingsRepository
          .updateStatus(id, ListingStatus.Rejected, Some(user.id))
          .transact(xa)

        // TODO: Update rejection_reason field
        // For now, we need to add an updateRejectionReason method to repository

        _ <- logger.info(s"Listing rejected successfully: $id")
      } yield ()
  }
}
