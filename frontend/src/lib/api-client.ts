import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ApiError } from '@/types';
import { getAccessToken, getRefreshToken, setAuthTokens, clearAuthData } from '@/lib/auth';

/**
 * API base URL - uses Next.js API routes as proxy to backend
 */
const API_BASE_URL = '/api';

/**
 * Main API client instance
 */
export const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Accept': 'application/json',
  },
  timeout: 30000,
});

/**
 * Request interceptor
 * Adds Bearer token from localStorage to all outgoing requests
 */
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Add Bearer token if available
    const token = getAccessToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
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

    // Handle 401 Unauthorized errors
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = getRefreshToken();
      if (refreshToken) {
        try {
          // Try to refresh the token
          const refreshResponse = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken
          });

          const newTokens = refreshResponse.data;
          setAuthTokens(newTokens);

          // Retry the original request with new token
          const token = getAccessToken();
          if (token && originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`;
          }
          return apiClient(originalRequest);
        } catch (refreshError) {
          // Refresh failed, clear auth and redirect to login
          console.error('Token refresh failed:', refreshError);
          if (typeof globalThis.window !== 'undefined') {
            clearAuthData();
            globalThis.localStorage.removeItem('user');
            if (!globalThis.window.location.pathname.startsWith('/auth')) {
              globalThis.window.location.href = '/auth/login';
            }
          }
        }
      } else {
        // No refresh token, clear auth and redirect
        if (typeof globalThis.window !== 'undefined') {
          clearAuthData();
          globalThis.localStorage.removeItem('user');
          if (!globalThis.window.location.pathname.startsWith('/auth')) {
            globalThis.window.location.href = '/auth/login';
          }
        }
      }
    }

    const apiError: ApiError = {
      message: error.response?.data?.message || error.message || 'An unexpected error occurred',
      status: error.response?.status,
      error_code: error.response?.data?.error_code,
      details: error.response?.data?.details,
    };
    throw apiError;
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
export async function post<T>(url: string, data?: any, options?: { headers?: Record<string, string> }): Promise<T> {
  // If data is FormData, let axios set the Content-Type with the correct boundary
  const isFormData = data instanceof FormData;

  const config: any = {
    headers: {
      ...options?.headers,
      // Set Content-Type for JSON data
      ...(data && !isFormData ? { 'Content-Type': 'application/json' } : {}),
    },
  };

  const response = await apiClient.post<T>(url, data, config);
  return response.data;
}

/**
 * Generic PUT request
 */
export async function put<T>(url: string, data?: any, options?: { headers?: Record<string, string> }): Promise<T> {
  // If data is FormData, let axios set the Content-Type with the correct boundary
  const isFormData = data instanceof FormData;

  const config: any = {
    headers: {
      ...options?.headers,
      // Set Content-Type for JSON data
      ...(data && !isFormData ? { 'Content-Type': 'application/json' } : {}),
    },
  };

  const response = await apiClient.put<T>(url, data, config);
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
