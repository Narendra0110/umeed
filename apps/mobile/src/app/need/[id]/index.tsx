import { router, useLocalSearchParams } from 'expo-router';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { ListRow } from '@/components/list-row';
import { Body, Button, colors, EmptyState, ErrorState, LinkButton, Loading, Pill, Screen } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { confirmBlock, useBlockUser } from '@/lib/blocks';
import { timeAgo } from '@/lib/format';
import {
  canChoose,
  needStatusLabels,
  offerStatusLabels,
  useAcceptOffer,
  useCloseNeed,
  useNeed,
  useNeedOffers,
} from '@/lib/needs';
import { useProfile } from '@/lib/profile';
import type { Need, Offer } from '@/lib/types';

/**
 * One need. The person who asked sees the offers and chooses one, or closes the need.
 * Everyone else can offer one of their available items.
 */
export default function NeedScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const need = useNeed(id);
  const profile = useProfile(true);
  const isMine = need.data !== undefined && profile.data !== undefined && need.data.requesterId === profile.data.id;
  const offers = useNeedOffers(id, isMine);
  const close = useCloseNeed(id);
  const accept = useAcceptOffer();
  const block = useBlockUser();

  if (need.isPending || profile.isPending) {
    return <Loading />;
  }
  if (need.isError) {
    return (
      <Screen>
        {need.error instanceof ApiError && need.error.status === 404 ? (
          <EmptyState title="This need is not available" message="It may have been closed." />
        ) : (
          <ErrorState message={errorMessage(need.error)} onRetry={() => need.refetch()} retrying={need.isFetching} />
        )}
      </Screen>
    );
  }

  const item = need.data;
  const open = item.status === 'OPEN';

  const confirmClose = () =>
    Alert.alert('Close this need?', 'People will no longer see it, and offers still waiting will be declined.', [
      { text: 'Keep it open', style: 'cancel' },
      {
        text: 'Close',
        style: 'destructive',
        onPress: () => close.mutate(undefined, { onError: (error) => Alert.alert('Could not close', errorMessage(error)) }),
      },
    ]);

  const confirmChoose = (offer: Offer) =>
    Alert.alert(
      `Choose “${offer.donationTitle}” from ${offer.donorName}?`,
      'The item is reserved for you. You will both see each other’s phone number and you will see the pickup address. Your need will be marked fulfilled and other offers declined.',
      [
        { text: 'Not yet', style: 'cancel' },
        {
          text: 'Choose',
          onPress: () =>
            accept.mutate(offer.id, {
              onSuccess: (result) => router.replace(`/donation/${result.donationId}/pickup`),
              onError: (error) => Alert.alert('Could not choose this offer', errorMessage(error)),
            }),
        },
      ],
    );

  return (
    <Screen scroll>
      <Text style={styles.title} accessibilityRole="header">
        {item.title}
      </Text>
      <View style={styles.row}>
        <Pill label={needStatusLabels[item.status]} tone={open ? 'success' : 'neutral'} />
        {isMine ? <Pill label="Your need" tone="warning" /> : null}
      </View>
      <Text style={styles.meta}>
        {item.area}, {item.city} · asked by {item.requesterName} · {timeAgo(item.createdAt)}
      </Text>
      <Text style={styles.description}>{item.description}</Text>

      {isMine ? (
        <MyNeedOffers
          need={item}
          offers={offers.data}
          loading={offers.isPending}
          error={offers.isError ? errorMessage(offers.error) : null}
          onRetry={() => offers.refetch()}
          choosing={accept.isPending ? accept.variables : undefined}
          onChoose={confirmChoose}
        />
      ) : open ? (
        <View style={styles.actions}>
          <Button title="Offer an item" onPress={() => router.push(`/need/${item.id}/offer`)} />
          <Body>You can offer one of your listed items. If you have not posted it yet, post it first under Donate.</Body>
        </View>
      ) : (
        <Body>This need is no longer open.</Body>
      )}

      {isMine && open ? (
        <View style={styles.actions}>
          <Button title="Close this need" variant="danger" onPress={confirmClose} loading={close.isPending} />
        </View>
      ) : null}
      {!isMine ? (
        <LinkButton
          title={`Block ${item.requesterName}`}
          color={colors.muted}
          onPress={() =>
            confirmBlock(item.requesterName, (handlers) => block.mutate(item.requesterId, handlers), () => router.back())
          }
        />
      ) : null}
    </Screen>
  );
}

type MyNeedOffersProps = {
  need: Need;
  offers: Offer[] | undefined;
  loading: boolean;
  error: string | null;
  onRetry: () => void;
  choosing: string | undefined;
  onChoose: (offer: Offer) => void;
};

function MyNeedOffers({ need, offers, loading, error, onRetry, choosing, onChoose }: MyNeedOffersProps) {
  if (loading) {
    return <Loading />;
  }
  if (error) {
    return <ErrorState message={error} onRetry={onRetry} />;
  }
  if (!offers || offers.length === 0) {
    return (
      <Body>
        {need.status === 'OPEN'
          ? 'No offers yet. You will get a notification when someone offers an item.'
          : 'There were no offers.'}
      </Body>
    );
  }
  return (
    <View style={styles.offers}>
      <Text style={styles.section}>Offers</Text>
      {offers.map((offer) => (
        <ListRow
          key={offer.id}
          title={`${offer.donationTitle} · from ${offer.donorName}`}
          subtitle={`${offer.message ? `“${offer.message}” · ` : ''}${timeAgo(offer.createdAt)}${
            offer.status === 'PENDING' && offer.donationStatus !== 'AVAILABLE' ? '\nThis item is no longer available.' : ''
          }`}
          imagePath={offer.donationImageUrl ?? undefined}
          extra={
            <Pill
              label={offerStatusLabels[offer.status]}
              tone={offer.status === 'ACCEPTED' ? 'success' : offer.status === 'PENDING' ? 'info' : 'neutral'}
            />
          }
          onPress={offer.donationStatus !== 'REMOVED' ? () => router.push(`/donation/${offer.donationId}`) : undefined}
          actions={
            canChoose(offer, need) ? (
              <View style={styles.flex}>
                <Button
                  title="Choose this"
                  loading={choosing === offer.id}
                  disabled={choosing !== undefined}
                  onPress={() => onChoose(offer)}
                />
              </View>
            ) : offer.status === 'ACCEPTED' ? (
              <View style={styles.flex}>
                <Button
                  title="Pickup details"
                  variant="secondary"
                  onPress={() => router.push(`/donation/${offer.donationId}/pickup`)}
                />
              </View>
            ) : undefined
          }
        />
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  title: { fontSize: 24, fontWeight: '700', color: colors.text },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  meta: { fontSize: 15, color: colors.muted },
  description: { fontSize: 16, lineHeight: 23, color: colors.text },
  actions: { gap: 12, marginTop: 8 },
  offers: { gap: 10, marginTop: 8 },
  section: { fontSize: 15, fontWeight: '700', color: colors.muted, textTransform: 'uppercase' },
  flex: { flex: 1 },
});
