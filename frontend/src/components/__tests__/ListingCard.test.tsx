import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ListingCard } from '../ListingCard';
import { Listing } from '@/types';

const mockListing: Listing = {
  id: '1',
  ownerId: 'owner-1',
  title: 'Modern Downtown Apartment',
  description: 'Beautiful 2BR apartment with city views',
  price: 2500,
  city: 'San Francisco',
  images: ['https://example.com/image.jpg'],
  status: 'APPROVED',
  createdAt: new Date().toISOString(),
};

describe('ListingCard', () => {
  it('renders listing information correctly', () => {
    render(<ListingCard listing={mockListing} />);

    expect(screen.getByText('Modern Downtown Apartment')).toBeInTheDocument();
    expect(screen.getByText(/Beautiful 2BR apartment/)).toBeInTheDocument();
    expect(screen.getByText('San Francisco')).toBeInTheDocument();
    expect(screen.getByText('$2,500.00')).toBeInTheDocument();
  });

  it('displays pending status badge when listing is pending', () => {
    const pendingListing = { ...mockListing, status: 'PENDING' as const };
    render(<ListingCard listing={pendingListing} />);

    expect(screen.getByText('Pending')).toBeInTheDocument();
  });

  it('calls onClick handler when clicked', () => {
    const handleClick = vi.fn();
    render(<ListingCard listing={mockListing} onClick={handleClick} />);

    const card = screen.getByRole('article');
    fireEvent.click(card);

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('renders image with correct alt text', () => {
    render(<ListingCard listing={mockListing} />);

    const image = screen.getByAltText('Modern Downtown Apartment');
    expect(image).toBeInTheDocument();
  });

  it('handles listings without images', () => {
    const noImageListing = { ...mockListing, images: [] };
    render(<ListingCard listing={noImageListing} />);

    // Should still render without crashing
    expect(screen.getByText('Modern Downtown Apartment')).toBeInTheDocument();
  });
});
