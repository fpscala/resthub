/**
 * Server-side configuration module
 * This file can only be imported on the server side
 */

export const serverConfig = {
  backendApiUrl: process.env.BACKEND_API_URL!,
} as const;

// Validate required environment variables
if (!serverConfig.backendApiUrl) {
  throw new Error('BACKEND_API_URL is not defined in environment variables');
}

// Prevent client-side usage
if (typeof window !== 'undefined') {
  throw new Error('serverConfig can only be imported on the server side');
}

export default serverConfig;