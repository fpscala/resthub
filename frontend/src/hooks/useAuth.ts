import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { post, get } from '@/lib/api-client';
import {
  getStoredUser,
  setAuthTokens,
  setStoredUser,
  clearAuthData,
  isAdmin as checkIsAdmin,
  getStoredTokens,
} from '@/lib/auth';
import {
  User,
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  AuthTokens,
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
  const { data: storedUser } = useQuery<User | null>({
    queryKey: ['auth', 'stored-user'],
    queryFn: () => getStoredUser(),
    staleTime: Infinity, // User data doesn't change unless we update it
  });

  /**
   * Get current user from API
   */
  const { data: apiUser } = useQuery<User>({
    queryKey: ['auth', 'api-user'],
    queryFn: () => get<User>('/auth/me'),
    enabled: !!getStoredTokens(),
    retry: false,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });

  /**
   * Combined user data (localStorage first, then API)
   */
  const user = storedUser || (getStoredTokens() ? apiUser : null);

  /**
   * Login mutation
   */
  const login = useMutation({
    mutationFn: async (credentials: LoginRequest): Promise<AuthResponse> => {
      const response = await post<AuthTokens>('/auth/login', credentials);

      // After login, fetch user data
      const userData = await get<User>('/auth/me');

      return {
        user: userData,
        tokens: response
      };
    },
    onSuccess: (data) => {
      setAuthTokens(data.tokens);
      setStoredUser(data.user);
      queryClient.setQueryData(['auth', 'stored-user'], data.user);
      queryClient.invalidateQueries({ queryKey: ['auth'] });
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
      // Register the user (backend returns empty tokens for now)
      await post<AuthTokens>('/auth/register', userData);

      // Login immediately after registration to get real tokens
      const tokens = await post<AuthTokens>('/auth/login', {
        email: userData.email,
        password: userData.password
      });

      // Store real tokens in localStorage
      setAuthTokens(tokens);

      // Small delay to ensure localStorage is updated
      await new Promise(resolve => setTimeout(resolve, 100));

      // Then fetch user data with authenticated request
      const userResponse = await get<User>('/auth/me');

      return {
        user: userResponse,
        tokens
      };
    },
    onSuccess: (data) => {
      setStoredUser(data.user);
      queryClient.setQueryData(['auth', 'stored-user'], data.user);
      queryClient.invalidateQueries({ queryKey: ['auth'] });
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
    queryClient.setQueryData(['auth', 'stored-user'], null);
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
