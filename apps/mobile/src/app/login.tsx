import { zodResolver } from '@hookform/resolvers/zod';
import { getAuth, signInWithPhoneNumber } from '@react-native-firebase/auth';
import { router } from 'expo-router';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';

import { Body, Button, ErrorBanner, Field, Screen, Title } from '@/components/ui';
import { firebaseErrorMessage } from '@/lib/auth';
import { useLoginStore } from '@/lib/login-store';

const schema = z.object({
  phone: z
    .string()
    .trim()
    .regex(/^[6-9][0-9]{9}$/, 'Enter your 10-digit mobile number'),
});

type FormValues = z.infer<typeof schema>;

export default function LoginScreen() {
  'use no memo';
  const setPending = useLoginStore((state) => state.setPending);
  const [error, setError] = useState<string | null>(null);
  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { phone: '' } });

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
    <Screen>
      <Title>Welcome to Umeed</Title>
      <Body>Give what you no longer need. Find what you do. Log in with your mobile number.</Body>
      <Controller
        control={control}
        name="phone"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label="Mobile number"
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
      <Button title="Send code" onPress={onSubmit} loading={isSubmitting} />
      <Body>
        We send a one-time code to this number. Your number is shared with another person only after you
        accept their claim, or they accept yours, so you can arrange the pickup.
      </Body>
    </Screen>
  );
}
