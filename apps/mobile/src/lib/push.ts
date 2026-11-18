import {
  getInitialNotification,
  getMessaging,
  getToken,
  onMessage,
  onNotificationOpenedApp,
  onTokenRefresh,
} from '@react-native-firebase/messaging';
import { useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import { useEffect } from 'react';
import { PermissionsAndroid, Platform } from 'react-native';
import { create } from 'zustand';

import { apiFetch } from './api';

type NotificationData = Record<string, unknown> | undefined;

/** Where a tapped notification should take the user. The backend sends donationId and screen. */
export function notificationRoute(data: NotificationData): string | null {
  const isId = (value: unknown): value is string => typeof value === 'string' && /^[0-9a-f-]{36}$/i.test(value);
  if (data?.screen === 'chat' && isId(data.claimId)) {
    return `/chat/${data.claimId}`;
  }
  if (data?.screen === 'need' && isId(data.needId)) {
    return `/need/${data.needId}`;
  }
  const donationId = isId(data?.donationId) ? data.donationId : null;
  if (!donationId) {
    return null;
  }
  switch (data?.screen) {
    case 'claims':
      return `/donation/${donationId}/claims`;
    case 'pickup':
      return `/donation/${donationId}/pickup`;
    default:
      return `/donation/${donationId}`;
  }
}

/** Android 13 and later ask once before showing notifications. Earlier versions allow them by default. */
async function askForPermission(): Promise<void> {
  if (Platform.OS === 'android' && Number(Platform.Version) >= 33) {
    await PermissionsAndroid.request(PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS);
  }
}

async function sendToken(token: string): Promise<void> {
  await apiFetch<void>('/api/v1/devices', {
    method: 'PUT',
    body: { token, platform: Platform.OS === 'ios' ? 'IOS' : 'ANDROID' },
  });
}

/** Tells the server this phone belongs to the logged-in user. Problems are logged, never shown. */
export async function registerDevice(): Promise<void> {
  try {
    await askForPermission();
    const token = await getToken(getMessaging());
    if (token) {
      await sendToken(token);
    }
  } catch (cause) {
    if (__DEV__) {
      console.warn('Could not register for notifications', cause);
    }
  }
}

/** Called just before logging out, so the next person on this phone does not get our notifications. */
export async function unregisterDevice(): Promise<void> {
  try {
    const token = await getToken(getMessaging());
    if (token) {
      await apiFetch<void>('/api/v1/devices/unregister', { method: 'POST', body: { token } });
    }
  } catch (cause) {
    if (__DEV__) {
      console.warn('Could not unregister this phone', cause);
    }
  }
}

type Banner = { title: string; body: string; route: string | null };

type BannerState = {
  banner: Banner | null;
  show: (banner: Banner) => void;
  hide: () => void;
};

/** The notification shown at the top while the app is open. */
export const usePushBanner = create<BannerState>()((set) => ({
  banner: null,
  show: (banner) => set({ banner }),
  hide: () => set({ banner: null }),
}));

/**
 * For logged-in screens: registers this phone, keeps the token up to date, shows a banner for
 * notifications that arrive while the app is open, and opens the right screen when one is tapped.
 */
export function usePushNotifications() {
  const queryClient = useQueryClient();
  const showBanner = usePushBanner((state) => state.show);

  useEffect(() => {
    const messaging = getMessaging();
    void registerDevice();

    const stopTokenRefresh = onTokenRefresh(messaging, (token) => {
      sendToken(token).catch(() => undefined);
    });

    const stopForeground = onMessage(messaging, (message) => {
      // Something changed on the server: refresh what the screens show.
      void queryClient.invalidateQueries({ queryKey: ['claims'] });
      void queryClient.invalidateQueries({ queryKey: ['donations'] });
      void queryClient.invalidateQueries({ queryKey: ['pickup'] });
      void queryClient.invalidateQueries({ queryKey: ['chat'] });
      void queryClient.invalidateQueries({ queryKey: ['needs'] });
      showBanner({
        title: message.notification?.title ?? 'Umeed',
        body: message.notification?.body ?? '',
        route: notificationRoute(message.data),
      });
    });

    const open = (data: NotificationData) => {
      const route = notificationRoute(data);
      if (route) {
        router.push(route as never);
      }
    };
    const stopOpened = onNotificationOpenedApp(messaging, (message) => open(message.data));
    getInitialNotification(messaging)
      .then((message) => open(message?.data))
      .catch(() => undefined);

    return () => {
      stopTokenRefresh();
      stopForeground();
      stopOpened();
    };
  }, [queryClient, showBanner]);
}
