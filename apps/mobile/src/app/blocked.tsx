import { Alert, FlatList, RefreshControl, StyleSheet, View } from 'react-native';

import { ListRow } from '@/components/list-row';
import { Button, colors, EmptyState, ErrorState, Loading } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { type BlockedUser, useBlockedUsers, useUnblockUser } from '@/lib/blocks';
import { timeAgo } from '@/lib/format';
import { useT } from '@/lib/i18n';

/** The people I have blocked, with Unblock. */
export default function BlockedPeopleScreen() {
  const m = useT();
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
    Alert.alert(m.blocks.unblockTitle(person.name), m.blocks.unblockMessage, [
      { text: m.common.cancel, style: 'cancel' },
      {
        text: m.blocks.unblock,
        onPress: () =>
          unblock.mutate(person.userId, {
            onError: (error) => Alert.alert(m.blocks.couldNotUnblock, errorMessage(error)),
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
      ListEmptyComponent={<EmptyState title={m.blocks.emptyTitle} message={m.blocks.emptyMessage} />}
      renderItem={({ item }) => (
        <ListRow
          title={item.name}
          subtitle={m.blocks.blockedAgo(timeAgo(item.blockedAt))}
          actions={
            <View style={styles.flex}>
              <Button
                title={m.blocks.unblock}
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
