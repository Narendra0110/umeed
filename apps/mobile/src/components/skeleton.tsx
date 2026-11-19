import { useEffect } from 'react';
import { type DimensionValue, StyleSheet, View } from 'react-native';
import Animated, { useAnimatedStyle, useSharedValue, withRepeat, withTiming } from 'react-native-reanimated';

import { colors, radius, shadow } from './theme';

/**
 * Grey shapes that gently pulse while something loads (U02), shaped like what is coming,
 * instead of a spinning circle.
 */
export function Skeleton({
  width = '100%',
  height,
  round = radius.small,
}: {
  width?: DimensionValue;
  height: number;
  round?: number;
}) {
  const opacity = useSharedValue(0.55);
  useEffect(() => {
    opacity.value = withRepeat(withTiming(1, { duration: 700 }), -1, true);
  }, [opacity]);
  const pulse = useAnimatedStyle(() => ({ opacity: opacity.value }));
  return <Animated.View style={[{ width, height, borderRadius: round, backgroundColor: colors.surface }, pulse]} />;
}

/** Placeholder rows for Activity, Chats, Needs and Search. */
export function ListSkeleton({ rows = 5 }: { rows?: number }) {
  return (
    <View style={styles.list} accessibilityLabel="Loading" accessibilityRole="progressbar">
      {Array.from({ length: rows }, (_, i) => (
        <View key={i} style={styles.row}>
          <Skeleton width={68} height={68} round={radius.medium} />
          <View style={styles.rowText}>
            <Skeleton width="70%" height={18} />
            <Skeleton width="45%" height={14} />
          </View>
        </View>
      ))}
    </View>
  );
}

/** Placeholder for one Home card: a big photo, then a few lines. */
export function CardSkeleton() {
  return (
    <View style={styles.card} accessibilityLabel="Loading" accessibilityRole="progressbar">
      <Skeleton height={320} round={radius.large} />
      <Skeleton width="65%" height={24} />
      <Skeleton width="40%" height={16} />
      <Skeleton width="90%" height={16} />
      <View style={styles.spacer} />
      <Skeleton height={54} round={radius.medium} />
    </View>
  );
}

const styles = StyleSheet.create({
  list: { padding: 16, gap: 10 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 12,
    borderRadius: radius.large,
    backgroundColor: colors.card,
    ...shadow,
  },
  rowText: { flex: 1, gap: 8 },
  card: { flex: 1, padding: 16, gap: 12 },
  spacer: { flex: 1 },
});
