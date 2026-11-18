import { zodResolver } from '@hookform/resolvers/zod';
import { router, useLocalSearchParams } from 'expo-router';
import { Controller, useForm } from 'react-hook-form';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';

import { AuthImage } from '@/components/photos';
import { Body, Button, colors, EmptyState, ErrorBanner, ErrorState, Field, Loading, Screen, Title } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { useMyDonations } from '@/lib/donations';
import { offerableItems, useMakeOffer, useNeed } from '@/lib/needs';
import { offerSchema, type OfferFormValues } from '@/lib/schemas';

/** Offer one of my available items for a need, with an optional message. */
export default function OfferScreen() {
  'use no memo';
  const { id } = useLocalSearchParams<{ id: string }>();
  const need = useNeed(id);
  const mine = useMyDonations();
  const offer = useMakeOffer(id);
  const {
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<OfferFormValues>({ resolver: zodResolver(offerSchema), defaultValues: { donationId: '', message: '' } });

  if (need.isPending || mine.isPending) {
    return <Loading />;
  }
  if (need.isError || mine.isError) {
    const error = need.error ?? mine.error;
    return (
      <Screen>
        <ErrorState
          message={errorMessage(error)}
          onRetry={() => {
            need.refetch();
            mine.refetch();
          }}
        />
      </Screen>
    );
  }

  const items = offerableItems(mine.data.pages.flatMap((page) => page.items));
  if (items.length === 0) {
    return (
      <Screen>
        <EmptyState
          title="You have no items to offer"
          message="Post the item under Donate first, then come back to offer it."
          action={<Button title="Give an item" onPress={() => router.navigate('/donate')} />}
        />
      </Screen>
    );
  }

  const onSubmit = handleSubmit((values) =>
    offer.mutate(values, {
      onSuccess: () => {
        Alert.alert('Offer sent', `${need.data.requesterName} will be told about your offer.`);
        router.back();
      },
    }),
  );

  return (
    <Screen scroll>
      <Title>Offer for “{need.data.title}”</Title>
      <Body>Choose one of your items. It stays listed for others until {need.data.requesterName} chooses.</Body>
      <Controller
        control={control}
        name="donationId"
        render={({ field: { onChange, value } }) => (
          <View style={styles.items} accessibilityRole="radiogroup">
            {items.map((item) => {
              const selected = value === item.id;
              return (
                <Pressable
                  key={item.id}
                  onPress={() => onChange(item.id)}
                  accessibilityRole="radio"
                  accessibilityState={{ selected }}
                  style={[styles.item, selected ? styles.itemSelected : null]}>
                  {item.imageUrls[0] ? <AuthImage path={item.imageUrls[0]} style={styles.thumb} /> : null}
                  <Text style={styles.itemTitle} numberOfLines={2}>
                    {item.title}
                  </Text>
                </Pressable>
              );
            })}
            {errors.donationId?.message ? <Text style={styles.error}>{errors.donationId.message}</Text> : null}
          </View>
        )}
      />
      <Controller
        control={control}
        name="message"
        render={({ field: { onChange, onBlur, value } }) => (
          <Field
            label="Message (optional)"
            placeholder="For example, it is size 5 and barely used"
            multiline
            maxLength={300}
            value={value}
            onChangeText={onChange}
            onBlur={onBlur}
            error={errors.message?.message}
          />
        )}
      />
      <ErrorBanner message={offer.isError ? errorMessage(offer.error) : null} />
      <Button title="Send offer" onPress={onSubmit} loading={offer.isPending} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  items: { gap: 8 },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 10,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,
  },
  itemSelected: { borderColor: colors.primary, borderWidth: 2, backgroundColor: colors.primaryLight },
  thumb: { width: 56, height: 56, borderRadius: 8 },
  itemTitle: { flex: 1, fontSize: 16, color: colors.text },
  error: { fontSize: 14, color: colors.error },
});
