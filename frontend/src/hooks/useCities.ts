import { useQuery } from '@tanstack/react-query';
import { get } from '@/lib/api-client';

/**
 * Fallback shaharlar ro'yxati (API o'chsa ishlaydi)
 */
const FALLBACK_CITIES = [
  '',
  'Toshkent',
  'Samarqand',
  'Buxoro',
  'Namangan',
  'Andijon',
  'Farg\'ona',
  'Nukus',
  'Qarshi',
  'Termiz',
  'Jizzax',
  'Guliston',
  'Navoiy',
  'Urganch',
  'Marg\'ilon',
  'Angren',
  'Bekobod',
  'Chirchiq',
  'Denov',
  'Kitob',
] as const;

/**
 * Query keys for cities
 */
export const citiesKeys = {
  all: ['cities'] as const,
  list: () => [...citiesKeys.all, 'list'] as const,
};

/**
 * Shaharlar ro'yxatini oladi
 * Avval API dan harakat qiladi, o'chsa fallback ro'yxatdan foydalanadi
 */
export function useCities() {
  return useQuery({
    queryKey: citiesKeys.list(),
    queryFn: async (): Promise<string[]> => {
      try {
        // API dan shaharlarni olishga urinamiz
        const response = await get<string[]>('/cities');
        // response allaqachon string[] bo'lishi kerak, lekin tekshirib olamiz
        return Array.isArray(response) ? response : [...FALLBACK_CITIES];
      } catch (error) {
        console.warn('Failed to fetch cities from API, using fallback:', error);
        // API o'chsa fallback ro'yxatdan foydalanamiz
        return [...FALLBACK_CITIES];
      }
    },
    staleTime: 1000 * 60 * 60, // 1 soat cache'da saqlaymiz
    retry: 1, // Faqat 1 marta urinib ko'ramiz
  });
}