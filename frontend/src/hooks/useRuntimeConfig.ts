/**
 * Runtime configuration utilities
 * This provides environment variables that are resolved at runtime, not build time
 */

interface RuntimeConfig {
  NEXT_PUBLIC_API_URL: string;
  NEXT_PUBLIC_S3_BUCKET_URL: string;
}

/**
 * Get runtime configuration (can be used anywhere, including non-React components)
 */
export function getRuntimeConfig(): RuntimeConfig {
  // Check if window.__ENV__ is available and has valid values
  if (typeof window !== 'undefined' && (window as any).__ENV__) {
    const env = (window as any).__ENV__;

    // Check if the values look like actual URLs (not placeholders)
    const apiUrl = env.NEXT_PUBLIC_API_URL;
    const s3Url = env.NEXT_PUBLIC_S3_BUCKET_URL;

    // Validate that values are not placeholders or empty
    if (apiUrl && !apiUrl.includes('$') && apiUrl !== 'undefined') {
      return {
        NEXT_PUBLIC_API_URL: apiUrl,
        NEXT_PUBLIC_S3_BUCKET_URL: (s3Url && !s3Url.includes('$') && s3Url !== 'undefined')
          ? s3Url
          : 'http://localhost:9000'
      };
    }
  }

  // Fallback - only for development/debugging
  if (process.env.NODE_ENV === 'development') {
    console.warn('Runtime config not available, using fallback values');
  }

  return {
    NEXT_PUBLIC_API_URL: 'http://localhost:8080',
    NEXT_PUBLIC_S3_BUCKET_URL: 'http://localhost:9000'
  };
}

/**
 * React Hook to access runtime configuration
 */
export function useRuntimeConfig(): RuntimeConfig {
  return getRuntimeConfig();
}