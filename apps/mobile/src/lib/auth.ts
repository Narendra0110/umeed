import { getAuth, onAuthStateChanged } from '@react-native-firebase/auth';
import { useEffect, useState } from 'react';

export type AuthUser = ReturnType<typeof getAuth>['currentUser'];

/** The signed-in Firebase user, kept up to date. */
export function useAuthUser(): { initializing: boolean; user: AuthUser } {
  const [state, setState] = useState<{ initializing: boolean; user: AuthUser }>(() => ({
    initializing: true,
    user: getAuth().currentUser,
  }));

  useEffect(() => onAuthStateChanged(getAuth(), (user) => setState({ initializing: false, user })), []);

  return state;
}

/** Turns a Firebase error into a message for the user. */
export function firebaseErrorMessage(error: unknown): string {
  const code = typeof error === 'object' && error !== null && 'code' in error ? String(error.code) : '';
  switch (code) {
    case 'auth/invalid-phone-number':
      return 'This phone number is not valid.';
    case 'auth/invalid-verification-code':
      return 'The code is not correct. Please check and try again.';
    case 'auth/session-expired':
    case 'auth/code-expired':
      return 'The code has expired. Please request a new one.';
    case 'auth/too-many-requests':
      return 'Too many attempts. Please wait a while and try again.';
    case 'auth/network-request-failed':
      return 'No internet connection. Please try again.';
    default:
      return 'Login failed. Please try again.';
  }
}
