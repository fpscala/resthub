# NestHub Frontend MVP

Production-ready frontend for NestHub - a rental marketplace built with Next.js, React, TailwindCSS, and TanStack Query.

## 🚀 Tech Stack

- **Framework**: Next.js 14 (App Router)
- **Language**: TypeScript
- **Styling**: TailwindCSS
- **Data Fetching**: TanStack Query (React Query)
- **HTTP Client**: Axios
- **State Management**: Zustand (minimal, mainly using TanStack Query)
- **Forms**: Controlled components with validation
- **Testing**: Vitest + Testing Library
- **Code Quality**: ESLint + Prettier

## 📁 Project Structure

```
frontend/
├── src/
│   ├── app/                    # Next.js App Router pages
│   │   ├── layout.tsx         # Root layout with providers
│   │   ├── page.tsx           # Home page (/)
│   │   ├── providers.tsx      # TanStack Query provider
│   │   ├── listings/          # Listings pages
│   │   │   ├── page.tsx       # Browse listings (/listings)
│   │   │   └── [id]/
│   │   │       └── page.tsx   # Listing detail (/listings/:id)
│   │   ├── create/
│   │   │   └── page.tsx       # Create listing (protected)
│   │   ├── auth/
│   │   │   ├── login/
│   │   │   │   └── page.tsx   # Login page
│   │   │   └── register/
│   │   │       └── page.tsx   # Register page
│   │   └── admin/
│   │       └── page.tsx       # Admin panel (protected, role-based)
│   ├── components/            # Reusable components
│   │   ├── ui/                # Base UI components
│   │   │   ├── Button.tsx
│   │   │   └── Input.tsx
│   │   ├── ListingCard.tsx
│   │   ├── Pagination.tsx
│   │   ├── ImageUploader.tsx
│   │   ├── Navbar.tsx
│   │   ├── ProtectedRoute.tsx
│   │   └── AdminTable.tsx
│   ├── hooks/                 # Custom React hooks
│   │   ├── useAuth.ts         # Authentication hook
│   │   ├── useListings.ts     # Listings data hooks
│   │   ├── useAdmin.ts        # Admin operations hooks
│   │   └── useContract.ts     # Contract generation hook
│   ├── lib/                   # Utilities and helpers
│   │   ├── api-client.ts      # Axios instance with interceptors
│   │   ├── auth.ts            # Auth token management
│   │   ├── image-upload.ts    # Presigned URL upload logic
│   │   └── utils.ts           # Common utilities
│   ├── types/                 # TypeScript type definitions
│   │   └── index.ts
│   └── test/                  # Test utilities
│       └── setup.ts
├── package.json
├── tsconfig.json
├── tailwind.config.ts
├── next.config.js
├── vitest.config.ts
├── .env.example
├── .env.local
└── README.md
```

## 🔧 Setup & Installation

### Prerequisites

- Node.js 18+ (or use nvm with `.nvmrc`)
- pnpm, npm, or yarn

### Installation

```bash
# Clone the repository
git clone <repository-url>
cd frontend

# Install dependencies
npm install
# or
pnpm install
# or
yarn install

# Copy environment variables
cp .env.example .env.local

# Update .env.local with your backend API URL
# NEXT_PUBLIC_API_URL=http://localhost:8080/api
# NEXT_PUBLIC_S3_BUCKET_URL=http://localhost:9000
```

### Environment Variables

Required environment variables:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_S3_BUCKET_URL=http://localhost:9000
NODE_ENV=development
```

## 🏃 Running the Application

### Development

```bash
npm run dev
# or
pnpm dev
# or
yarn dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Build

```bash
npm run build
npm run start
```

### Testing

```bash
# Run tests
npm run test

# Run tests with UI
npm run test:ui

# Run tests with coverage
npm run test:coverage
```

### Linting & Formatting

```bash
# Run ESLint
npm run lint

# Fix linting issues
npm run lint:fix

# Format code with Prettier
npm run format

# Type check
npm run type-check
```

## 🔐 Authentication

### JWT-Based Authentication

**MVP Implementation** (Current):
- Access token stored in `localStorage`
- Token included in `Authorization: Bearer <token>` header
- Simple and works for MVP/development

**Security Caveats**:
- ⚠️ `localStorage` is vulnerable to XSS attacks
- JavaScript can access the token
- Not recommended for production

**Production Recommendation**:
- Store access token in **httpOnly cookie**
- Use refresh token rotation
- Implement CSRF protection
- httpOnly cookies cannot be accessed by JavaScript
- More secure against XSS attacks

### Auth Hooks

```tsx
import { useAuth } from '@/hooks/useAuth';

function MyComponent() {
  const { user, isAuthenticated, isAdmin, login, logout } = useAuth();

  const handleLogin = async () => {
    await login.mutateAsync({ email, password });
  };

  return (
    <div>
      {isAuthenticated ? (
        <p>Welcome, {user.name}!</p>
      ) : (
        <button onClick={handleLogin}>Login</button>
      )}
    </div>
  );
}
```

## 📡 API Integration

### Backend Endpoints

The frontend expects the following API endpoints:

```
POST   /auth/register          { name, email, password } -> { user, token }
POST   /auth/login             { email, password } -> { user, token }
GET    /listings               ?city&minPrice&maxPrice&page&size -> { items, total }
GET    /listings/:id           -> { Listing }
POST   /listings               { ownerId, title, description, price, city, images } -> { id }
GET    /s3/presign             ?key=uploads/{key} -> { url, publicUrl }
POST   /contracts/generate     { listingId } -> { pdfUrl }
GET    /admin/listings         ?status=pending -> { items }
POST   /admin/listings/:id/approve
POST   /admin/listings/:id/reject
```

### Data Fetching Hooks

#### useListings

```tsx
import { useListings } from '@/hooks/useListings';

function ListingsPage() {
  const { data, isLoading, error } = useListings({
    city: 'San Francisco',
    minPrice: 1000,
    maxPrice: 3000,
    page: 1,
    size: 12,
  });

  return <div>{/* Render listings */}</div>;
}
```

#### useCreateListing

```tsx
import { useCreateListing } from '@/hooks/useListings';

function CreateForm() {
  const createListing = useCreateListing();

  const handleSubmit = async (data) => {
    await createListing.mutateAsync({
      ownerId: user.id,
      title: 'Modern Apartment',
      description: 'Beautiful 2BR...',
      price: 2500,
      city: 'San Francisco',
      images: ['https://...'],
    });
    // Automatically redirects on success
  };
}
```

## 📤 Image Upload Flow

### Presigned URL Architecture

1. **Request presigned URL** from backend:
   ```
   GET /s3/presign?key=uploads/{uuid}.jpg
   -> { url: "presigned-PUT-url", publicUrl: "final-accessible-url" }
   ```

2. **Upload directly to S3/MinIO**:
   ```
   PUT {presigned-url}
   Headers: { Content-Type: image/jpeg }
   Body: [file-binary]
   ```

3. **Use publicUrl** in listing creation

### ImageUploader Component

```tsx
import { ImageUploader } from '@/components/ImageUploader';

function CreateListing() {
  const [images, setImages] = useState<string[]>([]);

  return (
    <ImageUploader
      onUploadComplete={(urls) => setImages(urls)}
      maxFiles={5}
    />
  );
}
```

**Features**:
- Client-side validation (max 5 files, 5MB each, jpg/png/webp)
- Per-file progress tracking
- Preview thumbnails
- Retry logic (up to 3 attempts)
- Error handling with rollback

## 🛡️ Protected Routes

### Usage

```tsx
import { ProtectedRoute } from '@/components/ProtectedRoute';

// Require authentication
export default function CreatePage() {
  return (
    <ProtectedRoute>
      <CreateListingForm />
    </ProtectedRoute>
  );
}

// Require admin role
export default function AdminPage() {
  return (
    <ProtectedRoute requireAdmin>
      <AdminPanel />
    </ProtectedRoute>
  );
}
```

## 🧪 Testing

### Component Tests

```tsx
// src/components/__tests__/ListingCard.test.tsx
import { render, screen } from '@testing-library/react';
import { ListingCard } from '../ListingCard';

test('renders listing card', () => {
  render(<ListingCard listing={mockListing} />);
  expect(screen.getByText('Modern Apartment')).toBeInTheDocument();
});
```

### Hook Tests

```tsx
// src/hooks/__tests__/useAuth.test.ts
import { renderHook, waitFor } from '@testing-library/react';
import { useAuth } from '../useAuth';

test('login mutation stores token', async () => {
  const { result } = renderHook(() => useAuth(), { wrapper });
  result.current.login.mutate({ email, password });
  await waitFor(() => expect(result.current.user).toBeDefined());
});
```

## 🎨 Design Tokens

### Colors

```css
primary: #0ea5e9 (sky-500)
accent: #d946ef (fuchsia-500)
gray: Various shades (50-900)
```

### Spacing

```css
Container: max-w-7xl px-4 sm:px-6 lg:px-8
Card padding: p-6
Gap: gap-4, gap-6
```

### Typography

```css
Heading: text-3xl font-bold
Subheading: text-xl font-semibold
Body: text-base
Small: text-sm
```

## 🚀 Deployment

### Production Build

```bash
npm run build
npm run start
```

### Deployment Platforms

#### Vercel (Recommended)

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel --prod
```

#### Docker

```dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:18-alpine AS runner
WORKDIR /app
COPY --from=builder /app/next.config.js ./
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json

EXPOSE 3000
CMD ["npm", "start"]
```

### Production Recommendations

1. **Security**:
   - Use httpOnly cookies for tokens
   - Implement CSRF protection
   - Enable HTTPS only
   - Add rate limiting
   - Sanitize user inputs

2. **Performance**:
   - Enable CDN for static assets
   - Implement image optimization (Next.js Image)
   - Use caching headers
   - Enable compression
   - Implement code splitting

3. **Monitoring**:
   - Set up error tracking (Sentry)
   - Add analytics (Vercel Analytics, Google Analytics)
   - Monitor API response times
   - Track Core Web Vitals

4. **Infrastructure**:
   - Use environment-specific configs
   - Set up CI/CD pipeline
   - Implement health checks
   - Add logging

## 🔗 Backend Integration Checklist

### Priority Endpoints to Test First

1. ✅ **Authentication**:
   - [ ] POST /auth/register
   - [ ] POST /auth/login
   - [ ] Test token in Authorization header

2. ✅ **Listings**:
   - [ ] GET /listings (with pagination)
   - [ ] GET /listings/:id
   - [ ] POST /listings

3. ✅ **Image Upload**:
   - [ ] GET /s3/presign
   - [ ] Test direct upload to S3/MinIO
   - [ ] Verify publicUrl accessibility

4. ✅ **Admin**:
   - [ ] GET /admin/listings?status=pending
   - [ ] POST /admin/listings/:id/approve
   - [ ] POST /admin/listings/:id/reject

### Common Integration Issues

1. **CORS**: Ensure backend allows origin `http://localhost:3000`
2. **Token Format**: Verify JWT format matches `Bearer <token>`
3. **Response Structure**: Check API responses match TypeScript types
4. **Error Handling**: Test 4xx and 5xx responses
5. **Pagination**: Verify page/size params work correctly
6. **File Upload**: Test presigned URL expiration handling

## 📝 TODO Comments

Search for `TODO` in the codebase for places requiring backend integration:

```bash
grep -r "TODO" src/
```

Key TODOs:
- Replace mock data with real API calls in hooks
- Implement proper error boundaries
- Add loading skeletons for better UX
- Implement refresh token logic
- Add user profile page
- Implement listing edit/delete

## 🤝 Contributing

1. Follow TypeScript best practices
2. Write tests for new features
3. Use Prettier for formatting
4. Follow component naming conventions
5. Document complex logic with comments

## 📄 License

MIT

## 👥 Support

For issues and questions:
- Create an issue in the repository
- Contact the development team

---

**Happy coding! 🎉**
