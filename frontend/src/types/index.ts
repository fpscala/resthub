// Domain Models

export interface User {
  id: string;
  name: string;
  email: string;
  role: 'USER' | 'ADMIN';
  createdAt?: string;
}

export interface Listing {
  id: string;
  ownerId: string;
  owner?: User;
  title: string;
  description: string;
  price: number;
  city: string;
  images: string[];
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
  updatedAt?: string;
}

export interface Contract {
  id: string;
  listingId: string;
  pdfUrl: string;
  createdAt: string;
}

// API Request/Response Types

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  user: User;
  token: string;
}

export interface CreateListingRequest {
  ownerId: string;
  title: string;
  description: string;
  price: number;
  city: string;
  images: string[];
}

export interface CreateListingResponse {
  id: string;
}

export interface PresignRequest {
  key: string;
}

export interface PresignResponse {
  url: string; // Presigned PUT URL
  publicUrl: string; // Final accessible URL
}

export interface GenerateContractRequest {
  listingId: string;
}

export interface GenerateContractResponse {
  pdfUrl: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page?: number;
  size?: number;
}

export interface ListingsQueryParams {
  city?: string;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
}

export interface AdminListingsQueryParams {
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  page?: number;
  size?: number;
}

// UI Component Props

export interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export interface ListingCardProps {
  listing: Listing;
  onClick?: () => void;
}

export interface ImageUploadResult {
  publicUrl: string;
  success: boolean;
  error?: string;
}

// Form Types

export interface LoginFormData {
  email: string;
  password: string;
}

export interface RegisterFormData {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface CreateListingFormData {
  title: string;
  description: string;
  price: number;
  city: string;
  images: File[];
}

// Error Types

export interface ApiError {
  message: string;
  status?: number;
  errors?: Record<string, string[]>;
}
