import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { Alert } from 'react-native';

import { Body, Button, ChoiceChips, ErrorBanner, Field, Screen, Title } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { reportReasonLabel } from '@/lib/format';
import { useT } from '@/lib/i18n';
import { type ReportTarget, useCreateReport } from '@/lib/reports';
import { reportSchema, type ReportFormValues } from '@/lib/schemas';
import { REPORT_REASONS } from '@/lib/types';

/** Report a listing or a need. The owner is never told who reported it. */
export function ReportForm({ target }: { target: ReportTarget }) {
  'use no memo';
  const m = useT();
  const report = useCreateReport(target);
  const thing = target.kind === 'need' ? m.report.need : m.report.listing;
  const owner = target.kind === 'need' ? m.report.asker : m.report.donor;
  const reasonChoices = REPORT_REASONS.map((value) => ({ value, label: reportReasonLabel(value) }));
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
          Alert.alert(m.report.thanksTitle, m.report.thanksMessage(thing));
          router.back();
        },
      },
    ),
  );

  const serverErrors = report.error instanceof ApiError ? report.error.fieldErrors : [];
  const serverError = (field: string) => serverErrors.find((f) => f.field === field)?.message;

  return (
    <Screen scroll>
      <Title>{m.report.title(thing)}</Title>
      <Body>{m.report.ownerNotTold(owner)}</Body>
      <Controller
        control={control}
        name="reason"
        render={({ field: { onChange, value } }) => (
          <ChoiceChips
            label={m.report.reason}
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
            label={reason === 'OTHER' ? m.report.details : m.report.detailsOptional}
            placeholder={m.report.detailsPlaceholder}
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
      <Button title={m.report.send} onPress={onSubmit} loading={report.isPending} />
    </Screen>
  );
}
