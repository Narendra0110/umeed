import { router } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, Alert, RefreshControl, SectionList, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ListRow, SectionTitle } from '@/components/list-row';
import { ListSkeleton } from '@/components/skeleton';
import { Button, colors, EmptyState, ErrorState, Pill, Segmented, Title } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { useMyClaims, useWithdrawClaim } from '@/lib/claims';
import { useMyDonations } from '@/lib/donations';
import { claimStatusLabel, donationStatusLabel, groupMyClaims, groupMyDonations, timeAgo } from '@/lib/format';
import { useT } from '@/lib/i18n';
import { needStatusLabel, offerStatusLabel, useMyNeeds, useMyOffers, useWithdrawOffer } from '@/lib/needs';
import type { Claim, ClaimStatus, Donation, DonationStatus, Need, Offer } from '@/lib/types';

type Tab = 'donations' | 'claims' | 'needs';

/** Activity: the items I gave, the items I claimed, and my needs and offers, grouped by what needs doing next. */
export default function ActivityScreen() {
  const m = useT();
  const [tab, setTab] = useState<Tab>('donations');

  return (
    <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
      <View style={styles.header}>
        <Title>{m.activity.title}</Title>
        <Segmented
          choices={[
            { value: 'donations', label: m.activity.myDonations },
            { value: 'claims', label: m.activity.myClaims },
            { value: 'needs', label: m.activity.needs },
          ]}
          value={tab}
          onChange={setTab}
        />
      </View>
      {tab === 'donations' ? <MyDonations /> : tab === 'claims' ? <MyClaims /> : <MyNeedsAndOffers />}
    </SafeAreaView>
  );
}

const donationTone: Record<DonationStatus, 'success' | 'warning' | 'neutral'> = {
  AVAILABLE: 'success',
  RESERVED: 'warning',
  COMPLETED: 'neutral',
  REMOVED: 'neutral',
};

function MyDonations() {
  const m = useT();
  const donations = useMyDonations();

  if (donations.isPending) {
    return <ListSkeleton />;
  }
  if (donations.isError) {
    return (
      <ErrorState
        message={errorMessage(donations.error)}
        onRetry={() => donations.refetch()}
        retrying={donations.isFetching}
      />
    );
  }
  const items = donations.data.pages.flatMap((page) => page.items);
  if (items.length === 0) {
    return (
      <EmptyState
        emoji="📦"
        title={m.activity.noDonationsTitle}
        message={m.activity.noDonationsMessage}
        action={<Button title={m.common.giveAnItem} onPress={() => router.navigate('/donate')} />}
      />
    );
  }

  // The item screen has every donor action: See claims and Remove listing while available,
  // Pickup details once reserved or given away. Own items are never on Home, so this is the way in.
  const open = (donation: Donation) => router.push(`/donation/${donation.id}`);

  return (
    <SectionList
      sections={groupMyDonations(items)}
      keyExtractor={(item) => item.id}
      contentContainerStyle={styles.list}
      stickySectionHeadersEnabled={false}
      refreshControl={<RefreshControl refreshing={donations.isRefetching} onRefresh={() => void donations.refetch()} />}
      onEndReached={() => donations.hasNextPage && !donations.isFetchingNextPage && donations.fetchNextPage()}
      ListFooterComponent={donations.isFetchingNextPage ? <ActivityIndicator color={colors.primary} /> : undefined}
      renderSectionHeader={({ section }) => <SectionTitle>{section.title}</SectionTitle>}
      renderItem={({ item }) => (
        <ListRow
          title={item.title}
          subtitle={`${item.area}, ${item.city} · ${timeAgo(item.createdAt)}`}
          imagePath={item.imageUrls[0]}
          extra={<Pill label={donationStatusLabel(item.status)} tone={donationTone[item.status]} />}
          onPress={() => open(item)}
        />
      )}
    />
  );
}

const claimTone: Record<ClaimStatus, 'success' | 'info' | 'neutral'> = {
  ACCEPTED: 'success',
  PENDING: 'info',
  REJECTED: 'neutral',
  WITHDRAWN: 'neutral',
};

function MyClaims() {
  const m = useT();
  const claims = useMyClaims();
  const withdraw = useWithdrawClaim();

  if (claims.isPending) {
    return <ListSkeleton />;
  }
  if (claims.isError) {
    return (
      <ErrorState message={errorMessage(claims.error)} onRetry={() => claims.refetch()} retrying={claims.isFetching} />
    );
  }
  const items = claims.data.pages.flatMap((page) => page.items);
  if (items.length === 0) {
    return (
      <EmptyState
        emoji="🙌"
        title={m.activity.noClaimsTitle}
        message={m.activity.noClaimsMessage}
        action={<Button title={m.activity.browse} onPress={() => router.navigate('/feed')} />}
      />
    );
  }

  const confirmWithdraw = (claim: Claim) =>
    Alert.alert(m.activity.withdrawClaimTitle, m.activity.withdrawClaimMessage(claim.donationTitle), [
      { text: m.activity.keepClaim, style: 'cancel' },
      {
        text: m.common.withdraw,
        style: 'destructive',
        onPress: () =>
          withdraw.mutate(claim.id, {
            onError: (error) => Alert.alert(m.common.couldNotWithdraw, errorMessage(error)),
          }),
      },
    ]);

  return (
    <SectionList
      sections={groupMyClaims(items)}
      keyExtractor={(item) => item.id}
      contentContainerStyle={styles.list}
      stickySectionHeadersEnabled={false}
      refreshControl={<RefreshControl refreshing={claims.isRefetching} onRefresh={() => void claims.refetch()} />}
      onEndReached={() => claims.hasNextPage && !claims.isFetchingNextPage && claims.fetchNextPage()}
      ListFooterComponent={claims.isFetchingNextPage ? <ActivityIndicator color={colors.primary} /> : undefined}
      renderSectionHeader={({ section }) => <SectionTitle>{section.title}</SectionTitle>}
      renderItem={({ item }) => {
        // A claim stays ACCEPTED after the handover; the item's status shows it was received.
        const received = item.status === 'ACCEPTED' && item.donationStatus === 'COMPLETED';
        return (
        <ListRow
          title={item.donationTitle}
          subtitle={item.message ? m.activity.yourMessage(item.message) : m.activity.claimedAgo(timeAgo(item.createdAt))}
          extra={
            received ? (
              <Pill label={m.activity.received} tone="success" />
            ) : (
              <Pill label={claimStatusLabel(item.status)} tone={claimTone[item.status]} />
            )
          }
          onPress={
            received || item.status === 'PENDING'
              ? () => router.push(`/donation/${item.donationId}`)
              : item.status === 'ACCEPTED'
                ? () => router.push(`/donation/${item.donationId}/pickup`)
                : undefined
          }
          actions={
            received ? undefined : item.status === 'PENDING' ? (
              <View style={styles.flex}>
                <Button
                  title={m.activity.withdrawClaim}
                  variant="secondary"
                  loading={withdraw.isPending && withdraw.variables === item.id}
                  onPress={() => confirmWithdraw(item)}
                />
              </View>
            ) : item.status === 'ACCEPTED' ? (
              <View style={styles.flex}>
                <Button title={m.common.pickupDetails} onPress={() => router.push(`/donation/${item.donationId}/pickup`)} />
              </View>
            ) : undefined
          }
        />
        );
      }}
    />
  );
}

type NeedsRow = { kind: 'need'; need: Need } | { kind: 'offer'; offer: Offer };

/** What I asked for (my needs) and what I offered for other people's needs. */
function MyNeedsAndOffers() {
  const m = useT();
  const needs = useMyNeeds();
  const offers = useMyOffers();
  const withdraw = useWithdrawOffer();

  if (needs.isPending || offers.isPending) {
    return <ListSkeleton />;
  }
  if (needs.isError || offers.isError) {
    return (
      <ErrorState
        message={errorMessage(needs.error ?? offers.error)}
        onRetry={() => {
          needs.refetch();
          offers.refetch();
        }}
        retrying={needs.isFetching || offers.isFetching}
      />
    );
  }
  const myNeeds = needs.data.pages.flatMap((page) => page.items);
  const myOffers = offers.data.pages.flatMap((page) => page.items);
  if (myNeeds.length === 0 && myOffers.length === 0) {
    return (
      <EmptyState
        emoji="🙏"
        title={m.activity.noNeedsTitle}
        message={m.activity.noNeedsMessage}
        action={<Button title={m.common.askForSomething} onPress={() => router.navigate('/donate')} />}
      />
    );
  }

  const sections: { title: string; data: NeedsRow[] }[] = [];
  if (myNeeds.length > 0) {
    sections.push({ title: m.activity.askedFor, data: myNeeds.map((need) => ({ kind: 'need', need })) });
  }
  if (myOffers.length > 0) {
    sections.push({ title: m.activity.offered, data: myOffers.map((offer) => ({ kind: 'offer', offer })) });
  }

  const confirmWithdraw = (offer: Offer) =>
    Alert.alert(m.activity.withdrawOfferTitle, m.activity.withdrawOfferMessage(offer.donationTitle, offer.needTitle), [
      { text: m.activity.keepOffer, style: 'cancel' },
      {
        text: m.common.withdraw,
        style: 'destructive',
        onPress: () =>
          withdraw.mutate(offer.id, { onError: (error) => Alert.alert(m.common.couldNotWithdraw, errorMessage(error)) }),
      },
    ]);

  return (
    <SectionList
      sections={sections}
      keyExtractor={(row) => (row.kind === 'need' ? `need-${row.need.id}` : `offer-${row.offer.id}`)}
      contentContainerStyle={styles.list}
      stickySectionHeadersEnabled={false}
      refreshControl={
        <RefreshControl
          refreshing={needs.isRefetching || offers.isRefetching}
          onRefresh={() => {
            void needs.refetch();
            void offers.refetch();
          }}
        />
      }
      renderSectionHeader={({ section }) => <SectionTitle>{section.title}</SectionTitle>}
      renderItem={({ item: row }) =>
        row.kind === 'need' ? (
          <ListRow
            title={row.need.title}
            subtitle={`${row.need.area}, ${row.need.city} · ${timeAgo(row.need.createdAt)}`}
            extra={
              <Pill
                label={
                  row.need.status === 'OPEN' && row.need.pendingOfferCount
                    ? m.activity.offersWaiting(row.need.pendingOfferCount)
                    : needStatusLabel(row.need.status)
                }
                tone={row.need.status === 'OPEN' ? (row.need.pendingOfferCount ? 'info' : 'success') : 'neutral'}
              />
            }
            onPress={() => router.push(`/need/${row.need.id}`)}
          />
        ) : (
          <ListRow
            title={m.activity.offerFor(row.offer.donationTitle, row.offer.needTitle)}
            subtitle={timeAgo(row.offer.createdAt)}
            imagePath={row.offer.donationImageUrl ?? undefined}
            extra={
              <Pill
                label={offerStatusLabel(row.offer.status)}
                tone={row.offer.status === 'ACCEPTED' ? 'success' : row.offer.status === 'PENDING' ? 'info' : 'neutral'}
              />
            }
            onPress={() => router.push(`/need/${row.offer.needId}`)}
            actions={
              row.offer.status === 'PENDING' ? (
                <View style={styles.flex}>
                  <Button
                    title={m.activity.withdrawOffer}
                    variant="secondary"
                    loading={withdraw.isPending && withdraw.variables === row.offer.id}
                    onPress={() => confirmWithdraw(row.offer)}
                  />
                </View>
              ) : row.offer.status === 'ACCEPTED' ? (
                <View style={styles.flex}>
                  <Button
                    title={m.common.pickupDetails}
                    onPress={() => router.push(`/donation/${row.offer.donationId}/pickup`)}
                  />
                </View>
              ) : undefined
            }
          />
        )
      }
    />
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  flex: { flex: 1 },
  header: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8, gap: 12 },
  list: { padding: 16, gap: 10, flexGrow: 1 },
});
