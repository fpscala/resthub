'use client';

import { useState } from 'react';
import { useCreateListing } from '@/hooks/useListings';
import { useAuth } from '@/hooks/useAuth';
import { ProtectedRoute } from '@/components/ProtectedRoute';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Select } from '@/components/ui/Select';
import { ImageUploader } from '@/components/ImageUploader';
import { CreateListingFormData } from '@/types';
import { useCities } from '@/hooks/useCities';

/**
 * Create listing page (protected)
 *
 * Features:
 * - Form validation
 * - Image upload with progress
 * - Optimistic UI with loading states
 * - Automatic redirect on success
 */
function CreateListingContent() {
  const { user } = useAuth();
  const createListing = useCreateListing();
  const { data: cities, isLoading: citiesLoading } = useCities();

  const [formData, setFormData] = useState({
    title: '',
    description: '',
    price: '',
    city: '',
    images: [] as string[],
  });

  const [errors, setErrors] = useState<Record<string, string>>({});

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!formData.title.trim()) {
      newErrors.title = 'Title is required';
    }

    if (!formData.description.trim()) {
      newErrors.description = 'Description is required';
    }

    if (!formData.price || Number(formData.price) <= 0) {
      newErrors.price = 'Price must be greater than 0';
    }

    if (!formData.city) {
      newErrors.city = 'Shaharni tanlang';
    }

    if (formData.images.length === 0) {
      newErrors.images = 'At least one image is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;
    if (!user) return;

    await createListing.mutateAsync({
      title: formData.title,
      description: formData.description,
      price: Number(formData.price),
      city: formData.city,
      images: formData.images,
    });
  };

  return (
    <div className="container-custom py-8">
      <div className="mx-auto max-w-2xl">
        <h1 className="mb-8 text-3xl font-bold text-gray-900">Create New Listing</h1>

        <form onSubmit={handleSubmit} className="space-y-6">
          <Input
            label="Title"
            placeholder="e.g., Modern Downtown Apartment"
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            error={errors.title}
            required
          />

          <div>
            <label htmlFor="description" className="label">
              Description
            </label>
            <textarea
              id="description"
              rows={6}
              className="input"
              placeholder="Describe your property in detail..."
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              required
            />
            {errors.description && <p className="error-text">{errors.description}</p>}
          </div>

          <Input
            label="Price (per month)"
            type="number"
            placeholder="e.g., 2500"
            value={formData.price}
            onChange={(e) => setFormData({ ...formData, price: e.target.value })}
            error={errors.price}
            required
          />

          <Select
            label="Shahar"
            value={formData.city}
            onChange={(e) => setFormData({ ...formData, city: e.target.value })}
            error={errors.city}
            options={(cities || []).filter(city => city !== '').map((city: string) => ({ // Empty qiymatni olmaymiz
              value: city,
              label: city
            }))}
            disabled={citiesLoading}
            required
          />

          <div>
            <ImageUploader
              onUploadComplete={(urls) => {
                setFormData({ ...formData, images: urls });
                setErrors({ ...errors, images: '' });
              }}
              maxFiles={5}
            />
            {errors.images && <p className="error-text">{errors.images}</p>}
          </div>

          <div className="flex gap-4">
            <Button
              type="submit"
              isLoading={createListing.isPending}
              disabled={createListing.isPending}
            >
              Create Listing
            </Button>
            <Button
              type="button"
              variant="secondary"
              onClick={() => window.history.back()}
              disabled={createListing.isPending}
            >
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function CreateListingPage() {
  return (
    <ProtectedRoute>
      <CreateListingContent />
    </ProtectedRoute>
  );
}
