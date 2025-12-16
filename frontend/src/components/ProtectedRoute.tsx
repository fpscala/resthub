'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';

export interface ProtectedRouteProps {
  children: React.ReactNode;
  requireAdmin?: boolean;
}

/**
 * Protected route wrapper that redirects unauthenticated users
 *
 * Usage:
 * ```tsx
 * <ProtectedRoute>
 *   <CreateListingPage />
 * </ProtectedRoute>
 *
 * // For admin-only routes:
 * <ProtectedRoute requireAdmin>
 *   <AdminPanel />
 * </ProtectedRoute>
 * ```
 */
export function ProtectedRoute({ children, requireAdmin = false }: ProtectedRouteProps) {
  const { isAuthenticated, isAdmin, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    // Only redirect after loading is complete
    if (!isLoading) {
      // Redirect to login if not authenticated
      if (!isAuthenticated) {
        router.push('/auth/login');
        return;
      }

      // Redirect to home if admin access required but user is not admin
      if (requireAdmin && !isAdmin) {
        router.push('/');
      }
    }
  }, [isAuthenticated, isAdmin, requireAdmin, router, isLoading]);

  // Show loading spinner while checking auth
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  // Don't render children if not authenticated or doesn't have required role
  if (!isAuthenticated || (requireAdmin && !isAdmin)) {
    return null;
  }

  return <>{children}</>;
}
