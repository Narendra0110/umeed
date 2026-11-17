import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiFetch } from './api';
import type { Pickup } from './types';

/** Private pickup details. The backend answers 403 for anyone but the donor and the accepted receiver. */
export function usePickup(donationId: string) {
  return useQuery({
    queryKey: ['pickup', donationId],
    queryFn: () => apiFetch<Pickup>(`/api/v1/donations/${donationId}/pickup`),
  });
}

export function useConfirmPickup(donationId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<Pickup>(`/api/v1/donations/${donationId}/pickup/confirm`, { method: 'POST' }),
    onSuccess: (pickup) => {
      queryClient.setQueryData(['pickup', donationId], pickup);
      queryClient.invalidateQueries({ queryKey: ['donations'] });
      queryClient.invalidateQueries({ queryKey: ['claims'] });
    },
  });
}

export function useCancelPickup(donationId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<void>(`/api/v1/donations/${donationId}/pickup/cancel`, { method: 'POST' }),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: ['pickup', donationId] });
      queryClient.invalidateQueries({ queryKey: ['donations'] });
      queryClient.invalidateQueries({ queryKey: ['claims'] });
    },
  });
}

/** Which side of the handover the signed-in user is on, matched by phone number. */
export function pickupRole(pickup: Pickup, myPhoneNumber: string | undefined): 'donor' | 'receiver' | null {
  if (!myPhoneNumber) {
    return null;
  }
  if (pickup.donor.phoneNumber === myPhoneNumber) {
    return 'donor';
  }
  if (pickup.receiver.phoneNumber === myPhoneNumber) {
    return 'receiver';
  }
  return null;
}
