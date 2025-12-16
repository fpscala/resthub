import { serverConfig } from '@/server/config';
import { extractBearerToken } from '@/server/auth-utils';
import { NextRequest, NextResponse } from 'next/server';

export const dynamic = 'force-dynamic';

export async function POST(req: NextRequest) {
  try {
    const token = extractBearerToken(req.headers.get('cookie'), req.headers.get('authorization'));

    if (!token) {
      return NextResponse.json(
        { message: 'Not authenticated' },
        { status: 401 }
      );
    }

    // Get the FormData from the request
    const formData = await req.formData();

    console.log('Received multipart upload request');
    console.log('FormData entries:');
    for (const [key, value] of formData.entries()) {
      if (value instanceof File) {
        console.log(`${key}: File(${value.name}, ${value.size} bytes, ${value.type})`);
        console.log('Last modified:', new Date(value.lastModified));
      } else {
        console.log(`${key}: ${value}`);
      }
    }

    // Check file type compatibility
    const fileEntry = formData.get('file');
    if (fileEntry instanceof File) {
      const file = fileEntry;
      const allowedTypes = ['image/png', 'image/jpeg', 'image/jpg'];
      console.log('File type:', file.type);
      console.log('Is allowed type:', allowedTypes.includes(file.type));

      if (!allowedTypes.includes(file.type)) {
        console.log('File type not allowed by backend');
        return NextResponse.json(
          { message: `File type ${file.type} is not allowed. Only PNG and JPEG images are supported.` },
          { status: 422 }
        );
      }
    }

    // Forward the FormData to the backend
    const backendUrl = `${serverConfig.backendApiUrl}/upload`;
    console.log('Forwarding to backend:', backendUrl);
    console.log('Request headers being sent:', {
      'Authorization': `Bearer ${token.substring(0, 20)}...`,
      'Content-Type': '[will be set automatically by fetch for FormData]'
    });

    // Create a new FormData to forward to backend
    const backendFormData = new FormData();
    for (const [key, value] of formData.entries()) {
      backendFormData.append(key, value);
    }

    const response = await fetch(backendUrl, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        // Important: Don't set Content-Type for FormData - let fetch set it with the correct boundary
      },
      body: backendFormData,
    });

    const responseText = await response.text();
    console.log('Backend response status:', response.status);
    console.log('Backend response headers:', Object.fromEntries(response.headers.entries()));
    console.log('Backend response body:', responseText);

    // Try to parse error details if it's a 422 error
    if (response.status === 422) {
      try {
        const errorData = JSON.parse(responseText);
        console.error('Backend validation error:', errorData);
      } catch (e) {
        console.error('Could not parse backend error response as JSON');
      }
    }

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