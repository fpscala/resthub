import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { get, post } from '@/lib/api-client';
import { Listing, PaginatedResponse, AdminListingsQueryParams } from '@/types';
import toast from 'react-hot-toast';

/**
 * Query keys for admin operations
 */
export const adminKeys = {
  all: ['admin'] as const,
  listings: (params: AdminListingsQueryParams) => [...adminKeys.all, 'listings', params] as const,
};

/**
 * Fetch admin listings with status filter
 *
 * Usage:
 * ```tsx
 * const { data, isLoading } = useAdminListings({ status: 'PENDING' });
 * ```
 */
export function useAdminListings(params: AdminListingsQueryParams = {}) {
  return useQuery({
    queryKey: adminKeys.listings(params),
    queryFn: async (): Promise<PaginatedResponse<Listing>> => {
      // TODO: Replace with actual API call
      // return get<PaginatedResponse<Listing>>('/admin/listings', params);

      // Mock data
      await new Promise((resolve) => setTimeout(resolve, 500));
      const mockPendingListings: Listing[] = [
        {
          id: '3',
          ownerId: 'user-1',
          title: 'Spacious 3BR House',
          description: 'Family-friendly house with backyard and garage.',
          price: 3200,
          city: 'Austin',
          images: [],
          status: 'PENDING',
          createdAt: new Date().toISOString(),
        },
      ];

      return {
        items: mockPendingListings.filter((l) =>
          params.status ? l.status === params.status : true
        ),
        total: mockPendingListings.length,
      };
    },
    staleTime: 1000 * 60, // 1 minute
  });
}

/**
 * Approve listing mutation
 *
 * Usage:
 * ```tsx
 * const approveListing = useApproveListing();
 * await approveListing.mutateAsync('listing-id');
 * ```
 */
export function useApproveListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (listingId: string): Promise<void> => {
      // TODO: Replace with actual API call
      // return post(`/admin/listings/${listingId}/approve`);

      await new Promise((resolve) => setTimeout(resolve, 500));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.all });
      toast.success('Listing approved successfully');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to approve listing');
    },
  });
}

/**
 * Reject listing mutation
 *
 * Usage:
 * ```tsx
 * const rejectListing = useRejectListing();
 * await rejectListing.mutateAsync('listing-id');
 * ```
 */
export function useRejectListing() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (listingId: string): Promise<void> => {
      // TODO: Replace with actual API call
      // return post(`/admin/listings/${listingId}/reject`);

      await new Promise((resolve) => setTimeout(resolve, 500));
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKeys.all });
      toast.success('Listing rejected');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to reject listing');
    },
  });
}
