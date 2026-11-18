import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { type ReactNode, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Alert, Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { NeedForm } from '@/components/need-form';
import {
  Body,
  Button,
  ChoiceChips,
  colors,
  ErrorBanner,
  Field,
  Loading,
  Screen,
  Segmented,
  Title,
} from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useCategories, useCreateDonation } from '@/lib/donations';
import { categoryLabel, conditionLabel } from '@/lib/format';
import { useT } from '@/lib/i18n';
import { useCurrentLocation } from '@/lib/location';
import { pickFromGallery, type PickResult, takePhoto } from '@/lib/photos';
import { useProfile } from '@/lib/profile';
import { donationSchema, type DonationFormValues, type DonationInput, MAX_PHOTOS } from '@/lib/schemas';
import { ITEM_CONDITIONS } from '@/lib/types';

const emptyForm: DonationFormValues = {
  photos: [],
  title: '',
  description: '',
  categoryId: '',
  itemCondition: undefined as unknown as DonationFormValues['itemCondition'],
  city: '',
  area: '',
  pickupAddress: '',
};

/** Donate has two forms: give an item, or ask for something you need. */
export default function DonateScreen() {
  const m = useT();
  const [mode, setMode] = useState<'give' | 'ask'>('give');
  const header = (
    <Segmented
      choices={[
        { value: 'give', label: m.common.giveAnItem },
        { value: 'ask', label: m.common.askForSomething },
      ]}
      value={mode}
      onChange={setMode}
    />
  );
  return mode === 'give' ? <GiveItemForm header={header} /> : <NeedForm header={header} />;
}

/** Post an item with photos. The pickup address stays private until a receiver is chosen. */
function GiveItemForm({ header }: { header: ReactNode }) {
  'use no memo';
  const m = useT();
  const categories = useCategories();
  const location = useCurrentLocation();
  const profile = useProfile(true);
  const create = useCreateDonation();
  const [photoError, setPhotoError] = useState<string | null>(null);
  const [picking, setPicking] = useState(false);
  const {
    control,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<DonationFormValues, unknown, DonationInput>({
    resolver: zodResolver(donationSchema),
    defaultValues: emptyForm,
  });
  const photos = watch('photos');

  const addPhotos = async (pick: () => Promise<PickResult>) => {
    setPhotoError(null);
    setPicking(true);
    try {
      const result = await pick();
      if ('error' in result) {
        setPhotoError(result.error);
      } else if (result.photos.length > 0) {
        setValue('photos', [...photos, ...result.photos].slice(0, MAX_PHOTOS), { shouldValidate: true });
      }
    } catch {
      setPhotoError(m.donate.photoFailed);
    } finally {
      setPicking(false);
    }
  };

  const removePhoto = (uri: string) =>
    setValue(
      'photos',
      photos.filter((photo) => photo.uri !== uri),
      { shouldValidate: true },
    );

  const coords = location.data?.coords ?? null;

  const onSubmit = handleSubmit((input) => {
    if (!coords) {
      return;
    }
    create.mutate(
      { input, location: coords },
      {
        onSuccess: (donation) => {
          reset(emptyForm);
          Alert.alert(m.donate.postedTitle, m.donate.postedMessage);
          router.push(`/donation/${donation.id}`);
        },
      },
    );
  });

  if (categories.isPending || profile.isPending) {
    return <Loading />;
  }

  const serverErrors = create.error instanceof ApiError ? create.error.fieldErrors : [];
  const serverError = (field: string) => serverErrors.find((f) => f.field === field)?.message;
  const categoryChoices = (categories.data ?? []).map((c) => ({ value: c.id, label: categoryLabel(c) }));
  const conditionChoices = ITEM_CONDITIONS.map((value) => ({ value, label: conditionLabel(value) }));

  return (
    <Screen scroll edges={['top', 'left', 'right']}>
      {header}
      <Title>{m.donate.title}</Title>
      <Body>{m.donate.free}</Body>

      <View style={styles.field}>
        <Text style={styles.label}>{m.donate.photos(photos.length, MAX_PHOTOS)}</Text>
        <View style={styles.photoRow}>
          {photos.map((photo, index) => (
            <View key={photo.uri} style={styles.photoBox}>
              <Image source={{ uri: photo.uri }} style={styles.photo} accessibilityLabel={m.donate.photoLabel(index + 1)} />
              {index === 0 ? <Text style={styles.cover}>{m.donate.cover}</Text> : null}
              <Pressable
                onPress={() => removePhoto(photo.uri)}
                style={styles.remove}
                accessibilityRole="button"
                accessibilityLabel={m.donate.removePhoto(index + 1)}
                hitSlop={8}>
                <Text style={styles.removeText}>×</Text>
              </Pressable>
            </View>
          ))}
        </View>
        {photos.length < MAX_PHOTOS ? (
          <View style={styles.photoButtons}>
            <View style={styles.flex}>
              <Button title={m.donate.camera} variant="secondary" loading={picking} onPress={() => addPhotos(takePhoto)} />
            </View>
            <View style={styles.flex}>
              <Button
                title={m.donate.gallery}
                variant="secondary"
                loading={picking}
                onPress={() => addPhotos(() => pickFromGallery(MAX_PHOTOS - photos.length))}
              />
            </View>
          </View>
        ) : null}
        {photoError || errors.photos?.message || serverError('photos') ? (
          <Text style={styles.error}>{photoError ?? errors.photos?.message ?? serverError('photos')}</Text>
        ) : null}
      </View>

      <Controller
        control={control}
        name="title"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={m.donate.titleLabel}
            placeholder={m.donate.titlePlaceholder}
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
            label={m.donate.description}
            placeholder={m.donate.descriptionPlaceholder}
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
        name="itemCondition"
        render={({ field: { onChange, value } }) => (
          <ChoiceChips
            label={m.donate.condition}
            choices={conditionChoices}
            value={value}
            onChange={onChange}
            error={errors.itemCondition?.message ?? serverError('itemCondition')}
          />
        )}
      />
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
            hint={m.donate.areaHint}
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.area?.message ?? serverError('area')}
          />
        )}
      />
      <Controller
        control={control}
        name="pickupAddress"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label={m.donate.address}
            placeholder={m.donate.addressPlaceholder}
            multiline
            maxLength={500}
            hint={m.donate.addressHint}
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.pickupAddress?.message ?? serverError('pickupAddress')}
          />
        )}
      />

      <LocationStatus
        pending={location.isPending || location.isFetching}
        granted={!!coords}
        failed={location.isError}
        onRetry={() => location.refetch()}
      />

      <ErrorBanner message={create.isError && serverErrors.length === 0 ? errorMessage(create.error) : null} />
      <Button title={m.donate.post} onPress={onSubmit} loading={create.isPending} disabled={!coords} />
    </Screen>
  );
}

function LocationStatus({
  pending,
  granted,
  failed,
  onRetry,
}: {
  pending: boolean;
  granted: boolean;
  failed: boolean;
  onRetry: () => void;
}) {
  const m = useT();
  if (pending) {
    return <Body>{m.common.findingLocation}</Body>;
  }
  if (granted) {
    return <Body>{m.donate.locationUsed}</Body>;
  }
  return (
    <View style={styles.locationBox}>
      <Text style={styles.locationText}>
        {failed ? m.donate.locationFailed : m.donate.locationNeeded}
      </Text>
      <Button title={m.common.allowLocation} variant="secondary" onPress={onRetry} />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  field: { gap: 8 },
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
  photoRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  photoBox: { width: 96, height: 96 },
  photo: { width: 96, height: 96, borderRadius: 10, backgroundColor: colors.surface },
  cover: {
    position: 'absolute',
    left: 4,
    bottom: 4,
    backgroundColor: 'rgba(0,0,0,0.6)',
    color: '#FFFFFF',
    fontSize: 11,
    paddingHorizontal: 6,
    borderRadius: 6,
  },
  remove: {
    position: 'absolute',
    top: -6,
    right: -6,
    width: 26,
    height: 26,
    borderRadius: 13,
    backgroundColor: colors.text,
    alignItems: 'center',
    justifyContent: 'center',
  },
  removeText: { color: '#FFFFFF', fontSize: 18, lineHeight: 20 },
  photoButtons: { flexDirection: 'row', gap: 8 },
  error: { fontSize: 14, color: colors.error },
  locationBox: { backgroundColor: colors.warningBackground, borderRadius: 12, padding: 12, gap: 10 },
  locationText: { color: colors.warning, fontSize: 15, lineHeight: 21 },
});
