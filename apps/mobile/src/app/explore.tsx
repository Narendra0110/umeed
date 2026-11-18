import { router } from 'expo-router';
import { useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Platform, RefreshControl, StyleSheet, Text, View } from 'react-native';
import MapView, { Marker, PROVIDER_GOOGLE } from 'react-native-maps';

import { ListRow } from '@/components/list-row';
import { Button, ChoiceChips, colors, EmptyState, ErrorState, Field, Loading, Segmented } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { useCategories } from '@/lib/donations';
import { DISTANCE_CHOICES, emptyFilters, type SearchFilters, spreadPins, useSearch } from '@/lib/explore';
import { conditionLabels, distanceKm, formatDistance } from '@/lib/format';
import { useCurrentLocation } from '@/lib/location';
import type { Donation } from '@/lib/types';

/** How long typing must pause before searching, so every key press does not start a request. */
const TYPING_PAUSE_MS = 400;
/** The middle of India, used for the map when the phone's location is not known. */
const INDIA = { latitude: 22.5, longitude: 79.0, latitudeDelta: 22, longitudeDelta: 22 };

/**
 * Search: words, category, distance and city, shown as a list or on a map. Shows the same items as
 * Home (never your own, claimed or blocked ones), but also items marked "Not interested".
 */
export default function ExploreScreen() {
  const location = useCurrentLocation();
  const coords = location.data?.coords ?? null;
  const categories = useCategories();
  const [draft, setDraft] = useState({ text: '', city: '' });
  const [filters, setFilters] = useState<SearchFilters>(emptyFilters);
  const [view, setView] = useState<'list' | 'map'>('list');
  const [showFilters, setShowFilters] = useState(false);

  // Typed words take effect after a short pause.
  useEffect(() => {
    const timer = setTimeout(
      () => setFilters((current) => ({ ...current, text: draft.text, city: draft.city })),
      TYPING_PAUSE_MS,
    );
    return () => clearTimeout(timer);
  }, [draft]);

  const search = useSearch(filters, coords, !location.isPending);
  const items = search.data?.pages.flatMap((page) => page.items) ?? [];

  const categoryChoices = [
    { value: 'all', label: 'All' },
    ...(categories.data ?? []).map((category) => ({ value: category.id, label: category.name })),
  ];
  const distanceChoices = [
    { value: 'any', label: 'Any distance' },
    ...DISTANCE_CHOICES.map((km) => ({ value: String(km), label: `${km} km` })),
  ];
  const activeFilters = [filters.categoryId, filters.city.trim(), filters.maxKm].filter(Boolean).length;

  let results;
  if (location.isPending || search.isPending) {
    results = <Loading />;
  } else if (search.isError) {
    results = (
      <ErrorState message={errorMessage(search.error)} onRetry={() => search.refetch()} retrying={search.isFetching} />
    );
  } else if (view === 'map') {
    results = <ResultsMap items={items} near={coords} />;
  } else {
    results = (
      <FlatList
        data={items}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        keyboardShouldPersistTaps="handled"
        refreshControl={<RefreshControl refreshing={search.isRefetching} onRefresh={() => void search.refetch()} />}
        onEndReached={() => search.hasNextPage && !search.isFetchingNextPage && search.fetchNextPage()}
        ListFooterComponent={search.isFetchingNextPage ? <ActivityIndicator color={colors.primary} /> : undefined}
        ListEmptyComponent={
          <EmptyState
            title="Nothing found"
            message="Try other words, another category or a larger distance."
            action={
              <Button
                title="Clear search"
                variant="secondary"
                onPress={() => {
                  setDraft({ text: '', city: '' });
                  setFilters(emptyFilters);
                }}
              />
            }
          />
        }
        renderItem={({ item }) => (
          <ListRow
            title={item.title}
            subtitle={`${conditionLabels[item.itemCondition]} · ${item.area}, ${item.city}${
              coords ? ` · ${formatDistance(distanceKm(coords, item))}` : ''
            }`}
            imagePath={item.imageUrls[0]}
            onPress={() => router.push(`/donation/${item.id}`)}
          />
        )}
      />
    );
  }

  return (
    <View style={styles.screen}>
      <View style={styles.controls}>
        <Field
          label="Search"
          placeholder="For example, school bag"
          returnKeyType="search"
          maxLength={100}
          value={draft.text}
          onChangeText={(text) => setDraft((current) => ({ ...current, text }))}
          onSubmitEditing={() => setFilters((current) => ({ ...current, text: draft.text }))}
        />
        <View style={styles.row}>
          <View style={styles.flex}>
            <Segmented
              choices={[
                { value: 'list', label: 'List' },
                { value: 'map', label: 'Map' },
              ]}
              value={view}
              onChange={setView}
            />
          </View>
          <Button
            title={showFilters ? 'Hide filters' : activeFilters ? `Filters (${activeFilters})` : 'Filters'}
            variant="secondary"
            onPress={() => setShowFilters((shown) => !shown)}
          />
        </View>
        {showFilters ? (
          <View style={styles.filters}>
            <ChoiceChips
              label="Category"
              choices={categoryChoices}
              value={filters.categoryId ?? 'all'}
              onChange={(value) => setFilters((current) => ({ ...current, categoryId: value === 'all' ? null : value }))}
            />
            {coords ? (
              <ChoiceChips
                label="Distance"
                choices={distanceChoices}
                value={filters.maxKm ? String(filters.maxKm) : 'any'}
                onChange={(value) =>
                  setFilters((current) => ({ ...current, maxKm: value === 'any' ? null : Number(value) }))
                }
              />
            ) : (
              <Text style={styles.hint}>Allow location on Home to filter by distance.</Text>
            )}
            <Field
              label="City"
              placeholder="For example, Pune"
              autoCapitalize="words"
              maxLength={100}
              value={draft.city}
              onChangeText={(city) => setDraft((current) => ({ ...current, city }))}
            />
          </View>
        ) : null}
      </View>
      {results}
    </View>
  );
}

/** Results on a map. Pins sit at the approximate location (about 1 km), never at an address. */
function ResultsMap({ items, near }: { items: Donation[]; near: { latitude: number; longitude: number } | null }) {
  const first = items[0];
  const centre = near ?? (first ? { latitude: first.latitude, longitude: first.longitude } : null);
  const region = centre ? { ...centre, latitudeDelta: 0.25, longitudeDelta: 0.25 } : INDIA;

  return (
    <View style={styles.flex}>
      <MapView
        style={styles.flex}
        provider={Platform.OS === 'android' ? PROVIDER_GOOGLE : undefined}
        initialRegion={region}
        showsUserLocation={!!near}
        toolbarEnabled={false}>
        {spreadPins(items).map((pin) => (
          <Marker
            key={pin.donation.id}
            coordinate={{ latitude: pin.latitude, longitude: pin.longitude }}
            title={pin.donation.title}
            description={`${pin.donation.area} · tap to open`}
            pinColor={colors.primary}
            onCalloutPress={() => router.push(`/donation/${pin.donation.id}`)}
          />
        ))}
      </MapView>
      {items.length === 0 ? (
        <View style={styles.mapNotice} pointerEvents="none">
          <Text style={styles.mapNoticeText}>Nothing found here. Try other words or filters.</Text>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  flex: { flex: 1 },
  controls: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8, gap: 10 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  filters: { gap: 10 },
  hint: { fontSize: 14, color: colors.muted },
  list: { padding: 16, gap: 10, flexGrow: 1 },
  mapNotice: {
    position: 'absolute',
    top: 12,
    left: 16,
    right: 16,
    padding: 12,
    borderRadius: 12,
    backgroundColor: colors.background,
    elevation: 3,
  },
  mapNoticeText: { textAlign: 'center', color: colors.muted, fontSize: 14 },
});
