import { Alert, FlatList, RefreshControl, StyleSheet, View } from 'react-native';

import { ListRow } from '@/components/list-row';
import { Button, colors, EmptyState, ErrorState, Loading } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { type BlockedUser, useBlockedUsers, useUnblockUser } from '@/lib/blocks';
import { timeAgo } from '@/lib/format';

/** The people I have blocked, with Unblock. */
export default function BlockedPeopleScreen() {
  const blocked = useBlockedUsers();
  const unblock = useUnblockUser();

  if (blocked.isPending) {
    return <Loading />;
  }
  if (blocked.isError) {
    return (
      <ErrorState message={errorMessage(blocked.error)} onRetry={() => blocked.refetch()} retrying={blocked.isFetching} />
    );
  }

  const confirmUnblock = (person: BlockedUser) =>
    Alert.alert(`Unblock ${person.name}?`, 'You will see each other’s items again. Claims that were closed stay closed.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Unblock',
        onPress: () =>
          unblock.mutate(person.userId, {
            onError: (error) => Alert.alert('Could not unblock', errorMessage(error)),
          }),
      },
    ]);

  return (
    <FlatList
      data={blocked.data}
      keyExtractor={(person) => person.userId}
      style={styles.screen}
      contentContainerStyle={styles.list}
      refreshControl={<RefreshControl refreshing={blocked.isRefetching} onRefresh={() => void blocked.refetch()} />}
      ListEmptyComponent={
        <EmptyState
          title="You have not blocked anyone"
          message="You can block someone from an item’s page or from the claims on your item."
        />
      }
      renderItem={({ item }) => (
        <ListRow
          title={item.name}
          subtitle={`Blocked ${timeAgo(item.blockedAt)}`}
          actions={
            <View style={styles.flex}>
              <Button
                title="Unblock"
                variant="secondary"
                loading={unblock.isPending && unblock.variables === item.userId}
                onPress={() => confirmUnblock(item)}
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
  flex: { flex: 1 },
  list: { padding: 16, gap: 10, flexGrow: 1 },
});
