import { router } from 'expo-router';
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ListRow } from '@/components/list-row';
import { colors, EmptyState, ErrorState, Loading, Pill, Title } from '@/components/ui';
import { errorMessage } from '@/lib/api';
import { useConversations } from '@/lib/chat';
import { timeAgo } from '@/lib/format';
import type { Conversation } from '@/lib/types';

/** Chats: one conversation per handover, newest activity first. */
export default function ChatsScreen() {
  const conversations = useConversations();

  return (
    <SafeAreaView style={styles.safe} edges={['top', 'left', 'right']}>
      <View style={styles.header}>
        <Title>Chats</Title>
      </View>
      {conversations.isPending ? (
        <Loading />
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
              title="No chats yet"
              message="When a donor chooses you, or you choose someone for your item, you can message each other here."
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
  if (!conversation.lastMessage || !conversation.lastMessageAt) {
    return conversation.open ? 'Say hello and arrange the pickup.' : 'No messages';
  }
  const who = conversation.lastMessageMine ? 'You: ' : '';
  return `${who}${conversation.lastMessage} · ${timeAgo(conversation.lastMessageAt)}`;
}

function ConversationPill({ conversation }: { conversation: Conversation }) {
  if (conversation.unreadCount > 0) {
    return <Pill label={`${conversation.unreadCount} new`} tone="info" />;
  }
  if (!conversation.open) {
    return <Pill label="Closed" tone="neutral" />;
  }
  return null;
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  header: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 8 },
  list: { padding: 16, gap: 10, flexGrow: 1 },
});
