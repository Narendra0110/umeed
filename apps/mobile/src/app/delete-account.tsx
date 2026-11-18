import { zodResolver } from '@hookform/resolvers/zod';
import { getAuth, onIdTokenChanged, signInWithPhoneNumber } from '@react-native-firebase/auth';
import { useQueryClient } from '@tanstack/react-query';
import { router } from 'expo-router';
import { useEffect, useRef, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Alert, StyleSheet, Text, View } from 'react-native';
import { z } from 'zod';

import { Body, Button, colors, ErrorBanner, Field, Screen, Title } from '@/components/ui';
import { deleteAccountEverywhere, signedInSince } from '@/lib/account';
import { errorMessage } from '@/lib/api';
import { firebaseErrorMessage } from '@/lib/auth';

const schema = z.object({
  code: z
    .string()
    .trim()
    .regex(/^[0-9]{6}$/, 'Enter the 6-digit code'),
});
type FormValues = z.infer<typeof schema>;

type Confirmation = Awaited<ReturnType<typeof signInWithPhoneNumber>>;

/**
 * Deletes the account. The person proves it is them with a fresh SMS code, which Firebase also needs
 * before it deletes a sign-in. The server refuses while a pickup is in progress.
 */
export default function DeleteAccountScreen() {
  'use no memo';
  const queryClient = useQueryClient();
  const phoneNumber = getAuth().currentUser?.phoneNumber ?? null;
  const [confirmation, setConfirmation] = useState<Confirmation | null>(null);
  const [sentAt, setSentAt] = useState(0);
  const [sending, setSending] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const started = useRef(false);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { code: '' } });

  const runDeletion = async () => {
    if (started.current) {
      return;
    }
    started.current = true;
    setDeleting(true);
    setError(null);
    try {
      await deleteAccountEverywhere();
      queryClient.clear();
      router.replace('/login');
      Alert.alert('Account deleted', 'Your Umeed account and your details have been deleted.');
    } catch (e) {
      started.current = false;
      setDeleting(false);
      setError(errorMessage(e));
    }
  };

  // On Android the code can be read automatically, which signs the user in again without typing it.
  useEffect(() => {
    if (!confirmation) {
      return undefined;
    }
    return onIdTokenChanged(getAuth(), (user) => {
      if (signedInSince(user, sentAt)) {
        void runDeletion();
      }
    });
    // runDeletion is guarded by a ref, so the listener does not need to follow it.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [confirmation, sentAt]);

  const sendCode = async () => {
    if (!phoneNumber) {
      setError('Please log out and log in again, then try once more.');
      return;
    }
    setError(null);
    setSending(true);
    try {
      const startedAt = Date.now();
      const pending = await signInWithPhoneNumber(getAuth(), phoneNumber);
      setSentAt(startedAt);
      setConfirmation(pending);
    } catch (e) {
      setError(firebaseErrorMessage(e));
    } finally {
      setSending(false);
    }
  };

  const onVerify = handleSubmit(async ({ code }) => {
    if (!confirmation) {
      return;
    }
    setError(null);
    try {
      await confirmation.confirm(code);
    } catch (e) {
      // The code may already have been used by automatic verification; a fresh sign-in is what matters.
      if (!signedInSince(getAuth().currentUser, sentAt)) {
        setError(firebaseErrorMessage(e));
        return;
      }
    }
    await runDeletion();
  });

  return (
    <Screen scroll>
      <Title>Delete your account</Title>
      <Body>This cannot be undone. When your account is deleted:</Body>
      <View style={styles.list}>
        <Text style={styles.point}>• Items you are still giving away are removed, with their photos.</Text>
        <Text style={styles.point}>• Your open claims are withdrawn and your claim messages are erased.</Text>
        <Text style={styles.point}>• Your name, phone number and pickup addresses are erased.</Text>
        <Text style={styles.point}>
          • Items you already handed over stay in the other person’s history as from “Umeed user”.
        </Text>
      </View>
      <Body>If you are in the middle of a pickup, finish or cancel it first.</Body>

      {!confirmation ? (
        <>
          <Body>To confirm it is you, we will send a code to {phoneNumber ?? 'your number'}.</Body>
          <ErrorBanner message={error} />
          <Button title="Send code" variant="danger" onPress={sendCode} loading={sending} />
          <Button title="Keep my account" variant="secondary" onPress={() => router.back()} />
        </>
      ) : (
        <>
          <Controller
            control={control}
            name="code"
            render={({ field: { onChange, onBlur, value } }) => (
              <Field
                label="Code"
                placeholder="123456"
                keyboardType="number-pad"
                autoComplete="sms-otp"
                textContentType="oneTimeCode"
                maxLength={6}
                value={value}
                onChangeText={(text) => onChange(text.replace(/[^0-9]/g, ''))}
                onBlur={onBlur}
                error={errors.code?.message}
              />
            )}
          />
          <ErrorBanner message={error} />
          <Button
            title="Delete my account"
            variant="danger"
            onPress={onVerify}
            loading={isSubmitting || deleting}
          />
          <Button title="Keep my account" variant="secondary" onPress={() => router.back()} />
        </>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  list: { gap: 6 },
  point: { fontSize: 15, lineHeight: 22, color: colors.text },
});
