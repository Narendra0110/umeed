import { Image, type ImageStyle } from 'expo-image';
import { useState } from 'react';
import { FlatList, type StyleProp, StyleSheet, Text, View } from 'react-native';

import { colors } from '@/components/ui';
import { fileUrl } from '@/lib/api';
import { useT } from '@/lib/i18n';
import { useAuthHeaders } from '@/lib/session';

/** A stored photo. Photos are private to logged-in users, so the request carries the Firebase token. */
export function AuthImage({ path, style, label }: { path: string; style: StyleProp<ImageStyle>; label?: string }) {
  const headers = useAuthHeaders();
  if (!headers) {
    return <View style={[style as object, styles.placeholder]} />;
  }
  return (
    <Image
      source={{ uri: fileUrl(path), headers }}
      style={style}
      contentFit="cover"
      transition={150}
      accessibilityLabel={label}
      cachePolicy="memory-disk"
    />
  );
}

/** Swipe sideways through a donation's photos, with dots showing which one is visible. */
export function PhotoCarousel({ paths, width, height, title }: { paths: string[]; width: number; height: number; title: string }) {
  const m = useT();
  const [index, setIndex] = useState(0);

  if (paths.length === 0 || width === 0) {
    return <View style={[styles.placeholder, { width, height }]} />;
  }

  return (
    <View style={{ width, height }}>
      <FlatList
        data={paths}
        keyExtractor={(path) => path}
        horizontal
        pagingEnabled
        showsHorizontalScrollIndicator={false}
        nestedScrollEnabled
        onMomentumScrollEnd={(event) => setIndex(Math.round(event.nativeEvent.contentOffset.x / width))}
        renderItem={({ item, index: i }) => (
          <AuthImage
            path={item}
            style={{ width, height }}
            label={m.feed.photo(title, i + 1, paths.length)}
          />
        )}
      />
      {paths.length > 1 ? (
        <View style={styles.dots} pointerEvents="none">
          {paths.map((path, i) => (
            <View key={path} style={[styles.dot, i === index ? styles.dotActive : null]} />
          ))}
        </View>
      ) : null}
      {paths.length > 1 ? (
        <View style={styles.counter} pointerEvents="none">
          <Text style={styles.counterText}>
            {index + 1}/{paths.length}
          </Text>
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  placeholder: { backgroundColor: colors.surface },
  dots: {
    position: 'absolute',
    bottom: 12,
    left: 0,
    right: 0,
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 6,
  },
  dot: { width: 7, height: 7, borderRadius: 4, backgroundColor: 'rgba(255,255,255,0.55)' },
  dotActive: { backgroundColor: '#FFFFFF' },
  counter: {
    position: 'absolute',
    top: 12,
    right: 12,
    backgroundColor: 'rgba(0,0,0,0.5)',
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingVertical: 3,
  },
  counterText: { color: '#FFFFFF', fontSize: 13, fontWeight: '600' },
});
