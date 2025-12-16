'use client';

import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import { User } from '@/types';
import { AuthManager } from '@/lib/auth-manager';
import { get } from '@/lib/api-client';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isInitialized, setIsInitialized] = useState(false);

  // Derive authentication state
  const isAuthenticated = !!user;
  const isAdmin = user?.role?.name === 'ADMIN' || user?.role?.name === 'SUPER_ADMIN';

  // Fetch current user data
  const fetchUser = async (): Promise<User | null> => {
    try {
      const userData = await get<User>('/auth/me');
      return userData;
    } catch (error) {
      console.error('Failed to fetch user:', error);
      return null;
    }
  };

  // Initialize auth state
  useEffect(() => {
    let mounted = true;

    const initializeAuth = async () => {
      setIsLoading(true);

      try {
        const token = await AuthManager.initialize();
        if (token && mounted) {
          const userData = await fetchUser();
          if (userData) {
            setUser(userData);
          }
        }
      } catch (error) {
        console.error('Auth initialization failed:', error);
        AuthManager.clearSession();
      } finally {
        if (mounted) {
          setIsLoading(false);
          setIsInitialized(true);
        }
      }
    };

    initializeAuth();

    return () => {
      mounted = false;
    };
  }, []);

  // Login function
  const login = async (email: string, password: string): Promise<void> => {
    setIsLoading(true);

    try {
      await AuthManager.login({ email, password });
      const userData = await fetchUser();

      if (!userData) {
        throw new Error('Failed to fetch user after login');
      }

      setUser(userData);
    } catch (error) {
      console.error('Login failed:', error);
      AuthManager.clearSession();
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  // Logout function
  const logout = async (): Promise<void> => {
    setIsLoading(true);

    try {
      await AuthManager.logout();
    } catch (error) {
      console.error('Logout request failed:', error);
    } finally {
      AuthManager.clearSession();
      setUser(null);
      setIsLoading(false);
    }
  };

  // Refresh function
  const refresh = async (): Promise<void> => {
    try {
      await AuthManager.refresh();
      const userData = await fetchUser();
      setUser(userData);
    } catch (error) {
      console.error('Refresh failed:', error);
      AuthManager.clearSession();
      setUser(null);
      throw error;
    }
  };

  const value: AuthContextType = {
    user,
    isLoading,
    isAuthenticated,
    isAdmin,
    login,
    logout,
    refresh,
  };

  // Don't render children until auth is initialized
  if (!isInitialized) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}