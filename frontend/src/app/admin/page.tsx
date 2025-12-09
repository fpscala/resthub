'use client';

import { useState } from 'react';
import { useAdminListings, useApproveListing, useRejectListing } from '@/hooks/useAdmin';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { AdminTable } from '@/components/AdminTable';
import { AdminListingsQueryParams } from '@/types';

/**
 * Admin page (protected, admin-only)
 *
 * Features:
 * - View pending listings
 * - Approve/reject actions
 * - Role-based access control
 */
function AdminContent() {
  const [filters, setFilters] = useState<AdminListingsQueryParams>({
    status: 'PENDING',
  });

  const { data, isLoading, error } = useAdminListings(filters);
  const approveMutation = useApproveListing();
  const rejectMutation = useRejectListing();

  const handleApprove = async (id: string) => {
    await approveMutation.mutateAsync(id);
  };

  const handleReject = async (id: string) => {
    await rejectMutation.mutateAsync(id);
  };

  return (
    <div className="container-custom py-8">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Admin Panel</h1>
        <p className="mt-2 text-gray-600">Manage pending listings and user reports</p>
      </div>

      {/* Status Filter */}
      <div className="mb-6">
        <label className="label">Filter by Status</label>
        <select
          className="input max-w-xs"
          value={filters.status || ''}
          onChange={(e) =>
            setFilters({
              ...filters,
              status: e.target.value as 'PENDING' | 'APPROVED' | 'REJECTED' | undefined,
            })
          }
        >
          <option value="">All</option>
          <option value="PENDING">Pending</option>
          <option value="APPROVED">Approved</option>
          <option value="REJECTED">Rejected</option>
        </select>
      </div>

      {/* Listings Table */}
      {isLoading ? (
        <div className="h-64 animate-pulse rounded-lg bg-gray-200" />
      ) : error ? (
        <div className="rounded-lg bg-red-50 p-4 text-red-800">
          Error loading listings. Please try again.
        </div>
      ) : data ? (
        <AdminTable
          listings={data.items}
          onApprove={handleApprove}
          onReject={handleReject}
          isLoading={approveMutation.isPending || rejectMutation.isPending}
        />
      ) : null}
    </div>
  );
}

export default function AdminPage() {
  return (
    <ProtectedRoute requireAdmin>
      <AdminContent />
    </ProtectedRoute>
  );
}
