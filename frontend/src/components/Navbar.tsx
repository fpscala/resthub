'use client';

import Link from 'next/link';
import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/Button';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils';

/**
 * Navigation bar with auth-aware menu
 */
export function Navbar() {
  const { user, isAuthenticated, logout, isAdmin } = useAuth();
  const pathname = usePathname();

  const isActive = (path: string) => pathname === path;

  return (
    <nav className="border-b border-gray-200 bg-white shadow-sm">
      <div className="container-custom">
        <div className="flex h-16 items-center justify-between">
          {/* Logo */}
          <Link href="/" className="text-2xl font-bold text-primary-600">
            NestHub
          </Link>

          {/* Navigation Links */}
          <div className="flex items-center gap-6">
            <Link
              href="/"
              className={cn(
                'text-sm font-medium transition-colors hover:text-primary-600',
                isActive('/') ? 'text-primary-600' : 'text-gray-700'
              )}
            >
              Home
            </Link>
            <Link
              href="/listings"
              className={cn(
                'text-sm font-medium transition-colors hover:text-primary-600',
                isActive('/listings') ? 'text-primary-600' : 'text-gray-700'
              )}
            >
              Browse Listings
            </Link>

            {isAuthenticated ? (
              <>
                <Link
                  href="/create"
                  className={cn(
                    'text-sm font-medium transition-colors hover:text-primary-600',
                    isActive('/create') ? 'text-primary-600' : 'text-gray-700'
                  )}
                >
                  Create Listing
                </Link>

                {isAdmin && (
                  <Link
                    href="/admin"
                    className={cn(
                      'text-sm font-medium transition-colors hover:text-primary-600',
                      isActive('/admin') ? 'text-primary-600' : 'text-gray-700'
                    )}
                  >
                    Admin
                  </Link>
                )}

                <div className="flex items-center gap-3">
                  <span className="text-sm text-gray-600">Hello, {user?.name}</span>
                  <Button variant="secondary" onClick={logout} className="text-sm">
                    Logout
                  </Button>
                </div>
              </>
            ) : (
              <>
                <Link href="/auth/login">
                  <Button variant="secondary" className="text-sm">
                    Login
                  </Button>
                </Link>
                <Link href="/auth/register">
                  <Button variant="primary" className="text-sm">
                    Sign Up
                  </Button>
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
