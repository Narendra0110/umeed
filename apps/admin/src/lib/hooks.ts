'use client';

import { onIdTokenChanged, type User } from 'firebase/auth';
import { useCallback, useEffect, useState } from 'react';

import { apiFetch, errorMessage } from './api';
import { firebaseAuth } from './firebase';

/** The Firebase user in this browser. ready is false until Firebase has restored any earlier login. */
export function useFirebaseUser() {
  const [state, setState] = useState<{ ready: boolean; user: User | null }>({ ready: false, user: null });
  useEffect(() => {
    const auth = firebaseAuth();
    if (!auth) {
      setState({ ready: true, user: null });
      return;
    }
    return onIdTokenChanged(auth, (user) => setState({ ready: true, user }));
  }, []);
  return state;
}

/**
 * Loads one backend path. Pass null to wait. reload() fetches again; setData() replaces the result,
 * for example with what an action returned.
 */
export function useApi<T>(path: string | null) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(path !== null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (path === null) {
      return;
    }
    let current = true;
    setLoading(true);
    setError(null);
    apiFetch<T>(path)
      .then((result) => current && setData(result))
      .catch((failure) => current && setError(errorMessage(failure)))
      .finally(() => current && setLoading(false));
    return () => {
      current = false;
    };
  }, [path, version]);

  const reload = useCallback(() => setVersion((value) => value + 1), []);
  return { data, error, loading, reload, setData };
}
