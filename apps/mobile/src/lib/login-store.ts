import { signInWithPhoneNumber } from '@react-native-firebase/auth';
import { create } from 'zustand';

type Confirmation = Awaited<ReturnType<typeof signInWithPhoneNumber>>;

type LoginState = {
  phoneNumber: string | null;
  confirmation: Confirmation | null;
  setPending: (phoneNumber: string, confirmation: Confirmation) => void;
  clear: () => void;
};

/** Holds the pending OTP confirmation between the phone screen and the code screen. */
export const useLoginStore = create<LoginState>()((set) => ({
  phoneNumber: null,
  confirmation: null,
  setPending: (phoneNumber, confirmation) => set({ phoneNumber, confirmation }),
  clear: () => set({ phoneNumber: null, confirmation: null }),
}));
