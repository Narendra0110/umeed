import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';

import { PushBanner } from '@/components/push-banner';
import { ApiError } from '@/lib/api';
import { useT } from '@/lib/i18n';

function shouldRetry(failureCount: number, error: unknown) {
  // Do not retry answers that will not change, such as 401, 403 or 404.
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return false;
  }
  return failureCount < 2;
}

export default function RootLayout() {
  const s = useT().screens;
  const [queryClient] = useState(
    () => new QueryClient({ defaultOptions: { queries: { retry: shouldRetry } } }),
  );

  return (
    <QueryClientProvider client={queryClient}>
      <StatusBar style="dark" />
      <Stack screenOptions={{ headerShadowVisible: false }}>
        <Stack.Screen name="index" options={{ headerShown: false }} />
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
    </QueryClientProvider>
  );
}
