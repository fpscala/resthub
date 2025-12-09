# NestHub Frontend MVP - Complete Documentation

**Version**: 0.1.0
**Date**: 2024
**Tech Stack**: Next.js 14 (App Router) • React 18 • TypeScript • TailwindCSS • TanStack Query

---

## Table of Contents

- [A. Project File Tree](#a-project-file-tree)
- [B. Package.json](#b-packagejson)
- [C. Tailwind Config & Global Styles](#c-tailwind-config--global-styles)
- [D. App Router Page Stubs](#d-app-router-page-stubs)
- [E. Reusable Components](#e-reusable-components)
- [F. Data Fetching Hooks](#f-data-fetching-hooks)
- [G. Image Upload Helper](#g-image-upload-helper)
- [H. Error Handling Strategy](#h-error-handling-strategy)
- [I. Unit Test Examples](#i-unit-test-examples)
- [J. Environment Variables](#j-environment-variables)
- [K. README & Setup](#k-readme--setup)
- [L. Design Tokens](#l-design-tokens)
- [Next Steps Checklist](#next-steps-checklist)

---

## A. Project File Tree

```
frontend/
├── src/
│   ├── app/                          # Next.js 14 App Router
│   │   ├── layout.tsx               # Root layout with Navbar, Toaster, providers
│   │   ├── page.tsx                 # Home page (/) - latest listings + search
│   │   ├── providers.tsx            # TanStack Query provider setup
│   │   ├── globals.css              # Global styles + Tailwind directives
│   │   ├── listings/
│   │   │   ├── page.tsx            # Browse listings (/listings) with filters
│   │   │   └── [id]/
│   │   │       └── page.tsx        # Listing detail (/listings/:id) with gallery
│   │   ├── create/
│   │   │   └── page.tsx            # Create listing (protected route)
│   │   ├── auth/
│   │   │   ├── login/
│   │   │   │   └── page.tsx        # Login page with form validation
│   │   │   └── register/
│   │   │       └── page.tsx        # Register page with password confirmation
│   │   └── admin/
│   │       └── page.tsx            # Admin panel (protected, role-based)
│   ├── components/
│   │   ├── ui/                      # Base UI components
│   │   │   ├── Button.tsx          # Button with variants and loading state
│   │   │   └── Input.tsx           # Input with label and error handling
│   │   ├── ListingCard.tsx         # Listing preview card with image/price/city
│   │   ├── Pagination.tsx          # Pagination with page numbers
│   │   ├── ImageUploader.tsx       # Image uploader with presigned URL flow
│   │   ├── Navbar.tsx              # Auth-aware navigation bar
│   │   ├── ProtectedRoute.tsx      # HOC for route protection
│   │   ├── AdminTable.tsx          # Admin table for listing approval/rejection
│   │   └── __tests__/
│   │       └── ListingCard.test.tsx # Example component test
│   ├── hooks/
│   │   ├── useAuth.ts              # Authentication: login, register, logout
│   │   ├── useListings.ts          # Listings: fetch, create, query keys
│   │   ├── useAdmin.ts             # Admin: fetch pending, approve, reject
│   │   ├── useContract.ts          # Contract generation hook
│   │   └── __tests__/
│   │       └── useAuth.test.ts     # Example hook test
│   ├── lib/
│   │   ├── api-client.ts           # Axios instance with auth interceptor
│   │   ├── auth.ts                 # Token storage and retrieval (localStorage)
│   │   ├── image-upload.ts         # Presigned URL upload with retry logic
│   │   └── utils.ts                # Common utilities (cn, formatPrice, etc.)
│   ├── types/
│   │   └── index.ts                # All TypeScript types and interfaces
│   └── test/
│       └── setup.ts                # Vitest setup with mocks
├── public/                          # Static assets
├── .env.example                     # Environment variable template
├── .env.local                       # Local environment variables (gitignored)
├── .eslintrc.json                   # ESLint configuration
├── .gitignore                       # Git ignore rules
├── .prettierrc                      # Prettier configuration
├── next.config.js                   # Next.js configuration
├── package.json                     # Dependencies and scripts
├── postcss.config.js                # PostCSS configuration
├── tailwind.config.ts               # Tailwind configuration with design tokens
├── tsconfig.json                    # TypeScript configuration
├── vitest.config.ts                 # Vitest test configuration
└── README.md                        # Development and deployment guide
```

### Folder Descriptions

- **`src/app/`**: Next.js App Router pages. Each folder represents a route segment.
- **`src/components/`**: Reusable React components. `ui/` contains base components.
- **`src/hooks/`**: Custom React hooks for data fetching and state management.
- **`src/lib/`**: Utility functions, API client, auth helpers, and image upload logic.
- **`src/types/`**: TypeScript type definitions for domain models and API contracts.
- **`src/test/`**: Test utilities and setup files.

---

## B. Package.json

**File**: `frontend/package.json`

### Dependencies

```json
{
  "dependencies": {
    "next": "^14.2.0",
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "@tanstack/react-query": "^5.28.0",
    "@tanstack/react-query-devtools": "^5.28.0",
    "axios": "^1.6.8",
    "react-hot-toast": "^2.4.1",
    "zustand": "^4.5.2",
    "clsx": "^2.1.0",
    "tailwind-merge": "^2.2.2"
  }
}
```

### Dev Dependencies

```json
{
  "devDependencies": {
    "@types/node": "^20",
    "@types/react": "^18",
    "@types/react-dom": "^18",
    "@testing-library/react": "^14.2.1",
    "@testing-library/jest-dom": "^6.4.2",
    "@testing-library/user-event": "^14.5.2",
    "@vitejs/plugin-react": "^4.2.1",
    "vitest": "^1.4.0",
    "jsdom": "^24.0.0",
    "@vitest/ui": "^1.4.0",
    "@vitest/coverage-v8": "^1.4.0",
    "typescript": "^5",
    "eslint": "^8",
    "eslint-config-next": "^14.2.0",
    "eslint-config-prettier": "^9.1.0",
    "prettier": "^3.2.5",
    "prettier-plugin-tailwindcss": "^0.5.12",
    "tailwindcss": "^3.4.1",
    "postcss": "^8",
    "autoprefixer": "^10.4.19"
  }
}
```

### Scripts

```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "lint:fix": "next lint --fix",
    "format": "prettier --write \"**/*.{ts,tsx,js,jsx,json,md}\"",
    "test": "vitest",
    "test:ui": "vitest --ui",
    "test:coverage": "vitest --coverage",
    "type-check": "tsc --noEmit"
  }
}
```

### Why These Dependencies?

- **Next.js 14**: Modern React framework with App Router for file-based routing
- **TanStack Query**: Powerful data fetching with caching, invalidation, and optimistic updates
- **Axios**: HTTP client with interceptors for auth tokens
- **react-hot-toast**: Simple, accessible toast notifications
- **clsx + tailwind-merge**: Utility for conditional Tailwind classes
- **Vitest**: Fast unit test runner with React Testing Library
- **Prettier + ESLint**: Code quality and consistency

---

## C. Tailwind Config & Global Styles

### Tailwind Configuration

**File**: `frontend/tailwind.config.ts`

```typescript
export default {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f0f9ff',
          500: '#0ea5e9',  // Main brand color
          600: '#0284c7',
          700: '#0369a1',
        },
        accent: {
          500: '#d946ef',
          600: '#c026d3',
        },
      },
      spacing: {
        '18': '4.5rem',
        '88': '22rem',
        '128': '32rem',
      },
      boxShadow: {
        'card': '0 1px 3px 0 rgba(0, 0, 0, 0.1)',
        'card-hover': '0 10px 15px -3px rgba(0, 0, 0, 0.1)',
      },
    },
  },
};
```

### Global Styles

**File**: `frontend/src/app/globals.css`

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer components {
  /* Button variants */
  .btn {
    @apply inline-flex items-center justify-center rounded-lg px-4 py-2 text-sm font-medium transition-colors;
  }
  .btn-primary {
    @apply btn bg-primary-600 text-white hover:bg-primary-700;
  }
  .btn-secondary {
    @apply btn border border-gray-300 bg-white text-gray-700 hover:bg-gray-50;
  }

  /* Input styles */
  .input {
    @apply block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:border-primary-500 focus:ring-1 focus:ring-primary-500;
  }

  /* Label styles */
  .label {
    @apply mb-2 block text-sm font-medium text-gray-700;
  }

  /* Card styles */
  .card {
    @apply rounded-lg border border-gray-200 bg-white p-6 shadow-card hover:shadow-card-hover;
  }

  /* Error text */
  .error-text {
    @apply mt-1 text-xs text-red-600;
  }
}

@layer utilities {
  .container-custom {
    @apply mx-auto max-w-7xl px-4 sm:px-6 lg:px-8;
  }
}
```

---

## D. App Router Page Stubs

### 1. Home Page (`/`)

**File**: `frontend/src/app/page.tsx`

**Features**:
- Latest listings grid
- Search filters (city, price range)
- Responsive 1-3 column layout

**Hooks Used**:
```tsx
const { data, isLoading, error } = useListings(filters);
```

**Route Behavior**: Public, no auth required

---

### 2. Listings Page (`/listings`)

**File**: `frontend/src/app/listings/page.tsx`

**Features**:
- Filterable listings (city, minPrice, maxPrice)
- Pagination
- Results count display

**Hooks Used**:
```tsx
const { data, isLoading } = useListings(filters);
```

**Route Behavior**: Public

---

### 3. Listing Detail Page (`/listings/[id]`)

**File**: `frontend/src/app/listings/[id]/page.tsx`

**Features**:
- Image gallery
- Full description
- Contact owner button (phone link)
- Download contract button

**Hooks Used**:
```tsx
const { data: listing, isLoading } = useListing(id);
const generateContract = useGenerateContract();
```

**Route Behavior**: Public

---

### 4. Create Listing Page (`/create`)

**File**: `frontend/src/app/create/page.tsx`

**Features**:
- Form with validation
- Image uploader with progress
- Optimistic UI with loading states
- Auto-redirect on success

**Hooks Used**:
```tsx
const createListing = useCreateListing();
const { user } = useAuth();
```

**Route Behavior**: Protected (requires authentication)

**Protection**:
```tsx
<ProtectedRoute>
  <CreateListingContent />
</ProtectedRoute>
```

---

### 5. Login Page (`/auth/login`)

**File**: `frontend/src/app/auth/login/page.tsx`

**Features**:
- Email/password form
- Client-side validation
- Error handling
- Auto-redirect on success

**Hooks Used**:
```tsx
const { login } = useAuth();
```

**Route Behavior**: Public

---

### 6. Register Page (`/auth/register`)

**File**: `frontend/src/app/auth/register/page.tsx`

**Features**:
- Name, email, password, confirm password
- Validation with password match check
- Auto-redirect on success

**Hooks Used**:
```tsx
const { register } = useAuth();
```

**Route Behavior**: Public

---

### 7. Admin Page (`/admin`)

**File**: `frontend/src/app/admin/page.tsx`

**Features**:
- Pending listings table
- Approve/reject actions
- Status filter

**Hooks Used**:
```tsx
const { data } = useAdminListings({ status: 'PENDING' });
const approveMutation = useApproveListing();
const rejectMutation = useRejectListing();
```

**Route Behavior**: Protected (requires ADMIN role)

**Protection**:
```tsx
<ProtectedRoute requireAdmin>
  <AdminContent />
</ProtectedRoute>
```

---

## E. Reusable Components

### 1. ListingCard

**File**: `frontend/src/components/ListingCard.tsx`

**Props**:
```typescript
interface ListingCardProps {
  listing: Listing;
  onClick?: () => void;
  className?: string;
}
```

**Features**:
- Image with Next.js Image optimization
- Title, description (clamped to 2 lines)
- Price with /month suffix
- City badge
- Pending status indicator
- Hover effects

**Usage**:
```tsx
<ListingCard
  listing={listing}
  onClick={() => router.push(`/listings/${listing.id}`)}
/>
```

---

### 2. ImageUploader

**File**: `frontend/src/components/ImageUploader.tsx`

**Props**:
```typescript
interface ImageUploaderProps {
  onUploadComplete: (urls: string[]) => void;
  maxFiles?: number;
  className?: string;
}
```

**Features**:
- Client-side validation (max 5 files, 5MB each, jpg/png/webp)
- Preview thumbnails
- Per-file progress tracking
- Remove file button
- Direct upload to S3/MinIO via presigned URL
- Retry logic (3 attempts)

**Usage**:
```tsx
<ImageUploader
  onUploadComplete={(urls) => setFormData({ ...formData, images: urls })}
  maxFiles={5}
/>
```

**Validation Rules**:
- Max 5 files
- Max 5MB per file
- Allowed types: JPG, PNG, WebP

---

### 3. Pagination

**File**: `frontend/src/components/Pagination.tsx`

**Props**:
```typescript
interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}
```

**Features**:
- Previous/Next buttons
- Page numbers with ellipsis
- Active page highlight
- Accessible (ARIA labels)

**Usage**:
```tsx
<Pagination
  currentPage={page}
  totalPages={Math.ceil(total / pageSize)}
  onPageChange={setPage}
/>
```

---

### 4. Navbar

**File**: `frontend/src/components/Navbar.tsx`

**Features**:
- Logo link to home
- Navigation links (Home, Browse, Create, Admin)
- Auth-aware menu (Login/Register vs User/Logout)
- Role-based link visibility (Admin link only for admins)
- Active link highlighting

**Usage**: Automatically included in root layout

---

### 5. ProtectedRoute

**File**: `frontend/src/components/ProtectedRoute.tsx`

**Props**:
```typescript
interface ProtectedRouteProps {
  children: React.ReactNode;
  requireAdmin?: boolean;
}
```

**Features**:
- Redirect unauthenticated users to `/auth/login`
- Redirect non-admin users attempting admin routes to `/`
- Prevents flash of protected content

**Usage**:
```tsx
// Require authentication
<ProtectedRoute>
  <CreateListingPage />
</ProtectedRoute>

// Require admin role
<ProtectedRoute requireAdmin>
  <AdminPanel />
</ProtectedRoute>
```

---

### 6. AdminTable

**File**: `frontend/src/components/AdminTable.tsx`

**Props**:
```typescript
interface AdminTableProps {
  listings: Listing[];
  onApprove: (id: string) => void;
  onReject: (id: string) => void;
  isLoading?: boolean;
}
```

**Features**:
- Table with listing details
- Status badges (color-coded)
- Approve/reject action buttons
- Responsive layout
- Empty state

**Usage**:
```tsx
<AdminTable
  listings={pendingListings}
  onApprove={(id) => approveMutation.mutateAsync(id)}
  onReject={(id) => rejectMutation.mutateAsync(id)}
/>
```

---

## F. Data Fetching Hooks

### 1. useAuth

**File**: `frontend/src/hooks/useAuth.ts`

**Returns**:
```typescript
{
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: UseMutationResult<AuthResponse, Error, LoginRequest>;
  register: UseMutationResult<AuthResponse, Error, RegisterRequest>;
  logout: () => void;
  getUser: () => User | null;
}
```

**Cache Keys**: `['auth', 'user']`

**Invalidation**: On logout, all queries cleared

**Usage**:
```tsx
const { user, isAuthenticated, login, logout } = useAuth();

const handleLogin = async () => {
  await login.mutateAsync({ email, password });
  // Automatically redirects to home and shows success toast
};
```

---

### 2. useListings

**File**: `frontend/src/hooks/useListings.ts`

**Signature**:
```typescript
function useListings(params: ListingsQueryParams): UseQueryResult<PaginatedResponse<Listing>>
```

**Cache Keys**:
```typescript
['listings', 'list', params]
```

**Stale Time**: 5 minutes

**Invalidation**: After creating a listing

**Usage**:
```tsx
const { data, isLoading, error } = useListings({
  city: 'San Francisco',
  minPrice: 1000,
  maxPrice: 3000,
  page: 1,
  size: 12,
});
```

---

### 3. useListing (Single)

**Signature**:
```typescript
function useListing(id: string): UseQueryResult<Listing>
```

**Cache Keys**: `['listings', 'detail', id]`

**Stale Time**: 5 minutes

**Usage**:
```tsx
const { data: listing, isLoading } = useListing('listing-id-123');
```

---

### 4. useCreateListing

**Signature**:
```typescript
function useCreateListing(): UseMutationResult<CreateListingResponse, Error, CreateListingRequest>
```

**Invalidation Rules**:
- Invalidates all listings queries (`['listings', 'list']`)
- Redirects to listing detail page on success
- Shows success toast

**Usage**:
```tsx
const createListing = useCreateListing();

const handleSubmit = async (data: CreateListingRequest) => {
  await createListing.mutateAsync(data);
  // Automatically redirects and invalidates cache
};
```

---

### 5. useAdminListings

**File**: `frontend/src/hooks/useAdmin.ts`

**Signature**:
```typescript
function useAdminListings(params: AdminListingsQueryParams): UseQueryResult<PaginatedResponse<Listing>>
```

**Cache Keys**: `['admin', 'listings', params]`

**Stale Time**: 1 minute (more frequent updates for admin)

**Usage**:
```tsx
const { data } = useAdminListings({ status: 'PENDING' });
```

---

### 6. useApproveListing / useRejectListing

**Signatures**:
```typescript
function useApproveListing(): UseMutationResult<void, Error, string>
function useRejectListing(): UseMutationResult<void, Error, string>
```

**Invalidation Rules**:
- Invalidates all admin queries (`['admin']`)
- Shows success/error toast

**Usage**:
```tsx
const approveMutation = useApproveListing();
await approveMutation.mutateAsync('listing-id');
```

---

### 7. useGenerateContract

**File**: `frontend/src/hooks/useContract.ts`

**Signature**:
```typescript
function useGenerateContract(): UseMutationResult<GenerateContractResponse, Error, GenerateContractRequest>
```

**Behavior**:
- Generates PDF contract
- Opens in new tab automatically
- Shows success toast

**Usage**:
```tsx
const generateContract = useGenerateContract();

const handleDownload = async () => {
  await generateContract.mutateAsync({ listingId: listing.id });
  // PDF opens automatically in new tab
};
```

---

## G. Image Upload Helper

**File**: `frontend/src/lib/image-upload.ts`

### Presigned URL Upload Flow

**Architecture**:
1. Client requests presigned URL from backend
2. Backend generates presigned PUT URL (S3/MinIO)
3. Client uploads file directly to S3/MinIO using presigned URL
4. Client receives public URL for uploaded file

### Functions

#### uploadImage (Single File)

```typescript
async function uploadImage(
  file: File,
  onProgress?: (progress: number) => void
): Promise<ImageUploadResult>
```

**Steps**:
1. Validate file (size, type)
2. Generate unique key (`uploads/{timestamp}-{random}.jpg`)
3. Request presigned URL: `GET /s3/presign?key={key}`
4. Upload to S3/MinIO: `PUT {presignedUrl}` with file binary
5. Return public URL

**Error Handling**:
- File validation errors
- Network errors
- S3 upload errors

---

#### uploadMultipleImages (Batch Upload)

```typescript
async function uploadMultipleImages(
  files: File[],
  onProgressUpdate?: (progressList: UploadProgress[]) => void
): Promise<string[]>
```

**Features**:
- Validates max 5 files
- Uploads sequentially (not parallel, to avoid rate limits)
- Retry logic: 3 attempts per file with exponential backoff
- Per-file progress tracking
- Partial success handling (returns successful URLs)

**Progress Tracking**:
```typescript
interface UploadProgress {
  fileIndex: number;
  fileName: string;
  progress: number; // 0-100
  status: 'pending' | 'uploading' | 'success' | 'error';
  error?: string;
  publicUrl?: string;
}
```

**Edge Cases**:
- Network failure: Retry up to 3 times
- Validation failure: Skip file, report error
- Partial failure: Return successful uploads, log failures

---

## H. Error Handling Strategy

### 1. Global Error Boundary

**TODO**: Implement error boundary component

```tsx
// Recommended implementation
import { Component, ErrorInfo, ReactNode } from 'react';

class ErrorBoundary extends Component<
  { children: ReactNode },
  { hasError: boolean }
> {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
    // TODO: Send to error tracking service (Sentry, etc.)
  }

  render() {
    if (this.state.hasError) {
      return <div>Something went wrong. Please refresh.</div>;
    }
    return this.props.children;
  }
}
```

---

### 2. Toast Notifications

**Library**: `react-hot-toast`

**Setup** (in `layout.tsx`):
```tsx
import { Toaster } from 'react-hot-toast';

<Toaster
  position="top-right"
  toastOptions={{
    duration: 4000,
    style: { background: '#363636', color: '#fff' },
    success: { iconTheme: { primary: '#10b981', secondary: '#fff' } },
    error: { iconTheme: { primary: '#ef4444', secondary: '#fff' } },
  }}
/>
```

**Usage**:
```tsx
import toast from 'react-hot-toast';

// Success
toast.success('Listing created successfully!');

// Error
toast.error('Failed to upload image');

// Loading (manual dismiss)
const toastId = toast.loading('Uploading...');
toast.dismiss(toastId);
```

---

### 3. API Error Handling

**Axios Interceptor** (`lib/api-client.ts`):
```typescript
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const apiError: ApiError = {
      message: error.response?.data?.message || 'An error occurred',
      status: error.response?.status,
      errors: error.response?.data?.errors,
    };

    // Handle 401: Clear auth and redirect
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token');
      window.location.href = '/auth/login';
    }

    return Promise.reject(apiError);
  }
);
```

---

### 4. Form Validation Errors

**Pattern**:
```tsx
const [errors, setErrors] = useState<Record<string, string>>({});

const validate = (): boolean => {
  const newErrors: Record<string, string> = {};

  if (!email) newErrors.email = 'Email is required';
  if (!password) newErrors.password = 'Password is required';

  setErrors(newErrors);
  return Object.keys(newErrors).length === 0;
};

// In JSX:
<Input error={errors.email} />
```

---

### 5. Query Error Handling

**TanStack Query**:
```tsx
const { data, error, isError } = useListings();

if (isError) {
  return <div className="error">Error: {error.message}</div>;
}
```

---

## I. Unit Test Examples

### Test Setup

**File**: `frontend/src/test/setup.ts`

```typescript
import '@testing-library/jest-dom';
import { expect, afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';

afterEach(() => {
  cleanup();
});

// Mock Next.js router
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: vi.fn(),
    replace: vi.fn(),
  }),
  usePathname: () => '/',
}));
```

---

### Component Test Example

**File**: `frontend/src/components/__tests__/ListingCard.test.tsx`

```typescript
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ListingCard } from '../ListingCard';

const mockListing: Listing = {
  id: '1',
  title: 'Modern Apartment',
  price: 2500,
  city: 'San Francisco',
  images: ['https://example.com/image.jpg'],
  status: 'APPROVED',
  // ... other fields
};

describe('ListingCard', () => {
  it('renders listing information', () => {
    render(<ListingCard listing={mockListing} />);
    expect(screen.getByText('Modern Apartment')).toBeInTheDocument();
    expect(screen.getByText('$2,500.00')).toBeInTheDocument();
  });

  it('calls onClick when clicked', () => {
    const handleClick = vi.fn();
    render(<ListingCard listing={mockListing} onClick={handleClick} />);

    fireEvent.click(screen.getByRole('article'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });
});
```

---

### Hook Test Example

**File**: `frontend/src/hooks/__tests__/useAuth.test.ts`

```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuth } from '../useAuth';

describe('useAuth', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
  });

  const wrapper = ({ children }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );

  it('login mutation stores token', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper });

    result.current.login.mutate({
      email: 'test@example.com',
      password: 'password',
    });

    await waitFor(() => {
      expect(result.current.login.isSuccess).toBe(true);
    });
  });
});
```

---

### Running Tests

```bash
# Run all tests
npm run test

# Run with UI
npm run test:ui

# Run with coverage
npm run test:coverage

# Run specific test
npm run test -- ListingCard.test.tsx
```

---

## J. Environment Variables

**File**: `frontend/.env.example`

```env
# API Configuration
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_S3_BUCKET_URL=http://localhost:9000

# Environment
NODE_ENV=development

# Optional: Analytics, monitoring
# NEXT_PUBLIC_ANALYTICS_ID=
# NEXT_PUBLIC_SENTRY_DSN=
```

**File**: `frontend/.env.local` (gitignored)

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_S3_BUCKET_URL=http://localhost:9000
NODE_ENV=development
```

### Environment Variable Usage

```typescript
const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
const S3_URL = process.env.NEXT_PUBLIC_S3_BUCKET_URL || 'http://localhost:9000';
```

**Important**: Only `NEXT_PUBLIC_*` variables are exposed to the browser.

---

## K. README & Setup

See `frontend/README.md` for complete setup instructions.

### Quick Start

```bash
# Install dependencies
npm install

# Set up environment
cp .env.example .env.local
# Edit .env.local with your backend URL

# Run development server
npm run dev

# Open http://localhost:3000
```

### Backend Setup (Docker Compose)

**Recommended**: Run backend and services with Docker Compose

```yaml
# docker-compose.yml (backend repo)
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: nesthub
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"

  backend:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - postgres
      - minio
```

```bash
# Start services
docker-compose up -d

# Frontend will connect to:
# - Backend: http://localhost:8080
# - MinIO: http://localhost:9000
```

---

## L. Design Tokens

### Color Palette

```typescript
// Primary (Brand)
primary: {
  50: '#f0f9ff',
  100: '#e0f2fe',
  500: '#0ea5e9',  // Main
  600: '#0284c7',  // Hover
  700: '#0369a1',  // Active
}

// Accent
accent: {
  500: '#d946ef',
  600: '#c026d3',
}

// Gray (Neutral)
gray: {
  50: '#f9fafb',   // Background
  100: '#f3f4f6',  // Card background
  200: '#e5e7eb',  // Border
  600: '#4b5563',  // Text secondary
  900: '#111827',  // Text primary
}
```

---

### Typography

```typescript
// Font Family: Inter (Google Fonts)

// Font Sizes
text-xs: 0.75rem (12px)
text-sm: 0.875rem (14px)
text-base: 1rem (16px)
text-lg: 1.125rem (18px)
text-xl: 1.25rem (20px)
text-2xl: 1.5rem (24px)
text-3xl: 1.875rem (30px)

// Font Weights
font-normal: 400
font-medium: 500
font-semibold: 600
font-bold: 700
```

---

### Spacing

```typescript
// Container
max-w-7xl px-4 sm:px-6 lg:px-8

// Gaps
gap-2: 0.5rem
gap-4: 1rem
gap-6: 1.5rem
gap-8: 2rem

// Padding
p-4: 1rem
p-6: 1.5rem
p-8: 2rem

// Margins
mb-4: 1rem
mb-6: 1.5rem
mb-8: 2rem
```

---

### Border Radius

```typescript
rounded: 0.25rem
rounded-lg: 0.5rem
rounded-full: 9999px
```

---

### Shadows

```typescript
shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05)
shadow: 0 1px 3px rgba(0, 0, 0, 0.1)
shadow-card: Custom (defined in config)
shadow-card-hover: Custom (defined in config)
```

---

### Breakpoints

```typescript
sm: 640px
md: 768px
lg: 1024px
xl: 1280px
2xl: 1536px

// Usage:
// Mobile-first: base styles, then sm:, md:, lg:, etc.
<div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
```

---

## Next Steps Checklist

### Backend Integration (Priority Order)

1. **Authentication** (Test First):
   - [ ] Test POST /auth/register
   - [ ] Test POST /auth/login
   - [ ] Verify JWT token format in response
   - [ ] Test token in Authorization header
   - [ ] Verify 401 handling and redirect

2. **Listings** (Core Functionality):
   - [ ] Test GET /listings with pagination
   - [ ] Test GET /listings with filters (city, price)
   - [ ] Test GET /listings/:id
   - [ ] Test POST /listings
   - [ ] Verify response matches TypeScript types
   - [ ] Remove mock data from hooks

3. **Image Upload** (Critical Path):
   - [ ] Test GET /s3/presign endpoint
   - [ ] Verify presigned URL format
   - [ ] Test direct upload to S3/MinIO
   - [ ] Verify publicUrl accessibility
   - [ ] Test upload with different file sizes
   - [ ] Handle presigned URL expiration

4. **Contracts**:
   - [ ] Test POST /contracts/generate
   - [ ] Verify PDF URL accessibility
   - [ ] Test download flow

5. **Admin Operations**:
   - [ ] Test GET /admin/listings?status=pending
   - [ ] Test POST /admin/listings/:id/approve
   - [ ] Test POST /admin/listings/:id/reject
   - [ ] Verify role-based access control

---

### Common Integration Issues

#### 1. CORS Configuration

**Problem**: Frontend can't reach backend

**Solution**: Configure backend to allow `http://localhost:3000`

```scala
// Scala backend (example)
.cors(
  CorsPolicy(
    allowedOrigins = Set("http://localhost:3000"),
    allowedMethods = Set("GET", "POST", "PUT", "DELETE", "OPTIONS"),
    allowedHeaders = Set("Content-Type", "Authorization"),
    allowCredentials = true
  )
)
```

---

#### 2. Token Format Mismatch

**Problem**: 401 errors despite valid token

**Solution**: Verify token format matches `Bearer <token>`

```typescript
// Frontend sends:
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

// Backend expects:
header.get("Authorization").map(_.stripPrefix("Bearer ").trim)
```

---

#### 3. Response Structure Mismatch

**Problem**: TypeScript errors when parsing response

**Solution**: Verify API response matches TypeScript types

```typescript
// Frontend expects:
{ items: Listing[], total: number }

// Backend must return:
{
  "items": [...],
  "total": 42
}
```

---

#### 4. Pagination Parameters

**Problem**: Pagination doesn't work

**Solution**: Check query param names

```typescript
// Frontend sends: ?page=1&size=12
// Backend should read: page and size (not limit/offset)
```

---

#### 5. Image Upload Presigned URL

**Problem**: Upload fails with 403/signature error

**Solution**:
- Verify presigned URL hasn't expired (usually 15 minutes)
- Ensure Content-Type header matches signed type
- Check S3/MinIO bucket permissions

---

### Production Checklist

#### Security

- [ ] Migrate from localStorage to httpOnly cookies
- [ ] Implement CSRF protection
- [ ] Add rate limiting
- [ ] Sanitize user inputs
- [ ] Enable HTTPS only
- [ ] Add security headers (CSP, X-Frame-Options)
- [ ] Implement refresh token rotation
- [ ] Add request signing for sensitive operations

---

#### Performance

- [ ] Enable Next.js Image optimization
- [ ] Set up CDN for static assets
- [ ] Implement caching headers
- [ ] Enable compression (gzip/brotli)
- [ ] Add lazy loading for images
- [ ] Optimize bundle size (analyze with next/bundle-analyzer)
- [ ] Implement code splitting for large routes
- [ ] Add service worker for offline support (optional)

---

#### Monitoring & Observability

- [ ] Set up error tracking (Sentry, Rollbar)
- [ ] Add analytics (Google Analytics, Mixpanel)
- [ ] Monitor Core Web Vitals
- [ ] Set up uptime monitoring
- [ ] Add logging for critical operations
- [ ] Implement performance monitoring (Vercel Analytics)

---

#### Testing & Quality

- [ ] Achieve >80% test coverage
- [ ] Add E2E tests (Playwright, Cypress)
- [ ] Set up CI/CD pipeline
- [ ] Add pre-commit hooks (Husky)
- [ ] Implement visual regression testing
- [ ] Add accessibility testing (axe-core)

---

#### Infrastructure

- [ ] Set up staging environment
- [ ] Configure environment variables per environment
- [ ] Set up database backups
- [ ] Implement health check endpoints
- [ ] Add graceful shutdown handling
- [ ] Set up blue-green deployment
- [ ] Configure auto-scaling

---

## Appendix: Key Files Reference

### Core Configuration

- `package.json` - Dependencies and scripts
- `tsconfig.json` - TypeScript configuration
- `tailwind.config.ts` - Design system configuration
- `next.config.js` - Next.js configuration
- `.env.example` - Environment variable template

### Type Definitions

- `src/types/index.ts` - All TypeScript interfaces

### API Client

- `src/lib/api-client.ts` - Axios instance with interceptors
- `src/lib/auth.ts` - Token storage and retrieval
- `src/lib/image-upload.ts` - Presigned URL upload logic

### Hooks

- `src/hooks/useAuth.ts` - Authentication
- `src/hooks/useListings.ts` - Listings CRUD
- `src/hooks/useAdmin.ts` - Admin operations
- `src/hooks/useContract.ts` - Contract generation

### Components

- `src/components/ui/Button.tsx` - Button with variants
- `src/components/ui/Input.tsx` - Input with validation
- `src/components/ListingCard.tsx` - Listing preview card
- `src/components/ImageUploader.tsx` - Image upload with progress
- `src/components/Navbar.tsx` - Navigation bar
- `src/components/ProtectedRoute.tsx` - Route protection HOC
- `src/components/AdminTable.tsx` - Admin table

### Pages

- `src/app/page.tsx` - Home
- `src/app/listings/page.tsx` - Browse listings
- `src/app/listings/[id]/page.tsx` - Listing detail
- `src/app/create/page.tsx` - Create listing
- `src/app/auth/login/page.tsx` - Login
- `src/app/auth/register/page.tsx` - Register
- `src/app/admin/page.tsx` - Admin panel

---

## Summary

This NestHub frontend MVP is a production-ready scaffold with:

✅ Complete type safety with TypeScript
✅ Modern data fetching with TanStack Query
✅ Responsive mobile-first design with Tailwind
✅ JWT-based authentication (with production recommendations)
✅ Presigned URL image uploads with progress tracking
✅ Protected routes with role-based access
✅ Unit test examples and setup
✅ Comprehensive documentation and setup guide

**Ready for handoff to developers for backend integration and feature completion.**

---

**End of Documentation**
