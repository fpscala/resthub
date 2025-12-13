import { useMutation } from '@tanstack/react-query';
import { post } from '@/lib/api-client';
import { GenerateContractRequest, GenerateContractResponse } from '@/types';
import toast from 'react-hot-toast';

/**
 * Generate contract PDF mutation
 *
 * Usage:
 * ```tsx
 * const generateContract = useGenerateContract();
 *
 * const handleDownload = async () => {
 *   const result = await generateContract.mutateAsync({ listingId: 'listing-123' });
 *   window.open(result.pdfUrl, '_blank');
 * };
 * ```
 */
export function useGenerateContract() {
  return useMutation({
    mutationFn: async (
      data: GenerateContractRequest
    ): Promise<GenerateContractResponse> => {
      return post<GenerateContractResponse>('/contracts/generate', data);
    },
    onSuccess: (data) => {
      toast.success('Contract generated successfully');
      // Automatically download
      window.open(data.pdfUrl, '_blank');
    },
    onError: (error: any) => {
      toast.error(error.message || 'Failed to generate contract');
    },
  });
}
