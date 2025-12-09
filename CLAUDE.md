# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**NestHub** is a rental marketplace platform with a Next.js 14 frontend and a planned Scala backend. The repository contains:
- `frontend/`: Next.js 14 application with TypeScript, TailwindCSS, and TanStack Query
- Backend implementation pending (will be Scala-based)

## Development Commands

### Frontend (from `frontend/` directory)

```bash
# Development
npm run dev              # Start dev server on http://localhost:3000
npm run build            # Production build
npm run start            # Start production server

# Code Quality
npm run lint             # Run ESLint
npm run lint:fix         # Fix ESLint issues automatically
npm run format           # Format code with Prettier
npm run type-check       # TypeScript type checking (no emit)

# Testing
npm run test             # Run Vitest tests
npm run test:ui          # Run tests with Vitest UI
npm run test:coverage    # Run tests with coverage report
```

## Architecture & Key Patterns

### Data Fetching Strategy

All server state is managed through **TanStack Query** (React Query), never through local state. The pattern is:

1. **Custom hooks** (`src/hooks/`) wrap TanStack Query's `useQuery` and `useMutation`
2. **API client** (`src/lib/api-client.ts`) provides the HTTP layer with:
   - Automatic JWT token injection via request interceptor
   - 401 auto-redirect via response interceptor
   - Generic `get/post/put/del` wrapper functions
3. **Cache invalidation** is handled in mutation hooks to keep UI in sync

Example:
```typescript
// Hook wraps query logic
export function useListings(params: ListingsQueryParams) {
  return useQuery({
    queryKey: ['listings', 'list', params],
    queryFn: () => get<PaginatedResponse<Listing>>('/listings', params),
    staleTime: 1000 * 60 * 5,
  });
}

// Mutation invalidates cache
export function useCreateListing() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data) => post('/listings', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['listings'] });
    },
  });
}
```

**Never fetch data directly in components** - always use the provided hooks.

### Authentication Flow

- JWT tokens stored in `localStorage` (MVP only - production should use httpOnly cookies)
- Token automatically added to requests via axios interceptor
- User object cached in TanStack Query with key `['auth', 'user']`
- `useAuth()` hook provides: `{ user, isAuthenticated, isAdmin, login, register, logout }`
- 401 responses automatically clear auth and redirect to `/auth/login`

### Route Protection

Use the `<ProtectedRoute>` component for authenticated routes:

```typescript
// Require authentication
<ProtectedRoute>
  <CreateListingForm />
</ProtectedRoute>

// Require admin role
<ProtectedRoute requireAdmin>
  <AdminPanel />
</ProtectedRoute>
```

Never check `user` manually in page components - use `ProtectedRoute`.

### Image Upload Architecture

Images use **presigned URL flow** to upload directly to S3/MinIO:

1. Request presigned URL from backend: `GET /s3/presign?key=uploads/{uuid}.jpg`
2. Upload file directly to returned presigned URL: `PUT {presignedUrl}` with file binary
3. Use returned `publicUrl` in listing creation

The `ImageUploader` component and `src/lib/image-upload.ts` handle this flow with retry logic and progress tracking.

### Type Safety

All API contracts are defined in `src/types/index.ts`:
- Domain models: `User`, `Listing`, `Contract`
- Request/Response types: `LoginRequest`, `AuthResponse`, etc.
- Query params: `ListingsQueryParams`, `AdminListingsQueryParams`

**Always define types for new API endpoints** before implementing hooks.

### File Organization

```
frontend/src/
├── app/              # Next.js App Router - file-based routing
│   ├── layout.tsx   # Root layout with Navbar, providers, Toaster
│   ├── page.tsx     # Home page
│   └── [route]/     # Route segments (listings, auth, admin, create)
├── components/      # React components
│   ├── ui/          # Base components (Button, Input)
│   └── [Feature]    # Feature components (ListingCard, ImageUploader)
├── hooks/           # TanStack Query hooks for API operations
├── lib/             # Pure utilities (no React)
│   ├── api-client.ts   # Axios instance + interceptors
│   ├── auth.ts         # localStorage token/user helpers
│   ├── image-upload.ts # Presigned URL upload logic
│   └── utils.ts        # Misc utilities (cn, formatPrice)
├── types/           # TypeScript definitions
└── test/            # Test setup and utilities
```

### Component Patterns

1. **Server Components by default** - Add `'use client'` only when needed (hooks, interactivity, browser APIs)
2. **Colocation** - Place component tests in `__tests__/` folders next to components
3. **Compound patterns** - `ui/` folder contains primitive components, feature components compose them

### TanStack Query Cache Keys

Follow this convention for queryKey structure:
```typescript
['auth', 'user']                    // Current user
['listings', 'list', params]        // Paginated listings with filters
['listings', 'detail', id]          // Single listing
['admin', 'listings', params]       // Admin queries
```

When invalidating, use prefix matching:
```typescript
queryClient.invalidateQueries({ queryKey: ['listings'] }); // Invalidates all listing queries
```

### Styling Conventions

- Use Tailwind utility classes (mobile-first responsive design)
- Custom classes defined in `globals.css` under `@layer components`
- Use `cn()` utility from `lib/utils.ts` for conditional classes
- Design tokens in `tailwind.config.ts` (colors, spacing, shadows)

Example:
```typescript
import { cn } from '@/lib/utils';

<div className={cn(
  "btn btn-primary",
  isLoading && "opacity-50 cursor-not-allowed"
)} />
```

### Environment Variables

Required variables in `.env.local`:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_S3_BUCKET_URL=http://localhost:9000
```

Access in code: `process.env.NEXT_PUBLIC_API_URL`

**Only `NEXT_PUBLIC_*` prefixed variables are exposed to the browser.**

## Backend Integration Status

The frontend is currently **standalone** with API contracts defined but not yet connected to a backend.

Expected backend endpoints (see `NESTHUB_MVP_DOCUMENTATION.md` section B.2):
- `POST /auth/register` - User registration
- `POST /auth/login` - User authentication
- `GET /listings` - List rentals with filtering/pagination
- `GET /listings/:id` - Get single listing
- `POST /listings` - Create listing (authenticated)
- `GET /s3/presign?key=...` - Get presigned upload URL
- `POST /contracts/generate` - Generate PDF contract
- `GET /admin/listings` - Admin: list pending listings
- `POST /admin/listings/:id/approve` - Admin: approve listing
- `POST /admin/listings/:id/reject` - Admin: reject listing

When integrating:
1. Ensure backend CORS allows `http://localhost:3000`
2. Verify JWT token format matches `Bearer <token>`
3. Check API response structure matches TypeScript types
4. Test presigned URL flow with actual S3/MinIO instance

## Known Limitations & TODOs

- Auth tokens in `localStorage` - migrate to httpOnly cookies for production
- Mock data may exist in hooks - replace with real API calls
- Error boundary not yet implemented
- Refresh token logic not implemented
- No user profile edit page
- No listing edit/delete functionality

Grep for `TODO` comments to find specific integration points:
```bash
grep -r "TODO" frontend/src/
```

## Testing Strategy

- Component tests in `__tests__/` folders using Vitest + Testing Library
- Test setup with Next.js mocks in `src/test/setup.ts`
- QueryClientProvider wrapper required for testing hooks
- Focus on user interactions, not implementation details

Example test pattern:
```typescript
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClientProvider } from '@tanstack/react-query';

const wrapper = ({ children }) => (
  <QueryClientProvider client={queryClient}>
    {children}
  </QueryClientProvider>
);

test('hook fetches data', async () => {
  const { result } = renderHook(() => useListings(), { wrapper });
  await waitFor(() => expect(result.current.isSuccess).toBe(true));
});
```

## Common Pitfalls

1. **Don't fetch data in components** - Always use custom hooks from `src/hooks/`
2. **Don't store server state in React state** - Use TanStack Query cache
3. **Don't forget cache invalidation** - Mutations must invalidate relevant queries
4. **Don't use `'use client'` unnecessarily** - Server components are faster
5. **Don't hardcode API URLs** - Use `process.env.NEXT_PUBLIC_API_URL`
6. **Don't skip TypeScript types** - All API contracts must be typed in `types/index.ts`

## Additional Documentation

- Comprehensive feature documentation: `NESTHUB_MVP_DOCUMENTATION.md`
- Frontend README: `frontend/README.md`
- API contracts and types: `frontend/src/types/index.ts`
