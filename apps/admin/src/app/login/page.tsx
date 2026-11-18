'use client';

import { type ConfirmationResult, RecaptchaVerifier, signInWithPhoneNumber } from 'firebase/auth';
import { useRouter } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';

import { firebaseAuth, firebaseConfigured } from '@/lib/firebase';
import { toE164 } from '@/lib/format';
import { useFirebaseUser } from '@/lib/hooks';

/** Login with a phone number and SMS code, the same Firebase login the app uses. */
export default function LoginPage() {
  const router = useRouter();
  const { ready, user } = useFirebaseUser();
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [confirmation, setConfirmation] = useState<ConfirmationResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const verifier = useRef<RecaptchaVerifier | null>(null);

  useEffect(() => {
    if (ready && user) {
      router.replace('/');
    }
  }, [ready, user, router]);

  useEffect(() => () => verifier.current?.clear(), []);

  if (!firebaseConfigured) {
    return (
      <main className="login">
        <h1>Umeed Admin</h1>
        <div className="error">
          Firebase is not set up for the dashboard yet. Copy .env.example to .env.local in apps/admin, fill in the
          Web app values from the Firebase console, and restart the dashboard.
        </div>
      </main>
    );
  }

  const sendCode = async () => {
    const auth = firebaseAuth();
    const number = toE164(phone);
    if (!auth || !number) {
      setError('Enter a 10-digit mobile number, or a full number starting with +.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      verifier.current ??= new RecaptchaVerifier(auth, 'recaptcha', { size: 'invisible' });
      setConfirmation(await signInWithPhoneNumber(auth, number, verifier.current));
    } catch (failure) {
      verifier.current?.clear();
      verifier.current = null;
      setError(firebaseMessage(failure));
    } finally {
      setBusy(false);
    }
  };

  const confirmCode = async () => {
    if (!confirmation || !/^\d{6}$/.test(code)) {
      setError('Enter the 6-digit code from the SMS.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await confirmation.confirm(code);
      router.replace('/');
    } catch (failure) {
      setError(firebaseMessage(failure));
      setBusy(false);
    }
  };

  return (
    <main className="login">
      <h1>Umeed Admin</h1>
      <p className="muted">Only accounts marked as admin can use this dashboard.</p>
      {!confirmation ? (
        <form
          className="stack"
          onSubmit={(event) => {
            event.preventDefault();
            void sendCode();
          }}>
          <label>
            Phone number
            <input
              type="tel"
              inputMode="tel"
              autoComplete="tel"
              placeholder="98765 43210"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
            />
          </label>
          <button type="submit" className="button primary" disabled={busy}>
            {busy ? 'Sending…' : 'Send code'}
          </button>
        </form>
      ) : (
        <form
          className="stack"
          onSubmit={(event) => {
            event.preventDefault();
            void confirmCode();
          }}>
          <label>
            Code sent to {toE164(phone)}
            <input
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              value={code}
              onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))}
              autoFocus
            />
          </label>
          <button type="submit" className="button primary" disabled={busy}>
            {busy ? 'Checking…' : 'Log in'}
          </button>
          <button
            type="button"
            className="button secondary"
            disabled={busy}
            onClick={() => {
              setConfirmation(null);
              setCode('');
            }}>
            Use another number
          </button>
        </form>
      )}
      {error ? <p className="field-error">{error}</p> : null}
      <div id="recaptcha" />
    </main>
  );
}

function firebaseMessage(failure: unknown): string {
  const code = typeof failure === 'object' && failure && 'code' in failure ? String(failure.code) : '';
  switch (code) {
    case 'auth/invalid-verification-code':
      return 'That code is not right. Check the SMS and try again.';
    case 'auth/code-expired':
      return 'That code has expired. Use another number to request a new one.';
    case 'auth/too-many-requests':
      return 'Too many attempts. Wait a while and try again.';
    case 'auth/invalid-phone-number':
      return 'That phone number is not valid.';
    case 'auth/unauthorized-domain':
      return 'This address is not allowed for login. Add it under Authentication > Settings > Authorized domains.';
    default:
      return 'Could not log in. Please try again.';
  }
}
