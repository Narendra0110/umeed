import { canChoose, needBody, offerableItems } from '@/lib/needs';
import { needSchema, offerSchema } from '@/lib/schemas';
import type { Donation, Need, Offer } from '@/lib/types';

jest.mock('@react-native-firebase/auth', () => ({ getAuth: jest.fn() }));

const need: Need = {
  id: 'n1',
  title: 'School shoes',
  description: 'For my daughter, size 5.',
  status: 'OPEN',
  categoryId: 'c1',
  city: 'Pune',
  area: 'Kothrud',
  latitude: 18.5,
  longitude: 73.8,
  requesterId: 'u1',
  requesterName: 'Asha',
  pendingOfferCount: 1,
  createdAt: '2026-11-18T10:00:00Z',
};

const offer: Offer = {
  id: 'o1',
  needId: 'n1',
  needTitle: 'School shoes',
  donationId: 'd1',
  donationTitle: 'Kids shoes',
  donationStatus: 'AVAILABLE',
  donationImageUrl: null,
  donorId: 'u2',
  donorName: 'Dev',
  message: null,
  status: 'PENDING',
  createdAt: '2026-11-18T10:05:00Z',
};

describe('canChoose', () => {
  it('allows a waiting offer of an available item on an open need', () => {
    expect(canChoose(offer, need)).toBe(true);
  });

  it('refuses when the item was taken, the offer is not waiting or the need is closed', () => {
    expect(canChoose({ ...offer, donationStatus: 'RESERVED' }, need)).toBe(false);
    expect(canChoose({ ...offer, status: 'WITHDRAWN' }, need)).toBe(false);
    expect(canChoose(offer, { ...need, status: 'CLOSED' })).toBe(false);
  });
});

describe('offerableItems', () => {
  it('keeps only items that are still available', () => {
    const items = [
      { id: 'a', status: 'AVAILABLE' },
      { id: 'b', status: 'RESERVED' },
      { id: 'c', status: 'COMPLETED' },
    ] as Donation[];
    expect(offerableItems(items).map((item) => item.id)).toEqual(['a']);
  });
});

describe('needSchema', () => {
  const valid = { title: 'School shoes', description: 'For my daughter, size 5.', categoryId: 'c1', city: 'Pune', area: 'Kothrud' };

  it('accepts a complete need and trims it', () => {
    const result = needSchema.safeParse({ ...valid, title: '  School shoes  ' });
    expect(result.success && result.data.title).toBe('School shoes');
  });

  it('rejects a short title, a short description and a missing category', () => {
    expect(needSchema.safeParse({ ...valid, title: 'ab' }).success).toBe(false);
    expect(needSchema.safeParse({ ...valid, description: 'Too short' }).success).toBe(false);
    expect(needSchema.safeParse({ ...valid, categoryId: '' }).success).toBe(false);
  });

  it('builds the request with the approximate location', () => {
    expect(needBody(valid, { latitude: 18.52, longitude: 73.85 })).toEqual({
      ...valid,
      latitude: 18.52,
      longitude: 73.85,
    });
  });
});

describe('offerSchema', () => {
  it('needs an item and limits the message', () => {
    expect(offerSchema.safeParse({ donationId: '', message: '' }).success).toBe(false);
    expect(offerSchema.safeParse({ donationId: 'd1', message: 'x'.repeat(301) }).success).toBe(false);
    expect(offerSchema.safeParse({ donationId: 'd1', message: '' }).success).toBe(true);
  });
});
