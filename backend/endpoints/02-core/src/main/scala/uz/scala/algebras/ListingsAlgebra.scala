package uz.scala.algebras

import cats.effect.MonadCancelThrow
import cats.implicits._
import doobie.syntax.connectionio._
import io.scalaland.chimney.dsl._
import org.typelevel.log4cats.Logger

import uz.scala.Language
import uz.scala.domain.AuthedUser
import uz.scala.domain.ListingId
import uz.scala.domain.ResponseData
import uz.scala.domain.enums.ListingStatus
import uz.scala.domain.listings._
import uz.scala.effects.Calendar
import uz.scala.effects.GenUUID
import uz.scala.exception.AError
import uz.scala.repos.ListingsRepository
import uz.scala.repos.UsersRepository
import uz.scala.repos.dto
import uz.scala.shared.ResponseMessages._
import uz.scala.syntax.refined._
import uz.scala.utils.ID

trait ListingsAlgebra[F[_]] {
  def create(input: CreateListingInput)(implicit user: AuthedUser, lang: Language): F[ListingId]
  def findById(id: ListingId)(implicit lang: Language): F[ListingOutput]
  def search(filters: ListingFilters): F[ResponseData[ListingOutput]]
  def myListings(implicit user: AuthedUser): F[List[ListingOutput]]
  def delete(id: ListingId)(implicit user: AuthedUser, lang: Language): F[Unit]
}

object ListingsAlgebra {
  def make[F[_]: MonadCancelThrow: Calendar: GenUUID: Logger](
      listingsRepository: ListingsRepository[doobie.ConnectionIO],
      usersRepository: UsersRepository[doobie.ConnectionIO],
    )(implicit
      xa: doobie.Transactor[F]
    ): ListingsAlgebra[F] =
    new Impl[F](listingsRepository, usersRepository)

  private class Impl[F[_]: MonadCancelThrow: GenUUID: Calendar](
      listingsRepository: ListingsRepository[doobie.ConnectionIO],
      usersRepository: UsersRepository[doobie.ConnectionIO],
    )(implicit
      logger: Logger[F],
      xa: doobie.Transactor[F],
    ) extends ListingsAlgebra[F] {
    override def create(
        input: CreateListingInput
      )(implicit
        user: AuthedUser,
        lang: Language,
      ): F[ListingId] =
      for {
        _ <- logger.info(s"Creating listing: ${input.title} by user ${user.id}")

        // Validate images list not empty
        _ <-
          if (input.images.isEmpty)
            AError.BadRequest(INVALID_IMAGES(lang)).raiseError[F, Unit]
          else
            ().pure[F]

        // Validate price is positive
        _ <-
          if (input.price.amount <= 0)
            AError.BadRequest(INVALID_PRICE(lang)).raiseError[F, Unit]
          else
            ().pure[F]

        // Generate listing ID
        listingId <- ID.make[F, ListingId]
        now <- Calendar[F].currentZonedDateTime

        // Create listing with PENDING status
        listing = dto.Listing(
          id = listingId,
          ownerId = user.id,
          title = input.title,
          description = input.description,
          price = input.price,
          city = input.city,
          images = input.images,
          status = ListingStatus.Pending,
          rejectionReason = None,
          createdAt = now,
          updatedAt = now,
          approvedAt = None,
          approvedBy = None,
        )

        _ <- listingsRepository.create(listing).transact(xa)
        _ <- logger.info(s"Listing created successfully: $listingId")
      } yield listingId

    override def findById(id: ListingId)(implicit lang: Language): F[ListingOutput] =
      for {
        _ <- logger.info(s"Finding listing by id: $id")

        now <- Calendar[F].currentZonedDateTime
        listingOpt <- listingsRepository.findById(id).transact(xa)
        listing <- listingOpt.fold(
          AError.BadRequest(LISTING_NOT_FOUND(lang)).raiseError[F, dto.Listing]
        )(_.pure[F])

        // Get owner info
        ownerOpt <- usersRepository.findById(listing.ownerId).transact(xa)
        owner <- ownerOpt.fold(
          AError.Internal("Owner not found").raiseError[F, dto.User]
        )(_.pure[F])

        // Convert to domain User - TODO: Load role from database based on owner.roleId
        // For now, we'll create a dummy role
        ownerDomain = owner.toDomain(
          uz.scala
            .domain
            .users
            .Role(
              id = owner.roleId,
              name = "USER", // TODO: Load from database
              privileges = List.empty, // TODO: Load from database
              description = None,
              isSystem = false,
              createdAt = owner.createdAt,
              updatedAt = None,
            )
        )

        // Convert to ListingOutput
        output = listing.toDomain(ownerDomain)
      } yield output

    override def search(filters: ListingFilters): F[ResponseData[ListingOutput]] =
      for {
        _ <- logger.info(s"Searching listings with filters: $filters")

        // Force status to APPROVED for public search
        publicFilters = filters.copy(status = Some(ListingStatus.Approved))

        // Get listings
        listings <- listingsRepository.findByFilters(publicFilters).transact(xa)
        total <- listingsRepository.count(publicFilters).transact(xa)

        // Get all unique owner IDs
        ownerIds = listings.map(_.ownerId).distinct

        // Fetch all owners in one query
        owners <- ownerIds
          .traverse { ownerId =>
            usersRepository.findById(ownerId).transact(xa).map(owner => ownerId -> owner)
          }
          .map(_.toMap)

        // Convert to ListingOutput
        outputs = listings.map { listing =>
          val ownerOpt = owners.get(listing.ownerId).flatten
          val ownerDomain = ownerOpt
            .map { owner =>
              owner
                .into[uz.scala.domain.users.User]
                .withFieldComputed(_.role, _ => ???) // TODO: Load role
                .transform
            }
            .getOrElse(???) // Should not happen

          listing
            .into[ListingOutput]
            .withFieldConst(_.owner, ownerDomain)
            .transform
        }

      } yield ResponseData(outputs, total)

    override def myListings(implicit user: AuthedUser): F[List[ListingOutput]] =
      for {
        _ <- logger.info(s"Getting listings for user: ${user.id}")

        // Get user's listings (all statuses)
        filters = ListingFilters(status = None, page = None, size = None)
        allListings <- listingsRepository.findByFilters(filters).transact(xa)

        // Filter by owner
        userListings = allListings.filter(_.ownerId == user.id)

        // Get owner info (current user)
        ownerOpt <- usersRepository.findById(user.id).transact(xa)
        owner <- ownerOpt.fold(
          AError.Internal("Owner not found").raiseError[F, dto.User]
        )(_.pure[F])

        ownerDomain = owner.toDomain(user.role)

        // Convert to ListingOutput
        outputs = userListings.map(_.toDomain(ownerDomain))
      } yield outputs

    override def delete(
        id: ListingId
      )(implicit
        user: AuthedUser,
        lang: Language,
      ): F[Unit] =
      for {
        _ <- logger.info(s"Deleting listing: $id by user ${user.id}")

        // Check listing exists
        listingOpt <- listingsRepository.findById(id).transact(xa)
        listing <- listingOpt.fold(
          AError.BadRequest(LISTING_NOT_FOUND(lang)).raiseError[F, dto.Listing]
        )(_.pure[F])

        // Check user is owner
        _ <-
          if (listing.ownerId != user.id)
            AError.NotAllowed(NOT_LISTING_OWNER(lang)).raiseError[F, Unit]
          else
            ().pure[F]

        // Delete listing
        _ <- listingsRepository.delete(id).transact(xa)
        _ <- logger.info(s"Listing deleted successfully: $id")
      } yield ()
  }
}
