import { emptyFilters, searchQuery, spreadPins } from '@/lib/explore';
import type { Donation } from '@/lib/types';

jest.mock('@react-native-firebase/auth', () => ({ getAuth: jest.fn() }));

const near = { latitude: 18.5204, longitude: 73.8567 };

function params(query: string) {
  return Object.fromEntries(new URLSearchParams(query).entries());
}

describe('searchQuery', () => {
  it('sends only the paging when nothing is filled in and the location is unknown', () => {
    expect(params(searchQuery(emptyFilters, null))).toEqual({ page: '0', size: '30' });
  });

  it('sends trimmed words, category, city and the rounded location', () => {
    const query = searchQuery({ text: '  school bag ', categoryId: 'c1', city: ' Pune ', maxKm: 10 }, near, 2);
    expect(params(query)).toEqual({
      page: '2',
      size: '30',
      q: 'school bag',
      categoryId: 'c1',
      city: 'Pune',
      lat: '18.52',
      lng: '73.86',
      maxKm: '10',
    });
  });

  it('leaves the distance out when the location is unknown', () => {
    const query = params(searchQuery({ ...emptyFilters, maxKm: 5 }, null));
    expect(query.maxKm).toBeUndefined();
    expect(query.lat).toBeUndefined();
  });
});

describe('spreadPins', () => {
  const at = (id: string, latitude: number, longitude: number) => ({ id, latitude, longitude }) as Donation;

  it('keeps a lone item exactly where it is', () => {
    const [pin] = spreadPins([at('a', 18.52, 73.86)]);
    expect(pin.latitude).toBe(18.52);
    expect(pin.longitude).toBe(73.86);
  });

  it('moves items that share a point apart, by less than 200 metres', () => {
    const pins = spreadPins([at('a', 18.52, 73.86), at('b', 18.52, 73.86), at('c', 18.52, 73.86)]);
    const positions = new Set(pins.map((pin) => `${pin.latitude},${pin.longitude}`));
    expect(positions.size).toBe(3);
    for (const pin of pins) {
      expect(Math.abs(pin.latitude - 18.52)).toBeLessThan(0.002);
      expect(Math.abs(pin.longitude - 73.86)).toBeLessThan(0.002);
    }
  });
});
