import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiFetch } from './api';
import type { Coordinates } from './format';
import type { NeedInput } from './schemas';
import type { AcceptedOffer, Donation, Need, NeedStatus, Offer, OfferStatus, Page } from './types';

const PAGE_SIZE = 20;

export const needStatusLabels: Record<NeedStatus, string> = {
  OPEN: 'Open',
  FULFILLED: 'Fulfilled',
  CLOSED: 'Closed',
};

export const offerStatusLabels: Record<OfferStatus, string> = {
  PENDING: 'Waiting',
  ACCEPTED: 'Chosen',
  DECLINED: 'Not chosen',
  WITHDRAWN: 'Withdrawn',
};

/** The items a donor can offer: only their own items that are still available. */
export function offerableItems(donations: Donation[]): Donation[] {
  return donations.filter((donation) => donation.status === 'AVAILABLE');
}

/** An offer can be chosen only while it is waiting and its item is still available. */
export function canChoose(offer: Offer, need: Need): boolean {
  return need.status === 'OPEN' && offer.status === 'PENDING' && offer.donationStatus === 'AVAILABLE';
}

/** Open needs from others, nearest first when coordinates are given (rounded as in the item feed). */
export function useNeedsFeed(near: Coordinates | null, enabled: boolean) {
  const lat = near ? near.latitude.toFixed(2) : null;
  const lng = near ? near.longitude.toFixed(2) : null;
  return useInfiniteQuery({
    queryKey: ['needs', 'feed', lat, lng],
    queryFn: ({ pageParam }) => {
      const params = new URLSearchParams({ page: String(pageParam), size: String(PAGE_SIZE) });
      if (lat && lng) {
        params.set('lat', lat);
        params.set('lng', lng);
      }
      return apiFetch<Page<Need>>(`/api/v1/needs?${params.toString()}`);
    },
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined),
    enabled,
  });
}

export function useNeed(id: string) {
  return useQuery({
    queryKey: ['needs', 'one', id],
    queryFn: () => apiFetch<Need>(`/api/v1/needs/${id}`),
  });
}

export function useMyNeeds() {
  return useInfiniteQuery({
    queryKey: ['needs', 'mine'],
    queryFn: ({ pageParam }) => apiFetch<Page<Need>>(`/api/v1/needs/mine?page=${pageParam}&size=${PAGE_SIZE}`),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined),
  });
}

/** Only for the person who asked. */
export function useNeedOffers(needId: string, enabled: boolean) {
  return useQuery({
    queryKey: ['needs', 'offers', needId],
    queryFn: () => apiFetch<Offer[]>(`/api/v1/needs/${needId}/offers`),
    enabled,
  });
}

export function useMyOffers() {
  return useInfiniteQuery({
    queryKey: ['needs', 'my-offers'],
    queryFn: ({ pageParam }) => apiFetch<Page<Offer>>(`/api/v1/offers/mine?page=${pageParam}&size=${PAGE_SIZE}`),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined),
  });
}

/** The fields of a need plus the approximate location, as JSON. */
export function needBody(input: NeedInput, location: Coordinates) {
  return { ...input, latitude: location.latitude, longitude: location.longitude };
}

export function useCreateNeed() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ input, location }: { input: NeedInput; location: Coordinates }) =>
      apiFetch<Need>('/api/v1/needs', { method: 'POST', body: needBody(input, location) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['needs'] }),
  });
}

export function useCloseNeed(needId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<Need>(`/api/v1/needs/${needId}/close`, { method: 'POST' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['needs'] }),
  });
}

export function useMakeOffer(needId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (offer: { donationId: string; message: string }) =>
      apiFetch<Offer>(`/api/v1/needs/${needId}/offers`, {
        method: 'POST',
        body: { donationId: offer.donationId, message: offer.message || null },
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['needs'] }),
  });
}

/** Choosing an offer reserves the item, so items, claims and pickups change too. */
export function useAcceptOffer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (offerId: string) => apiFetch<AcceptedOffer>(`/api/v1/offers/${offerId}/accept`, { method: 'POST' }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['needs'] });
      void queryClient.invalidateQueries({ queryKey: ['donations'] });
      void queryClient.invalidateQueries({ queryKey: ['claims'] });
      void queryClient.invalidateQueries({ queryKey: ['chat'] });
    },
  });
}

export function useWithdrawOffer() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (offerId: string) => apiFetch<void>(`/api/v1/offers/${offerId}/withdraw`, { method: 'POST' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['needs'] }),
  });
}
