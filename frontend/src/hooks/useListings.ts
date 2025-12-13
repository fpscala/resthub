import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { get, post, del } from '@/lib/api-client';
import {
  Listing,
  PaginatedResponse,
  ListingsQueryParams,
  CreateListingRequest,
  CreateListingResponse,
  SuccessResponse,
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
      return get<PaginatedResponse<Listing>>('/listings', params);
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
      return get<Listing>(`/listings/${id}`);
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
      return post<CreateListingResponse>('/listings', data);
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

/**
 * Delete listing mutation
 *
 * Usage:
 * ```tsx
 * const deleteListing = useDeleteListing();
 *
 * const handleDelete = async (id: string) => {
 *   await deleteListing.mutateAsync(id);
 * };
 * ```
 */
export function useDeleteListing() {
  const queryClient = useQueryClient();
  const router = useRouter();

  return useMutation({
    mutationFn: async (id: string): Promise<SuccessResponse> => {
      return del<SuccessResponse>(`/listings/${id}`);
    },
    onSuccess: (_, id) => {
      // Invalidate listings cache to refetch
      queryClient.invalidateQueries({ queryKey: listingsKeys.lists() });
      // Remove specific listing from cache
      queryClient.removeQueries({ queryKey: listingsKeys.detail(id) });
      toast.success('Listing deleted successfully!');
      router.push('/listings');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to delete listing');
    },
  });
}

/**
 * Get my listings (for current user)
 */
export function useMyListings() {
  return useQuery({
    queryKey: ['listings', 'my'],
    queryFn: async (): Promise<{ data: Listing[] }> => {
      return get<{ data: Listing[] }>('/listings/my');
    },
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
}
