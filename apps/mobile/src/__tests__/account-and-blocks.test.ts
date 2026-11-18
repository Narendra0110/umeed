import { deleteUser, getAuth, signOut } from '@react-native-firebase/auth';

import { deleteAccountEverywhere, signedInSince } from '@/lib/account';
import { ApiError } from '@/lib/api';

jest.mock('@react-native-firebase/auth', () => ({
  getAuth: jest.fn(),
  deleteUser: jest.fn(),
  signOut: jest.fn(),
}));

const mockedGetAuth = getAuth as jest.Mock;
const mockedDeleteUser = deleteUser as jest.Mock;
const mockedSignOut = signOut as jest.Mock;
const fetchMock = jest.fn();

function respond(status: number, body?: unknown) {
  fetchMock.mockResolvedValueOnce({
    status,
    ok: status >= 200 && status < 300,
    text: async () => (body === undefined ? '' : JSON.stringify(body)),
  });
}

/** A fake Firebase auth whose current user disappears once it is deleted, as the real one does. */
function fakeAuth() {
  const auth: { currentUser: unknown } = {
    currentUser: { getIdToken: jest.fn().mockResolvedValue('test-token'), metadata: {} },
  };
  mockedGetAuth.mockReturnValue(auth);
  mockedDeleteUser.mockImplementation(async () => {
    auth.currentUser = null;
  });
  return auth;
}

beforeEach(() => {
  fetchMock.mockReset();
  mockedDeleteUser.mockReset();
  mockedSignOut.mockReset();
  globalThis.fetch = fetchMock as unknown as typeof fetch;
});

describe('deleteAccountEverywhere', () => {
  it('deletes the server account first, then the Firebase sign-in', async () => {
    const auth = fakeAuth();
    respond(204);

    await deleteAccountEverywhere();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toMatch(/\/api\/v1\/users\/me$/);
    expect(init.method).toBe('DELETE');
    expect(mockedDeleteUser).toHaveBeenCalledTimes(1);
    expect(auth.currentUser).toBeNull();
    expect(mockedSignOut).not.toHaveBeenCalled();
  });

  it('changes nothing when the server refuses, for example during a pickup', async () => {
    fakeAuth();
    respond(409, { status: 409, message: 'Finish or cancel your pickups before deleting your account' });

    const error = (await deleteAccountEverywhere().catch((e: unknown) => e)) as ApiError;

    expect(error).toBeInstanceOf(ApiError);
    expect(error.message).toBe('Finish or cancel your pickups before deleting your account');
    expect(mockedDeleteUser).not.toHaveBeenCalled();
    expect(mockedSignOut).not.toHaveBeenCalled();
  });

  it('still signs out when Firebase cannot delete the sign-in', async () => {
    fakeAuth();
    mockedDeleteUser.mockRejectedValueOnce(new Error('auth/requires-recent-login'));
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
    respond(204);

    await deleteAccountEverywhere();

    expect(mockedSignOut).toHaveBeenCalledTimes(1);
    warn.mockRestore();
  });
});

describe('signedInSince', () => {
  const sentAt = Date.parse('2026-11-18T10:00:00Z');

  it('accepts a sign-in after the code was sent', () => {
    expect(signedInSince({ metadata: { lastSignInTime: '2026-11-18T10:00:30Z' } }, sentAt)).toBe(true);
  });

  it('allows a few seconds of clock difference', () => {
    expect(signedInSince({ metadata: { lastSignInTime: '2026-11-18T09:59:58Z' } }, sentAt)).toBe(true);
  });

  it('rejects an older sign-in, a missing time and no user', () => {
    expect(signedInSince({ metadata: { lastSignInTime: '2026-11-17T10:00:00Z' } }, sentAt)).toBe(false);
    expect(signedInSince({ metadata: {} }, sentAt)).toBe(false);
    expect(signedInSince(null, sentAt)).toBe(false);
  });
});
