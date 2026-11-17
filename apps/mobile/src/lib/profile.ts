import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiFetch } from './api';

export type UserProfile = {
  id: string;
  phoneNumber: string;
  displayName: string | null;
  profileComplete: boolean;
  role: string;
  status: string;
};

export const profileQueryKey = ['users', 'me'] as const;

/** The signed-in user's profile. The first call registers the user on the backend. */
export function useProfile(enabled: boolean) {
  return useQuery({
    queryKey: profileQueryKey,
    queryFn: () => apiFetch<UserProfile>('/api/v1/users/me'),
    enabled,
  });
}

export function useUpdateDisplayName() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (displayName: string) =>
      apiFetch<UserProfile>('/api/v1/users/me', { method: 'PUT', body: { displayName } }),
    onSuccess: (profile) => queryClient.setQueryData(profileQueryKey, profile),
  });
}
