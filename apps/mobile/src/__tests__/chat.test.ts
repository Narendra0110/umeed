import { getAuth } from '@react-native-firebase/auth';
import { QueryClient } from '@tanstack/react-query';

import { fetchThread, mergeMessages, totalUnread } from '@/lib/chat';
import type { ChatMessage, ChatThread, Conversation } from '@/lib/types';

jest.mock('@react-native-firebase/auth', () => ({ getAuth: jest.fn() }));

const mockedGetAuth = getAuth as jest.Mock;
const fetchMock = jest.fn();

function message(id: string, mine = false, read = false): ChatMessage {
  return { id, mine, body: `Message ${id}`, createdAt: '2026-11-18T10:00:00Z', read };
}

function thread(messages: ChatMessage[]): ChatThread {
  return {
    claimId: 'c1',
    donationId: 'd1',
    donationTitle: 'Lamp',
    otherPartyName: 'Asha',
    open: true,
    messages,
  };
}

function respond(body: unknown) {
  fetchMock.mockResolvedValueOnce({ status: 200, ok: true, text: async () => JSON.stringify(body) });
}

beforeEach(() => {
  fetchMock.mockReset();
  globalThis.fetch = fetchMock as unknown as typeof fetch;
  mockedGetAuth.mockReturnValue({ currentUser: { getIdToken: jest.fn().mockResolvedValue('test-token') } });
});

describe('mergeMessages', () => {
  it('adds new messages after the ones already shown', () => {
    const merged = mergeMessages([message('a'), message('b')], [message('c')]);
    expect(merged.map((m) => m.id)).toEqual(['a', 'b', 'c']);
  });

  it('never shows a message twice, for example one just sent from this phone', () => {
    const merged = mergeMessages([message('a'), message('b', true)], [message('b', true), message('c')]);
    expect(merged.map((m) => m.id)).toEqual(['a', 'b', 'c']);
  });

  it('takes updated read marks when nothing new arrived', () => {
    const merged = mergeMessages([message('a', true, false)], [message('a', true, true)]);
    expect(merged[0].read).toBe(true);
  });
});

describe('totalUnread', () => {
  it('adds up unread messages across conversations', () => {
    const conversations = [{ unreadCount: 2 }, { unreadCount: 0 }, { unreadCount: 3 }] as Conversation[];
    expect(totalUnread(conversations)).toBe(5);
    expect(totalUnread(undefined)).toBe(0);
  });
});

describe('fetchThread', () => {
  it('loads the whole conversation the first time', async () => {
    const queryClient = new QueryClient();
    respond(thread([message('a'), message('b')]));

    const result = await fetchThread(queryClient, 'c1');

    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/v1\/claims\/c1\/messages$/);
    expect(result.messages).toHaveLength(2);
  });

  it('afterwards asks only for messages after the last one and adds them', async () => {
    const queryClient = new QueryClient();
    queryClient.setQueryData(['chat', 'c1'], thread([message('a'), message('b')]));
    respond({ ...thread([message('c')]), open: false });

    const result = await fetchThread(queryClient, 'c1');

    expect(fetchMock.mock.calls[0][0]).toMatch(/\/api\/v1\/claims\/c1\/messages\?after=b$/);
    expect(result.messages.map((m) => m.id)).toEqual(['a', 'b', 'c']);
    // The conversation details (here: closed) come from the newest answer.
    expect(result.open).toBe(false);
  });
});
