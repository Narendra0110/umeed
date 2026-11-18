import { deleteUser, getAuth, signOut } from '@react-native-firebase/auth';

import { apiFetch } from './api';

type SignedInUser = { metadata: { lastSignInTime?: string } };

/**
 * True when the user signed in at or after the given moment. Firebase only deletes a sign-in that is
 * recent, and on Android the SMS code can sign the user in by itself without being typed.
 */
export function signedInSince(user: SignedInUser | null, sinceMs: number): boolean {
  const lastSignIn = user?.metadata.lastSignInTime;
  if (!lastSignIn) {
    return false;
  }
  const at = Date.parse(lastSignIn);
  // A few seconds of slack: the phone's clock and Firebase's clock are not exactly the same.
  return !Number.isNaN(at) && at >= sinceMs - 5000;
}

/**
 * Deletes the account on the Umeed server, then the Firebase sign-in, then signs out.
 * The server goes first: if it refuses (for example a pickup is in progress), nothing else happens.
 * If Firebase cannot delete the sign-in, the Umeed account is still gone, so the user is signed out anyway.
 */
export async function deleteAccountEverywhere(): Promise<void> {
  await apiFetch<void>('/api/v1/users/me', { method: 'DELETE' });
  const auth = getAuth();
  const user = auth.currentUser;
  if (user) {
    try {
      await deleteUser(user);
    } catch (cause) {
      if (__DEV__) {
        console.warn('Could not delete the Firebase sign-in', cause);
      }
    }
  }
  if (auth.currentUser) {
    await signOut(auth);
  }
}
