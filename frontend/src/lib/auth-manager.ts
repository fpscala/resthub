import { AuthTokens } from '@/types';

// Refresh token storage abstraction
const RT_STORAGE_KEY = 'nesthub_rt';

class RefreshTokenStorage {
  static set(token: string): void {
    if (typeof globalThis.window === 'undefined') return;
    globalThis.window.localStorage.setItem(RT_STORAGE_KEY, token);
  }

  static get(): string | null {
    if (typeof globalThis.window === 'undefined') return null;
    return globalThis.window.localStorage.getItem(RT_STORAGE_KEY);
  }

  static remove(): void {
    if (typeof globalThis.window === 'undefined') return;
    globalThis.window.localStorage.removeItem(RT_STORAGE_KEY);
  }
}

// Request queue for managing pending requests during token refresh
interface PendingRequest {
  resolve: (token: string) => void;
  reject: (error: Error) => void;
}

class AuthManagerClass {
  private accessToken: string | null = null;
  private refreshPromise: Promise<string> | null = null;
  private pendingRequests: PendingRequest[] = [];
  private isRefreshing = false;

  // Initialize auth state from refresh token
  async initialize(): Promise<string | null> {
    const refreshToken = RefreshTokenStorage.get();
    if (!refreshToken) {
      return null;
    }

    try {
      return await this.refresh(refreshToken);
    } catch (error) {
      console.error('Failed to initialize auth session:', error);
      this.clearSession();
      return null;
    }
  }

  // Get current access token from memory
  getAccessToken(): string | null {
    return this.accessToken;
  }

  // Set new tokens
  setTokens(tokens: AuthTokens): void {
    this.accessToken = tokens.accessToken;
    RefreshTokenStorage.set(tokens.refreshToken);
  }

  // Login with credentials
  async login(credentials: { email: string; password: string }): Promise<AuthTokens> {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || 'Login failed');
    }

    const tokens: AuthTokens = await response.json();
    this.setTokens(tokens);
    return tokens;
  }

  // Refresh access token using refresh token
  async refresh(refreshToken?: string): Promise<string> {
    // Prevent parallel refresh calls
    if (this.isRefreshing && this.refreshPromise) {
      return this.refreshPromise;
    }

    const token = refreshToken || RefreshTokenStorage.get();
    if (!token) {
      throw new Error('No refresh token available');
    }

    this.isRefreshing = true;
    this.refreshPromise = this.performRefresh(token);

    try {
      const newAccessToken = await this.refreshPromise;
      this.resolvePendingRequests(newAccessToken);
      return newAccessToken;
    } catch (error) {
      this.rejectPendingRequests(error as Error);
      throw error;
    } finally {
      this.isRefreshing = false;
      this.refreshPromise = null;
    }
  }

  private async performRefresh(token: string): Promise<string> {
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken: token }),
    });

    if (!response.ok) {
      if (response.status === 401) {
        // Refresh token is revoked or invalid
        this.clearSession();
      }
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || 'Token refresh failed');
    }

    const tokens: AuthTokens = await response.json();
    this.accessToken = tokens.accessToken;

    // Update refresh token (token rotation)
    if (tokens.refreshToken && tokens.refreshToken !== token) {
      RefreshTokenStorage.set(tokens.refreshToken);
    }

    return this.accessToken;
  }

  // Queue a request to be resolved after token refresh
  private addPendingRequest(): Promise<string> {
    return new Promise<string>((resolve, reject) => {
      this.pendingRequests.push({ resolve, reject });
    });
  }

  private resolvePendingRequests(token: string): void {
    this.pendingRequests.forEach(({ resolve }) => resolve(token));
    this.pendingRequests = [];
  }

  private rejectPendingRequests(error: Error): void {
    this.pendingRequests.forEach(({ reject }) => reject(error));
    this.pendingRequests = [];
  }

  // Logout user
  async logout(): Promise<void> {
    const refreshToken = RefreshTokenStorage.get();

    if (refreshToken) {
      try {
        await fetch('/api/auth/logout', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({ refreshToken }),
        });
      } catch (error) {
        console.error('Logout request failed:', error);
      }
    }

    this.clearSession();
  }

  // Clear all auth data
  clearSession(): void {
    this.accessToken = null;
    RefreshTokenStorage.remove();
    this.pendingRequests = [];
    this.isRefreshing = false;
    this.refreshPromise = null;
  }

  // Get method for external components to wait for refresh
  waitForRefresh(): Promise<string> {
    if (this.isRefreshing && this.refreshPromise) {
      return this.addPendingRequest();
    }

    if (this.accessToken) {
      return Promise.resolve(this.accessToken);
    }

    return Promise.reject(new Error('No access token available'));
  }
}

// Export singleton instance
export const AuthManager = new AuthManagerClass();

// Export types for external use
export type { AuthManagerClass };