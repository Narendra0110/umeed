import { claimSchema, donationSchema, MAX_PHOTOS, reportSchema } from '@/lib/schemas';

const photo = (n: number) => ({ uri: `file:///photo-${n}.jpg`, name: `photo-${n}.jpg`, type: 'image/jpeg' });

const validDonation = {
  photos: [photo(1)],
  title: 'School bag',
  description: 'Blue school bag, used for one year.',
  categoryId: '7d8a1e1c-0000-4000-8000-000000000001',
  itemCondition: 'GOOD',
  city: 'Pune',
  area: 'Kothrud',
  pickupAddress: 'Flat 4, Shanti Apartments, near the bus stop',
};

function fieldErrors(result: { success: boolean; error?: { issues: { path: PropertyKey[]; message: string }[] } }) {
  return Object.fromEntries((result.error?.issues ?? []).map((issue) => [String(issue.path[0]), issue.message]));
}

describe('donationSchema', () => {
  it('accepts a complete donation and trims text', () => {
    const result = donationSchema.safeParse({ ...validDonation, title: '  School bag  ' });
    expect(result.success).toBe(true);
    expect(result.data?.title).toBe('School bag');
  });

  it('needs at least one photo and at most five', () => {
    expect(fieldErrors(donationSchema.safeParse({ ...validDonation, photos: [] })).photos).toBe(
      'Add at least one photo',
    );
    const six = Array.from({ length: MAX_PHOTOS + 1 }, (_, i) => photo(i));
    expect(fieldErrors(donationSchema.safeParse({ ...validDonation, photos: six })).photos).toBe(
      'You can add at most 5 photos',
    );
  });

  it('uses the same length limits as the backend', () => {
    const errors = fieldErrors(
      donationSchema.safeParse({
        ...validDonation,
        title: 'ab',
        description: 'too short',
        pickupAddress: 'short',
        city: 'x'.repeat(101),
      }),
    );
    expect(errors.title).toBe('Title must be between 3 and 100 characters');
    expect(errors.description).toBe('Description must be between 10 and 1000 characters');
    expect(errors.pickupAddress).toBe('Pickup address must be between 10 and 500 characters');
    expect(errors.city).toBe('City must be at most 100 characters');
  });

  it('counts only real characters, not surrounding spaces', () => {
    expect(fieldErrors(donationSchema.safeParse({ ...validDonation, area: '   ' })).area).toBe('Area is required');
  });

  it('needs a category and a known condition', () => {
    const errors = fieldErrors(
      donationSchema.safeParse({ ...validDonation, categoryId: '', itemCondition: 'BROKEN' }),
    );
    expect(errors.categoryId).toBe('Choose a category');
    expect(errors.itemCondition).toBe('Choose the condition');
  });
});

describe('claimSchema', () => {
  it('allows an empty message', () => {
    expect(claimSchema.safeParse({ message: '' }).success).toBe(true);
  });

  it('limits the message to 300 characters', () => {
    expect(fieldErrors(claimSchema.safeParse({ message: 'x'.repeat(301) })).message).toBe(
      'Message must be at most 300 characters',
    );
  });
});

describe('reportSchema', () => {
  it('makes details optional for a listed reason', () => {
    expect(reportSchema.safeParse({ reason: 'SELLING_OR_ASKING_MONEY', details: '' }).success).toBe(true);
  });

  it('needs details when the reason is OTHER', () => {
    expect(fieldErrors(reportSchema.safeParse({ reason: 'OTHER', details: '  ' })).details).toBe(
      'Please describe the problem',
    );
    expect(reportSchema.safeParse({ reason: 'OTHER', details: 'The photos are of a different item' }).success).toBe(
      true,
    );
  });

  it('needs a reason', () => {
    expect(fieldErrors(reportSchema.safeParse({ details: '' })).reason).toBe('Choose a reason');
  });
});
