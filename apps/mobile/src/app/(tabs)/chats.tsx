import { router } from 'expo-router';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ListRow } from '@/components/list-row';
import { ListSkeleton } from '@/components/skeleton';
import { colors, EmptyState, ErrorState, Pill, Title } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { useConversations } from '@/lib/chat';
import { timeAgo } from '@/lib/format';
import { t, useT } from '@/lib/i18n';
import type { Conversation } from '@/lib/types';

/** Chats: one conversation per handover, newest activity first. */
export default function ChatsScreen() {
  const m = useT();
  const conversations = useConversations();

  return (
    <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
      <View style={styles.header}>
        <Title>{m.chats.title}</Title>
      </View>
      {conversations.isPending ? (
        <ListSkeleton />
      ) : conversations.isError ? (
        <ErrorState
          message={errorMessage(conversations.error)}
          onRetry={() => conversations.refetch()}
          retrying={conversations.isFetching}
        />
      ) : (
        <FlatList
          data={conversations.data}
          keyExtractor={(conversation) => conversation.claimId}
          contentContainerStyle={styles.list}
          refreshControl={
            <RefreshControl refreshing={conversations.isRefetching} onRefresh={() => void conversations.refetch()} />
          }
          ListEmptyComponent={
            <EmptyState
              emoji="💬"
              title={m.chats.emptyTitle}
              message={m.chats.emptyMessage}
            />
          }
          renderItem={({ item }) => (
            <ListRow
              title={`${item.otherPartyName} · ${item.donationTitle}`}
              subtitle={preview(item)}
              extra={<ConversationPill conversation={item} />}
              onPress={() => router.push(`/chat/${item.claimId}`)}
            />
          )}
        />
      )}
    </SafeAreaView>
  );
}

function preview(conversation: Conversation): string {
  const m = t().chats;
  if (!conversation.lastMessage || !conversation.lastMessageAt) {
    return conversation.open ? m.sayHello : m.noMessages;
  }
  const who = conversation.lastMessageMine ? m.you : '';
  return `${who}${conversation.lastMessage} · ${timeAgo(conversation.lastMessageAt)}`;
}

function ConversationPill({ conversation }: { conversation: Conversation }) {
  const m = useT();
  if (conversation.unreadCount > 0) {
    return <Pill label={m.chats.newCount(conversation.unreadCount)} tone="info" />;
  }
  if (!conversation.open) {
    return <Pill label={m.chats.closed} tone="neutral" />;
  }
  return null;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8 },
  list: { padding: 16, gap: 10, flexGrow: 1 },
});
