import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';

import { apiFetch } from './api';
import type { Coordinates } from './format';
import type { Donation, Page } from './types';

const PAGE_SIZE = 30;

/** The distance choices on the search screen; null means any distance. */
export const DISTANCE_CHOICES = [2, 5, 10, 25] as const;

export type SearchFilters = {
  text: string;
  categoryId: string | null;
  city: string;
  /** Needs the phone's location; ignored without it. */
  maxKm: number | null;
};

export const emptyFilters: SearchFilters = { text: '', categoryId: null, city: '', maxKm: null };

/**
 * The query string for /api/v1/donations/search. Empty filters are left out. Without a location the
 * distance filter cannot work, so it is left out too and results come newest first.
 */
export function searchQuery(filters: SearchFilters, near: Coordinates | null, page = 0, size = PAGE_SIZE): string {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  const text = filters.text.trim();
  const city = filters.city.trim();
  if (text) {
    params.set('q', text);
  }
  if (filters.categoryId) {
    params.set('categoryId', filters.categoryId);
  }
  if (city) {
    params.set('city', city);
  }
  if (near) {
    params.set('lat', near.latitude.toFixed(2));
    params.set('lng', near.longitude.toFixed(2));
    if (filters.maxKm) {
      params.set('maxKm', String(filters.maxKm));
    }
  }
  return params.toString();
}

export function useSearch(filters: SearchFilters, near: Coordinates | null, enabled: boolean) {
  return useInfiniteQuery({
    queryKey: ['donations', 'search', searchQuery(filters, near)],
    queryFn: ({ pageParam }) =>
      apiFetch<Page<Donation>>(`/api/v1/donations/search?${searchQuery(filters, near, pageParam)}`),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined),
    enabled,
  });
}

/** "Not interested": the item leaves Home for good (until the list is cleared in Profile). */
export function useSkipDonation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (donationId: string) => apiFetch<void>(`/api/v1/donations/${donationId}/skip`, { method: 'POST' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['donations', 'feed'] }),
  });
}

/** "Show hidden items again". */
export function useClearSkips() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<void>('/api/v1/donations/skips', { method: 'DELETE' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['donations', 'feed'] }),
  });
}

export type MapPin = { donation: Donation; latitude: number; longitude: number };

/**
 * Item locations are rounded to about 1 km, so several items often share exactly the same point and
 * their pins would hide each other. Items at the same point are placed on a small circle around it
 * (about 150 m), in a stable order. The first item at a point stays exactly on it.
 */
export function spreadPins(donations: Donation[]): MapPin[] {
  const seen = new Map<string, number>();
  return donations.map((donation) => {
    const key = `${donation.latitude.toFixed(4)},${donation.longitude.toFixed(4)}`;
    const index = seen.get(key) ?? 0;
    seen.set(key, index + 1);
    if (index === 0) {
      return { donation, latitude: donation.latitude, longitude: donation.longitude };
    }
    const angle = (index - 1) * ((2 * Math.PI) / 6);
    const ring = 0.0014 * Math.ceil(index / 6);
    return {
      donation,
      latitude: donation.latitude + ring * Math.sin(angle),
      longitude: donation.longitude + ring * Math.cos(angle),
    };
  });
}
