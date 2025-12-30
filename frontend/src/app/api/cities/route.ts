import { serverConfig } from '@/server/config';
import { extractBearerToken } from '@/server/auth-utils';
import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

export async function GET(req: NextRequest) {
  try {
    const { searchParams } = new URL(req.url);
    const queryString = searchParams.toString();

    // Extract token if available, but don't require authentication for cities
    const token = extractBearerToken(req.headers.get('cookie'));

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    // Add authorization header if token is present
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const baseUrl = `${serverConfig.backendApiUrl}/cities`;
    const url = queryString ? `${baseUrl}?${queryString}` : baseUrl;
    const response = await fetch(url, {
      method: 'GET',
      headers,
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