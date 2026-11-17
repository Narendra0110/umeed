import { zodResolver } from '@hookform/resolvers/zod';
import { router } from 'expo-router';
import { useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Alert, Image, Pressable, StyleSheet, Text, View } from 'react-native';

import { Body, Button, ChoiceChips, colors, ErrorBanner, Field, Loading, Screen, Title } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useCategories, useCreateDonation } from '@/lib/donations';
import { conditionLabels } from '@/lib/format';
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

const conditionChoices = ITEM_CONDITIONS.map((value) => ({ value, label: conditionLabels[value] }));

/** Donate: post an item with photos. The pickup address stays private until a receiver is chosen. */
export default function DonateScreen() {
  'use no memo';
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
      setPhotoError('Could not add that photo. Please try another one.');
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
          Alert.alert('Posted', 'Your item is now in the feed. You will see claims under Activity.');
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
  const categoryChoices = (categories.data ?? []).map((c) => ({ value: c.id, label: c.name }));

  return (
    <Screen scroll edges={['top', 'left', 'right']}>
      <Title>Give an item</Title>
      <Body>Umeed is free. Never ask for or accept money for an item.</Body>

      <View style={styles.field}>
        <Text style={styles.label}>
          Photos ({photos.length}/{MAX_PHOTOS})
        </Text>
        <View style={styles.photoRow}>
          {photos.map((photo, index) => (
            <View key={photo.uri} style={styles.photoBox}>
              <Image source={{ uri: photo.uri }} style={styles.photo} accessibilityLabel={`Photo ${index + 1}`} />
              {index === 0 ? <Text style={styles.cover}>Cover</Text> : null}
              <Pressable
                onPress={() => removePhoto(photo.uri)}
                style={styles.remove}
                accessibilityRole="button"
                accessibilityLabel={`Remove photo ${index + 1}`}
                hitSlop={8}>
                <Text style={styles.removeText}>×</Text>
              </Pressable>
            </View>
          ))}
        </View>
        {photos.length < MAX_PHOTOS ? (
          <View style={styles.photoButtons}>
            <View style={styles.flex}>
              <Button title="Camera" variant="secondary" loading={picking} onPress={() => addPhotos(takePhoto)} />
            </View>
            <View style={styles.flex}>
              <Button
                title="Gallery"
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
            label="Title"
            placeholder="For example, School bag, good condition"
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
            label="Description"
            placeholder="Size, age, anything missing or damaged"
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
        name="itemCondition"
        render={({ field: { onChange, value } }) => (
          <ChoiceChips
            label="Condition"
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
            hint="City and area are shown to everyone."
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
            label="Pickup address"
            placeholder="House number, street, landmark"
            multiline
            maxLength={500}
            hint="Private. Shown only to the person you choose."
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
      <Button title="Post item" onPress={onSubmit} loading={create.isPending} disabled={!coords} />
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
  if (pending) {
    return <Body>Finding your approximate location…</Body>;
  }
  if (granted) {
    return <Body>Your approximate location (about 1 km) is used to show the item to people nearby.</Body>;
  }
  return (
    <View style={styles.locationBox}>
      <Text style={styles.locationText}>
        {failed
          ? 'Your location could not be found.'
          : 'Location is needed to post, so that people nearby can find the item. Only an approximate position (about 1 km) is saved.'}
      </Text>
      <Button title="Allow location" variant="secondary" onPress={onRetry} />
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
