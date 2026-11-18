import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import type { ReactNode } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { Body, Button, ChoiceChips, colors, ErrorBanner, Field, Loading, Screen, Title } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useCategories } from '@/lib/donations';
import { useCurrentLocation } from '@/lib/location';
import { useCreateNeed } from '@/lib/needs';
import { needSchema, type NeedFormValues, type NeedInput } from '@/lib/schemas';

const emptyForm: NeedFormValues = { title: '', description: '', categoryId: '', city: '', area: '' };

/** Donate, "Ask for something": post a need. No photos and no address; donors offer their own items. */
export function NeedForm({ header }: { header: ReactNode }) {
  'use no memo';
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
          Alert.alert('Posted', 'People nearby can now offer you an item. You will be notified of each offer.');
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
  const categoryChoices = (categories.data ?? []).map((c) => ({ value: c.id, label: c.name }));

  return (
    <Screen scroll edges={['top', 'left', 'right']}>
      {header}
      <Title>Ask for something</Title>
      <Body>Tell people nearby what you need. Donors can offer one of their items, and you choose.</Body>
      <Controller
        control={control}
        name="title"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label="What do you need?"
            placeholder="For example, School shoes, size 5"
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
            label="Details"
            placeholder="Who it is for, size, anything that helps"
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
        <ErrorBanner message={`Categories could not be loaded. ${errorMessage(categories.error)}`} />
      ) : (
        <Controller
          control={control}
          name="categoryId"
          render={({ field: { onChange, value } }) => (
            <ChoiceChips
              label="Category"
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
            label="City"
            placeholder="For example, Pune"
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
            label="Area"
            placeholder="For example, Kothrud"
            autoCapitalize="words"
            maxLength={100}
            hint="City and area are shown to everyone. No address is needed."
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.area?.message ?? serverError('area')}
          />
        )}
      />
      {location.isPending || location.isFetching ? (
        <Body>Finding your approximate location…</Body>
      ) : coords ? (
        <Body>Your approximate location (about 1 km) is used to show your need to people nearby.</Body>
      ) : (
        <View style={styles.locationBox}>
          <Text style={styles.locationText}>
            Location is needed so that people nearby can see what you need. Only an approximate position (about 1
            km) is saved.
          </Text>
          <Button title="Allow location" variant="secondary" onPress={() => location.refetch()} />
        </View>
      )}
      <ErrorBanner message={create.isError && serverErrors.length === 0 ? errorMessage(create.error) : null} />
      <Button title="Post what I need" onPress={onSubmit} loading={create.isPending} disabled={!coords} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  locationBox: { backgroundColor: colors.warningBackground, borderRadius: 12, padding: 12, gap: 10 },
  locationText: { color: colors.warning, fontSize: 15, lineHeight: 21 },
});
