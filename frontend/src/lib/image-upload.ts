import { get } from '@/lib/api-client';
import { uploadToPresignedUrl } from '@/lib/api-client';
import { PresignResponse, ImageUploadResult } from '@/types';
import { generateFileKey, validateImageFile } from '@/lib/utils';

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
    // Validate file
    const validation = validateImageFile(file);
    if (!validation.valid) {
      return {
        publicUrl: '',
        success: false,
        error: validation.error,
      };
    }

    // Generate unique key for the file
    const fileKey = generateFileKey(file.name);

    // Step 1: Request presigned URL from backend
    // TODO: Replace with actual API call once backend is ready
    const presignData = await mockGetPresignedUrl(fileKey);
    // const presignData = await get<PresignResponse>(`/s3/presign?key=${fileKey}`);

    // Step 2: Upload file to S3/MinIO using presigned URL
    await uploadToPresignedUrl(presignData.url, file, onProgress);

    // Step 3: Return public URL
    return {
      publicUrl: presignData.publicUrl,
      success: true,
    };
  } catch (error: any) {
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

/**
 * Mock presigned URL generator for development
 * TODO: Remove once backend is ready
 */
async function mockGetPresignedUrl(key: string): Promise<PresignResponse> {
  await new Promise((resolve) => setTimeout(resolve, 200));

  // In real implementation, backend would generate these URLs
  const bucketUrl = process.env.NEXT_PUBLIC_S3_BUCKET_URL || 'http://localhost:9000';

  return {
    url: `${bucketUrl}/uploads/${key}?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...`,
    publicUrl: `${bucketUrl}/uploads/${key}`,
  };
}
