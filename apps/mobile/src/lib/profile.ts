import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { apiFetch } from './api';
import { isLanguage, type Language, useLanguageStore } from './i18n';

export type UserProfile = {
  id: string;
  phoneNumber: string;
  displayName: string | null;
  profileComplete: boolean;
  role: string;
  status: string;
  language: Language;
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

/** Saves the language to the account, so it follows the person to another phone and to notifications. */
export function useUpdateLanguage() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (language: Language) =>
      apiFetch<UserProfile>('/api/v1/users/me/language', { method: 'PUT', body: { language } }),
    onSuccess: (profile) => queryClient.setQueryData(profileQueryKey, profile),
  });
}

/**
 * What to do with the language once the account is known:
 * - a new account (no name yet), or a language picked on this phone before logging in: save the phone's
 *   language to the account;
 * - otherwise the account's saved language wins and the app switches to it.
 * Returns null when the two already agree.
 */
export function languageAction(
  profile: Pick<UserProfile, 'profileComplete' | 'language'>,
  current: Language,
  chosenOnThisPhone: boolean,
): { save: Language } | { use: Language } | null {
  const saved = isLanguage(profile.language) ? profile.language : 'en';
  if (saved === current) {
    return null;
  }
  if (!profile.profileComplete || chosenOnThisPhone) {
    return { save: current };
  }
  return { use: saved };
}

/** Applies languageAction once per profile load. Errors are ignored: the app keeps the language it shows. */
export function syncLanguage(
  profile: UserProfile,
  save: (language: Language) => void,
): void {
  const { language, chosen, setLanguage } = useLanguageStore.getState();
  const action = languageAction(profile, language, chosen);
  if (!action) {
    return;
  }
  if ('save' in action) {
    save(action.save);
  } else {
    setLanguage(action.use);
  }
}
