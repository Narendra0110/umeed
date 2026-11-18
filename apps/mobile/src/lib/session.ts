import { getAuth, signOut } from '@react-native-firebase/auth';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';

import { unregisterDevice } from './push';

/**
 * Headers for loading photos. Photos are private to logged-in users, so the image component must
 * send the Firebase token too. Firebase tokens last one hour; this refreshes the copy every 30 minutes.
 */
export function useAuthHeaders(): Record<string, string> | undefined {
  const token = useQuery({
    queryKey: ['auth', 'idToken'],
    queryFn: async () => {
      const user = getAuth().currentUser;
      return user ? user.getIdToken() : null;
    },
    staleTime: 30 * 60 * 1000,
    refetchInterval: 30 * 60 * 1000,
  });
  return token.data ? { Authorization: `Bearer ${token.data}` } : undefined;
}

/** Signs out, forgets everything cached for this user and returns to the login screen. */
export function useLogOut() {
  const queryClient = useQueryClient();
  return async () => {
    // While still logged in, so the server accepts it. Never blocks logging out.
    await unregisterDevice();
    await signOut(getAuth());
    queryClient.clear();
    router.replace('/login');
  };
}
