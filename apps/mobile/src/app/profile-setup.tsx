import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';

import { Body, Button, ErrorBanner, Field, Screen, Title } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useUpdateDisplayName } from '@/lib/profile';

const schema = z.object({
  displayName: z
    .string()
    .trim()
    .min(2, 'Your name must be at least 2 characters')
    .max(80, 'Your name must be at most 80 characters'),
});

type FormValues = z.infer<typeof schema>;

export default function ProfileSetupScreen() {
  'use no memo';
  const update = useUpdateDisplayName();
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: { displayName: '' } });

  const onSubmit = handleSubmit(({ displayName }) => {
    update.mutate(displayName, { onSuccess: () => router.replace('/feed') });
  });

  const serverFieldError =
    update.error instanceof ApiError
      ? update.error.fieldErrors.find((f) => f.field === 'displayName')?.message
      : undefined;

  return (
    <Screen>
      <Title>What should we call you?</Title>
      <Body>Your name is shown on the items you give and the requests you make.</Body>
      <Controller
        control={control}
        name="displayName"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label="Your name"
            placeholder="For example, Asha"
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
      <Button title="Continue" onPress={onSubmit} loading={update.isPending} />
    </Screen>
  );
}
