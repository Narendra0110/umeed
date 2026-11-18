import { getAuth } from '@react-native-firebase/auth';

import { t, useLanguageStore } from './i18n';

/**
 * The backend address. The default suits a phone connected by USB: run-android.cmd runs
 * "adb reverse tcp:8080 tcp:8080", so localhost:8080 on the phone reaches the PC's backend.
 * Override with EXPO_PUBLIC_API_URL in a .env file (for example http://10.0.2.2:8080 for the
 * Android emulator, or the PC's network address for a phone on Wi-Fi).
 */
export const API_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';

export type FieldError = { field: string; message: string };

/** An error from the backend, in the shape of its ApiError response, or a network failure (status 0). */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: FieldError[];

  constructor(status: number, message: string, fieldErrors: FieldError[] = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

type RequestOptions = { method?: string; body?: unknown };

/** Calls the backend with the signed-in user's Firebase token. */
export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const user = getAuth().currentUser;
  if (!user) {
    throw new ApiError(401, t().errors.logInAgain);
  }
  const token = await user.getIdToken();
  const hasBody = options.body !== undefined;
  // A FormData body (photos) is sent as multipart; fetch sets its Content-Type with the boundary.
  const isForm = typeof FormData !== 'undefined' && options.body instanceof FormData;

  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method: options.method ?? 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/json',
        // The backend answers with Hindi messages when asked (F07B).
        'Accept-Language': useLanguageStore.getState().language,
        ...(hasBody && !isForm ? { 'Content-Type': 'application/json' } : {}),
      },
      body: !hasBody ? undefined : isForm ? (options.body as FormData) : JSON.stringify(options.body),
    });
  } catch (cause) {
    if (__DEV__) {
      console.warn(`Network request failed: ${options.method ?? 'GET'} ${API_URL}${path}`, cause);
    }
    throw new ApiError(0, t().errors.noServer);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  const data = parseJson(await response.text());
  if (!response.ok) {
    const fallback = response.status === 401 ? t().errors.logInAgain : t().errors.generic;
    throw new ApiError(
      response.status,
      typeof data?.message === 'string' ? data.message : fallback,
      Array.isArray(data?.fieldErrors) ? data.fieldErrors : [],
    );
  }
  return data as T;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
function parseJson(text: string): any {
  if (!text) {
    return undefined;
  }
  try {
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

/** The full address of a stored photo. The backend returns photo addresses relative to the server. */
export function fileUrl(path: string): string {
  return /^https?:\/\//.test(path) ? path : `${API_URL}${path}`;
}

/**
 * The backend's answer to anything a suspended (or deleted) account tries to do, in English and
 * (from F07B, when the app asks for Hindi) in Hindi.
 */
export const ACCOUNT_NOT_ACTIVE = ['Account is not active', 'खाता सक्रिय नहीं है'];

export function isAccountNotActive(error: unknown): boolean {
  return error instanceof ApiError && error.status === 403 && ACCOUNT_NOT_ACTIVE.includes(error.message);
}

/** A short message suitable for showing to the user, in the current language. */
export function errorMessage(error: unknown): string {
  if (isAccountNotActive(error)) {
    return t().errors.suspended;
  }
  if (error instanceof ApiError) {
    return error.fieldErrors[0]?.message ?? error.message;
  }
  return t().errors.generic;
}
