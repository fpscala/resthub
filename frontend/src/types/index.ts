// Domain Models

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string;
  role: Role;
  status: 'ACTIVE' | 'INACTIVE' | 'EXPIRED';
  createdAt: string;
  updatedAt?: string;
  marketId?: string;
}

export interface Role {
  id: string;
  name: string;
  privileges: Privilege[];
  description?: string;
  isSystem?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface Privilege {
  // User management
  'create_user'?: boolean;
  'update_user'?: boolean;
  'update_any_user'?: boolean;
  'delete_user'?: boolean;
  'view_users'?: boolean;
  'create_super_user'?: boolean;
  // Role management
  'create_role'?: boolean;
  'update_role'?: boolean;
  'delete_role'?: boolean;
  'view_roles'?: boolean;
  // Listings
  'admin_listings_view_all'?: boolean;
  'admin_listings_approve'?: boolean;
  'admin_listings_reject'?: boolean;
  // Assets
  'create_asset'?: boolean;
}

export interface Listing {
  id: string;
  ownerId: string;
  title: string;
  description: string;
  price: number;
  city: string;
  images: string[];
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  rejectionReason?: string;
  createdAt: string;
  updatedAt: string;
  approvedAt?: string;
  approvedBy?: string;
}

export interface Contract {
  id: string;
  listingId: string;
  pdfUrl: string;
  createdAt: string;
}

// API Request/Response Types

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface AuthResponse {
  user: User;
  tokens: AuthTokens;
}

export interface CreateListingRequest {
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
  data: T[];
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
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
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
  error_code?: string;
  details?: Record<string, any>;
}

// Additional useful types
export interface SuccessResponse {
  message: string;
}

export interface ObjectIdResponse {
  id: string;
  message?: string;
}

export interface RejectListingRequest {
  reason: string;
}
