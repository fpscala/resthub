import { User } from '@/types';

/**
 * Get stored access token
 *
 * SECURITY NOTE (MVP):
 * - Currently storing token in localStorage for simplicity
 * - PRODUCTION RECOMMENDATION: Use httpOnly cookies with refresh token rotation
 * - localStorage is vulnerable to XSS attacks
 * - httpOnly cookies cannot be accessed by JavaScript, providing better security
 *
 * @returns access token or null
 */
export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('access_token');
}

/**
 * Store access token
 *
 * @param token - JWT access token
 */
export function setAccessToken(token: string): void {
  if (typeof window === 'undefined') return;
  localStorage.setItem('access_token', token);
}

/**
 * Remove access token
 */
export function removeAccessToken(): void {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('access_token');
}

/**
 * Get stored user
 */
export function getStoredUser(): User | null {
  if (typeof window === 'undefined') return null;
  const userStr = localStorage.getItem('user');
  if (!userStr) return null;

  try {
    return JSON.parse(userStr) as User;
  } catch {
    return null;
  }
}

/**
 * Store user data
 */
export function setStoredUser(user: User): void {
  if (typeof window === 'undefined') return;
  localStorage.setItem('user', JSON.stringify(user));
}

/**
 * Remove stored user
 */
export function removeStoredUser(): void {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('user');
}

/**
 * Check if user is authenticated
 */
export function isAuthenticated(): boolean {
  return !!getAccessToken();
}

/**
 * Check if user has admin role
 */
export function isAdmin(user: User | null): boolean {
  return user?.role === 'ADMIN';
}

/**
 * Clear all auth data
 */
export function clearAuthData(): void {
  removeAccessToken();
  removeStoredUser();
}
