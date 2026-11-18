import { type FirebaseApp, getApps, initializeApp } from 'firebase/app';
import { type Auth, getAuth } from 'firebase/auth';

const config = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
};

/** True when .env.local has all four Firebase values. */
export const firebaseConfigured = Object.values(config).every((value) => typeof value === 'string' && value !== '');

let auth: Auth | null = null;

/**
 * Firebase Auth for the browser, created on first use. Returns null when the config is missing or when
 * called on the server (pages are pre-rendered at build time, where there is no signed-in user).
 */
export function firebaseAuth(): Auth | null {
  if (!firebaseConfigured || typeof window === 'undefined') {
    return null;
  }
  if (!auth) {
    const app: FirebaseApp = getApps()[0] ?? initializeApp(config);
    auth = getAuth(app);
  }
  return auth;
}
