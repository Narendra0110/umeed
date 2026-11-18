import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { Alert } from 'react-native';

import { Body, Button, ChoiceChips, ErrorBanner, Field, Screen, Title } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { reportReasonLabels } from '@/lib/format';
import { type ReportTarget, useCreateReport } from '@/lib/reports';
import { reportSchema, type ReportFormValues } from '@/lib/schemas';
import { REPORT_REASONS } from '@/lib/types';

const reasonChoices = REPORT_REASONS.map((value) => ({ value, label: reportReasonLabels[value] }));

const words = {
  donation: { thing: 'listing', owner: 'donor' },
  need: { thing: 'need', owner: 'person who asked' },
} as const;

/** Report a listing or a need. The owner is never told who reported it. */
export function ReportForm({ target }: { target: ReportTarget }) {
  'use no memo';
  const report = useCreateReport(target);
  const { thing, owner } = words[target.kind];
  const {
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ReportFormValues>({
    resolver: zodResolver(reportSchema),
    defaultValues: { reason: undefined as unknown as ReportFormValues['reason'], details: '' },
  });
  const reason = watch('reason');

  const onSubmit = handleSubmit(({ reason: chosen, details }) =>
    report.mutate(
      { reason: chosen, details },
      {
        onSuccess: () => {
          Alert.alert('Thank you', `We will review this ${thing}.`);
          router.back();
        },
      },
    ),
  );

  const serverErrors = report.error instanceof ApiError ? report.error.fieldErrors : [];
  const serverError = (field: string) => serverErrors.find((f) => f.field === field)?.message;

  return (
    <Screen scroll>
      <Title>{`What is wrong with this ${thing}?`}</Title>
      <Body>{`The ${owner} will not be told who reported it.`}</Body>
      <Controller
        control={control}
        name="reason"
        render={({ field: { onChange, value } }) => (
          <ChoiceChips
            label="Reason"
            choices={reasonChoices}
            value={value}
            onChange={onChange}
            error={errors.reason?.message ?? serverError('reason')}
          />
        )}
      />
      <Controller
        control={control}
        name="details"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={reason === 'OTHER' ? 'Details' : 'Details (optional)'}
            placeholder="Anything that helps us understand the problem"
            multiline
            maxLength={500}
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.details?.message ?? serverError('details')}
          />
        )}
      />
      <ErrorBanner message={report.isError && serverErrors.length === 0 ? errorMessage(report.error) : null} />
      <Button title="Send report" onPress={onSubmit} loading={report.isPending} />
    </Screen>
  );
}
