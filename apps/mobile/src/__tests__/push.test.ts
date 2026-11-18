import { getAuth } from '@react-native-firebase/auth';
import { getToken } from '@react-native-firebase/messaging';
import { Platform } from 'react-native';

import { notificationRoute, registerDevice, unregisterDevice } from '@/lib/push';

jest.mock('@react-native-firebase/auth', () => ({ getAuth: jest.fn() }));
jest.mock('@react-native-firebase/messaging', () => ({
  getMessaging: jest.fn(() => ({})),
  getToken: jest.fn(),
  getInitialNotification: jest.fn(),
  onMessage: jest.fn(),
  onNotificationOpenedApp: jest.fn(),
  onTokenRefresh: jest.fn(),
}));

const mockedGetAuth = getAuth as jest.Mock;
const mockedGetToken = getToken as jest.Mock;
const fetchMock = jest.fn();
const DONATION = '0b4a4c1e-5d1c-4f3e-9a52-6f1f2c3d4e5f';

beforeEach(() => {
  fetchMock.mockReset();
  mockedGetToken.mockReset();
  globalThis.fetch = fetchMock as unknown as typeof fetch;
  mockedGetAuth.mockReturnValue({ currentUser: { getIdToken: jest.fn().mockResolvedValue('test-token') } });
  fetchMock.mockResolvedValue({ status: 204, ok: true, text: async () => '' });
});

describe('notificationRoute', () => {
  it('opens the screen the backend asked for', () => {
    expect(notificationRoute({ donationId: DONATION, screen: 'claims' })).toBe(`/donation/${DONATION}/claims`);
    expect(notificationRoute({ donationId: DONATION, screen: 'pickup' })).toBe(`/donation/${DONATION}/pickup`);
    expect(notificationRoute({ donationId: DONATION, screen: 'item' })).toBe(`/donation/${DONATION}`);
    expect(notificationRoute({ donationId: DONATION })).toBe(`/donation/${DONATION}`);
  });

  it('opens the chat for a new message', () => {
    const claimId = '9c1d2e3f-4a5b-4c6d-8e7f-0a1b2c3d4e5f';
    expect(notificationRoute({ donationId: DONATION, claimId, screen: 'chat' })).toBe(`/chat/${claimId}`);
    // A chat notification without a usable claim falls back to the item.
    expect(notificationRoute({ donationId: DONATION, claimId: 'x', screen: 'chat' })).toBe(`/donation/${DONATION}`);
  });

  it('opens a need for offers', () => {
    const needId = '1c1d2e3f-4a5b-4c6d-8e7f-0a1b2c3d4e5f';
    expect(notificationRoute({ needId, screen: 'need' })).toBe(`/need/${needId}`);
    expect(notificationRoute({ needId: 'bad', screen: 'need' })).toBeNull();
  });

  it('ignores missing or malformed data', () => {
    expect(notificationRoute(undefined)).toBeNull();
    expect(notificationRoute({ screen: 'pickup' })).toBeNull();
    expect(notificationRoute({ donationId: '../../login', screen: 'item' })).toBeNull();
  });
});

describe('registerDevice', () => {
  it('sends this phone’s token to the server', async () => {
    mockedGetToken.mockResolvedValue('device-token-1');

    await registerDevice();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toMatch(/\/api\/v1\/devices$/);
    expect(init.method).toBe('PUT');
    // The test environment may present itself as either platform.
    expect(JSON.parse(init.body)).toEqual({
      token: 'device-token-1',
      platform: Platform.OS === 'ios' ? 'IOS' : 'ANDROID',
    });
  });

  it('never throws when Firebase cannot give a token', async () => {
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
    mockedGetToken.mockRejectedValue(new Error('SERVICE_NOT_AVAILABLE'));

    await expect(registerDevice()).resolves.toBeUndefined();
    expect(fetchMock).not.toHaveBeenCalled();
    warn.mockRestore();
  });
});

describe('unregisterDevice', () => {
  it('asks the server to forget this phone', async () => {
    mockedGetToken.mockResolvedValue('device-token-1');

    await unregisterDevice();

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toMatch(/\/api\/v1\/devices\/unregister$/);
    expect(init.method).toBe('POST');
    expect(JSON.parse(init.body)).toEqual({ token: 'device-token-1' });
  });

  it('never blocks logging out when the server cannot be reached', async () => {
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});
    mockedGetToken.mockResolvedValue('device-token-1');
    fetchMock.mockRejectedValue(new TypeError('Network request failed'));

    await expect(unregisterDevice()).resolves.toBeUndefined();
    warn.mockRestore();
  });
});
