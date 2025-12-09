import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuth } from '../useAuth';
import * as apiClient from '@/lib/api-client';
import * as auth from '@/lib/auth';

// Mock dependencies
vi.mock('@/lib/api-client');
vi.mock('@/lib/auth');
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
  }),
}));
vi.mock('react-hot-toast', () => ({
  default: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('useAuth', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
    vi.clearAllMocks();
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  it('returns null user when not authenticated', () => {
    vi.mocked(auth.getStoredUser).mockReturnValue(null);

    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  it('returns user when authenticated', async () => {
    const mockUser = {
      id: '1',
      name: 'Test User',
      email: 'test@example.com',
      role: 'USER' as const,
    };

    vi.mocked(auth.getStoredUser).mockReturnValue(mockUser);

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.user).toEqual(mockUser);
      expect(result.current.isAuthenticated).toBe(true);
    });
  });

  it('login mutation stores token and user', async () => {
    const mockAuthResponse = {
      user: {
        id: '1',
        name: 'Test User',
        email: 'test@example.com',
        role: 'USER' as const,
      },
      token: 'test-token',
    };

    vi.mocked(apiClient.post).mockResolvedValue(mockAuthResponse);
    vi.mocked(auth.getStoredUser).mockReturnValue(null);

    const { result } = renderHook(() => useAuth(), { wrapper });

    result.current.login.mutate({
      email: 'test@example.com',
      password: 'password123',
    });

    await waitFor(() => {
      expect(result.current.login.isSuccess).toBe(true);
    });

    expect(auth.setAccessToken).toHaveBeenCalledWith('test-token');
    expect(auth.setStoredUser).toHaveBeenCalledWith(mockAuthResponse.user);
  });

  it('logout clears auth data', () => {
    const mockUser = {
      id: '1',
      name: 'Test User',
      email: 'test@example.com',
      role: 'USER' as const,
    };

    vi.mocked(auth.getStoredUser).mockReturnValue(mockUser);

    const { result } = renderHook(() => useAuth(), { wrapper });

    result.current.logout();

    expect(auth.clearAuthData).toHaveBeenCalled();
  });

  it('identifies admin users correctly', async () => {
    const adminUser = {
      id: '1',
      name: 'Admin User',
      email: 'admin@example.com',
      role: 'ADMIN' as const,
    };

    vi.mocked(auth.getStoredUser).mockReturnValue(adminUser);

    const { result } = renderHook(() => useAuth(), { wrapper });

    await waitFor(() => {
      expect(result.current.isAdmin).toBe(true);
    });
  });
});
