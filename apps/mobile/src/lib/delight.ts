import { File, Paths } from 'expo-file-system';
import * as Haptics from 'expo-haptics';
import { create } from 'zustand';

/**
 * U03 small joys: the one-time welcome slides and the short success moment (a check mark and a light
 * vibration) after posting, claiming, choosing someone and handing over.
 */

const WELCOME_FILE = 'welcome-seen';

/** True once the welcome slides were finished or skipped on this phone. Problems count as seen. */
export function hasSeenWelcome(): boolean {
  try {
    return new File(Paths.document, WELCOME_FILE).exists;
  } catch {
    return true;
  }
}

export function markWelcomeSeen(): void {
  try {
    const file = new File(Paths.document, WELCOME_FILE);
    if (!file.exists) {
      file.create();
    }
  } catch {
    // Not being able to remember only means the welcome shows again next time.
  }
}

type CelebrationState = {
  message: string | null;
  /** Bumped each time, so the same message twice still replays the animation. */
  key: number;
  show: (message: string) => void;
  hide: () => void;
};

export const useCelebration = create<CelebrationState>()((set) => ({
  message: null,
  key: 0,
  show: (message) => set((state) => ({ message, key: state.key + 1 })),
  hide: () => set({ message: null }),
}));

/** Shows the success moment with a light "done" vibration. */
export function celebrate(message: string): void {
  useCelebration.getState().show(message);
  Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success).catch(() => undefined);
}
