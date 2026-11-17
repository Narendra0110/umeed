import {
  distanceKm,
  formatDistance,
  groupMyClaims,
  groupMyDonations,
  timeAgo,
} from '@/lib/format';
import type { ClaimStatus, DonationStatus } from '@/lib/types';

describe('groupMyDonations', () => {
  const item = (id: string, status: DonationStatus) => ({ id, status });

  it('orders sections available, reserved, given away and keeps item order', () => {
    const sections = groupMyDonations([
      item('a', 'COMPLETED'),
      item('b', 'AVAILABLE'),
      item('c', 'RESERVED'),
      item('d', 'AVAILABLE'),
    ]);
    expect(sections.map((s) => s.key)).toEqual(['available', 'reserved', 'completed']);
    expect(sections[0].data.map((d) => d.id)).toEqual(['b', 'd']);
  });

  it('leaves out empty sections and removed items', () => {
    const sections = groupMyDonations([item('a', 'AVAILABLE'), item('b', 'REMOVED')]);
    expect(sections.map((s) => s.key)).toEqual(['available']);
  });

  it('returns no sections for no items', () => {
    expect(groupMyDonations([])).toEqual([]);
  });
});

describe('groupMyClaims', () => {
  const claim = (id: string, status: ClaimStatus) => ({ id, status });

  it('puts accepted first, then waiting, then rejected and withdrawn together', () => {
    const sections = groupMyClaims([
      claim('a', 'PENDING'),
      claim('b', 'REJECTED'),
      claim('c', 'ACCEPTED'),
      claim('d', 'WITHDRAWN'),
    ]);
    expect(sections.map((s) => s.key)).toEqual(['accepted', 'pending', 'closed']);
    expect(sections[2].data.map((c) => c.id)).toEqual(['b', 'd']);
  });
});

describe('distanceKm', () => {
  it('is zero for the same place', () => {
    expect(distanceKm({ latitude: 18.52, longitude: 73.85 }, { latitude: 18.52, longitude: 73.85 })).toBe(0);
  });

  it('matches the known distance between Pune and Mumbai (about 120 km)', () => {
    const km = distanceKm({ latitude: 18.52, longitude: 73.86 }, { latitude: 19.08, longitude: 72.88 });
    expect(km).toBeGreaterThan(115);
    expect(km).toBeLessThan(125);
  });
});

describe('formatDistance', () => {
  it('says within 1 km below one kilometre, because coordinates are rounded to about 1 km', () => {
    expect(formatDistance(0.4)).toBe('Within 1 km');
  });

  it('rounds to whole kilometres', () => {
    expect(formatDistance(3.6)).toBe('About 4 km away');
  });
});

describe('timeAgo', () => {
  const now = new Date('2026-11-17T12:00:00Z');
  const ago = (seconds: number) => new Date(now.getTime() - seconds * 1000).toISOString();

  it('describes recent times', () => {
    expect(timeAgo(ago(30), now)).toBe('Just now');
    expect(timeAgo(ago(5 * 60), now)).toBe('5 min ago');
    expect(timeAgo(ago(3 * 3600), now)).toBe('3 h ago');
    expect(timeAgo(ago(26 * 3600), now)).toBe('1 day ago');
    expect(timeAgo(ago(3 * 86400), now)).toBe('3 days ago');
  });

  it('treats a time slightly in the future as just now', () => {
    expect(timeAgo(ago(-20), now)).toBe('Just now');
  });

  it('shows a date after a week', () => {
    expect(timeAgo('2026-10-01T10:00:00Z', now)).toMatch(/2026/);
  });
});
