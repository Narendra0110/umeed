import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Alert, StyleSheet, Text, View } from 'react-native';
import type { z } from 'zod';

import {
  Body,
  Button,
  colors,
  ErrorBanner,
  ErrorState,
  Field,
  LanguageSwitch,
  LinkButton,
  Loading,
  Screen,
  Title,
} from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useClearSkips } from '@/lib/explore';
import { t, useT } from '@/lib/i18n';
import { useProfile, useUpdateDisplayName, useUpdateLanguage } from '@/lib/profile';
import { displayNameSchema } from '@/lib/schemas';
import { useLogOut } from '@/lib/session';

type FormValues = z.infer<typeof displayNameSchema>;

/** Profile: name and phone number, language, change the name, log out. */
export default function ProfileScreen() {
  const m = useT();
  const profile = useProfile(true);
  const logOut = useLogOut();
  const clearSkips = useClearSkips();
  const updateLanguage = useUpdateLanguage();

  if (profile.isPending) {
    return <Loading />;
  }
  if (profile.isError) {
    return (
      <Screen edges={['top', 'left', 'right']}>
        <ErrorState
          message={errorMessage(profile.error)}
          onRetry={() => profile.refetch()}
          retrying={profile.isFetching}
        />
        <Button title={m.common.logOut} variant="secondary" onPress={logOut} />
      </Screen>
    );
  }

  const confirmLogOut = () =>
    Alert.alert(m.profile.logOutTitle, m.profile.logOutMessage, [
      { text: m.common.cancel, style: 'cancel' },
      { text: m.common.logOut, style: 'destructive', onPress: logOut },
    ]);

  return (
    <Screen scroll edges={['top', 'left', 'right']}>
      <Title>{m.profile.title}</Title>
      <NameEditor currentName={profile.data.displayName ?? ''} />
      <View style={styles.info}>
        <Text style={styles.label}>{m.profile.phone}</Text>
        <Text style={styles.value}>{profile.data.phoneNumber}</Text>
        <Body>{m.profile.phoneNote}</Body>
      </View>
      <LanguageSwitch
        onChange={(language) =>
          updateLanguage.mutate(language, {
            onError: (error) => Alert.alert(t().language.couldNotSave, errorMessage(error)),
          })
        }
      />
      <Button title={m.profile.blocked} variant="secondary" onPress={() => router.push('/blocked')} />
      <Button
        title={m.profile.showHidden}
        variant="secondary"
        loading={clearSkips.isPending}
        onPress={() =>
          Alert.alert(m.profile.showHiddenTitle, m.profile.showHiddenMessage, [
            { text: m.common.cancel, style: 'cancel' },
            {
              text: m.profile.showThem,
              onPress: () =>
                clearSkips.mutate(undefined, {
                  onSuccess: () => Alert.alert(t().profile.done, t().profile.doneMessage),
                  onError: (error) => Alert.alert(t().profile.couldNotDo, errorMessage(error)),
                }),
            },
          ])
        }
      />
      <Button title={m.common.logOut} variant="secondary" onPress={confirmLogOut} />
      <View style={styles.danger}>
        <LinkButton title={m.profile.deleteAccount} color={colors.error} onPress={() => router.push('/delete-account')} />
      </View>
    </Screen>
  );
}

function NameEditor({ currentName }: { currentName: string }) {
  'use no memo';
  const m = useT();
  const [editing, setEditing] = useState(false);
  const update = useUpdateDisplayName();
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(displayNameSchema), defaultValues: { displayName: currentName } });

  if (!editing) {
    return (
      <View style={styles.info}>
        <Text style={styles.label}>{m.common.yourName}</Text>
        <Text style={styles.value}>{currentName}</Text>
        <Button
          title={m.profile.changeName}
          variant="secondary"
          onPress={() => {
            reset({ displayName: currentName });
            update.reset();
            setEditing(true);
          }}
        />
      </View>
    );
  }

  const serverFieldError =
    update.error instanceof ApiError
      ? update.error.fieldErrors.find((f) => f.field === 'displayName')?.message
      : undefined;

  const onSubmit = handleSubmit(({ displayName }) =>
    update.mutate(displayName, { onSuccess: () => setEditing(false) }),
  );

  return (
    <View style={styles.info}>
      <Controller
        control={control}
        name="displayName"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={m.common.yourName}
            autoCapitalize="words"
            autoComplete="name"
            maxLength={80}
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.displayName?.message ?? serverFieldError}
          />
        )}
      />
      <ErrorBanner message={update.isError && !serverFieldError ? errorMessage(update.error) : null} />
      <Button title={m.profile.save} onPress={onSubmit} loading={update.isPending} />
      <Button title={m.common.cancel} variant="secondary" onPress={() => setEditing(false)} />
    </View>
  );
}

const styles = StyleSheet.create({
  info: { gap: 8 },
  label: { fontSize: 14, fontWeight: '600', color: colors.muted },
  value: { fontSize: 20, color: colors.text },
  danger: { marginTop: 16, alignItems: 'center' },
});
