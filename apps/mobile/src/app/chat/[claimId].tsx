import { Stack, useLocalSearchParams } from 'expo-router';
import { useEffect, useState } from 'react';
import {
  Alert,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { colors, EmptyState, ErrorState, Loading, Screen } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useChatThread, useMarkChatRead, useSendMessage } from '@/lib/chat';
import { timeAgo } from '@/lib/format';
import { useT } from '@/lib/i18n';
import type { ChatMessage } from '@/lib/types';

const MAX_LENGTH = 1000;

/** The conversation between a donor and the chosen receiver. Read-only once the pickup is over. */
export default function ChatScreen() {
  const m = useT();
  const { claimId } = useLocalSearchParams<{ claimId: string }>();
  const thread = useChatThread(claimId);
  const send = useSendMessage(claimId);
  const markRead = useMarkChatRead(claimId);
  const insets = useSafeAreaInsets();
  const [text, setText] = useState('');

  const messages = thread.data?.messages;
  const lastFromOther = messages?.length ? !messages[messages.length - 1].mine : false;
  const lastId = messages?.length ? messages[messages.length - 1].id : null;
  const { mutate: markAsRead } = markRead;

  // Whenever a new message from the other person is on screen, it has been read.
  useEffect(() => {
    if (lastId && lastFromOther) {
      markAsRead();
    }
  }, [lastId, lastFromOther, markAsRead]);

  if (thread.isPending) {
    return <Loading />;
  }
  if (thread.isError) {
    const status = thread.error instanceof ApiError ? thread.error.status : 0;
    return (
      <Screen>
        {status === 403 || status === 404 ? (
          <EmptyState title={m.chat.notAvailableTitle} message={m.chat.notAvailableMessage} />
        ) : (
          <ErrorState message={errorMessage(thread.error)} onRetry={() => thread.refetch()} retrying={thread.isFetching} />
        )}
      </Screen>
    );
  }

  const chat = thread.data;
  const trimmed = text.trim();

  const onSend = () => {
    if (!trimmed || send.isPending) {
      return;
    }
    send.mutate(trimmed, {
      onSuccess: () => setText(''),
      onError: (error) => Alert.alert(m.chat.notSent, errorMessage(error)),
    });
  };

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}>
      <Stack.Screen options={{ title: `${chat.otherPartyName} · ${chat.donationTitle}` }} />
      <FlatList
        inverted
        data={[...chat.messages].reverse()}
        keyExtractor={(message) => message.id}
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyText}>
              {chat.open
                ? m.chat.helloTo(chat.otherPartyName)
                : m.chat.noMessages}
            </Text>
          </View>
        }
        renderItem={({ item }) => <Bubble message={item} />}
      />
      {chat.open ? (
        <View style={[styles.composer, { paddingBottom: Math.max(insets.bottom, 8) }]}>
          <TextInput
            style={styles.input}
            value={text}
            onChangeText={setText}
            placeholder={m.chat.placeholder}
            placeholderTextColor={colors.muted}
            multiline
            maxLength={MAX_LENGTH}
            accessibilityLabel={m.chat.messageLabel}
          />
          <Pressable
            onPress={onSend}
            disabled={!trimmed || send.isPending}
            accessibilityRole="button"
            accessibilityLabel={m.chat.send}
            style={({ pressed }) => [
              styles.send,
              !trimmed || send.isPending ? styles.sendDisabled : null,
              pressed ? styles.sendPressed : null,
            ]}>
            <Text style={styles.sendText}>{send.isPending ? '…' : m.chat.send}</Text>
          </Pressable>
        </View>
      ) : (
        <View style={[styles.closed, { paddingBottom: Math.max(insets.bottom, 12) }]}>
          <Text style={styles.closedText}>{m.chat.closed}</Text>
        </View>
      )}
    </KeyboardAvoidingView>
  );
}

function Bubble({ message }: { message: ChatMessage }) {
  return (
    <View style={[styles.bubble, message.mine ? styles.mine : styles.theirs]}>
      <Text style={[styles.body, message.mine ? styles.mineText : null]} selectable>
        {message.body}
      </Text>
      <Text style={[styles.time, message.mine ? styles.mineTime : null]}>{timeAgo(message.createdAt)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  list: { padding: 12, gap: 8, flexGrow: 1 },
  empty: { flex: 1, justifyContent: 'center', padding: 24, transform: [{ scaleY: -1 }] },
  emptyText: { textAlign: 'center', color: colors.muted, fontSize: 15, lineHeight: 21 },
  bubble: { maxWidth: '80%', borderRadius: 16, paddingHorizontal: 12, paddingVertical: 8, gap: 2 },
  mine: { alignSelf: 'flex-end', backgroundColor: colors.primary },
  theirs: { alignSelf: 'flex-start', backgroundColor: colors.surface },
  body: { fontSize: 16, lineHeight: 22, color: colors.text },
  mineText: { color: colors.background },
  time: { fontSize: 11, color: colors.muted, alignSelf: 'flex-end' },
  mineTime: { color: colors.primaryLight },
  composer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: 8,
    paddingHorizontal: 12,
    paddingTop: 8,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    backgroundColor: colors.background,
  },
  input: {
    flex: 1,
    minHeight: 44,
    maxHeight: 120,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 22,
    paddingHorizontal: 14,
    paddingVertical: 10,
    fontSize: 16,
    color: colors.text,
  },
  send: {
    minHeight: 44,
    minWidth: 64,
    borderRadius: 22,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 16,
  },
  sendDisabled: { opacity: 0.4 },
  sendPressed: { backgroundColor: colors.primaryPressed },
  sendText: { color: colors.background, fontSize: 16, fontWeight: '600' },
  closed: { padding: 12, borderTopWidth: 1, borderTopColor: colors.border, backgroundColor: colors.surface },
  closedText: { textAlign: 'center', color: colors.muted, fontSize: 14 },
});
