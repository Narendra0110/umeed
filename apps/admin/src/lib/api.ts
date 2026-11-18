import { firebaseAuth } from './firebase';

export type FieldError = { field: string; message: string };

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

async function token(): Promise<string> {
  const user = firebaseAuth()?.currentUser;
  if (!user) {
    throw new ApiError(401, 'You are not logged in');
  }
  return user.getIdToken();
}

/** Calls the backend (through the /api rewrite) with the admin's Firebase token. */
export async function apiFetch<T>(path: string, options: { method?: string; body?: unknown } = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      method: options.method ?? 'GET',
      headers: {
        Authorization: `Bearer ${await token()}`,
        ...(options.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
      cache: 'no-store',
    });
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(0, 'Cannot reach the Umeed server. Is the backend running?');
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  const data = text ? safeJson(text) : null;
  if (!response.ok) {
    const message =
      (data && typeof data.message === 'string' && data.message) ||
      (response.status >= 500 ? 'The server had a problem. Please try again.' : `Request failed (${response.status})`);
    throw new ApiError(response.status, message, Array.isArray(data?.fieldErrors) ? data.fieldErrors : []);
  }
  return data as T;
}

/** A photo from the backend, which needs the token, as a temporary browser URL. */
export async function fetchImage(path: string): Promise<string> {
  const response = await fetch(path, { headers: { Authorization: `Bearer ${await token()}` } });
  if (!response.ok) {
    throw new ApiError(response.status, 'Photo not available');
  }
  return URL.createObjectURL(await response.blob());
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.fieldErrors[0]?.message ?? error.message;
  }
  return 'Something went wrong. Please try again.';
}

// The backend's error body: { message, fieldErrors }. Anything else is ignored.
function safeJson(text: string): { message?: unknown; fieldErrors?: FieldError[] } | null {
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}
