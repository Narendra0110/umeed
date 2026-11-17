import { donationFormData } from '@/lib/donations';
import { pickupRole } from '@/lib/pickup';
import type { DonationInput } from '@/lib/schemas';
import type { Pickup } from '@/lib/types';

jest.mock('@react-native-firebase/auth', () => ({ getAuth: jest.fn() }));

/**
 * The test environment's FormData is not React Native's (which accepts { uri, name, type } files),
 * so this stand-in records exactly what the app appends.
 */
class RecordingFormData {
  readonly parts: [string, unknown][] = [];
  append(name: string, value: unknown) {
    this.parts.push([name, value]);
  }
}

describe('donationFormData', () => {
  const realFormData = globalThis.FormData;
  beforeAll(() => {
    globalThis.FormData = RecordingFormData as unknown as typeof FormData;
  });
  afterAll(() => {
    globalThis.FormData = realFormData;
  });

  const input: DonationInput = {
    photos: [
      { uri: 'file:///a.jpg', name: 'a.jpg', type: 'image/jpeg' },
      { uri: 'file:///b.jpg', name: 'b.jpg', type: 'image/jpeg' },
    ],
    title: 'School bag',
    description: 'Blue school bag, used for one year.',
    categoryId: 'cat-1',
    itemCondition: 'LIKE_NEW',
    city: 'Pune',
    area: 'Kothrud',
    pickupAddress: 'Flat 4, Shanti Apartments',
  };

  const partsOf = (form: FormData) => (form as unknown as RecordingFormData).parts;

  it('has every field the backend expects, with the coordinates as text', () => {
    const form = donationFormData(input, { latitude: 18.5074, longitude: 73.8077 });
    const fields = Object.fromEntries(partsOf(form).filter(([name]) => name !== 'photos'));
    expect(fields).toEqual({
      categoryId: 'cat-1',
      title: 'School bag',
      description: 'Blue school bag, used for one year.',
      itemCondition: 'LIKE_NEW',
      city: 'Pune',
      area: 'Kothrud',
      latitude: '18.5074',
      longitude: '73.8077',
      pickupAddress: 'Flat 4, Shanti Apartments',
    });
  });

  it('adds each photo as a file named "photos", cover first', () => {
    const form = donationFormData(input, { latitude: 18.5, longitude: 73.8 });
    const files = partsOf(form)
      .filter(([name]) => name === 'photos')
      .map(([, value]) => value);
    expect(files).toEqual([
      { uri: 'file:///a.jpg', name: 'a.jpg', type: 'image/jpeg' },
      { uri: 'file:///b.jpg', name: 'b.jpg', type: 'image/jpeg' },
    ]);
  });
});

describe('pickupRole', () => {
  const pickup: Pickup = {
    donationId: 'd1',
    donationTitle: 'School bag',
    status: 'RESERVED',
    pickupAddress: 'Flat 4',
    donor: { name: 'Asha', phoneNumber: '+919876543210' },
    receiver: { name: 'Ravi', phoneNumber: '+919812345678' },
    donorConfirmed: false,
    receiverConfirmed: false,
  };

  it('finds which side the signed-in user is on by phone number', () => {
    expect(pickupRole(pickup, '+919876543210')).toBe('donor');
    expect(pickupRole(pickup, '+919812345678')).toBe('receiver');
  });

  it('returns null for anyone else or when the profile is not loaded', () => {
    expect(pickupRole(pickup, '+919000000000')).toBeNull();
    expect(pickupRole(pickup, undefined)).toBeNull();
  });
});
