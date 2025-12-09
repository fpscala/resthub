'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useListings } from '@/hooks/useListings';
import { ListingCard } from '@/components/ListingCard';
import { Pagination } from '@/components/Pagination';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { ListingsQueryParams } from '@/types';

/**
 * Listings page - browse all listings with filters and pagination
 *
 * Features:
 * - Filterable by city, price range
 * - Paginated results
 * - Responsive grid layout
 */
export default function ListingsPage() {
  const router = useRouter();
  const [filters, setFilters] = useState<ListingsQueryParams>({
    city: '',
    minPrice: undefined,
    maxPrice: undefined,
    page: 1,
    size: 12,
  });

  const { data, isLoading, error } = useListings(filters);

  const totalPages = data ? Math.ceil(data.total / (filters.size || 12)) : 0;

  const handlePageChange = (page: number) => {
    setFilters({ ...filters, page });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setFilters({ ...filters, page: 1 });
  };

  return (
    <div className="container-custom py-8">
      <h1 className="mb-8 text-3xl font-bold text-gray-900">Browse Listings</h1>

      {/* Filters */}
      <div className="mb-8">
        <form
          onSubmit={handleSearch}
          className="rounded-lg border border-gray-200 bg-white p-6 shadow-card"
        >
          <div className="grid gap-4 md:grid-cols-4">
            <Input
              label="City"
              placeholder="Filter by city"
              value={filters.city || ''}
              onChange={(e) => setFilters({ ...filters, city: e.target.value })}
            />
            <Input
              label="Min Price"
              type="number"
              placeholder="Min price"
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
              placeholder="Max price"
              value={filters.maxPrice || ''}
              onChange={(e) =>
                setFilters({
                  ...filters,
                  maxPrice: e.target.value ? Number(e.target.value) : undefined,
                })
              }
            />
            <div className="flex items-end">
              <Button type="submit" className="w-full">
                Apply Filters
              </Button>
            </div>
          </div>
        </form>
      </div>

      {/* Results */}
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
      ) : data && data.items.length > 0 ? (
        <>
          <div className="mb-4 text-sm text-gray-600">
            Showing {data.items.length} of {data.total} listings
          </div>

          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {data.items.map((listing) => (
              <ListingCard
                key={listing.id}
                listing={listing}
                onClick={() => router.push(`/listings/${listing.id}`)}
              />
            ))}
          </div>

          {/* Pagination */}
          <div className="mt-12">
            <Pagination
              currentPage={filters.page || 1}
              totalPages={totalPages}
              onPageChange={handlePageChange}
            />
          </div>
        </>
      ) : (
        <div className="rounded-lg border border-gray-200 bg-white p-12 text-center">
          <p className="text-gray-600">No listings found matching your criteria.</p>
        </div>
      )}
    </div>
  );
}
