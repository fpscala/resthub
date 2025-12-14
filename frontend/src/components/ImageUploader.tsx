'use client';

import { useState, useRef, ChangeEvent } from 'react';
import { uploadMultipleImages, UploadProgress } from '@/lib/image-upload';
import { Button } from '@/components/ui/Button';
import { cn } from '@/lib/utils';
import toast from 'react-hot-toast';

export interface ImageUploaderProps {
  onUploadComplete: (urls: string[]) => void;
  maxFiles?: number;
  className?: string;
}

/**
 * Image uploader component with presigned URL flow
 *
 * Features:
 * - Client-side validation (max 5 files, 5MB each, jpg/png/webp)
 * - Progress tracking per file
 * - Preview thumbnails
 * - Direct upload to S3/MinIO via presigned PUT URL
 *
 * Usage:
 * ```tsx
 * <ImageUploader
 *   onUploadComplete={(urls) => setFormData({ ...formData, images: urls })}
 *   maxFiles={5}
 * />
 * ```
 */
export function ImageUploader({
  onUploadComplete,
  maxFiles = 5,
  className,
}: ImageUploaderProps) {
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [previews, setPreviews] = useState<string[]>([]);
  const [uploadProgress, setUploadProgress] = useState<UploadProgress[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const validateAndAddFiles = (files: File[]) => {
    const currentCount = selectedFiles.length;
    const newFilesCount = files.length;
    const totalCount = currentCount + newFilesCount;

    if (totalCount > maxFiles) {
      toast.error(`Maximum ${maxFiles} files allowed. You can add ${maxFiles - currentCount} more.`);
      return;
    }

    // Validate each file
    const validFiles: File[] = [];
    for (const file of files) {
      if (file.size > 5 * 1024 * 1024) {
        toast.error(`${file.name} exceeds 5MB limit`);
        continue;
      }

      if (!['image/jpeg', 'image/jpg', 'image/png', 'image/webp'].includes(file.type)) {
        toast.error(`${file.name} is not a valid image type`);
        continue;
      }

      validFiles.push(file);
    }

    if (validFiles.length > 0) {
      // Add to existing files
      const updatedFiles = [...selectedFiles, ...validFiles];
      setSelectedFiles(updatedFiles);

      // Generate previews for new files
      const newPreviews = validFiles.map((file) => URL.createObjectURL(file));
      setPreviews([...previews, ...newPreviews]);

      toast.success(`${validFiles.length} image(s) added`);
    }
  };

  const handleFileSelect = (e: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    validateAndAddFiles(files);
    // Reset input
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    const files = Array.from(e.dataTransfer.files);
    validateAndAddFiles(files);
  };

  const handleUpload = async () => {
    if (selectedFiles.length === 0) {
      toast.error('Please select files to upload');
      return;
    }

    setIsUploading(true);

    try {
      const urls = await uploadMultipleImages(selectedFiles, (progressList) => {
        setUploadProgress(progressList);
      });

      if (urls.length > 0) {
        toast.success(`Successfully uploaded ${urls.length} image(s)`);
        onUploadComplete(urls);

        // Reset state
        setSelectedFiles([]);
        setPreviews([]);
        setUploadProgress([]);
        if (fileInputRef.current) {
          fileInputRef.current.value = '';
        }
      } else {
        toast.error('All uploads failed. Please try again.');
      }
    } catch (error: any) {
      toast.error(error.message || 'Upload failed');
    } finally {
      setIsUploading(false);
    }
  };

  const handleRemoveFile = (index: number) => {
    const newFiles = selectedFiles.filter((_, i) => i !== index);
    const newPreviews = previews.filter((_, i) => i !== index);

    setSelectedFiles(newFiles);
    setPreviews(newPreviews);

    // Revoke object URL to prevent memory leak
    URL.revokeObjectURL(previews[index]);
  };

  return (
    <div className={cn('space-y-4', className)}>
      {/* Drag & Drop Area */}
      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={cn(
          'relative rounded-lg border-2 border-dashed p-8 text-center transition-colors',
          isDragging
            ? 'border-blue-500 bg-blue-50'
            : 'border-gray-300 bg-gray-50 hover:border-gray-400',
          isUploading && 'pointer-events-none opacity-50'
        )}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept="image/jpeg,image/jpg,image/png,image/webp"
          multiple
          onChange={handleFileSelect}
          disabled={isUploading}
          className="hidden"
        />

        <div className="space-y-4">
          <div className="flex justify-center">
            <svg
              className="h-16 w-16 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
              />
            </svg>
          </div>

          <div>
            <p className="text-lg font-medium text-gray-700">
              {isDragging ? 'Drop images here' : 'Drag & drop images here'}
            </p>
            <p className="mt-1 text-sm text-gray-500">or</p>
            <Button
              type="button"
              variant="secondary"
              onClick={() => fileInputRef.current?.click()}
              disabled={isUploading || selectedFiles.length >= maxFiles}
              className="mt-2"
            >
              Browse Files
            </Button>
          </div>

          <p className="text-xs text-gray-500">
            Max {maxFiles} files • 5MB each • JPG, PNG, WebP
            {selectedFiles.length > 0 && (
              <span className="ml-2 font-medium text-blue-600">
                ({selectedFiles.length}/{maxFiles} selected)
              </span>
            )}
          </p>
        </div>
      </div>

      {/* Previews */}
      {previews.length > 0 && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4">
          {previews.map((preview, index) => (
            <div key={index} className="relative">
              <img
                src={preview}
                alt={`Preview ${index + 1}`}
                className="h-32 w-full rounded-lg object-cover"
              />

              {/* Remove button */}
              {!isUploading && (
                <button
                  type="button"
                  onClick={() => handleRemoveFile(index)}
                  className="absolute right-2 top-2 rounded-full bg-red-500 p-1 text-white hover:bg-red-600"
                  aria-label={`Remove image ${index + 1}`}
                >
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-4 w-4"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                </button>
              )}

              {/* Progress indicator */}
              {isUploading && uploadProgress[index] && (
                <div className="absolute inset-0 flex items-center justify-center rounded-lg bg-black bg-opacity-50">
                  <div className="text-center text-white">
                    {uploadProgress[index].status === 'uploading' && (
                      <div>
                        <div className="text-2xl font-bold">
                          {uploadProgress[index].progress}%
                        </div>
                        <div className="text-xs">Uploading...</div>
                      </div>
                    )}
                    {uploadProgress[index].status === 'success' && (
                      <div className="text-2xl">✓</div>
                    )}
                    {uploadProgress[index].status === 'error' && (
                      <div className="text-2xl">✗</div>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Upload button */}
      {selectedFiles.length > 0 && (
        <Button
          type="button"
          onClick={handleUpload}
          isLoading={isUploading}
          disabled={isUploading}
        >
          Upload {selectedFiles.length} Image{selectedFiles.length > 1 ? 's' : ''}
        </Button>
      )}
    </div>
  );
}
