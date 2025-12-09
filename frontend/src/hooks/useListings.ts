import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { get, post } from '@/lib/api-client';
import {
  Listing,
  PaginatedResponse,
  ListingsQueryParams,
  CreateListingRequest,
  CreateListingResponse,
} from '@/types';
import toast from 'react-hot-toast';
import { useRouter } from 'next/navigation';

/**
 * Query keys for listings
 */
export const listingsKeys = {
  all: ['listings'] as const,
  lists: () => [...listingsKeys.all, 'list'] as const,
  list: (params: ListingsQueryParams) => [...listingsKeys.lists(), params] as const,
  details: () => [...listingsKeys.all, 'detail'] as const,
  detail: (id: string) => [...listingsKeys.details(), id] as const,
};

/**
 * Fetch paginated listings with filters
 *
 * Usage:
 * ```tsx
 * const { data, isLoading, error } = useListings({
 *   city: 'San Francisco',
 *   minPrice: 1000,
 *   maxPrice: 3000,
 *   page: 1,
 *   size: 12,
 * });
 * ```
 */
export function useListings(params: ListingsQueryParams = {}) {
  return useQuery({
    queryKey: listingsKeys.list(params),
    queryFn: async (): Promise<PaginatedResponse<Listing>> => {
      // TODO: Replace with actual API call once backend is ready
      // return get<PaginatedResponse<Listing>>('/listings', params);

      // Mock data for development
      await new Promise((resolve) => setTimeout(resolve, 500));
      return {
        items: mockListings.filter((listing) => {
          if (params.city && listing.city !== params.city) return false;
          if (params.minPrice && listing.price < params.minPrice) return false;
          if (params.maxPrice && listing.price > params.maxPrice) return false;
          return true;
        }),
        total: mockListings.length,
        page: params.page || 1,
        size: params.size || 12,
      };
    },
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
}

/**
 * Fetch single listing by ID
 *
 * Usage:
 * ```tsx
 * const { data: listing, isLoading } = useListing('listing-id-123');
 * ```
 */
export function useListing(id: string) {
  return useQuery({
    queryKey: listingsKeys.detail(id),
    queryFn: async (): Promise<Listing> => {
      // TODO: Replace with actual API call
      // return get<Listing>(`/listings/${id}`);

      // Mock data
      await new Promise((resolve) => setTimeout(resolve, 300));
      const listing = mockListings.find((l) => l.id === id);
      if (!listing) throw new Error('Listing not found');
      return listing;
    },
    enabled: !!id,
    staleTime: 1000 * 60 * 5,
  });
}

/**
 * Create new listing mutation
 *
 * Usage:
 * ```tsx
 * const createListing = useCreateListing();
 *
 * const handleSubmit = async (data: CreateListingRequest) => {
 *   await createListing.mutateAsync(data);
 * };
 * ```
 */
export function useCreateListing() {
  const queryClient = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: async (data: CreateListingRequest): Promise<CreateListingResponse> => {
      // TODO: Replace with actual API call
      // return post<CreateListingResponse>('/listings', data);

      // Mock response
      await new Promise((resolve) => setTimeout(resolve, 1000));
      return { id: `listing-${Date.now()}` };
    },
    onSuccess: (data) => {
      // Invalidate listings cache to refetch
      queryClient.invalidateQueries({ queryKey: listingsKeys.lists() });
      toast.success('Listing created successfully!');
      router.push(`/listings/${data.id}`);
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to create listing');
    },
  });
}

// Mock data for development
const mockListings: Listing[] = [
  {
    id: '1',
    ownerId: 'user-1',
    title: 'Modern Downtown Apartment',
    description: 'Beautiful 2BR apartment in the heart of downtown with amazing city views.',
    price: 2500,
    city: 'San Francisco',
    images: ['https://via.placeholder.com/400x300?text=Apt+1'],
    status: 'APPROVED',
    createdAt: new Date().toISOString(),
  },
  {
    id: '2',
    ownerId: 'user-2',
    title: 'Cozy Studio Near Park',
    description: 'Perfect studio for one person, close to Central Park.',
    price: 1800,
    city: 'New York',
    images: ['https://via.placeholder.com/400x300?text=Studio'],
    status: 'APPROVED',
    createdAt: new Date().toISOString(),
  },
  {
    id: '3',
    ownerId: 'user-1',
    title: 'Spacious 3BR House',
    description: 'Family-friendly house with backyard and garage.',
    price: 3200,
    city: 'Austin',
    images: ['https://via.placeholder.com/400x300?text=House'],
    status: 'PENDING',
    createdAt: new Date().toISOString(),
  },
];
