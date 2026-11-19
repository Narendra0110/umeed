import { LinearGradient } from 'expo-linear-gradient';
import { router, useLocalSearchParams } from 'expo-router';
import { useState } from 'react';
import { Alert, StyleSheet, Text, useWindowDimensions, View } from 'react-native';

import { font } from '@/components/theme';
import { ClaimSheet } from '@/components/claim-sheet';
import { PhotoCarousel } from '@/components/photos';
import { Body, Button, colors, EmptyState, ErrorState, LinkButton, Loading, Pill, Screen } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { confirmBlock, useBlockUser } from '@/lib/blocks';
import { celebrate } from '@/lib/delight';
import { useDonation, useRemoveDonation } from '@/lib/donations';
import { conditionLabel, distanceKm, donationStatusLabel, formatDistance, timeAgo } from '@/lib/format';
import { useT } from '@/lib/i18n';
import { useCurrentLocation } from '@/lib/location';
import { usePickup } from '@/lib/pickup';
import { useProfile } from '@/lib/profile';

/**
 * One donated item. The donor sees their own actions; the accepted receiver is sent to the pickup
 * details; everyone else can claim (while available) or report.
 */
export default function DonationScreen() {
  const m = useT();
  const { id } = useLocalSearchParams<{ id: string }>();
  const donation = useDonation(id);
  const profile = useProfile(true);
  const location = useCurrentLocation();
  const remove = useRemoveDonation();
  const block = useBlockUser();
  const { width } = useWindowDimensions();
  const [claimOpen, setClaimOpen] = useState(false);
  const [claimed, setClaimed] = useState(false);
  // Once an item is taken, only the donor and the accepted receiver can read its pickup details.
  // For anyone else who is not the donor, that answer tells whether the item was reserved for them.
  const takenByOther =
    donation.data !== undefined &&
    profile.data !== undefined &&
    donation.data.donorId !== profile.data.id &&
    (donation.data.status === 'RESERVED' || donation.data.status === 'COMPLETED');
  const pickup = usePickup(id, { enabled: takenByOther, probe: true });
  const isReceiver = takenByOther && pickup.isSuccess;

  if (donation.isPending || profile.isPending) {
    return <Loading />;
  }
  if (donation.isError) {
    if (donation.error instanceof ApiError && donation.error.status === 404) {
      return (
        <Screen>
          <EmptyState emoji="🫥" title={m.item.goneTitle} message={m.item.goneMessage} />
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
    Alert.alert(m.item.removeTitle, m.item.removeMessage, [
      { text: m.item.keepListing, style: 'cancel' },
      {
        text: m.item.remove,
        style: 'destructive',
        onPress: () =>
          remove.mutate(item.id, {
            onSuccess: () => router.back(),
            onError: (error) => Alert.alert(m.item.couldNotRemove, errorMessage(error)),
          }),
      },
    ]);

  return (
    <Screen scroll>
      <View style={styles.bleed}>
        <PhotoCarousel paths={item.imageUrls} width={width} height={Math.round(width * 0.85)} title={item.title} />
        <LinearGradient colors={['transparent', 'rgba(20,12,4,0.78)']} style={styles.fade} pointerEvents="none">
          <Text style={styles.title} accessibilityRole="header">
            {item.title}
          </Text>
        </LinearGradient>
      </View>
      <View style={styles.row}>
        <Pill label={conditionLabel(item.itemCondition)} tone="info" />
        {item.status !== 'AVAILABLE' ? <Pill label={donationStatusLabel(item.status)} tone="warning" /> : null}
        {isMine ? <Pill label={m.item.yourItem} tone="warning" /> : null}
      </View>
      <Text style={styles.meta}>
        {item.area}, {item.city}
        {coords ? ` · ${formatDistance(distanceKm(coords, item))}` : ''}
      </Text>
      <Text style={styles.meta}>{m.item.givenByAgo(item.donorName, timeAgo(item.createdAt))}</Text>
      <Text style={styles.description}>{item.description}</Text>

      {isMine ? (
        <View style={styles.actions}>
          {item.status === 'AVAILABLE' ? (
            <>
              <Button title={m.item.seeClaims} onPress={() => router.push(`/donation/${item.id}/claims`)} />
              <Button title={m.item.removeListing} variant="danger" onPress={confirmRemove} loading={remove.isPending} />
            </>
          ) : (
            <Button title={m.common.pickupDetails} onPress={() => router.push(`/donation/${item.id}/pickup`)} />
          )}
        </View>
      ) : (
        <View style={styles.actions}>
          {isReceiver ? (
            <>
              <Body>
                {item.status === 'COMPLETED'
                  ? m.item.youReceived
                  : m.item.reservedForYou}
              </Body>
              <Button title={m.common.pickupDetails} onPress={() => router.push(`/donation/${item.id}/pickup`)} />
            </>
          ) : item.status !== 'AVAILABLE' ? (
            takenByOther && pickup.isPending ? null : (
              <Body>
                {item.status === 'COMPLETED'
                  ? m.item.alreadyGiven
                  : m.item.reservedForOther}
              </Body>
            )
          ) : claimed ? (
            <View style={styles.claimedBox}>
              <Text style={styles.claimedText}>{m.item.claimedBox}</Text>
            </View>
          ) : (
            <Button title={m.feed.claim} onPress={() => setClaimOpen(true)} />
          )}
          <LinkButton
            title={m.item.reportListing}
            color={colors.muted}
            onPress={() => router.push(`/donation/${item.id}/report`)}
          />
          <LinkButton
            title={m.common.block(item.donorName)}
            color={colors.muted}
            onPress={() =>
              confirmBlock(
                item.donorName,
                (handlers) => block.mutate(item.donorId, handlers),
                () => router.back(),
              )
            }
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
            celebrate(m.celebrate.claimSent);
          }}
        />
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  bleed: { marginHorizontal: -24, marginTop: -24 },
  fade: { position: 'absolute', left: 0, right: 0, bottom: 0, paddingHorizontal: 24, paddingTop: 56, paddingBottom: 16 },
  title: { fontSize: 26, lineHeight: 34, fontFamily: font.bold, color: '#FFFFFF' },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  meta: { fontSize: 15, color: colors.muted, fontFamily: font.regular },
  description: { fontSize: 16, lineHeight: 23, color: colors.text, fontFamily: font.regular },
  actions: { gap: 12, marginTop: 8 },
  claimedBox: {
    minHeight: 52,
    borderRadius: 12,
    backgroundColor: colors.successBackground,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  claimedText: { color: colors.success, fontSize: 15, fontFamily: font.semibold, textAlign: 'center' },
});
