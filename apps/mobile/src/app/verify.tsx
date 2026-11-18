import { zodResolver } from '@hookform/resolvers/zod';
import { Redirect, router } from 'expo-router';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import type { z } from 'zod';

import { Body, Button, ErrorBanner, Field, Screen, Title } from '@/components/ui';
import { firebaseErrorMessage, useAuthUser } from '@/lib/auth';
import { useT } from '@/lib/i18n';
import { useLoginStore } from '@/lib/login-store';
import { codeSchema } from '@/lib/schemas';

type FormValues = z.infer<typeof codeSchema>;

export default function VerifyScreen() {
  'use no memo';
  const m = useT();
  const { phoneNumber, confirmation, clear } = useLoginStore();
  const { user } = useAuthUser();
  const [error, setError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(codeSchema), defaultValues: { code: '' } });

  // On Android the code can be read automatically, which signs the user in without typing it.
  useEffect(() => {
    if (user) {
      clear();
      router.replace('/');
    }
  }, [user, clear]);

  if (!confirmation) {
    return <Redirect href="/login" />;
  }

  const onSubmit = handleSubmit(async ({ code }) => {
    setError(null);
    try {
      await confirmation.confirm(code);
      clear();
      router.replace('/');
    } catch (e) {
      setError(firebaseErrorMessage(e));
    }
  });

  return (
    <Screen>
      <Title>{m.verify.title}</Title>
      <Body>{m.verify.sentTo(phoneNumber ?? '')}</Body>
      <Controller
        control={control}
        name="code"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={m.common.code}
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
      <Button title={m.verify.verify} onPress={onSubmit} loading={isSubmitting} />
      <Button title={m.verify.otherNumber} variant="secondary" onPress={() => router.back()} />
    </Screen>
  );
}
