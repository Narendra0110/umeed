import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert } from 'react-native';

import { apiFetch, errorMessage } from './api';

export type BlockedUser = {
  userId: string;
  name: string;
  blockedAt: string;
};

export function useBlockedUsers() {
  return useQuery({
    queryKey: ['blocks'],
    queryFn: () => apiFetch<BlockedUser[]>('/api/v1/blocks'),
  });
}

/** A block hides items both ways and closes open claims, so the feed, claims and blocks lists all change. */
function useRefreshAfterBlockChange() {
  const queryClient = useQueryClient();
  return () =>
    Promise.all([
      queryClient.invalidateQueries({ queryKey: ['blocks'] }),
      queryClient.invalidateQueries({ queryKey: ['donations'] }),
      queryClient.invalidateQueries({ queryKey: ['claims'] }),
    ]);
}

export function useBlockUser() {
  const refresh = useRefreshAfterBlockChange();
  return useMutation({
    mutationFn: (userId: string) => apiFetch<void>('/api/v1/blocks', { method: 'POST', body: { userId } }),
    onSuccess: refresh,
  });
}

export function useUnblockUser() {
  const refresh = useRefreshAfterBlockChange();
  return useMutation({
    mutationFn: (userId: string) => apiFetch<void>(`/api/v1/blocks/${userId}`, { method: 'DELETE' }),
    onSuccess: refresh,
  });
}

/** Asks before blocking, explains what a block does, and reports the result. */
export function confirmBlock(
  name: string,
  block: (onDone: { onSuccess: () => void; onError: (error: unknown) => void }) => void,
  afterBlocked?: () => void,
) {
  Alert.alert(
    `Block ${name}?`,
    'You will not see each other’s items or be able to claim them, and any open claims between you will be closed. You can unblock them later from your Profile.',
    [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Block',
        style: 'destructive',
        onPress: () =>
          block({
            onSuccess: () => {
              Alert.alert('Blocked', `You will no longer see items from ${name}.`);
              afterBlocked?.();
            },
            onError: (error) => Alert.alert('Could not block', errorMessage(error)),
          }),
      },
    ],
  );
}
