'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';

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
  const { isAuthenticated, isAdmin, user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    // Redirect to login if not authenticated
    if (!isAuthenticated) {
      router.push('/auth/login');
      return;
    }

    // Redirect to home if admin access required but user is not admin
    if (requireAdmin && !isAdmin) {
      router.push('/');
    }
  }, [isAuthenticated, isAdmin, requireAdmin, router]);

  // Show nothing while checking auth (prevents flash of protected content)
  if (!isAuthenticated) {
    return null;
  }

  if (requireAdmin && !isAdmin) {
    return null;
  }

  return <>{children}</>;
}
