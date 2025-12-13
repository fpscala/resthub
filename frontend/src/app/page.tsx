'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useListings } from '@/hooks/useListings';
import { ListingCard } from '@/components/ListingCard';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { ListingsQueryParams } from '@/types';

/**
 * Home page - shows latest listings with search
 *
 * Features:
 * - Latest listings grid
 * - Search by city and price range
 * - Responsive layout (1-3 columns)
 */
export default function HomePage() {
  const router = useRouter();
  const [filters, setFilters] = useState<ListingsQueryParams>({
    city: '',
    minPrice: undefined,
    maxPrice: undefined,
    page: 1,
    size: 12,
  });

  const { data, isLoading, error } = useListings(filters);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    // Trigger refetch with current filters
    setFilters({ ...filters, page: 1 });
  };

  return (
    <div className="container-custom py-8">
      {/* Hero Section */}
      <section className="mb-12 text-center">
        <h1 className="text-4xl font-bold text-gray-900 sm:text-5xl">
          Find Your Perfect Rental
        </h1>
        <p className="mt-4 text-lg text-gray-600">
          Browse thousands of properties available for rent in your area
        </p>
      </section>

      {/* Search Section */}
      <section className="mb-12">
        <form
          onSubmit={handleSearch}
          className="mx-auto max-w-4xl rounded-lg border border-gray-200 bg-white p-6 shadow-card"
        >
          <div className="grid gap-4 md:grid-cols-3">
            <Input
              label="City"
              placeholder="e.g., San Francisco"
              value={filters.city || ''}
              onChange={(e) => setFilters({ ...filters, city: e.target.value })}
            />
            <Input
              label="Min Price"
              type="number"
              placeholder="e.g., 1000"
              value={filters.minPrice || ''}
              onChange={(e) =>
                setFilters({
                  ...filters,
                  minPrice: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            />
            <Input
              label="Max Price"
              type="number"
              placeholder="e.g., 3000"
              value={filters.maxPrice || ''}
              onChange={(e) =>
                setFilters({
                  ...filters,
                  maxPrice: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            />
          </div>
          <div className="mt-4">
            <Button type="submit" className="w-full md:w-auto">
              Search Listings
            </Button>
          </div>
        </form>
      </section>

      {/* Listings Grid */}
      <section>
        <h2 className="mb-6 text-2xl font-bold text-gray-900">Latest Listings</h2>

        {isLoading ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="h-80 animate-pulse rounded-lg bg-gray-200" />
            ))}
          </div>
        ) : error ? (
          <div className="rounded-lg bg-red-50 p-4 text-red-800">
            Error loading listings. Please try again.
          </div>
        ) : data && data.data.length > 0 ? (
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {data.data.map((listing) => (
              <ListingCard
                key={listing.id}
                listing={listing}
                onClick={() => router.push(`/listings/${listing.id}`)}
              />
            ))}
          </div>
        ) : (
          <div className="rounded-lg border border-gray-200 bg-white p-12 text-center">
            <p className="text-gray-600">No listings found. Try adjusting your filters.</p>
          </div>
        )}
      </section>
    </div>
  );
}
