import { getAuth, onAuthStateChanged } from '@react-native-firebase/auth';
import { useEffect, useState } from 'react';

import { t } from './i18n';

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
  const m = t().firebase;
  switch (code) {
    case 'auth/invalid-phone-number':
      return m.invalidPhone;
    case 'auth/invalid-verification-code':
      return m.wrongCode;
    case 'auth/session-expired':
    case 'auth/code-expired':
      return m.codeExpired;
    case 'auth/too-many-requests':
      return m.tooMany;
    case 'auth/network-request-failed':
      return m.offline;
    default:
      return m.failed;
  }
}
