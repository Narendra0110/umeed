import { getAuth, signOut } from '@react-native-firebase/auth';
import { useQueryClient } from '@tanstack/react-query';
import { Redirect } from 'expo-router';
import { useEffect } from 'react';

import { Body, Button, ErrorBanner, LanguageSwitch, Loading, Screen, Title } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { useAuthUser } from '@/lib/auth';
import { useT } from '@/lib/i18n';
import { syncLanguage, useProfile, useUpdateLanguage } from '@/lib/profile';

/** Decides where the user starts: login, profile setup or home. Also brings the app and account languages together. */
export default function Index() {
  const m = useT();
  const { initializing, user } = useAuthUser();
  const profile = useProfile(!!user);
  const queryClient = useQueryClient();
  const updateLanguage = useUpdateLanguage();
  const { mutate: saveLanguage } = updateLanguage;

  useEffect(() => {
    if (profile.data) {
      syncLanguage(profile.data, (language) => saveLanguage(language));
    }
    // Once per loaded account, not on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [profile.data?.id, profile.data?.language, saveLanguage]);

  const logOut = async () => {
    await signOut(getAuth());
    queryClient.clear();
  };

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
        <Title>{m.start.couldNotLoad}</Title>
        <ErrorBanner message={errorMessage(profile.error)} />
        <Button title={m.common.tryAgain} onPress={() => profile.refetch()} loading={profile.isFetching} />
        <Button title={m.common.logOut} variant="secondary" onPress={logOut} />
        <Body>{m.start.serverHint}</Body>
      </Screen>
    );
  }
  if (profile.data.status === 'SUSPENDED') {
    return (
      <Screen>
        <Title>{m.start.suspendedTitle}</Title>
        <Body>{m.errors.suspended}</Body>
        <Button title={m.start.checkAgain} onPress={() => profile.refetch()} loading={profile.isFetching} />
        <Button title={m.common.logOut} variant="secondary" onPress={logOut} />
        <LanguageSwitch onChange={(language) => saveLanguage(language)} />
      </Screen>
    );
  }
  return <Redirect href={profile.data.profileComplete ? '/feed' : '/profile-setup'} />;
}
