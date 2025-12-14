'use client';

import { useListing } from '@/hooks/useListings';
import { useGenerateContract } from '@/hooks/useContract';
import { Button } from '@/components/ui/Button';
import { ImageGallery } from '@/components/ImageGallery';
import { formatPrice, formatDate } from '@/lib/utils';

/**
 * Listing detail page
 *
 * Features:
 * - Image gallery
 * - Full listing details
 * - Owner contact button (phone link)
 * - Download contract button
 */
export default function ListingDetailPage({ params }: { params: { id: string } }) {
  const { id } = params;
  const { data: listing, isLoading, error } = useListing(id);
  const generateContract = useGenerateContract();

  const handleDownloadContract = async () => {
    if (!listing) return;
    await generateContract.mutateAsync({ listingId: listing.id });
  };

  if (isLoading) {
    return (
      <div className="container-custom py-12">
        <div className="animate-pulse space-y-8">
          <div className="h-96 rounded-lg bg-gray-200" />
          <div className="h-8 w-2/3 rounded bg-gray-200" />
          <div className="h-24 rounded bg-gray-200" />
        </div>
      </div>
    );
  }

  if (error || !listing) {
    return (
      <div className="container-custom py-12">
        <div className="rounded-lg bg-red-50 p-4 text-red-800">
          Listing not found or error loading listing.
        </div>
      </div>
    );
  }

  return (
    <div className="container-custom py-8">
      {/* Image Gallery */}
      <div className="mb-8">
        <ImageGallery images={listing.images} title={listing.title} />
      </div>

      <div className="grid gap-8 lg:grid-cols-3">
        {/* Main Content */}
        <div className="lg:col-span-2">
          <div className="mb-4">
            <h1 className="text-3xl font-bold text-gray-900">{listing.title}</h1>
            <div className="mt-2 flex items-center gap-4 text-sm text-gray-600">
              <span className="inline-flex items-center">
                <svg
                  className="mr-1 h-4 w-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                  />
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                  />
                </svg>
                {listing.city}
              </span>
              <span>Posted {formatDate(listing.createdAt)}</span>
            </div>
          </div>

          <div className="prose max-w-none">
            <h2 className="text-xl font-semibold text-gray-900">Description</h2>
            <p className="mt-2 whitespace-pre-line text-gray-700">{listing.description}</p>
          </div>
        </div>

        {/* Sidebar */}
        <div className="lg:col-span-1">
          <div className="sticky top-4 rounded-lg border border-gray-200 bg-white p-6 shadow-card">
            <div className="mb-6">
              <div className="text-3xl font-bold text-primary-600">
                {formatPrice(listing.price)}
                <span className="text-base font-normal text-gray-500">/month</span>
              </div>
            </div>

            {/* TODO: Add owner contact info once user relationships are implemented */}
            <Button
              variant="primary"
              className="mb-3 w-full"
              onClick={() => {
                // TODO: Replace with actual owner phone number
                window.location.href = 'tel:+1234567890';
              }}
            >
              Contact Owner
            </Button>

            <Button
              variant="secondary"
              className="w-full"
              onClick={handleDownloadContract}
              isLoading={generateContract.isPending}
            >
              Download Contract
            </Button>

            <div className="mt-6 border-t border-gray-200 pt-6">
              <h3 className="mb-3 font-semibold text-gray-900">Property Details</h3>
              <dl className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <dt className="text-gray-600">Status:</dt>
                  <dd className="font-medium text-gray-900">{listing.status}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-gray-600">Listing ID:</dt>
                  <dd className="font-medium text-gray-900">{listing.id}</dd>
                </div>
              </dl>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
