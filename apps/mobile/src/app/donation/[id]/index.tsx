import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Alert, StyleSheet, Text, useWindowDimensions, View } from 'react-native';

import { ClaimSheet } from '@/components/claim-sheet';
import { PhotoCarousel } from '@/components/photos';
import { Body, Button, colors, EmptyState, ErrorState, LinkButton, Loading, Pill, Screen } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useDonation, useRemoveDonation } from '@/lib/donations';
import { conditionLabels, distanceKm, donationStatusLabels, formatDistance, timeAgo } from '@/lib/format';
import { useCurrentLocation } from '@/lib/location';
import { useProfile } from '@/lib/profile';

/** One donated item. The donor sees their own actions; everyone else can claim or report. */
export default function DonationScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const donation = useDonation(id);
  const profile = useProfile(true);
  const location = useCurrentLocation();
  const remove = useRemoveDonation();
  const { width } = useWindowDimensions();
  const [claimOpen, setClaimOpen] = useState(false);
  const [claimed, setClaimed] = useState(false);

  if (donation.isPending || profile.isPending) {
    return <Loading />;
  }
  if (donation.isError) {
    if (donation.error instanceof ApiError && donation.error.status === 404) {
      return (
        <Screen>
          <EmptyState title="This item is no longer listed" message="The donor may have removed it." />
        </Screen>
      );
    }
    return (
      <Screen>
        <ErrorState
          message={errorMessage(donation.error)}
          onRetry={() => donation.refetch()}
          retrying={donation.isFetching}
        />
      </Screen>
    );
  }

  const item = donation.data;
  const isMine = item.donorId === profile.data?.id;
  const coords = location.data?.coords;

  const confirmRemove = () =>
    Alert.alert(
      'Remove this listing?',
      'It will leave the feed and any waiting claims will be declined. This cannot be undone.',
      [
        { text: 'Keep listing', style: 'cancel' },
        {
          text: 'Remove',
          style: 'destructive',
          onPress: () =>
            remove.mutate(item.id, {
              onSuccess: () => router.back(),
              onError: (error) => Alert.alert('Could not remove', errorMessage(error)),
            }),
        },
      ],
    );

  return (
    <Screen scroll>
      <View style={styles.bleed}>
        <PhotoCarousel paths={item.imageUrls} width={width} height={Math.round(width * 0.85)} title={item.title} />
      </View>
      <Text style={styles.title} accessibilityRole="header">
        {item.title}
      </Text>
      <View style={styles.row}>
        <Pill label={conditionLabels[item.itemCondition]} tone="info" />
        {item.status !== 'AVAILABLE' ? <Pill label={donationStatusLabels[item.status]} tone="warning" /> : null}
        {isMine ? <Pill label="Your item" tone="warning" /> : null}
      </View>
      <Text style={styles.meta}>
        {item.area}, {item.city}
        {coords ? ` · ${formatDistance(distanceKm(coords, item))}` : ''}
      </Text>
      <Text style={styles.meta}>
        Given by {item.donorName} · {timeAgo(item.createdAt)}
      </Text>
      <Text style={styles.description}>{item.description}</Text>

      {isMine ? (
        <View style={styles.actions}>
          {item.status === 'AVAILABLE' ? (
            <>
              <Button title="See claims" onPress={() => router.push(`/donation/${item.id}/claims`)} />
              <Button title="Remove listing" variant="danger" onPress={confirmRemove} loading={remove.isPending} />
            </>
          ) : (
            <Button title="Pickup details" onPress={() => router.push(`/donation/${item.id}/pickup`)} />
          )}
        </View>
      ) : (
        <View style={styles.actions}>
          {item.status !== 'AVAILABLE' ? (
            <Body>This item has already been reserved for someone.</Body>
          ) : claimed ? (
            <View style={styles.claimedBox}>
              <Text style={styles.claimedText}>Claimed. You will find it under Activity.</Text>
            </View>
          ) : (
            <Button title="Claim" onPress={() => setClaimOpen(true)} />
          )}
          <LinkButton
            title="Report this listing"
            color={colors.muted}
            onPress={() => router.push(`/donation/${item.id}/report`)}
          />
        </View>
      )}

      {claimOpen ? (
        <ClaimSheet
          visible
          donationId={item.id}
          donationTitle={item.title}
          donorName={item.donorName}
          onClose={() => setClaimOpen(false)}
          onClaimed={() => {
            setClaimOpen(false);
            setClaimed(true);
          }}
        />
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  bleed: { marginHorizontal: -24, marginTop: -24 },
  title: { fontSize: 24, fontWeight: '700', color: colors.text },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  meta: { fontSize: 15, color: colors.muted },
  description: { fontSize: 16, lineHeight: 23, color: colors.text },
  actions: { gap: 12, marginTop: 8 },
  claimedBox: {
    minHeight: 52,
    borderRadius: 12,
    backgroundColor: colors.successBackground,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  claimedText: { color: colors.success, fontSize: 15, fontWeight: '600', textAlign: 'center' },
});
