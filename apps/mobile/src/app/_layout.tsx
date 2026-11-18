import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useState } from 'react';

import { ApiError } from '@/lib/api';

function shouldRetry(failureCount: number, error: unknown) {
  // Do not retry answers that will not change, such as 401, 403 or 404.
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
    return false;
  }
  return failureCount < 2;
}

export default function RootLayout() {
  const [queryClient] = useState(
    () => new QueryClient({ defaultOptions: { queries: { retry: shouldRetry } } }),
  );

  return (
    <QueryClientProvider client={queryClient}>
      <StatusBar style="dark" />
      <Stack screenOptions={{ headerShadowVisible: false }}>
        <Stack.Screen name="index" options={{ headerShown: false }} />
        <Stack.Screen name="login" options={{ title: 'Log in' }} />
        <Stack.Screen name="verify" options={{ title: 'Enter code' }} />
        <Stack.Screen name="profile-setup" options={{ title: 'Your name', headerBackVisible: false }} />
        <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
        <Stack.Screen name="donation/[id]/index" options={{ title: 'Item' }} />
        <Stack.Screen name="donation/[id]/claims" options={{ title: 'Claims' }} />
        <Stack.Screen name="donation/[id]/pickup" options={{ title: 'Pickup' }} />
        <Stack.Screen name="donation/[id]/report" options={{ title: 'Report listing', presentation: 'modal' }} />
        <Stack.Screen name="blocked" options={{ title: 'Blocked people' }} />
        <Stack.Screen name="delete-account" options={{ title: 'Delete account' }} />
      </Stack>
    </QueryClientProvider>
  );
}
