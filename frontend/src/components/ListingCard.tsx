'use client';

import Image from 'next/image';
import Link from 'next/link';
import { Listing } from '@/types';
import { formatPrice } from '@/lib/utils';
import { cn } from '@/lib/utils';

export interface ListingCardProps {
  listing: Listing;
  onClick?: () => void;
  className?: string;
}

/**
 * Listing card component for displaying property previews
 *
 * Usage:
 * ```tsx
 * <ListingCard
 *   listing={listing}
 *   onClick={() => router.push(`/listings/${listing.id}`)}
 * />
 * ```
 */
export function ListingCard({ listing, onClick, className }: ListingCardProps) {
  const imageUrl = listing.images[0] || 'https://via.placeholder.com/400x300?text=No+Image';
  const isForSale = listing.listingType === 'FOR_SALE';
  const priceLabel = isForSale ? '' : '/mo';

  const content = (
    <div
      className={cn(
        'card group cursor-pointer overflow-hidden transition-all',
        className
      )}
      onClick={onClick}
      role="article"
      aria-label={`Listing: ${listing.title}`}
    >
      {/* Image */}
      <div className="relative h-48 w-full overflow-hidden rounded-lg bg-gray-200">
        <Image
          src={imageUrl}
          alt={listing.title}
          fill
          className="object-cover transition-transform group-hover:scale-105"
          sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
        />
        {/* Listing Type Badge */}
        <span className={`absolute left-2 top-2 rounded-full px-2 py-1 text-xs font-semibold text-white ${
          isForSale ? 'bg-green-600' : 'bg-blue-600'
        }`}>
          {isForSale ? 'Sale' : 'Rent'}
        </span>
        {listing.status === 'PENDING' && (
          <span className="absolute right-2 top-2 rounded-full bg-yellow-500 px-2 py-1 text-xs font-semibold text-white">
            Pending
          </span>
        )}
      </div>

      {/* Content */}
      <div className="mt-4">
        <h3 className="line-clamp-1 text-lg font-semibold text-gray-900 group-hover:text-primary-600">
          {listing.title}
        </h3>
        <p className="mt-1 line-clamp-2 text-sm text-gray-600">{listing.description}</p>

        {/* Property quick info */}
        <div className="mt-2 flex items-center gap-3 text-xs text-gray-500">
          {listing.rooms && (
            <span className="flex items-center gap-1">
              <span>🛏</span> {listing.rooms} rooms
            </span>
          )}
          {listing.district && (
            <span className="flex items-center gap-1">
              <span>📍</span> {listing.district}
            </span>
          )}
        </div>

        <div className="mt-3 flex items-center justify-between">
          <span className="text-2xl font-bold text-primary-600">
            {formatPrice(listing.price)}
            <span className="text-sm font-normal text-gray-500">{priceLabel}</span>
          </span>
          <span className="rounded-full bg-gray-100 px-3 py-1 text-xs font-medium text-gray-700">
            {listing.city}
          </span>
        </div>
      </div>
    </div>
  );

  // Wrap in Link if we're not using custom onClick
  if (!onClick) {
    return <Link href={`/listings/${listing.id}`}>{content}</Link>;
  }

  return content;
}
