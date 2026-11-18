import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import type { ReactNode } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { Body, Button, ChoiceChips, colors, ErrorBanner, Field, Loading, Screen, Title } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useCategories } from '@/lib/donations';
import { useCurrentLocation } from '@/lib/location';
import { useT } from '@/lib/i18n';
import { useCreateNeed } from '@/lib/needs';
import { categoryLabel } from '@/lib/format';
import { needSchema, type NeedFormValues, type NeedInput } from '@/lib/schemas';

const emptyForm: NeedFormValues = { title: '', description: '', categoryId: '', city: '', area: '' };

/** Donate, "Ask for something": post a need. No photos and no address; donors offer their own items. */
export function NeedForm({ header }: { header: ReactNode }) {
  'use no memo';
  const m = useT();
  const categories = useCategories();
  const location = useCurrentLocation();
  const create = useCreateNeed();
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<NeedFormValues, unknown, NeedInput>({ resolver: zodResolver(needSchema), defaultValues: emptyForm });

  const coords = location.data?.coords ?? null;

  const onSubmit = handleSubmit((input) => {
    if (!coords) {
      return;
    }
    create.mutate(
      { input, location: coords },
      {
        onSuccess: (need) => {
          reset(emptyForm);
          Alert.alert(m.donate.postedTitle, m.needForm.postedMessage);
          router.push(`/need/${need.id}`);
        },
      },
    );
  });

  if (categories.isPending) {
    return <Loading />;
  }

  const serverErrors = create.error instanceof ApiError ? create.error.fieldErrors : [];
  const serverError = (field: string) => serverErrors.find((f) => f.field === field)?.message;
  const categoryChoices = (categories.data ?? []).map((c) => ({ value: c.id, label: categoryLabel(c) }));

  return (
    <Screen scroll edges={['top', 'left', 'right']}>
      {header}
      <Title>{m.common.askForSomething}</Title>
      <Body>{m.needForm.intro}</Body>
      <Controller
        control={control}
        name="title"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={m.needForm.what}
            placeholder={m.needForm.whatPlaceholder}
            maxLength={100}
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.title?.message ?? serverError('title')}
          />
        )}
      />
      <Controller
        control={control}
        name="description"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={m.needForm.details}
            placeholder={m.needForm.detailsPlaceholder}
            multiline
            maxLength={1000}
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.description?.message ?? serverError('description')}
          />
        )}
      />
      {categories.isError ? (
        <ErrorBanner message={m.common.categoriesFailed(errorMessage(categories.error))} />
      ) : (
        <Controller
          control={control}
          name="categoryId"
          render={({ field: { onChange, value } }) => (
            <ChoiceChips
              label={m.common.category}
              choices={categoryChoices}
              value={value || undefined}
              onChange={onChange}
              error={errors.categoryId?.message ?? serverError('categoryId')}
            />
          )}
        />
      )}
      <Controller
        control={control}
        name="city"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={m.common.city}
            placeholder={m.common.cityPlaceholder}
            autoCapitalize="words"
            maxLength={100}
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.city?.message ?? serverError('city')}
          />
        )}
      />
      <Controller
        control={control}
        name="area"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={m.common.area}
            placeholder={m.common.areaPlaceholder}
            autoCapitalize="words"
            maxLength={100}
            hint={m.needForm.areaHint}
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.area?.message ?? serverError('area')}
          />
        )}
      />
      {location.isPending || location.isFetching ? (
        <Body>{m.common.findingLocation}</Body>
      ) : coords ? (
        <Body>{m.needForm.locationUsed}</Body>
      ) : (
        <View style={styles.locationBox}>
          <Text style={styles.locationText}>{m.needForm.locationNeeded}</Text>
          <Button title={m.common.allowLocation} variant="secondary" onPress={() => location.refetch()} />
        </View>
      )}
      <ErrorBanner message={create.isError && serverErrors.length === 0 ? errorMessage(create.error) : null} />
      <Button title={m.needForm.post} onPress={onSubmit} loading={create.isPending} disabled={!coords} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  locationBox: { backgroundColor: colors.warningBackground, borderRadius: 12, padding: 12, gap: 10 },
  locationText: { color: colors.warning, fontSize: 15, lineHeight: 21 },
});
