# Runtime Configuration Strategy

## Overview

This application uses **runtime configuration** instead of build-time environment variables for client-side configuration. This allows the same Docker image to be deployed to different environments without rebuilding.

## Architecture

### Development Mode
- `public/runtime-config.js` contains hardcoded values
- No envsubst, no placeholders
- Works immediately with `npm run dev`

### Production Mode
- `public/runtime-config.js.template` contains `$VAR` placeholders
- Docker entrypoint uses `envsubst` to inject real values
- Generated `public/runtime-config.js` contains actual values

## Files

### `public/runtime-config.js` (DEV)
```js
window.__ENV__ = {
  NEXT_PUBLIC_API_URL: "http://localhost:3000/api",
  NEXT_PUBLIC_S3_BUCKET_URL: "http://localhost:9000"
};
```

### `public/runtime-config.js.template` (PROD)
```js
window.__ENV__ = {
  NEXT_PUBLIC_API_URL: "$NEXT_PUBLIC_API_URL",
  NEXT_PUBLIC_S3_BUCKET_URL: "$NEXT_PUBLIC_S3_BUCKET_URL"
};
```

### `docker-entrypoint.sh`
- Sets default values for environment variables
- Uses `envsubst` to generate runtime config from template
- Prints configuration for debugging

## Usage in Code

### Helper Function
```typescript
// src/hooks/useRuntimeConfig.ts
export function getRuntimeConfig(): RuntimeConfig | null {
  if (typeof window === 'undefined') return null;
  return (window as any).__ENV__ ?? null;
}

export function useRuntimeConfig(): RuntimeConfig {
  const config = getRuntimeConfig();
  if (!config) {
    throw new Error('Runtime config not available');
  }
  return config;
}
```

### Component Usage
```typescript
import { useRuntimeConfig } from '@/hooks/useRuntimeConfig';

function MyComponent() {
  const config = useRuntimeConfig();
  // Use config.NEXT_PUBLIC_API_URL, config.NEXT_PUBLIC_S3_BUCKET_URL
}
```

### API Client
```typescript
// src/lib/api-client.ts
function getBaseUrl(): string {
  const config = getRuntimeConfig();
  return config?.NEXT_PUBLIC_API_URL || 'http://localhost:3000/api';
}
```

## Loading Order

The runtime config is loaded before any application code:

```tsx
// app/layout.tsx
<html>
  <head>
    <script src="/runtime-config.js" /> {/* Synchronous load */}
  </head>
  <body>
    {/* App code */}
  </body>
</html>
```

## Environment Variables

### Required
- `NEXT_PUBLIC_API_URL` - Backend API URL
- `NEXT_PUBLIC_S3_BUCKET_URL` - S3/MinIO bucket URL

### Defaults (Production)
- `NEXT_PUBLIC_API_URL`: `http://localhost:8080`
- `NEXT_PUBLIC_S3_BUCKET_URL`: `http://localhost:9000`

## Verification

### Development
1. Visit `http://localhost:3000/runtime-config.js`
2. Check browser console: `window.__ENV__`
3. No "Runtime config not available" warnings

### Production
1. Check container: `cat /app/public/runtime-config.js`
2. Verify injected values (no placeholders)
3. Change env vars + restart to update behavior

## Important Rules

1. **Never use `process.env.NEXT_PUBLIC_*` in client code**
2. **Always access via `getRuntimeConfig()` or `useRuntimeConfig()`**
3. **Never rebuild Docker image to change configuration**
4. **Always load `runtime-config.js` before app code**
5. **Never use `__VAR__` placeholders - only `$VAR`**

## Migration Notes

This approach replaces the problematic `__VAR__` placeholder pattern that:
- Didn't work in development (no envsubst)
- Caused literal placeholders to appear in URLs
- Required rebuilds for configuration changes

The new pattern provides:
- ✅ Immediate development experience
- ✅ True runtime configuration in production
- ✅ No Docker rebuilds for config changes
- ✅ Clean separation of build vs runtime concerns