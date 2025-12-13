import { User, AuthTokens } from '@/types';

/**
 * Get stored access token
 *
 * SECURITY NOTE (MVP):
 * - Currently storing tokens in localStorage for simplicity
 * - PRODUCTION RECOMMENDATION: Use httpOnly cookies with refresh token rotation
 * - localStorage is vulnerable to XSS attacks
 * - httpOnly cookies cannot be accessed by JavaScript, providing better security
 *
 * @returns access token or null
 */
export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null;
  const tokens = getStoredTokens();
  return tokens?.accessToken || null;
}

/**
 * Get stored refresh token
 */
export function getRefreshToken(): string | null {
  if (typeof window === 'undefined') return null;
  const tokens = getStoredTokens();
  return tokens?.refreshToken || null;
}

/**
 * Get stored tokens
 */
export function getStoredTokens(): AuthTokens | null {
  if (typeof window === 'undefined') return null;
  const tokensStr = localStorage.getItem('auth_tokens');
  if (!tokensStr) return null;

  try {
    return JSON.parse(tokensStr) as AuthTokens;
  } catch {
    return null;
  }
}

/**
 * Store auth tokens
 */
export function setAuthTokens(tokens: AuthTokens): void {
  if (typeof window === 'undefined') return;
  console.log('Setting auth tokens:', tokens); // Debug log
  localStorage.setItem('auth_tokens', JSON.stringify(tokens));
  console.log('Tokens stored in localStorage:', localStorage.getItem('auth_tokens')); // Debug log
}

/**
 * Remove auth tokens
 */
export function removeAuthTokens(): void {
  if (typeof window === 'undefined') return;
  localStorage.removeItem('auth_tokens');
}

/**
 * Store access token (legacy compatibility)
 */
export function setAccessToken(token: string): void {
  if (typeof window === 'undefined') return;
  const existingTokens = getStoredTokens();
  const tokens: AuthTokens = {
    accessToken: token,
    refreshToken: existingTokens?.refreshToken || '',
    tokenType: 'Bearer',
    expiresIn: existingTokens?.expiresIn || 900
  };
  setAuthTokens(tokens);
}

/**
 * Remove access token (legacy compatibility)
 */
export function removeAccessToken(): void {
  removeAuthTokens();
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
  return user?.role?.name === 'ADMIN' || user?.role?.name === 'SUPER_ADMIN';
}

/**
 * Check if user has specific privilege
 */
export function hasPrivilege(user: User | null, privilege: keyof User['role']['privileges']): boolean {
  if (!user?.role) return false;
  return !!user.role.privileges[privilege];
}

/**
 * Clear all auth data
 */
export function clearAuthData(): void {
  removeAuthTokens();
  removeStoredUser();
}
