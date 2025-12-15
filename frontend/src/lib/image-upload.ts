import { ImageUploadResult } from '@/types';
import { validateImageFile } from '@/lib/utils';
import { post } from '@/lib/api-client';

export interface UploadProgress {
  fileIndex: number;
  fileName: string;
  progress: number;
  status: 'pending' | 'uploading' | 'success' | 'error';
  error?: string;
  publicUrl?: string;
}

/**
 * Upload single image using multipart form data
 *
 * Steps:
 * 1. Create FormData with file and metadata
 * 2. Send multipart request to backend (POST /upload)
 * 3. Backend handles S3/MinIO upload and returns public URL
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

    // Create FormData for multipart upload
    const formData = new FormData();
    formData.append('file', file);

    // Add optional type parameter if needed
    formData.append('type', 'listing');

    try {
      // Upload file via multipart to backend
      console.log('Uploading file to backend via multipart');
      console.log('FormData contents:');
      for (const [key, value] of formData.entries()) {
        if (value instanceof File) {
          console.log(`  ${key}: File(${value.name}, ${value.size} bytes, ${value.type})`);
        } else {
          console.log(`  ${key}: ${value}`);
        }
      }

      // Upload via multipart - let axios set the proper Content-Type with boundary
      const response = await post<{ publicUrl: string }>('/upload', formData);

      console.log('Upload response:', response);

      if (!response.publicUrl) {
        console.error('Invalid response from upload endpoint:', response);
        throw new Error('Upload succeeded but no public URL returned');
      }

      // Return public URL
      console.log('Upload successful, URL:', response.publicUrl);
      return {
        publicUrl: response.publicUrl,
        success: true,
      };
    } catch (error: any) {
      console.error('Multipart upload failed:', error);
      throw new Error(`Upload failed: ${error.message}`);
    }
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

