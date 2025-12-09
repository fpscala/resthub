import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { post } from '@/lib/api-client';
import {
  getStoredUser,
  setAccessToken,
  setStoredUser,
  clearAuthData,
  isAdmin as checkIsAdmin,
} from '@/lib/auth';
import {
  User,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
} from '@/types';
import toast from 'react-hot-toast';

/**
 * Authentication hook providing login, register, logout functionality
 *
 * Usage:
 * ```tsx
 * const { user, isLoading, login, register, logout, isAdmin } = useAuth();
 *
 * const handleLogin = async (email: string, password: string) => {
 *   await login.mutateAsync({ email, password });
 * };
 * ```
 */
export function useAuth() {
  const router = useRouter();
  const queryClient = useQueryClient();

  // Get current user from localStorage
  const { data: user } = useQuery<User | null>({
    queryKey: ['auth', 'user'],
    queryFn: () => getStoredUser(),
    staleTime: Infinity, // User data doesn't change unless we update it
  });

  /**
   * Login mutation
   */
  const login = useMutation({
    mutationFn: async (credentials: LoginRequest): Promise<AuthResponse> => {
      return post<AuthResponse>('/auth/login', credentials);
    },
    onSuccess: (data) => {
      setAccessToken(data.token);
      setStoredUser(data.user);
      queryClient.setQueryData(['auth', 'user'], data.user);
      toast.success('Login successful!');
      router.push('/');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Login failed');
    },
  });

  /**
   * Register mutation
   */
  const register = useMutation({
    mutationFn: async (userData: RegisterRequest): Promise<AuthResponse> => {
      return post<AuthResponse>('/auth/register', userData);
    },
    onSuccess: (data) => {
      setAccessToken(data.token);
      setStoredUser(data.user);
      queryClient.setQueryData(['auth', 'user'], data.user);
      toast.success('Registration successful!');
      router.push('/');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Registration failed');
    },
  });

  /**
   * Logout function
   */
  const logout = () => {
    clearAuthData();
    queryClient.setQueryData(['auth', 'user'], null);
    queryClient.clear(); // Clear all cached queries
    toast.success('Logged out successfully');
    router.push('/auth/login');
  };

  /**
   * Get user profile (could be extended to fetch from API)
   */
  const getUser = () => user;

  return {
    user,
    isLoading: login.isPending || register.isPending,
    isAuthenticated: !!user,
    isAdmin: checkIsAdmin(user || null),
    login,
    register,
    logout,
    getUser,
  };
}
