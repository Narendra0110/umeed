import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import type { z } from 'zod';

import { Body, Button, ErrorBanner, Field, Screen, Title } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { useUpdateDisplayName } from '@/lib/profile';
import { displayNameSchema } from '@/lib/schemas';

type FormValues = z.infer<typeof displayNameSchema>;

export default function ProfileSetupScreen() {
  'use no memo';
  const m = useT();
  const update = useUpdateDisplayName();
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(displayNameSchema), defaultValues: { displayName: '' } });

  const onSubmit = handleSubmit(({ displayName }) => {
    update.mutate(displayName, { onSuccess: () => router.replace('/feed') });
  });

  const serverFieldError =
    update.error instanceof ApiError
      ? update.error.fieldErrors.find((f) => f.field === 'displayName')?.message
      : undefined;

  return (
    <Screen>
      <Title>{m.profileSetup.title}</Title>
      <Body>{m.profileSetup.intro}</Body>
      <Controller
        control={control}
        name="displayName"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={m.common.yourName}
            placeholder={m.profileSetup.placeholder}
            autoCapitalize="words"
            autoComplete="name"
            textContentType="name"
            maxLength={80}
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.displayName?.message ?? serverFieldError}
          />
        )}
      />
      <ErrorBanner message={update.isError && !serverFieldError ? errorMessage(update.error) : null} />
      <Button title={m.common.continue} onPress={onSubmit} loading={update.isPending} />
    </Screen>
  );
}
