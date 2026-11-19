import { router } from 'expo-router';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Animated, { FadeInRight } from 'react-native-reanimated';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, font, radius } from '@/components/theme';
import { Button, LanguageSwitch, LinkButton } from '@/components/ui';
import { markWelcomeSeen } from '@/lib/delight';
import { useT } from '@/lib/i18n';

/** Three short slides shown once, before the first login (U03). */
export default function WelcomeScreen() {
  const m = useT();
  const [index, setIndex] = useState(0);
  const slides = [
    { emoji: '🎁', title: m.welcome.giveTitle, text: m.welcome.giveText },
    { emoji: '🙏', title: m.welcome.askTitle, text: m.welcome.askText },
    { emoji: '🤝', title: m.welcome.meetTitle, text: m.welcome.meetText },
  ];
  const slide = slides[index];
  const last = index === slides.length - 1;

  const finish = () => {
    markWelcomeSeen();
    router.replace('/login');
  };

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.top}>
        <LanguageSwitch />
      </View>
      <Animated.View key={index} entering={FadeInRight.duration(280)} style={styles.slide}>
        <View style={styles.badge}>
          <Text style={styles.emoji} accessibilityElementsHidden importantForAccessibility="no">
            {slide.emoji}
          </Text>
        </View>
        <Text style={styles.title} accessibilityRole="header">
          {slide.title}
        </Text>
        <Text style={styles.text}>{slide.text}</Text>
      </Animated.View>
      <View style={styles.dots}>
        {slides.map((s, i) => (
          <View key={s.emoji} style={[styles.dot, i === index ? styles.dotActive : null]} />
        ))}
      </View>
      <View style={styles.bottom}>
        <Button title={last ? m.welcome.start : m.welcome.next} onPress={last ? finish : () => setIndex(index + 1)} />
        {!last ? <LinkButton title={m.welcome.skip} color={colors.muted} onPress={finish} /> : null}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  top: { paddingHorizontal: 24, paddingTop: 12 },
  slide: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 32, gap: 16 },
  badge: {
    width: 150,
    height: 150,
    borderRadius: 75,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  emoji: { fontSize: 76 },
  title: { fontSize: 28, lineHeight: 36, fontFamily: font.extraBold, color: colors.text, textAlign: 'center' },
  text: { fontSize: 17, lineHeight: 26, fontFamily: font.regular, color: colors.muted, textAlign: 'center' },
  dots: { flexDirection: 'row', justifyContent: 'center', gap: 8, marginBottom: 20 },
  dot: { width: 8, height: 8, borderRadius: radius.pill, backgroundColor: colors.border },
  dotActive: { width: 24, backgroundColor: colors.primary },
  bottom: { paddingHorizontal: 24, paddingBottom: 24, gap: 8, alignItems: 'stretch' },
});
