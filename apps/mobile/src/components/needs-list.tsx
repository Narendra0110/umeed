import { router } from 'expo-router';
import { ActivityIndicator, FlatList, RefreshControl, StyleSheet } from 'react-native';

import { ListRow } from '@/components/list-row';
import { Button, colors, EmptyState, ErrorState, Loading } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { type Coordinates, distanceKm, formatDistance, timeAgo } from '@/lib/format';
import { useNeedsFeed } from '@/lib/needs';

/** Home, Needs view: what people nearby have asked for. Tapping one opens it to offer an item. */
export function NeedsList({ near, locationReady }: { near: Coordinates | null; locationReady: boolean }) {
  const needs = useNeedsFeed(near, locationReady);

  if (!locationReady || needs.isPending) {
    return <Loading />;
  }
  if (needs.isError) {
    return <ErrorState message={errorMessage(needs.error)} onRetry={() => needs.refetch()} retrying={needs.isFetching} />;
  }
  const items = needs.data.pages.flatMap((page) => page.items);

  return (
    <FlatList
      data={items}
      keyExtractor={(need) => need.id}
      contentContainerStyle={styles.list}
      refreshControl={<RefreshControl refreshing={needs.isRefetching} onRefresh={() => void needs.refetch()} />}
      onEndReached={() => needs.hasNextPage && !needs.isFetchingNextPage && needs.fetchNextPage()}
      ListFooterComponent={needs.isFetchingNextPage ? <ActivityIndicator color={colors.primary} /> : undefined}
      ListEmptyComponent={
        <EmptyState
          title="Nobody nearby needs anything right now"
          message="When people ask for something, it appears here and you can offer one of your items."
          action={<Button title="Ask for something" variant="secondary" onPress={() => router.navigate('/donate')} />}
        />
      }
      renderItem={({ item }) => (
        <ListRow
          title={item.title}
          subtitle={`${item.requesterName} · ${item.area}, ${item.city}${
            near ? ` · ${formatDistance(distanceKm(near, item))}` : ''
          } · ${timeAgo(item.createdAt)}\n${item.description}`}
          onPress={() => router.push(`/need/${item.id}`)}
        />
      )}
    />
  );
}

const styles = StyleSheet.create({
  list: { padding: 16, gap: 10, flexGrow: 1 },
});
