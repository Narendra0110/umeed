import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiFetch } from './api';
import type { Coordinates } from './format';
import type { DonationInput } from './schemas';
import type { Category, Donation, Page } from './types';

const PAGE_SIZE = 20;

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => apiFetch<Category[]>('/api/v1/categories'),
    staleTime: 60 * 60 * 1000,
  });
}

/**
 * The feed of available donations, nearest first when coordinates are given, otherwise newest first.
 * Coordinates are rounded to two decimals so small movements of the phone do not start a new query.
 */
export function useFeed(near: Coordinates | null, enabled: boolean) {
  const lat = near ? near.latitude.toFixed(2) : null;
  const lng = near ? near.longitude.toFixed(2) : null;
  return useInfiniteQuery({
    queryKey: ['donations', 'feed', lat, lng],
    queryFn: ({ pageParam }) => {
      const params = new URLSearchParams({ page: String(pageParam), size: String(PAGE_SIZE) });
      if (lat && lng) {
        params.set('lat', lat);
        params.set('lng', lng);
      }
      return apiFetch<Page<Donation>>(`/api/v1/donations?${params.toString()}`);
    },
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined),
    enabled,
  });
}

export function useDonation(id: string) {
  return useQuery({
    queryKey: ['donations', 'one', id],
    queryFn: () => apiFetch<Donation>(`/api/v1/donations/${id}`),
  });
}

export function useMyDonations() {
  return useInfiniteQuery({
    queryKey: ['donations', 'mine'],
    queryFn: ({ pageParam }) =>
      apiFetch<Page<Donation>>(`/api/v1/donations/mine?page=${pageParam}&size=${PAGE_SIZE}`),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined),
  });
}

/** Builds the multipart form the backend expects: the details as fields and the photos as "photos". */
export function donationFormData(input: DonationInput, location: Coordinates): FormData {
  const form = new FormData();
  form.append('categoryId', input.categoryId);
  form.append('title', input.title);
  form.append('description', input.description);
  form.append('itemCondition', input.itemCondition);
  form.append('city', input.city);
  form.append('area', input.area);
  form.append('latitude', String(location.latitude));
  form.append('longitude', String(location.longitude));
  form.append('pickupAddress', input.pickupAddress);
  for (const photo of input.photos) {
    // React Native's FormData accepts a file as { uri, name, type }.
    form.append('photos', { uri: photo.uri, name: photo.name, type: photo.type } as unknown as Blob);
  }
  return form;
}

export function useCreateDonation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ input, location }: { input: DonationInput; location: Coordinates }) =>
      apiFetch<Donation>('/api/v1/donations', { method: 'POST', body: donationFormData(input, location) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['donations'] }),
  });
}

export function useRemoveDonation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiFetch<void>(`/api/v1/donations/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['donations'] });
      queryClient.invalidateQueries({ queryKey: ['claims'] });
    },
  });
}
