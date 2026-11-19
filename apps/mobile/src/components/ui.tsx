import { type ReactNode, useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  type TextInputProps,
  View,
} from 'react-native';
import { type Edge, SafeAreaView } from 'react-native-safe-area-context';

import { type Language, useLanguageStore, useT } from '@/lib/i18n';

import { colors, font, radius, shadow } from './theme';

export { colors, font } from './theme';

type ScreenProps = {
  children: ReactNode;
  /** Scrollable content, for forms and long details. */
  scroll?: boolean;
  /** Tab screens have no header, so they also need the top safe area. */
  edges?: Edge[];
};

export function Screen({ children, scroll, edges = ['bottom', 'left', 'right'] }: ScreenProps) {
  return (
    <SafeAreaView style={styles.safe} edges={edges}>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        {scroll ? (
          <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
            {children}
          </ScrollView>
        ) : (
          <View style={[styles.content, styles.flex]}>{children}</View>
        )}
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

export function Title({ children }: { children: ReactNode }) {
  return (
    <Text style={styles.title} accessibilityRole="header">
      {children}
    </Text>
  );
}

export function Body({ children }: { children: ReactNode }) {
  return <Text style={styles.body}>{children}</Text>;
}

export function Loading() {
  return (
    <View style={styles.center}>
      <ActivityIndicator size="large" color={colors.primary} />
    </View>
  );
}

type FieldProps = TextInputProps & { label: string; error?: string; prefix?: string; hint?: string };

export function Field({ label, error, prefix, hint, multiline, onFocus, onBlur, ...inputProps }: FieldProps) {
  const [focused, setFocused] = useState(false);
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <View style={[styles.inputRow, focused ? styles.inputRowFocused : null, error ? styles.inputRowError : null]}>
        {prefix ? <Text style={styles.prefix}>{prefix}</Text> : null}
        <TextInput
          style={[styles.input, multiline ? styles.inputMultiline : null]}
          placeholderTextColor={colors.muted}
          accessibilityLabel={label}
          multiline={multiline}
          onFocus={(event) => {
            setFocused(true);
            onFocus?.(event);
          }}
          onBlur={(event) => {
            setFocused(false);
            onBlur?.(event);
          }}
          {...inputProps}
        />
      </View>
      {hint && !error ? <Text style={styles.hint}>{hint}</Text> : null}
      {error ? <Text style={styles.fieldError}>{error}</Text> : null}
    </View>
  );
}

type ButtonProps = {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'danger';
};

export function Button({ title, onPress, loading, disabled, variant = 'primary' }: ButtonProps) {
  const inactive = disabled || loading;
  const primary = variant === 'primary';
  return (
    <Pressable
      onPress={onPress}
      disabled={inactive}
      accessibilityRole="button"
      accessibilityState={{ disabled: !!inactive, busy: !!loading }}
      style={({ pressed }) => [
        styles.button,
        primary ? styles.buttonPrimary : variant === 'danger' ? styles.buttonDanger : styles.buttonSecondary,
        pressed && primary ? styles.buttonPrimaryPressed : null,
        pressed && !primary ? styles.buttonSecondaryPressed : null,
        pressed ? styles.buttonPressed : null,
        inactive ? styles.buttonInactive : null,
      ]}>
      {loading ? (
        <ActivityIndicator color={primary ? '#FFFFFF' : colors.primary} />
      ) : (
        <Text
          style={
            primary
              ? styles.buttonPrimaryText
              : variant === 'danger'
                ? styles.buttonDangerText
                : styles.buttonSecondaryText
          }>
          {title}
        </Text>
      )}
    </Pressable>
  );
}

/** A small text button, for less important actions such as "Report". */
export function LinkButton({ title, onPress, color = colors.primary }: { title: string; onPress: () => void; color?: string }) {
  return (
    <Pressable onPress={onPress} accessibilityRole="button" hitSlop={10} style={styles.linkButton}>
      <Text style={[styles.linkButtonText, { color }]}>{title}</Text>
    </Pressable>
  );
}

type Choice<T extends string> = { value: T; label: string };

/** A row of selectable chips for choosing one value, such as a category or a condition. */
export function ChoiceChips<T extends string>({
  label,
  choices,
  value,
  onChange,
  error,
}: {
  label: string;
  choices: Choice<T>[];
  value: T | undefined;
  onChange: (value: T) => void;
  error?: string;
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.chips} accessibilityRole="radiogroup" accessibilityLabel={label}>
        {choices.map((choice) => {
          const selected = choice.value === value;
          return (
            <Pressable
              key={choice.value}
              onPress={() => onChange(choice.value)}
              accessibilityRole="radio"
              accessibilityState={{ selected }}
              style={[styles.chip, selected ? styles.chipSelected : null]}>
              <Text style={[styles.chipText, selected ? styles.chipTextSelected : null]}>{choice.label}</Text>
            </Pressable>
          );
        })}
      </View>
      {error ? <Text style={styles.fieldError}>{error}</Text> : null}
    </View>
  );
}

/** Two or three tabs inside a screen, such as "My donations" and "My claims". */
export function Segmented<T extends string>({
  choices,
  value,
  onChange,
}: {
  choices: Choice<T>[];
  value: T;
  onChange: (value: T) => void;
}) {
  return (
    <View style={styles.segmented} accessibilityRole="tablist">
      {choices.map((choice) => {
        const selected = choice.value === value;
        return (
          <Pressable
            key={choice.value}
            onPress={() => onChange(choice.value)}
            accessibilityRole="tab"
            accessibilityState={{ selected }}
            style={[styles.segment, selected ? styles.segmentSelected : null]}>
            <Text style={[styles.segmentText, selected ? styles.segmentTextSelected : null]}>{choice.label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

type PillTone = 'neutral' | 'info' | 'success' | 'warning' | 'error';

const pillColors: Record<PillTone, { text: string; background: string }> = {
  neutral: { text: colors.muted, background: colors.surface },
  info: { text: colors.primaryPressed, background: colors.primaryLight },
  success: { text: colors.success, background: colors.successBackground },
  warning: { text: colors.warning, background: colors.warningBackground },
  error: { text: colors.error, background: colors.errorBackground },
};

export function Pill({ label, tone = 'neutral' }: { label: string; tone?: PillTone }) {
  return (
    <View style={[styles.pill, { backgroundColor: pillColors[tone].background }]}>
      <Text style={[styles.pillText, { color: pillColors[tone].text }]}>{label}</Text>
    </View>
  );
}

/** Shown when a list has nothing in it yet. */
export function EmptyState({
  title,
  message,
  action,
  emoji = '🌱',
}: {
  title: string;
  message?: string;
  action?: ReactNode;
  /** A big friendly picture above the title. */
  emoji?: string;
}) {
  return (
    <View style={styles.state}>
      <View style={styles.stateBadge}>
        <Text style={styles.stateEmoji} accessibilityElementsHidden importantForAccessibility="no">
          {emoji}
        </Text>
      </View>
      <Text style={styles.stateTitle}>{title}</Text>
      {message ? <Text style={styles.stateMessage}>{message}</Text> : null}
      {action}
    </View>
  );
}

/** Shown when loading failed, with a way to try again. */
export function ErrorState({
  message,
  onRetry,
  retrying,
}: {
  message: string;
  onRetry: () => void;
  retrying?: boolean;
}) {
  const m = useT();
  return (
    <View style={styles.state}>
      <View style={styles.stateBadge}>
        <Text style={styles.stateEmoji} accessibilityElementsHidden importantForAccessibility="no">
          😕
        </Text>
      </View>
      <Text style={styles.stateTitle}>{m.common.couldNotLoad}</Text>
      <Text style={styles.stateMessage}>{message}</Text>
      <Button title={m.common.tryAgain} onPress={onRetry} loading={retrying} />
    </View>
  );
}

/**
 * English / हिन्दी switch. Each choice is written in its own language so anyone can find theirs.
 * onChange is called after the app has switched; the caller may also save it to the account.
 */
export function LanguageSwitch({ onChange }: { onChange?: (language: Language) => void }) {
  const m = useT();
  const language = useLanguageStore((state) => state.language);
  const setLanguage = useLanguageStore((state) => state.setLanguage);
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{m.language.label} / Language</Text>
      <Segmented
        choices={[
          { value: 'en', label: 'English' },
          { value: 'hi', label: 'हिन्दी' },
        ]}
        value={language}
        onChange={(value: Language) => {
          setLanguage(value, true);
          onChange?.(value);
        }}
      />
    </View>
  );
}

/** The Umeed wordmark with its tagline, for the login screen. */
export function Brand() {
  const m = useT();
  return (
    <View style={styles.brand} accessibilityRole="header">
      <Text style={styles.brandName}>
        Umeed <Text style={styles.brandHindi}>उम्मीद</Text>
      </Text>
      <Text style={styles.brandTagline}>{m.brandTagline}</Text>
    </View>
  );
}

export function ErrorBanner({ message }: { message?: string | null }) {
  if (!message) {
    return null;
  }
  return (
    <View style={styles.banner} accessibilityRole="alert">
      <Text style={styles.bannerText}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  flex: { flex: 1 },
  content: { padding: 24, gap: 16 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
  title: { fontSize: 28, lineHeight: 36, fontFamily: font.bold, color: colors.text },
  body: { fontSize: 16, lineHeight: 23, fontFamily: font.regular, color: colors.muted },
  field: { gap: 6 },
  label: { fontSize: 15, fontFamily: font.semibold, color: colors.text },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: colors.border,
    borderRadius: radius.medium,
    backgroundColor: colors.card,
    paddingHorizontal: 14,
    minHeight: 54,
  },
  inputRowFocused: { borderColor: colors.primary },
  inputRowError: { borderColor: colors.error },
  prefix: { fontSize: 18, fontFamily: font.semibold, color: colors.text, marginRight: 8 },
  input: { flex: 1, fontSize: 18, fontFamily: font.regular, color: colors.text, paddingVertical: 10 },
  inputMultiline: { minHeight: 100, textAlignVertical: 'top' },
  hint: { fontSize: 13, fontFamily: font.regular, color: colors.muted },
  fieldError: { fontSize: 14, fontFamily: font.medium, color: colors.error },
  button: {
    minHeight: 54,
    borderRadius: radius.medium,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 20,
  },
  buttonPrimary: { backgroundColor: colors.primary, ...shadow },
  buttonPrimaryPressed: { backgroundColor: colors.primaryPressed },
  buttonSecondary: { backgroundColor: colors.card, borderWidth: 1.5, borderColor: colors.border },
  buttonSecondaryPressed: { backgroundColor: colors.surface },
  buttonDanger: { backgroundColor: colors.card, borderWidth: 1.5, borderColor: colors.error },
  buttonPressed: { transform: [{ scale: 0.98 }] },
  buttonInactive: { opacity: 0.55 },
  buttonPrimaryText: { color: '#FFFFFF', fontSize: 17, fontFamily: font.bold },
  buttonSecondaryText: { color: colors.text, fontSize: 17, fontFamily: font.semibold },
  buttonDangerText: { color: colors.error, fontSize: 17, fontFamily: font.semibold },
  linkButton: { paddingVertical: 4 },
  linkButtonText: { fontSize: 15, fontFamily: font.semibold },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    borderWidth: 1.5,
    borderColor: colors.border,
    borderRadius: radius.pill,
    backgroundColor: colors.card,
    paddingHorizontal: 16,
    paddingVertical: 6,
    minHeight: 40,
    justifyContent: 'center',
  },
  chipSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
  chipText: { fontSize: 15, fontFamily: font.medium, color: colors.text },
  chipTextSelected: { color: '#FFFFFF', fontFamily: font.semibold },
  segmented: { flexDirection: 'row', backgroundColor: colors.surface, borderRadius: radius.medium, padding: 4 },
  segment: { flex: 1, minHeight: 42, borderRadius: radius.small, alignItems: 'center', justifyContent: 'center' },
  segmentSelected: { backgroundColor: colors.card, ...shadow },
  segmentText: { fontSize: 15, color: colors.muted, fontFamily: font.semibold },
  segmentTextSelected: { color: colors.primary, fontFamily: font.bold },
  pill: { alignSelf: 'flex-start', borderRadius: radius.pill, paddingHorizontal: 12, paddingVertical: 2 },
  pillText: { fontSize: 13, fontFamily: font.semibold },
  state: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12, padding: 24 },
  stateBadge: {
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: colors.primaryLight,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  stateEmoji: { fontSize: 42 },
  stateTitle: { fontSize: 21, lineHeight: 28, fontFamily: font.bold, color: colors.text, textAlign: 'center' },
  stateMessage: { fontSize: 16, lineHeight: 23, fontFamily: font.regular, color: colors.muted, textAlign: 'center' },
  brand: { alignItems: 'flex-start', gap: 0 },
  brandName: { fontSize: 40, lineHeight: 50, fontFamily: font.extraBold, color: colors.primary },
  brandHindi: { fontSize: 28, fontFamily: font.bold, color: colors.accent },
  brandTagline: { fontSize: 16, fontFamily: font.medium, color: colors.muted, marginTop: -4 },
  banner: { backgroundColor: colors.errorBackground, borderRadius: radius.medium, padding: 12 },
  bannerText: { color: colors.error, fontSize: 15, fontFamily: font.medium },
});