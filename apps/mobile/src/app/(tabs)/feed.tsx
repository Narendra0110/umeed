import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ClaimSheet } from '@/components/claim-sheet';
import { NeedsList } from '@/components/needs-list';
import { PhotoCarousel } from '@/components/photos';
import { Button, colors, EmptyState, ErrorState, LinkButton, Loading, Pill, Segmented } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { useFeed } from '@/lib/donations';
import { useSkipDonation } from '@/lib/explore';
import { conditionLabels, type Coordinates, distanceKm, formatDistance } from '@/lib/format';
import { useCurrentLocation } from '@/lib/location';
import type { Donation } from '@/lib/types';

/** How long the "Claimed" banner stays after a claim. */
const NOTICE_MS = 3500;

/**
 * Home has two views: Items (below) and Needs (what people nearby asked for, see NeedsList).
 * Items: one donated item per screen (section 2B of the project index). Swiping up moves to the
 * next item. Nearby items come first when location is allowed. Swiping past an item does not hide it;
 * "Not interested" does (F05), until the list is cleared in Profile.
 * The server sends only items the user can claim: never their own, never ones they already claimed.
 * After a claim the feed refreshes, the item drops out and the next one takes its place.
 */
export default function FeedScreen() {
  const location = useCurrentLocation();
  const coords = location.data?.coords ?? null;
  const feed = useFeed(coords, !location.isPending);
  const [height, setHeight] = useState(0);
  const [claiming, setClaiming] = useState<Donation | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [view, setView] = useState<'items' | 'needs'>('items');
  const skip = useSkipDonation();

  useEffect(() => {
    if (!notice) {
      return;
    }
    const timer = setTimeout(() => setNotice(null), NOTICE_MS);
    return () => clearTimeout(timer);
  }, [notice]);

  const items = feed.data?.pages.flatMap((page) => page.items) ?? [];

  let content;
  if (location.isPending || feed.isPending) {
    content = <Loading />;
  } else if (feed.isError) {
    content = (
      <ErrorState message={errorMessage(feed.error)} onRetry={() => feed.refetch()} retrying={feed.isFetching} />
    );
  } else if (items.length === 0) {
    content = (
      <EmptyState
        title="Nothing to claim right now"
        message="New items appear here as people near you give them away. Your own items and the ones you have claimed are under Activity."
        action={<Button title="Donate an item" onPress={() => router.navigate('/donate')} />}
      />
    );
  } else {
    content = (
      <View style={styles.flex} onLayout={(event) => setHeight(event.nativeEvent.layout.height)}>
        {height > 0 ? (
          <FlatList
            data={items}
            keyExtractor={(item) => item.id}
            pagingEnabled
            showsVerticalScrollIndicator={false}
            decelerationRate="fast"
            getItemLayout={(_, index) => ({ length: height, offset: height * index, index })}
            onEndReached={() => feed.hasNextPage && !feed.isFetchingNextPage && feed.fetchNextPage()}
            onEndReachedThreshold={2}
            refreshControl={<RefreshControl refreshing={feed.isRefetching} onRefresh={() => void feed.refetch()} />}
            ListFooterComponent={
              feed.isFetchingNextPage ? <ActivityIndicator style={styles.footer} color={colors.primary} /> : undefined
            }
            renderItem={({ item }) => (
              <FeedPost
                donation={item}
                height={height}
                near={coords}
                onClaim={() => setClaiming(item)}
                onSkip={() =>
                  skip.mutate(item.id, {
                    onSuccess: () => setNotice(`“${item.title}” is hidden. Show it again from Profile.`),
                    onError: (error) => setNotice(errorMessage(error)),
                  })
                }
              />
            )}
          />
        ) : null}
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
      <View style={styles.header}>
        <Text style={styles.brand}>Umeed</Text>
        <Pressable
          onPress={() => router.push('/explore')}
          accessibilityRole="button"
          accessibilityLabel="Search items"
          style={({ pressed }) => [styles.searchButton, pressed ? styles.searchPressed : null]}>
          <Text style={styles.searchText}>🔍 Search</Text>
        </Pressable>
        {location.data?.permission === 'denied' ? (
          <Pressable onPress={() => location.refetch()} accessibilityRole="button">
            <Text style={styles.locationHint}>Showing newest. Tap to use your location.</Text>
          </Pressable>
        ) : coords ? (
          <Text style={styles.locationHint}>Nearest first</Text>
        ) : null}
      </View>
      <View style={styles.switch}>
        <Segmented
          choices={[
            { value: 'items', label: 'Items' },
            { value: 'needs', label: 'Needs' },
          ]}
          value={view}
          onChange={setView}
        />
      </View>
      {notice ? (
        <View style={styles.notice} accessibilityRole="alert" accessibilityLiveRegion="polite">
          <Text style={styles.noticeText}>{notice}</Text>
        </View>
      ) : null}
      {view === 'items' ? content : <NeedsList near={coords} locationReady={!location.isPending} />}
      {claiming ? (
        <ClaimSheet
          visible
          donationId={claiming.id}
          donationTitle={claiming.title}
          donorName={claiming.donorName}
          onClose={() => setClaiming(null)}
          onClaimed={() => {
            setNotice(`Claimed “${claiming.title}”. Find it under Activity.`);
            setClaiming(null);
          }}
        />
      ) : null}
    </SafeAreaView>
  );
}

type FeedPostProps = {
  donation: Donation;
  height: number;
  near: Coordinates | null;
  onClaim: () => void;
  onSkip: () => void;
};

function FeedPost({ donation, height, near, onClaim, onSkip }: FeedPostProps) {
  const [width, setWidth] = useState(0);
  const photoHeight = Math.round(height * 0.58);
  const place = `${donation.area}, ${donation.city}`;
  const distance = near ? ` · ${formatDistance(distanceKm(near, donation))}` : '';

  return (
    <View style={[styles.post, { height }]} onLayout={(event) => setWidth(event.nativeEvent.layout.width)}>
      <PhotoCarousel paths={donation.imageUrls} width={width} height={photoHeight} title={donation.title} />
      <View style={styles.details}>
        <Pressable onPress={() => router.push(`/donation/${donation.id}`)} accessibilityRole="button">
          <Text style={styles.title} numberOfLines={2}>
            {donation.title}
          </Text>
        </Pressable>
        <View style={styles.metaRow}>
          <Pill label={conditionLabels[donation.itemCondition]} tone="info" />
        </View>
        <Text style={styles.meta} numberOfLines={1}>
          {place}
          {distance}
        </Text>
        <Text style={styles.meta}>Given by {donation.donorName}</Text>
        <Text style={styles.description} numberOfLines={3}>
          {donation.description}
        </Text>
        <View style={styles.spacer} />
        <Button title="Claim" onPress={onClaim} />
        <View style={styles.bottomRow}>
          <Text style={styles.swipeHint}>Swipe up for the next item</Text>
          <View style={styles.links}>
            <LinkButton title="Not interested" color={colors.muted} onPress={onSkip} />
            <LinkButton
              title="Report"
              color={colors.muted}
              onPress={() => router.push(`/donation/${donation.id}/report`)}
            />
          </View>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  flex: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 10,
    gap: 12,
  },
  brand: { fontSize: 24, fontWeight: '800', color: colors.primary },
  searchButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
  },
  searchPressed: { backgroundColor: colors.surface },
  searchText: { fontSize: 14, color: colors.text },
  switch: { paddingHorizontal: 16, paddingBottom: 8 },
  locationHint: { flexShrink: 1, fontSize: 13, color: colors.muted, textAlign: 'right' },
  footer: { padding: 24 },
  post: { backgroundColor: colors.background },
  details: { flex: 1, paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8, gap: 6 },
  title: { fontSize: 22, fontWeight: '700', color: colors.text },
  metaRow: { flexDirection: 'row', gap: 8 },
  meta: { fontSize: 15, color: colors.muted },
  description: { fontSize: 15, color: colors.text, lineHeight: 21 },
  spacer: { flex: 1 },
  notice: {
    marginHorizontal: 16,
    marginBottom: 8,
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 12,
    backgroundColor: colors.successBackground,
  },
  noticeText: { color: colors.success, fontSize: 15, fontWeight: '600', textAlign: 'center' },
  bottomRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', minHeight: 32 },
  swipeHint: { fontSize: 13, color: colors.muted, flexShrink: 1 },
  links: { flexDirection: 'row', gap: 16 },
});
