import { ApiError, errorMessage, isAccountNotActive, SUSPENDED_MESSAGE } from '@/lib/api';
import { reportPath } from '@/lib/reports';

jest.mock('@react-native-firebase/auth', () => ({ getAuth: jest.fn() }));

describe('reportPath', () => {
  it('sends listing reports to the donation and need reports to the need', () => {
    expect(reportPath({ kind: 'donation', id: 'd1' })).toBe('/api/v1/donations/d1/reports');
    expect(reportPath({ kind: 'need', id: 'n1' })).toBe('/api/v1/needs/n1/reports');
  });
});

describe('suspended accounts', () => {
  it('turns the backend refusal into a clear explanation', () => {
    const refusal = new ApiError(403, 'Account is not active');
    expect(isAccountNotActive(refusal)).toBe(true);
    expect(errorMessage(refusal)).toBe(SUSPENDED_MESSAGE);
  });

  it('leaves other refusals as they are', () => {
    const other = new ApiError(403, 'Only the donor can remove this donation');
    expect(isAccountNotActive(other)).toBe(false);
    expect(errorMessage(other)).toBe('Only the donor can remove this donation');
    expect(isAccountNotActive(new Error('Account is not active'))).toBe(false);
  });
});
