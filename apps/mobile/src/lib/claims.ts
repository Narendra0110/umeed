import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiFetch } from './api';
import type { Claim, Page } from './types';

/** Claims, donations and pickup details change together, so a change refreshes all three. */
function useRefreshAfterChange() {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['claims'] }),
      queryClient.invalidateQueries({ queryKey: ['donations'] }),
      queryClient.invalidateQueries({ queryKey: ['pickup'] }),
    ]);
}

export function useCreateClaim() {
  const refresh = useRefreshAfterChange();
  return useMutation({
    mutationFn: ({ donationId, message }: { donationId: string; message: string }) =>
      apiFetch<Claim>(`/api/v1/donations/${donationId}/claims`, {
        method: 'POST',
        body: message.trim() ? { message: message.trim() } : {},
      }),
    onSuccess: refresh,
  });
}

/** Donor only: every claim on one item, oldest first. */
export function useDonationClaims(donationId: string) {
  return useQuery({
    queryKey: ['claims', 'donation', donationId],
    queryFn: () => apiFetch<Claim[]>(`/api/v1/donations/${donationId}/claims`),
  });
}

export function useMyClaims() {
  return useInfiniteQuery({
    queryKey: ['claims', 'mine'],
    queryFn: ({ pageParam }) => apiFetch<Page<Claim>>(`/api/v1/claims/mine?page=${pageParam}&size=20`),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.hasNext ? last.page + 1 : undefined),
  });
}

export function useAcceptClaim() {
  const refresh = useRefreshAfterChange();
  return useMutation({
    mutationFn: (claimId: string) => apiFetch<Claim>(`/api/v1/claims/${claimId}/accept`, { method: 'POST' }),
    onSuccess: refresh,
  });
}

export function useWithdrawClaim() {
  const refresh = useRefreshAfterChange();
  return useMutation({
    mutationFn: (claimId: string) => apiFetch<Claim>(`/api/v1/claims/${claimId}/withdraw`, { method: 'POST' }),
    onSuccess: refresh,
  });
}
