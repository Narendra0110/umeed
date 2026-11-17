import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Alert, StyleSheet, Text, View } from 'react-native';
import { z } from 'zod';

import { Body, Button, colors, ErrorBanner, ErrorState, Field, Loading, Screen, Title } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useProfile, useUpdateDisplayName } from '@/lib/profile';
import { useLogOut } from '@/lib/session';

const schema = z.object({
  displayName: z
    .string()
    .trim()
    .min(2, 'Your name must be at least 2 characters')
    .max(80, 'Your name must be at most 80 characters'),
});
type FormValues = z.infer<typeof schema>;

/** Profile: name and phone number, change the name, log out. */
export default function ProfileScreen() {
  const profile = useProfile(true);
  const logOut = useLogOut();

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
        <Button title="Log out" variant="secondary" onPress={logOut} />
      </Screen>
    );
  }

  const confirmLogOut = () =>
    Alert.alert('Log out?', 'You will need your phone number and a new code to log in again.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Log out', style: 'destructive', onPress: logOut },
    ]);

  return (
    <Screen scroll edges={['top', 'left', 'right']}>
      <Title>Profile</Title>
      <NameEditor currentName={profile.data.displayName ?? ''} />
      <View style={styles.info}>
        <Text style={styles.label}>Phone number</Text>
        <Text style={styles.value}>{profile.data.phoneNumber}</Text>
        <Body>Your phone number is shown only to the person you hand an item to, or receive one from.</Body>
      </View>
      <Button title="Log out" variant="secondary" onPress={confirmLogOut} />
    </Screen>
  );
}

function NameEditor({ currentName }: { currentName: string }) {
  'use no memo';
  const [editing, setEditing] = useState(false);
  const update = useUpdateDisplayName();
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { displayName: currentName } });

  if (!editing) {
    return (
      <View style={styles.info}>
        <Text style={styles.label}>Your name</Text>
        <Text style={styles.value}>{currentName}</Text>
        <Button
          title="Change name"
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
            label="Your name"
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
      <Button title="Save" onPress={onSubmit} loading={update.isPending} />
      <Button title="Cancel" variant="secondary" onPress={() => setEditing(false)} />
    </View>
  );
}

const styles = StyleSheet.create({
  info: { gap: 8 },
  label: { fontSize: 14, fontWeight: '600', color: colors.muted },
  value: { fontSize: 20, color: colors.text },
});
