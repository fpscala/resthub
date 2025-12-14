import { ImageUploadResult } from '@/types';
import { validateImageFile } from '@/lib/utils';
import { getAccessToken } from '@/lib/auth';
import { getRuntimeConfig } from '@/hooks/useRuntimeConfig';

export interface UploadProgress {
  fileIndex: number;
  fileName: string;
  progress: number;
  status: 'pending' | 'uploading' | 'success' | 'error';
  error?: string;
  publicUrl?: string;
}

/**
 * Upload single image using presigned URL flow
 *
 * Steps:
 * 1. Request presigned URL from backend (GET /s3/presign?key=uploads/{uuid}.jpg)
 * 2. Upload file directly to S3/MinIO using PUT request
 * 3. Return public URL for the uploaded file
 *
 * @param file - Image file to upload
 * @param onProgress - Progress callback (0-100)
 * @returns Upload result with public URL
 */
export async function uploadImage(
  file: File,
  onProgress?: (progress: number) => void
): Promise<ImageUploadResult> {
  try {
    console.log('Starting upload for file:', file.name);

    // Validate file
    const validation = validateImageFile(file);
    if (!validation.valid) {
      console.error('Validation failed:', validation.error);
      return {
        publicUrl: '',
        success: false,
        error: validation.error,
      };
    }

    // Get runtime config for API URL
    const config = getRuntimeConfig();

    // Upload file directly to backend
    const formData = new FormData();
    formData.append('file', file);

    // Get and log token
    const token = getAccessToken();
    console.log('Token exists:', !!token);
    console.log('Token length:', token ? token.length : 0);
    console.log('API URL:', config.NEXT_PUBLIC_API_URL);

    // Use XMLHttpRequest for progress tracking
    const response = await new Promise<string>((resolve, reject) => {
      const xhr = new XMLHttpRequest();

      // Track upload progress
      if (onProgress) {
        xhr.upload.addEventListener('progress', (event) => {
          if (event.lengthComputable) {
            const progress = Math.round((event.loaded / event.total) * 100);
            console.log('Upload progress:', progress + '%');
            onProgress(progress);
          }
        });
      }

      xhr.addEventListener('load', () => {
        console.log('XHR status:', xhr.status);
        console.log('XHR response:', xhr.responseText);

        if (xhr.status === 201) {
          try {
            const result = JSON.parse(xhr.responseText);
            console.log('Parsed result:', result);
            resolve(result.publicUrl);
          } catch (e) {
            console.error('JSON parse error:', e);
            reject(new Error('Invalid response format'));
          }
        } else {
          console.error('Upload failed with status:', xhr.status);
          reject(new Error(`Upload failed with status ${xhr.status}: ${xhr.responseText}`));
        }
      });

      xhr.addEventListener('error', (error) => {
        console.error('XHR error:', error);
        reject(new Error('Upload failed'));
      });

      // Open and send request
      xhr.open('POST', `${config.NEXT_PUBLIC_API_URL}/upload`);
      console.log('Opening request to:', `${config.NEXT_PUBLIC_API_URL}/upload`);

      // Set headers after opening
      if (token) {
        xhr.setRequestHeader('Authorization', `Bearer ${token}`);
        console.log('Authorization header set');
      } else {
        console.warn('No token found, proceeding without auth');
      }

      console.log('Sending request...');
      xhr.send(formData);
    });

    // Return public URL
    console.log('Upload successful, URL:', response);
    return {
      publicUrl: response,
      success: true,
    };
  } catch (error: any) {
    console.error('Upload error:', error);
    return {
      publicUrl: '',
      success: false,
      error: error.message || 'Upload failed',
    };
  }
}

/**
 * Upload multiple images with progress tracking
 *
 * Features:
 * - Validates all files before upload
 * - Uploads sequentially with retry logic
 * - Provides per-file progress updates
 * - Handles partial failures gracefully
 *
 * @param files - Array of image files (max 5)
 * @param onProgressUpdate - Callback for progress updates
 * @returns Array of successful upload URLs
 */
export async function uploadMultipleImages(
  files: File[],
  onProgressUpdate?: (progressList: UploadProgress[]) => void
): Promise<string[]> {
  // Validate max file count
  if (files.length > 5) {
    throw new Error('Maximum 5 images allowed');
  }

  // Initialize progress tracking
  const progressList: UploadProgress[] = files.map((file, index) => ({
    fileIndex: index,
    fileName: file.name,
    progress: 0,
    status: 'pending',
  }));

  const updateProgress = (index: number, update: Partial<UploadProgress>) => {
    progressList[index] = { ...progressList[index], ...update };
    if (onProgressUpdate) {
      onProgressUpdate([...progressList]);
    }
  };

  const successfulUploads: string[] = [];

  // Upload files sequentially
  for (let i = 0; i < files.length; i++) {
    const file = files[i];

    updateProgress(i, { status: 'uploading' });

    // Retry logic: up to 3 attempts
    let attempts = 0;
    let success = false;

    while (attempts < 3 && !success) {
      try {
        const result = await uploadImage(file, (progress) => {
          updateProgress(i, { progress });
        });

        if (result.success && result.publicUrl) {
          updateProgress(i, {
            status: 'success',
            progress: 100,
            publicUrl: result.publicUrl,
          });
          successfulUploads.push(result.publicUrl);
          success = true;
        } else {
          throw new Error(result.error || 'Upload failed');
        }
      } catch (error: any) {
        attempts++;
        if (attempts >= 3) {
          // Failed after 3 attempts
          updateProgress(i, {
            status: 'error',
            error: error.message || 'Upload failed after 3 attempts',
          });
        } else {
          // Wait before retry (exponential backoff)
          await new Promise((resolve) => setTimeout(resolve, 1000 * attempts));
        }
      }
    }
  }

  return successfulUploads;
}

