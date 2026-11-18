import { router } from 'expo-router';
import { useState } from 'react';
import { ActivityIndicator, Alert, RefreshControl, SectionList, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ListRow, SectionTitle } from '@/components/list-row';
import { Button, colors, EmptyState, ErrorState, Loading, Pill, Segmented, Title } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { useMyClaims, useWithdrawClaim } from '@/lib/claims';
import { useMyDonations } from '@/lib/donations';
import { claimStatusLabels, donationStatusLabels, groupMyClaims, groupMyDonations, timeAgo } from '@/lib/format';
import type { Claim, ClaimStatus, Donation, DonationStatus } from '@/lib/types';

type Tab = 'donations' | 'claims';

/** Activity: the items I gave and the items I claimed, grouped by what needs doing next. */
export default function ActivityScreen() {
  const [tab, setTab] = useState<Tab>('donations');

  return (
    <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
      <View style={styles.header}>
        <Title>Activity</Title>
        <Segmented
          choices={[
            { value: 'donations', label: 'My donations' },
            { value: 'claims', label: 'My claims' },
          ]}
          value={tab}
          onChange={setTab}
        />
      </View>
      {tab === 'donations' ? <MyDonations /> : <MyClaims />}
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
  const donations = useMyDonations();

  if (donations.isPending) {
    return <Loading />;
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
        title="You have not given anything yet"
        message="Items you post appear here, with the claims people send you."
        action={<Button title="Give an item" onPress={() => router.navigate('/donate')} />}
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
          extra={<Pill label={donationStatusLabels[item.status]} tone={donationTone[item.status]} />}
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
  const claims = useMyClaims();
  const withdraw = useWithdrawClaim();

  if (claims.isPending) {
    return <Loading />;
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
        title="No claims yet"
        message="When you claim an item from the feed, you can follow it here."
        action={<Button title="Browse items" onPress={() => router.navigate('/feed')} />}
      />
    );
  }

  const confirmWithdraw = (claim: Claim) =>
    Alert.alert(
      'Withdraw your claim?',
      `You will not be able to claim “${claim.donationTitle}” again.`,
      [
        { text: 'Keep claim', style: 'cancel' },
        {
          text: 'Withdraw',
          style: 'destructive',
          onPress: () =>
            withdraw.mutate(claim.id, {
              onError: (error) => Alert.alert('Could not withdraw', errorMessage(error)),
            }),
        },
      ],
    );

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
          subtitle={item.message ? `Your message: ${item.message}` : `Claimed ${timeAgo(item.createdAt)}`}
          extra={
            received ? (
              <Pill label="Received" tone="success" />
            ) : (
              <Pill label={claimStatusLabels[item.status]} tone={claimTone[item.status]} />
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
                  title="Withdraw claim"
                  variant="secondary"
                  loading={withdraw.isPending && withdraw.variables === item.id}
                  onPress={() => confirmWithdraw(item)}
                />
              </View>
            ) : item.status === 'ACCEPTED' ? (
              <View style={styles.flex}>
                <Button title="Pickup details" onPress={() => router.push(`/donation/${item.donationId}/pickup`)} />
              </View>
            ) : undefined
          }
        />
        );
      }}
    />
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  flex: { flex: 1 },
  header: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8, gap: 12 },
  list: { padding: 16, gap: 10, flexGrow: 1 },
});
