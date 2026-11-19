import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import {
  Baloo2_400Regular,
  Baloo2_500Medium,
  Baloo2_600SemiBold,
  Baloo2_700Bold,
  Baloo2_800ExtraBold,
  useFonts,
} from '@expo-google-fonts/baloo-2';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect, useState } from 'react';

import { Celebration } from '@/components/celebration';
import { PushBanner } from '@/components/push-banner';
import { colors, font } from '@/components/theme';
import { ApiError } from '@/lib/api';
import { useT } from '@/lib/i18n';

function shouldRetry(failureCount: number, error: unknown) {
  // Do not retry answers that will not change, such as 401, 403 or 404.
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return false;
  }
  return failureCount < 2;
}

// Keep the splash screen up until the fonts are ready, so text never jumps from one font to another.
void SplashScreen.preventAutoHideAsync().catch(() => undefined);

export default function RootLayout() {
  const s = useT().screens;
  const [queryClient] = useState(
    () => new QueryClient({ defaultOptions: { queries: { retry: shouldRetry } } }),
  );
  const [fontsLoaded, fontError] = useFonts({
    Baloo2_400Regular,
    Baloo2_500Medium,
    Baloo2_600SemiBold,
    Baloo2_700Bold,
    Baloo2_800ExtraBold,
  });
  const ready = fontsLoaded || !!fontError;

  useEffect(() => {
    if (ready) {
      void SplashScreen.hideAsync().catch(() => undefined);
    }
  }, [ready]);

  if (!ready) {
    return null;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <StatusBar style="dark" />
      <Stack
        screenOptions={{
          headerShadowVisible: false,
          headerStyle: { backgroundColor: colors.background },
          headerTintColor: colors.primary,
          headerTitleStyle: { fontFamily: font.bold, color: colors.text },
          contentStyle: { backgroundColor: colors.background },
        }}>
        <Stack.Screen name="index" options={{ headerShown: false }} />
        <Stack.Screen name="welcome" options={{ headerShown: false }} />
        <Stack.Screen name="login" options={{ title: s.login }} />
        <Stack.Screen name="verify" options={{ title: s.verify }} />
        <Stack.Screen name="profile-setup" options={{ title: s.profileSetup, headerBackVisible: false }} />
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="donation/[id]/index" options={{ title: s.item }} />
        <Stack.Screen name="donation/[id]/claims" options={{ title: s.claims }} />
        <Stack.Screen name="donation/[id]/pickup" options={{ title: s.pickup }} />
        <Stack.Screen name="donation/[id]/report" options={{ title: s.reportListing, presentation: 'modal' }} />
        <Stack.Screen name="explore" options={{ title: s.search }} />
        <Stack.Screen name="chat/[claimId]" options={{ title: s.chat }} />
        <Stack.Screen name="need/[id]/index" options={{ title: s.need }} />
        <Stack.Screen name="need/[id]/offer" options={{ title: s.offer, presentation: 'modal' }} />
        <Stack.Screen name="need/[id]/report" options={{ title: s.reportNeed, presentation: 'modal' }} />
        <Stack.Screen name="blocked" options={{ title: s.blocked }} />
        <Stack.Screen name="delete-account" options={{ title: s.deleteAccount }} />
      </Stack>
      <PushBanner />
      <Celebration />
    </QueryClientProvider>
  );
}
