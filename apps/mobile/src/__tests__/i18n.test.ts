import { categoryLabel, conditionLabel, formatDistance, timeAgo } from '@/lib/format';
import { messagesFor, t, useLanguageStore } from '@/lib/i18n';
import { languageAction } from '@/lib/profile';
import { donationSchema } from '@/lib/schemas';

jest.mock('@react-native-firebase/auth', () => ({ getAuth: jest.fn() }));

/** Every text in the dictionary, flattened to "path: value"; functions are called with sample values. */
function flatten(value: unknown, path = ''): [string, string][] {
  if (typeof value === 'string') {
    return [[path, value]];
  }
  if (typeof value === 'function') {
    return [[path, String((value as (...args: unknown[]) => unknown)('X', 'Y', 'Z', 'W'))]];
  }
  return Object.entries(value as object).flatMap(([key, inner]) => flatten(inner, path ? `${path}.${key}` : key));
}

afterEach(() => useLanguageStore.setState({ language: 'en', chosen: false }));

describe('the Hindi texts', () => {
  const english = new Map(flatten(messagesFor('en')));
  const hindi = new Map(flatten(messagesFor('hi')));

  it('cover exactly the same texts as English', () => {
    expect([...hindi.keys()].sort()).toEqual([...english.keys()].sort());
  });

  it('are never empty', () => {
    for (const [path, text] of hindi) {
      expect({ path, empty: text.trim() === '' }).toEqual({ path, empty: false });
    }
  });
});

describe('switching language', () => {
  const now = new Date('2026-11-18T12:00:00Z');

  it('changes labels, times and distances everywhere', () => {
    expect(conditionLabel('LIKE_NEW')).toBe('Like new');
    expect(timeAgo('2026-11-18T09:00:00Z', now)).toBe('3 h ago');
    expect(formatDistance(4.6)).toBe('About 5 km away');

    useLanguageStore.getState().setLanguage('hi', true);
    expect(conditionLabel('LIKE_NEW')).toBe('लगभग नया');
    expect(timeAgo('2026-11-18T09:00:00Z', now)).toBe('3 घंटे पहले');
    expect(timeAgo('2026-11-17T11:00:00Z', now)).toBe('1 दिन पहले');
    expect(formatDistance(0.4)).toBe('1 किमी के अंदर');
    expect(useLanguageStore.getState().chosen).toBe(true);
  });

  it('shows known categories in the current language and unknown ones as the server names them', () => {
    useLanguageStore.getState().setLanguage('hi');
    expect(categoryLabel({ slug: 'clothes', name: 'Clothes' })).toBe('कपड़े');
    expect(categoryLabel({ slug: 'garden', name: 'Garden' })).toBe('Garden');
  });

  it('gives form errors in the language in use when the form is checked', () => {
    const empty = { photos: [], title: '', description: '', categoryId: '', city: '', area: '', pickupAddress: '' };
    const photoError = () => donationSchema.safeParse(empty).error?.issues.find((i) => i.path[0] === 'photos')?.message;
    expect(photoError()).toBe('Add at least one photo');
    useLanguageStore.getState().setLanguage('hi');
    expect(photoError()).toBe(t().form.addPhoto);
    expect(photoError()).toBe('कम से कम एक फ़ोटो जोड़ें');
  });
});

describe('languageAction', () => {
  const account = (language: 'en' | 'hi', profileComplete = true) => ({ language, profileComplete });

  it('does nothing when the account and the app agree', () => {
    expect(languageAction(account('hi'), 'hi', false)).toBeNull();
  });

  it('saves the app language for a new account', () => {
    expect(languageAction(account('en', false), 'hi', false)).toEqual({ save: 'hi' });
  });

  it('saves a language picked on this phone before logging in', () => {
    expect(languageAction(account('en'), 'hi', true)).toEqual({ save: 'hi' });
  });

  it('otherwise switches the app to the language saved on the account', () => {
    expect(languageAction(account('hi'), 'en', false)).toEqual({ use: 'hi' });
  });
});
