import { useMutation } from '@tanstack/react-query';
import { get } from '@/lib/api-client';
import { uploadToPresignedUrl } from '@/lib/api-client';
import { PresignResponse } from '@/types';
import toast from 'react-hot-toast';

/**
 * Generate presigned URL for file upload
 *
 * Usage:
 * ```tsx
 * const getPresignedUrl = useGetPresignedUrl();
 *
 * const handleUpload = async (file: File) => {
 *   const { presignedUrl, publicUrl } = await getPresignedUrl.mutateAsync({
 *     key: `uploads/listings/${file.name}`
 *   });
 *
 *   // Upload file to presigned URL
 *   await uploadToPresignedUrl(presignedUrl, file);
 *
 *   return publicUrl;
 * };
 * ```
 */
export function useGetPresignedUrl() {
  return useMutation({
    mutationFn: async (params: { key: string }): Promise<PresignResponse> => {
      return get<PresignResponse>('/s3/presign', { key: params.key });
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to get upload URL');
    },
  });
}

/**
 * Upload file with presigned URL flow
 *
 * Usage:
 * ```tsx
 * const uploadFile = useUploadFile();
 *
 * const handleFileSelect = async (file: File) => {
 *   const publicUrl = await uploadFile.mutateAsync(file);
 *   console.log('File uploaded to:', publicUrl);
 * };
 * ```
 */
export function useUploadFile() {
  const getPresignedUrl = useGetPresignedUrl();

  return useMutation({
    mutationFn: async (
      file: File,
      options?: {
        onProgress?: (progress: number) => void;
        keyPrefix?: string;
      }
    ): Promise<string> => {
      // Generate unique key for the file
      const timestamp = Date.now();
      const randomString = Math.random().toString(36).substring(2, 15);
      const fileExtension = file.name.split('.').pop();
      const key = options?.keyPrefix
        ? `${options.keyPrefix}/${timestamp}-${randomString}.${fileExtension}`
        : `uploads/${timestamp}-${randomString}.${fileExtension}`;

      // Get presigned URL
      const { presignedUrl, publicUrl } = await getPresignedUrl.mutateAsync({ key });

      // Upload file directly to S3/MinIO
      await uploadToPresignedUrl(presignedUrl, file, options?.onProgress);

      return publicUrl;
    },
    onSuccess: (publicUrl) => {
      toast.success('File uploaded successfully!');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to upload file');
    },
  });
}

/**
 * Upload multiple files
 *
 * Usage:
 * ```tsx
 * const uploadFiles = useUploadMultipleFiles();
 *
 * const handleFilesSelect = async (files: File[]) => {
 *   const urls = await uploadFiles.mutateAsync(files);
 *   console.log('Files uploaded to:', urls);
 * };
 * ```
 */
export function useUploadMultipleFiles() {
  const uploadFile = useUploadFile();

  return useMutation({
    mutationFn: async (
      files: File[],
      options?: {
        onProgress?: (progress: number) => void;
        keyPrefix?: string;
      }
    ): Promise<string[]> => {
      const uploadPromises = files.map((file, index) => {
        // Calculate individual file progress contribution
        const fileProgress = options?.onProgress
          ? (progress: number) => {
              const totalProgress = ((progress + (index * 100)) / files.length);
              options.onProgress?.(totalProgress);
            }
          : undefined;

        return uploadFile.mutateAsync(file, {
          onProgress: fileProgress,
          keyPrefix: options?.keyPrefix
        });
      });

      return Promise.all(uploadPromises);
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to upload files');
    },
  });
}