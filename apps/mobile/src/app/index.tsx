import { getAuth, signOut } from '@react-native-firebase/auth';
import { useQueryClient } from '@tanstack/react-query';
import { Redirect } from 'expo-router';

import { Body, Button, ErrorBanner, Loading, Screen, Title } from '@/components/ui';
import { errorMessage, SUSPENDED_MESSAGE } from '@/lib/api';
import { useAuthUser } from '@/lib/auth';
import { useProfile } from '@/lib/profile';

/** Decides where the user starts: login, profile setup or home. */
export default function Index() {
  const { initializing, user } = useAuthUser();
  const profile = useProfile(!!user);
  const queryClient = useQueryClient();

  if (initializing) {
    return <Loading />;
  }
  if (!user) {
    return <Redirect href="/login" />;
  }
  if (profile.isPending) {
    return <Loading />;
  }
  if (profile.isError) {
    return (
      <Screen>
        <Title>Could not load your account</Title>
        <ErrorBanner message={errorMessage(profile.error)} />
        <Button title="Try again" onPress={() => profile.refetch()} loading={profile.isFetching} />
        <Button
          title="Log out"
          variant="secondary"
          onPress={async () => {
            await signOut(getAuth());
            queryClient.clear();
          }}
        />
        <Body>If this keeps happening, check that the Umeed server is running.</Body>
      </Screen>
    );
  }
  if (profile.data.status === 'SUSPENDED') {
    return (
      <Screen>
        <Title>Account suspended</Title>
        <Body>{SUSPENDED_MESSAGE}</Body>
        <Button title="Check again" onPress={() => profile.refetch()} loading={profile.isFetching} />
        <Button
          title="Log out"
          variant="secondary"
          onPress={async () => {
            await signOut(getAuth());
            queryClient.clear();
          }}
        />
      </Screen>
    );
  }
  return <Redirect href={profile.data.profileComplete ? '/feed' : '/profile-setup'} />;
}
