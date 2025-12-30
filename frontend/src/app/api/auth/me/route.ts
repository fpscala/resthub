import { serverConfig } from '@/server/config';
import { NextRequest, NextResponse } from 'next/server';
import { extractBearerToken } from '@/server/auth-utils';

// Force dynamic rendering for this API route
export const dynamic = 'force-dynamic';

export async function GET(req: NextRequest) {
  try {
    // Extract Bearer token from Authorization header or HttpOnly cookie
    const cookieHeader = req.headers.get('cookie');
    const authHeader = req.headers.get('authorization');
    const token = extractBearerToken(cookieHeader, authHeader);

    if (!token) {
      console.error('Token extraction failed - auth header:', authHeader ? 'present' : 'missing', 'cookie header:', cookieHeader ? 'present' : 'missing');
      return NextResponse.json(
        { message: 'Bearer token not found' },
        { status: 401 }
      );
    }

    // Forward request to backend with Bearer token
    const response = await fetch(`${serverConfig.backendApiUrl}/auth/me`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      cache: 'no-store',
    });

    const responseText = await response.text();

    return new NextResponse(responseText, {
      status: response.status,
      headers: {
        'Content-Type': response.headers.get('content-type') || 'application/json',
      },
    });
  } catch (error) {
    console.error('API Proxy Error:', error);
    return NextResponse.json(
      { message: 'Internal server error' },
      { status: 500 }
    );
  }
}