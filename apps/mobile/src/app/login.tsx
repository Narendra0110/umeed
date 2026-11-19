import { zodResolver } from '@hookform/resolvers/zod';
import { getAuth, signInWithPhoneNumber } from '@react-native-firebase/auth';
import { router } from 'expo-router';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import type { z } from 'zod';

import { Body, Brand, Button, ErrorBanner, Field, LanguageSwitch, Screen, Title } from '@/components/ui';
import { firebaseErrorMessage } from '@/lib/auth';
import { useT } from '@/lib/i18n';
import { useLoginStore } from '@/lib/login-store';
import { phoneSchema } from '@/lib/schemas';

type FormValues = z.infer<typeof phoneSchema>;

/** Login with a mobile number. The language chosen here is saved to the account after login. */
export default function LoginScreen() {
  'use no memo';
  const m = useT();
  const setPending = useLoginStore((state) => state.setPending);
  const [error, setError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(phoneSchema), defaultValues: { phone: '' } });

  const onSubmit = handleSubmit(async ({ phone }) => {
    setError(null);
    const phoneNumber = `+91${phone}`;
    try {
      const confirmation = await signInWithPhoneNumber(getAuth(), phoneNumber);
      setPending(phoneNumber, confirmation);
      router.push('/verify');
    } catch (e) {
      setError(firebaseErrorMessage(e));
    }
  });

  return (
    <Screen scroll>
      <LanguageSwitch />
      <Brand />
      <Title>{m.login.title}</Title>
      <Body>{m.login.intro}</Body>
      <Controller
        control={control}
        name="phone"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={m.login.mobileNumber}
            prefix="+91"
            placeholder="98765 43210"
            keyboardType="phone-pad"
            autoComplete="tel"
            textContentType="telephoneNumber"
            maxLength={10}
            value={value}
            onChangeText={(text) => onChange(text.replace(/[^0-9]/g, ''))}
            onBlur={onBlur}
            error={errors.phone?.message}
          />
        )}
      />
      <ErrorBanner message={error} />
      <Button title={m.common.sendCode} onPress={onSubmit} loading={isSubmitting} />
      <Body>{m.login.privacy}</Body>
    </Screen>
  );
}
