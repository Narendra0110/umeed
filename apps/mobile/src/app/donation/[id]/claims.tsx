import { router, useLocalSearchParams } from 'expo-router';
import { Alert, FlatList, RefreshControl, StyleSheet, View } from 'react-native';

import { ListRow } from '@/components/list-row';
import { Body, Button, colors, EmptyState, ErrorState, LinkButton, Loading, Pill, Title } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { confirmBlock, useBlockUser } from '@/lib/blocks';
import { useAcceptClaim, useDonationClaims } from '@/lib/claims';
import { celebrate } from '@/lib/delight';
import { useDonation } from '@/lib/donations';
import { timeAgo } from '@/lib/format';
import { useT } from '@/lib/i18n';
import type { Claim } from '@/lib/types';

/** Donor only: everyone who claimed the item. Choosing one reserves the item for them. */
export default function DonationClaimsScreen() {
  const m = useT();
  const { id } = useLocalSearchParams<{ id: string }>();
  const donation = useDonation(id);
  const claims = useDonationClaims(id);
  const accept = useAcceptClaim();
  const block = useBlockUser();

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
      m.claims.giveToTitle(claim.claimantName),
      m.claims.giveToMessage,
      [
        { text: m.common.notYet, style: 'cancel' },
        {
          text: m.claims.choose,
          onPress: () =>
            accept.mutate(claim.id, {
              onSuccess: () => {
                celebrate(m.celebrate.chosen);
                router.replace(`/donation/${id}/pickup`);
              },
              onError: (error) => Alert.alert(m.claims.couldNotChoose, errorMessage(error)),
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
              ? m.claims.chooseIntro
              : m.claims.reservedIntro}
          </Body>
          {!available ? (
            <Button title={m.common.pickupDetails} onPress={() => router.push(`/donation/${id}/pickup`)} />
          ) : null}
        </View>
      }
      ListEmptyComponent={
        <EmptyState
          title={m.claims.emptyTitle}
          message={m.claims.emptyMessage}
        />
      }
      renderItem={({ item }) => (
        <ListRow
          title={item.claimantName}
          subtitle={`${item.message ? `“${item.message}”` : m.claims.noMessage} · ${timeAgo(item.createdAt)}`}
          extra={
            <Pill
              label={m.labels.claimStatusForDonor[item.status]}
              tone={item.status === 'ACCEPTED' ? 'success' : item.status === 'PENDING' ? 'info' : 'neutral'}
            />
          }
          actions={
            <View style={styles.flex}>
              {available && item.status === 'PENDING' ? (
                <Button
                  title={m.claims.giveTo(item.claimantName)}
                  loading={accept.isPending && accept.variables === item.id}
                  disabled={accept.isPending}
                  onPress={() => confirmAccept(item)}
                />
              ) : null}
              <LinkButton
                title={m.common.block(item.claimantName)}
                color={colors.muted}
                onPress={() =>
                  confirmBlock(item.claimantName, (handlers) => block.mutate(item.claimantId, handlers))
                }
              />
            </View>
          }
        />
      )}
    />
  );
}

const styles = StyleSheet.create({
  screen: { backgroundColor: colors.background },
  flex: { flex: 1, gap: 8 },
  list: { padding: 16, gap: 10, flexGrow: 1 },
  header: { gap: 10, marginBottom: 8 },
});
