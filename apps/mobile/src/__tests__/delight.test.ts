import * as Haptics from 'expo-haptics';

import { celebrate, hasSeenWelcome, markWelcomeSeen, useCelebration } from '@/lib/delight';

// A stand-in for expo-file-system: remembered files, or a broken file system.
const mockFiles = new Set<string>();
let mockBroken = false;
jest.mock('expo-file-system', () => ({
  Paths: { document: 'doc' },
  File: class {
    private readonly name: string;
    constructor(_dir: string, name: string) {
      if (mockBroken) throw new Error('no storage');
      this.name = name;
    }
    get exists() {
      return mockFiles.has(this.name);
    }
    create() {
      mockFiles.add(this.name);
    }
  },
}));
jest.mock('expo-haptics', () => ({
  notificationAsync: jest.fn(() => Promise.resolve()),
  NotificationFeedbackType: { Success: 'success' },
}));

beforeEach(() => {
  mockFiles.clear();
  mockBroken = false;
  useCelebration.setState({ message: null, key: 0 });
});

describe('the welcome slides', () => {
  it('show until they are finished once', () => {
    expect(hasSeenWelcome()).toBe(false);
    markWelcomeSeen();
    expect(hasSeenWelcome()).toBe(true);
    markWelcomeSeen();
    expect(hasSeenWelcome()).toBe(true);
  });

  it('are skipped rather than shown forever when storage fails', () => {
    mockBroken = true;
    expect(hasSeenWelcome()).toBe(true);
    expect(() => markWelcomeSeen()).not.toThrow();
  });
});

describe('the success moment', () => {
  it('shows the message with a success vibration and replays for the same message', () => {
    celebrate('Claim sent!');
    expect(useCelebration.getState().message).toBe('Claim sent!');
    expect(Haptics.notificationAsync).toHaveBeenCalledWith('success');
    const first = useCelebration.getState().key;
    celebrate('Claim sent!');
    expect(useCelebration.getState().key).toBe(first + 1);
    useCelebration.getState().hide();
    expect(useCelebration.getState().message).toBeNull();
  });
});
