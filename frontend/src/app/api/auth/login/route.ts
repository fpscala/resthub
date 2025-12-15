import { serverConfig } from '@/server/config';
import { NextRequest, NextResponse } from 'next/server';
import { createTokenCookie } from '@/server/auth-utils';

// Force dynamic rendering for this API route
export const dynamic = 'force-dynamic';

export async function POST(req: NextRequest) {
  try {
    const body = await req.text();

    const response = await fetch(`${serverConfig.backendApiUrl}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body,
      cache: 'no-store',
    });

    if (!response.ok) {
      // If login failed, forward the error response
      const errorText = await response.text();
      return new NextResponse(errorText, {
        status: response.status,
        headers: {
          'Content-Type': response.headers.get('content-type') || 'application/json',
        },
      });
    }

    // Parse successful response to extract Bearer token
    const responseData = await response.json();
    const accessToken = responseData.accessToken || responseData.token || responseData.access_token;

    if (!accessToken) {
      console.error('No access token in login response:', responseData);
      return NextResponse.json(
        { message: 'Authentication failed: No token received' },
        { status: 500 }
      );
    }

    // Return user data without the token, but store token in HttpOnly cookie
    const { accessToken: _, ...userData } = responseData;

    return NextResponse.json(userData, {
      status: 200,
      headers: {
        'Content-Type': 'application/json',
        'Set-Cookie': createTokenCookie(accessToken),
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