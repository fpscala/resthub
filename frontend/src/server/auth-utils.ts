/**
 * Server-side authentication utilities
 * This file contains helper functions for Bearer token authentication
 */

/**
 * Extract Bearer token from HttpOnly cookie or Authorization header
 */
export function extractBearerToken(cookieHeader: string | null, authHeader: string | null = null): string | null {
  // First try Authorization header (for development/testing)
  if (authHeader) {
    const match = authHeader.match(/^Bearer\s+(.+)$/);
    const token = match?.[1];
    if (token && token.length >= 10) return token;
  }

  // Fallback to cookie-based extraction (production)
  if (!cookieHeader) return null;

  // Look for access_token cookie - be more precise with matching
  const match = cookieHeader.match(/(?:^|;\s*)access_token=([^;]+)/);
  const token = match?.[1];

  // Additional validation - ensure token is not empty and looks reasonable
  if (!token || token.length < 10) return null;

  return token;
}

/**
 * Create HttpOnly cookie for Bearer token
 */
export function createTokenCookie(token: string): string {
  const isProd = process.env.NODE_ENV === 'production';
  const cookieParts = [
    `access_token=${token}`,
    'Path=/',
    'HttpOnly',
    'SameSite=Lax',
    'Max-Age=3600', // 1 hour
  ];

  // Add Secure flag only in production
  if (isProd) {
    cookieParts.push('Secure');
  }

  return cookieParts.join('; ');
}

/**
 * Clear authentication cookie
 */
export function clearTokenCookie(): string {
  const isProd = process.env.NODE_ENV === 'production';
  const cookieParts = [
    'access_token=',
    'Path=/',
    'HttpOnly',
    'SameSite=Lax',
    'Max-Age=0',
  ];

  // Add Secure flag only in production
  if (isProd) {
    cookieParts.push('Secure');
  }

  return cookieParts.join('; ');
}