import { donationFormData } from '@/lib/donations';
import { pickupRole } from '@/lib/pickup';
import type { DonationInput } from '@/lib/schemas';
import type { Pickup } from '@/lib/types';

jest.mock('@react-native-firebase/auth', () => ({ getAuth: jest.fn() }));
// expo-file-system's File is native; this stand-in only remembers which photo it points to.
jest.mock('expo-file-system', () => ({
  File: class MockFile {
    uri: string;
    constructor(mockUri: string) {
      this.uri = mockUri;
    }
  },
}));

/**
 * The test environment's FormData is not the phone's, so this stand-in records exactly what the
 * app appends.
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

  it('adds each photo as a file object named "photos", cover first', () => {
    const form = donationFormData(input, { latitude: 18.5, longitude: 73.8 });
    const files = partsOf(form)
      .filter(([name]) => name === 'photos')
      .map(([, value]) => value);
    // A plain { uri, name, type } object is rejected by Expo's fetch on the phone, so each photo
    // must be a File, not a plain object.
    expect(files.map((f) => (f as { constructor: { name: string } }).constructor.name)).toEqual([
      'MockFile',
      'MockFile',
    ]);
    expect(files.map((f) => (f as { uri: string }).uri)).toEqual(['file:///a.jpg', 'file:///b.jpg']);
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
