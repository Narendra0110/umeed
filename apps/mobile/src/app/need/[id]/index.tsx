import { router, useLocalSearchParams } from 'expo-router';
import { Alert, StyleSheet, Text, View } from 'react-native';

import { font } from '@/components/theme';
import { ListRow } from '@/components/list-row';
import { Body, Button, colors, EmptyState, ErrorState, LinkButton, Loading, Pill, Screen } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { confirmBlock, useBlockUser } from '@/lib/blocks';
import { timeAgo } from '@/lib/format';
import { useT } from '@/lib/i18n';
import {
  canChoose,
  needStatusLabel,
  offerStatusLabel,
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
  const m = useT();
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
          <EmptyState title={m.need.notAvailableTitle} message={m.need.notAvailableMessage} />
        ) : (
          <ErrorState message={errorMessage(need.error)} onRetry={() => need.refetch()} retrying={need.isFetching} />
        )}
      </Screen>
    );
  }

  const item = need.data;
  const open = item.status === 'OPEN';

  const confirmClose = () =>
    Alert.alert(m.need.closeTitle, m.need.closeMessage, [
      { text: m.need.keepOpen, style: 'cancel' },
      {
        text: m.need.close,
        style: 'destructive',
        onPress: () => close.mutate(undefined, { onError: (error) => Alert.alert(m.need.couldNotClose, errorMessage(error)) }),
      },
    ]);

  const confirmChoose = (offer: Offer) =>
    Alert.alert(m.need.chooseTitle(offer.donationTitle, offer.donorName), m.need.chooseMessage, [
      { text: m.common.notYet, style: 'cancel' },
      {
        text: m.need.choose,
        onPress: () =>
          accept.mutate(offer.id, {
            onSuccess: (result) => router.replace(`/donation/${result.donationId}/pickup`),
            onError: (error) => Alert.alert(m.need.couldNotChoose, errorMessage(error)),
          }),
      },
    ]);

  return (
    <Screen scroll>
      <Text style={styles.title} accessibilityRole="header">
        {item.title}
      </Text>
      <View style={styles.row}>
        <Pill label={needStatusLabel(item.status)} tone={open ? 'success' : 'neutral'} />
        {isMine ? <Pill label={m.need.yourNeed} tone="warning" /> : null}
      </View>
      <Text style={styles.meta}>{m.need.askedBy(item.area, item.city, item.requesterName, timeAgo(item.createdAt))}</Text>
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
          <Button title={m.need.offerItem} onPress={() => router.push(`/need/${item.id}/offer`)} />
          <Body>{m.need.offerHint}</Body>
        </View>
      ) : (
        <Body>{m.need.noLongerOpen}</Body>
      )}

      {isMine && open ? (
        <View style={styles.actions}>
          <Button title={m.need.closeNeed} variant="danger" onPress={confirmClose} loading={close.isPending} />
        </View>
      ) : null}
      {!isMine ? (
        <View style={styles.links}>
          <LinkButton
            title={m.common.report}
            color={colors.muted}
            onPress={() => router.push(`/need/${item.id}/report`)}
          />
          <LinkButton
            title={m.common.block(item.requesterName)}
            color={colors.muted}
            onPress={() =>
              confirmBlock(item.requesterName, (handlers) => block.mutate(item.requesterId, handlers), () => router.back())
            }
          />
        </View>
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
  const m = useT();
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
          ? m.need.noOffersYet
          : m.need.noOffers}
      </Body>
    );
  }
  return (
    <View style={styles.offers}>
      <Text style={styles.section}>{m.need.offers}</Text>
      {offers.map((offer) => (
        <ListRow
          key={offer.id}
          title={m.need.offerRow(offer.donationTitle, offer.donorName)}
          subtitle={`${offer.message ? `“${offer.message}” · ` : ''}${timeAgo(offer.createdAt)}${
            offer.status === 'PENDING' && offer.donationStatus !== 'AVAILABLE' ? `\n${m.need.unavailable}` : ''
          }`}
          imagePath={offer.donationImageUrl ?? undefined}
          extra={
            <Pill
              label={offerStatusLabel(offer.status)}
              tone={offer.status === 'ACCEPTED' ? 'success' : offer.status === 'PENDING' ? 'info' : 'neutral'}
            />
          }
          onPress={offer.donationStatus !== 'REMOVED' ? () => router.push(`/donation/${offer.donationId}`) : undefined}
          actions={
            canChoose(offer, need) ? (
              <View style={styles.flex}>
                <Button
                  title={m.need.chooseThis}
                  loading={choosing === offer.id}
                  disabled={choosing !== undefined}
                  onPress={() => onChoose(offer)}
                />
              </View>
            ) : offer.status === 'ACCEPTED' ? (
              <View style={styles.flex}>
                <Button
                  title={m.common.pickupDetails}
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
  title: { fontSize: 24, fontFamily: font.bold, color: colors.text },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  meta: { fontSize: 15, color: colors.muted, fontFamily: font.regular },
  description: { fontSize: 16, lineHeight: 23, color: colors.text, fontFamily: font.regular },
  actions: { gap: 12, marginTop: 8 },
  offers: { gap: 10, marginTop: 8 },
  section: { fontSize: 15, fontFamily: font.bold, color: colors.muted, textTransform: 'uppercase' },
  flex: { flex: 1 },
  links: { flexDirection: 'row', flexWrap: 'wrap', gap: 20, marginTop: 8 },
});
