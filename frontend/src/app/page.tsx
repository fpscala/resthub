'use client';

import { useRouter } from 'next/navigation';
import { useListings } from '@/hooks/useListings';
import { ListingCard } from '@/components/ListingCard';
import { Button } from '@/components/ui/Button';

/**
 * Home page - landing page with platform info and featured listings
 *
 * Features:
 * - Hero section with platform description
 * - How it works section
 * - Featured/Latest listings (limited to 6)
 * - Call to action buttons
 */
export default function HomePage() {
  const router = useRouter();

  // Fetch only 6 latest listings for preview
  const { data, isLoading } = useListings({ page: 1, size: 6 });

  return (
    <div>
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-blue-50 via-white to-purple-50 py-20">
        <div className="container-custom">
          <div className="mx-auto max-w-3xl text-center">
            <h1 className="text-5xl font-bold text-gray-900 sm:text-6xl">
              Find Your Perfect Rental Home
            </h1>
            <p className="mt-6 text-xl text-gray-600">
              NestHub connects renters and property owners across Uzbekistan.
              Discover your next home or list your property with ease.
            </p>
            <div className="mt-10 flex flex-wrap justify-center gap-4">
              <Button
                size="lg"
                onClick={() => router.push('/listings')}
              >
                Browse Properties
              </Button>
              <Button
                size="lg"
                variant="secondary"
                onClick={() => router.push('/create')}
              >
                List Your Property
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="bg-white py-16">
        <div className="container-custom">
          <div className="text-center">
            <h2 className="text-3xl font-bold text-gray-900">How NestHub Works</h2>
            <p className="mt-4 text-lg text-gray-600">
              Simple, transparent, and secure rental process
            </p>
          </div>

          <div className="mt-12 grid gap-8 md:grid-cols-3">
            {/* Step 1 */}
            <div className="text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-blue-100">
                <svg className="h-8 w-8 text-blue-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
              </div>
              <h3 className="mt-4 text-xl font-semibold text-gray-900">Search & Discover</h3>
              <p className="mt-2 text-gray-600">
                Browse verified listings with detailed photos and descriptions
              </p>
            </div>

            {/* Step 2 */}
            <div className="text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
                <svg className="h-8 w-8 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <h3 className="mt-4 text-xl font-semibold text-gray-900">Connect & Communicate</h3>
              <p className="mt-2 text-gray-600">
                Contact property owners directly through our platform
              </p>
            </div>

            {/* Step 3 */}
            <div className="text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-purple-100">
                <svg className="h-8 w-8 text-purple-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <h3 className="mt-4 text-xl font-semibold text-gray-900">Secure Your Home</h3>
              <p className="mt-2 text-gray-600">
                Generate contracts and finalize your rental agreement safely
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Featured Listings Section */}
      <section className="bg-gray-50 py-16">
        <div className="container-custom">
          <div className="mb-8 flex items-center justify-between">
            <div>
              <h2 className="text-3xl font-bold text-gray-900">Featured Listings</h2>
              <p className="mt-2 text-gray-600">Discover the latest properties available for rent</p>
            </div>
            <Button
              variant="secondary"
              onClick={() => router.push('/listings')}
            >
              View All
            </Button>
          </div>

          {isLoading ? (
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {[...Array(6)].map((_, i) => (
                <div key={i} className="h-80 animate-pulse rounded-lg bg-gray-200" />
              ))}
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
              <p className="text-gray-600">No listings available at the moment. Check back soon!</p>
            </div>
          )}

          {data && data.data.length > 0 && (
            <div className="mt-8 text-center">
              <Button
                size="lg"
                onClick={() => router.push('/listings')}
              >
                Explore All Properties
              </Button>
            </div>
          )}
        </div>
      </section>

      {/* CTA Section */}
      <section className="bg-blue-600 py-16">
        <div className="container-custom text-center">
          <h2 className="text-3xl font-bold text-white sm:text-4xl">
            Ready to Find Your Next Home?
          </h2>
          <p className="mt-4 text-xl text-blue-100">
            Join thousands of renters and property owners on NestHub
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-4">
            <Button
              size="lg"
              variant="secondary"
              onClick={() => router.push('/auth/register')}
              className="bg-white text-blue-600 hover:bg-gray-100"
            >
              Get Started Free
            </Button>
            <Button
              size="lg"
              onClick={() => router.push('/listings')}
              className="border-2 border-white bg-transparent text-white hover:bg-white hover:text-blue-600"
            >
              Browse Listings
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}
