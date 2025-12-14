import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiError, RefreshTokenRequest, AuthTokens } from '@/types';
import { getRefreshToken, setAuthTokens, removeAuthTokens } from './auth';
import { getRuntimeConfig } from '@/hooks/useRuntimeConfig';

// Get BASE_URL from runtime config or fallback to default
let BASE_URL: string;

// Try to get runtime config immediately
try {
  const config = getRuntimeConfig();
  BASE_URL = config.NEXT_PUBLIC_API_URL;
} catch {
  // Fallback for SSR or initialization
  BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
}

// Flag to prevent multiple refresh attempts
let isRefreshing = false;
let failedQueue: any[] = [];

/**
 * Process queued requests after token refresh
 */
const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });

  failedQueue = [];
};

/**
 * Main API client instance
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

/**
 * Request interceptor to add auth token
 */
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // TODO: In production, prefer httpOnly cookies over localStorage
    // For MVP, we use localStorage for simplicity
    const tokens = typeof window !== 'undefined' ? localStorage.getItem('auth_tokens') : null;

    if (tokens && config.headers) {
      try {
        const parsedTokens = JSON.parse(tokens) as AuthTokens;
        config.headers.Authorization = `${parsedTokens.tokenType} ${parsedTokens.accessToken}`;
      } catch (error) {
        // Invalid token format, continue without auth
      }
    } else {
      console.log('No tokens found or headers undefined'); // Debug log
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response interceptor for error handling and token refresh
 */
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<any>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // If error is not 401 or request already retried, reject
    if (error.response?.status !== 401 || originalRequest._retry) {
      const apiError: ApiError = {
        message: error.response?.data?.message || error.message || 'An unexpected error occurred',
        status: error.response?.status,
        error_code: error.response?.data?.error_code,
        details: error.response?.data?.details,
      };
      return Promise.reject(apiError);
    }

    // If we're already refreshing, queue this request
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      }).then((token) => {
        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${token}`;
        }
        return apiClient(originalRequest);
      }).catch((err) => {
        return Promise.reject(err);
      });
    }

    // Start refresh process
    isRefreshing = true;
    const refreshToken = getRefreshToken();

    if (!refreshToken) {
      // No refresh token, clear auth and redirect
      if (typeof window !== 'undefined') {
        removeAuthTokens();
        localStorage.removeItem('user');
        if (!window.location.pathname.startsWith('/auth')) {
          window.location.href = '/auth/login';
        }
      }
      processQueue(new Error('No refresh token'));
      isRefreshing = false;
      return Promise.reject(error);
    }

    try {
      // Attempt to refresh token
      const response = await axios.post<AuthTokens>(`${BASE_URL}/auth/refresh`, {
        refreshToken
      } as RefreshTokenRequest);

      const { accessToken, refreshToken: newRefreshToken, tokenType, expiresIn } = response.data;

      // Store new tokens
      setAuthTokens({
        accessToken,
        refreshToken: newRefreshToken || refreshToken,
        tokenType: tokenType || 'Bearer',
        expiresIn: expiresIn || 900
      });

      // Update Authorization header
      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `${tokenType || 'Bearer'} ${accessToken}`;
      }

      // Process queued requests
      processQueue(null, accessToken);
      isRefreshing = false;

      // Retry original request
      return apiClient(originalRequest);
    } catch (refreshError) {
      // Refresh failed, clear auth and redirect
      if (typeof window !== 'undefined') {
        removeAuthTokens();
        localStorage.removeItem('user');
        if (!window.location.pathname.startsWith('/auth')) {
          window.location.href = '/auth/login';
        }
      }

      processQueue(refreshError, null);
      isRefreshing = false;
      return Promise.reject(error);
    }
  }
);

/**
 * Generic GET request
 */
export async function get<T>(url: string, params?: any): Promise<T> {
  const response = await apiClient.get<T>(url, { params });
  return response.data;
}

/**
 * Generic POST request
 */
export async function post<T>(url: string, data?: any): Promise<T> {
  const response = await apiClient.post<T>(url, data);
  return response.data;
}

/**
 * Generic PUT request
 */
export async function put<T>(url: string, data?: any): Promise<T> {
  const response = await apiClient.put<T>(url, data);
  return response.data;
}

/**
 * Generic DELETE request
 */
export async function del<T>(url: string): Promise<T> {
  const response = await apiClient.delete<T>(url);
  return response.data;
}

/**
 * Upload file to presigned URL (direct to S3/MinIO)
 */
export async function uploadToPresignedUrl(
  presignedUrl: string,
  file: File,
  onProgress?: (progress: number) => void
): Promise<void> {
  await axios.put(presignedUrl, file, {
    headers: {
      'Content-Type': file.type,
    },
    onUploadProgress: (progressEvent) => {
      if (progressEvent.total && onProgress) {
        const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        onProgress(progress);
      }
    },
  });
}
