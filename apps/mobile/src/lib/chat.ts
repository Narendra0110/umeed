import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query';

import { apiFetch } from './api';
import type { ChatMessage, ChatThread, Conversation } from './types';

/** How often an open chat checks for new messages, and how often the Chats list refreshes. */
export const CHAT_POLL_MS = 5000;
export const CHATS_POLL_MS = 30000;

const threadKey = (claimId: string) => ['chat', claimId] as const;

/**
 * Adds newly fetched messages to the ones already shown, without duplicates, keeping the server's order.
 * A message sent from this phone may already be shown when the next check returns it again.
 */
export function mergeMessages(current: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const known = new Set(current.map((message) => message.id));
  const added = incoming.filter((message) => !known.has(message.id));
  if (added.length === 0) {
    // Read marks may have changed on messages we already have.
    const updates = new Map(incoming.map((message) => [message.id, message]));
    return current.map((message) => updates.get(message.id) ?? message);
  }
  return [...current, ...added];
}

/** The total number of unread messages across all conversations, for the Chats tab. */
export function totalUnread(conversations: Conversation[] | undefined): number {
  return (conversations ?? []).reduce((sum, conversation) => sum + conversation.unreadCount, 0);
}

export function useConversations(enabled = true) {
  return useQuery({
    queryKey: ['chat', 'list'],
    queryFn: () => apiFetch<Conversation[]>('/api/v1/chats'),
    refetchInterval: CHATS_POLL_MS,
    enabled,
  });
}

/**
 * One conversation, checked every few seconds while the screen is open. After the first load only the
 * newer messages are downloaded and added to what is already shown.
 */
export async function fetchThread(queryClient: QueryClient, claimId: string): Promise<ChatThread> {
  const previous = queryClient.getQueryData<ChatThread>(threadKey(claimId));
  const last = previous?.messages[previous.messages.length - 1];
  const path = `/api/v1/claims/${claimId}/messages${last ? `?after=${last.id}` : ''}`;
  const fresh = await apiFetch<ChatThread>(path);
  if (!previous || !last) {
    return fresh;
  }
  return { ...fresh, messages: mergeMessages(previous.messages, fresh.messages) };
}

export function useChatThread(claimId: string) {
  const queryClient = useQueryClient();
  return useQuery({
    queryKey: threadKey(claimId),
    queryFn: () => fetchThread(queryClient, claimId),
    refetchInterval: CHAT_POLL_MS,
  });
}

export function useSendMessage(claimId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (body: string) =>
      apiFetch<ChatMessage>(`/api/v1/claims/${claimId}/messages`, { method: 'POST', body: { body } }),
    onSuccess: (message) => {
      queryClient.setQueryData<ChatThread>(threadKey(claimId), (thread) =>
        thread ? { ...thread, messages: mergeMessages(thread.messages, [message]) } : thread,
      );
      void queryClient.invalidateQueries({ queryKey: ['chat', 'list'] });
    },
  });
}

/** Marks the other person's messages as read. Failures are ignored; the next visit tries again. */
export function useMarkChatRead(claimId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => apiFetch<void>(`/api/v1/claims/${claimId}/messages/read`, { method: 'POST' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['chat', 'list'] }),
  });
}
