'use client';

import { useState } from 'react';
import Image from 'next/image';
import { cn } from '@/lib/utils';

export interface ImageGalleryProps {
  images: string[];
  title: string;
}

/**
 * Image gallery component with lightbox/modal view
 *
 * Features:
 * - Click on any image to open in fullscreen modal
 * - Navigate between images with prev/next buttons
 * - Close with X button or ESC key
 * - Responsive thumbnail grid
 * - Keyboard navigation (arrow keys)
 *
 * Usage:
 * ```tsx
 * <ImageGallery images={listing.images} title={listing.title} />
 * ```
 */
export function ImageGallery({ images, title }: ImageGalleryProps) {
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  const openLightbox = (index: number) => {
    setSelectedIndex(index);
  };

  const closeLightbox = () => {
    setSelectedIndex(null);
  };

  const goToPrevious = () => {
    if (selectedIndex === null) return;
    setSelectedIndex((selectedIndex - 1 + images.length) % images.length);
  };

  const goToNext = () => {
    if (selectedIndex === null) return;
    setSelectedIndex((selectedIndex + 1) % images.length);
  };

  // Keyboard navigation
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (selectedIndex === null) return;

    if (e.key === 'Escape') closeLightbox();
    else if (e.key === 'ArrowLeft') goToPrevious();
    else if (e.key === 'ArrowRight') goToNext();
  };

  if (images.length === 0) {
    return (
      <div className="relative h-96 w-full overflow-hidden rounded-lg bg-gray-200">
        <div className="flex h-full items-center justify-center text-gray-500">
          No images available
        </div>
      </div>
    );
  }

  const mainImage = images[0];

  return (
    <>
      {/* Main Image */}
      <div
        className="relative h-96 w-full cursor-pointer overflow-hidden rounded-lg bg-gray-200 transition-transform hover:scale-[1.01]"
        onClick={() => openLightbox(0)}
      >
        <Image
          src={mainImage}
          alt={title}
          fill
          className="object-cover"
          priority
          sizes="(max-width: 1280px) 100vw, 1280px"
        />
        {/* Overlay with image count */}
        <div className="absolute bottom-4 right-4 rounded-lg bg-black bg-opacity-70 px-3 py-1 text-sm text-white">
          {images.length} {images.length === 1 ? 'photo' : 'photos'}
        </div>
      </div>

      {/* Thumbnail Grid */}
      {images.length > 1 && (
        <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
          {images.slice(1).map((image, index) => (
            <div
              key={index}
              className="relative h-24 cursor-pointer overflow-hidden rounded-lg bg-gray-200 transition-all hover:opacity-80 hover:ring-2 hover:ring-blue-500"
              onClick={() => openLightbox(index + 1)}
            >
              <Image
                src={image}
                alt={`${title} - Image ${index + 2}`}
                fill
                className="object-cover"
                sizes="(max-width: 768px) 50vw, 25vw"
              />
            </div>
          ))}
        </div>
      )}

      {/* Lightbox Modal */}
      {selectedIndex !== null && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-95"
          onClick={closeLightbox}
          onKeyDown={handleKeyDown}
          tabIndex={0}
          role="dialog"
          aria-modal="true"
        >
          {/* Close button */}
          <button
            onClick={closeLightbox}
            className="absolute right-4 top-4 z-10 rounded-full bg-white bg-opacity-20 p-2 text-white transition-all hover:bg-opacity-30"
            aria-label="Close gallery"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-6 w-6"
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

          {/* Image counter */}
          <div className="absolute left-1/2 top-4 -translate-x-1/2 rounded-lg bg-black bg-opacity-50 px-4 py-2 text-white">
            {selectedIndex + 1} / {images.length}
          </div>

          {/* Previous button */}
          {images.length > 1 && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                goToPrevious();
              }}
              className="absolute left-4 rounded-full bg-white bg-opacity-20 p-3 text-white transition-all hover:bg-opacity-30"
              aria-label="Previous image"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M15 19l-7-7 7-7"
                />
              </svg>
            </button>
          )}

          {/* Main image in modal */}
          <div
            className="relative mx-auto h-[80vh] w-[90vw] max-w-6xl"
            onClick={(e) => e.stopPropagation()}
          >
            <Image
              src={images[selectedIndex]}
              alt={`${title} - Image ${selectedIndex + 1}`}
              fill
              className="object-contain"
              sizes="90vw"
              priority
            />
          </div>

          {/* Next button */}
          {images.length > 1 && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                goToNext();
              }}
              className="absolute right-4 rounded-full bg-white bg-opacity-20 p-3 text-white transition-all hover:bg-opacity-30"
              aria-label="Next image"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-6 w-6"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 5l7 7-7 7"
                />
              </svg>
            </button>
          )}

          {/* Thumbnail strip at bottom */}
          {images.length > 1 && (
            <div className="absolute bottom-4 left-1/2 flex max-w-[90vw] -translate-x-1/2 gap-2 overflow-x-auto rounded-lg bg-black bg-opacity-50 p-2">
              {images.map((image, index) => (
                <div
                  key={index}
                  className={cn(
                    'relative h-16 w-16 flex-shrink-0 cursor-pointer overflow-hidden rounded transition-all',
                    index === selectedIndex
                      ? 'ring-2 ring-white ring-offset-2 ring-offset-black'
                      : 'opacity-60 hover:opacity-100'
                  )}
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelectedIndex(index);
                  }}
                >
                  <Image
                    src={image}
                    alt={`Thumbnail ${index + 1}`}
                    fill
                    className="object-cover"
                    sizes="64px"
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </>
  );
}
