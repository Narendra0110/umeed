import { router, useLocalSearchParams } from 'expo-router';
import { Alert, FlatList, RefreshControl, StyleSheet, View } from 'react-native';

import { ListRow } from '@/components/list-row';
import { Body, Button, colors, EmptyState, ErrorState, Loading, Pill, Title } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { useAcceptClaim, useDonationClaims } from '@/lib/claims';
import { useDonation } from '@/lib/donations';
import { timeAgo } from '@/lib/format';
import type { Claim } from '@/lib/types';

const statusLabel: Record<Claim['status'], string> = {
  PENDING: 'Waiting',
  ACCEPTED: 'Chosen',
  REJECTED: 'Not chosen',
  WITHDRAWN: 'Withdrawn',
};

/** Donor only: everyone who claimed the item. Choosing one reserves the item for them. */
export default function DonationClaimsScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const donation = useDonation(id);
  const claims = useDonationClaims(id);
  const accept = useAcceptClaim();

  if (donation.isPending || claims.isPending) {
    return <Loading />;
  }
  if (donation.isError || claims.isError) {
    const error = donation.error ?? claims.error;
    return (
      <ErrorState
        message={errorMessage(error)}
        onRetry={() => {
          donation.refetch();
          claims.refetch();
        }}
        retrying={donation.isFetching || claims.isFetching}
      />
    );
  }

  const available = donation.data.status === 'AVAILABLE';

  const confirmAccept = (claim: Claim) =>
    Alert.alert(
      `Give it to ${claim.claimantName}?`,
      'You will both see each other’s phone number, and they will see the pickup address. Everyone else who claimed it will be told it was given to someone else.',
      [
        { text: 'Not yet', style: 'cancel' },
        {
          text: 'Choose',
          onPress: () =>
            accept.mutate(claim.id, {
              onSuccess: () => router.replace(`/donation/${id}/pickup`),
              onError: (error) => Alert.alert('Could not choose', errorMessage(error)),
            }),
        },
      ],
    );

  return (
    <FlatList
      data={claims.data}
      keyExtractor={(claim) => claim.id}
      style={styles.screen}
      contentContainerStyle={styles.list}
      refreshControl={
        <RefreshControl
          refreshing={claims.isRefetching}
          onRefresh={() => {
            donation.refetch();
            claims.refetch();
          }}
        />
      }
      ListHeaderComponent={
        <View style={styles.header}>
          <Title>{donation.data.title}</Title>
          <Body>
            {available
              ? 'Choose who gets this item. Only one person can be chosen.'
              : 'This item is reserved. Open the pickup details to arrange the handover.'}
          </Body>
          {!available ? (
            <Button title="Pickup details" onPress={() => router.push(`/donation/${id}/pickup`)} />
          ) : null}
        </View>
      }
      ListEmptyComponent={
        <EmptyState
          title="No claims yet"
          message="People who claim this item from the feed will appear here. You can come back later."
        />
      }
      renderItem={({ item }) => (
        <ListRow
          title={item.claimantName}
          subtitle={`${item.message ? `“${item.message}”` : 'No message'} · ${timeAgo(item.createdAt)}`}
          extra={
            <Pill
              label={statusLabel[item.status]}
              tone={item.status === 'ACCEPTED' ? 'success' : item.status === 'PENDING' ? 'info' : 'neutral'}
            />
          }
          actions={
            available && item.status === 'PENDING' ? (
              <View style={styles.flex}>
                <Button
                  title={`Give to ${item.claimantName}`}
                  loading={accept.isPending && accept.variables === item.id}
                  disabled={accept.isPending}
                  onPress={() => confirmAccept(item)}
                />
              </View>
            ) : undefined
          }
        />
      )}
    />
  );
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background },
  flex: { flex: 1 },
  list: { padding: 16, gap: 10, flexGrow: 1 },
  header: { gap: 10, marginBottom: 8 },
});
