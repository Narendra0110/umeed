import { fireEvent, render, screen } from '@testing-library/react-native';

import { ChoiceChips, ErrorState, Segmented } from '@/components/ui';

describe('ErrorState', () => {
  it('shows the message and retries when asked', async () => {
    const onRetry = jest.fn();
    await render(<ErrorState message="Cannot reach the Umeed server." onRetry={onRetry} />);

    expect(screen.getByText('Cannot reach the Umeed server.')).toBeTruthy();
    await fireEvent.press(screen.getByRole('button', { name: 'Try again' }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });
});

describe('ChoiceChips', () => {
  const choices = [
    { value: 'NEW', label: 'New' },
    { value: 'GOOD', label: 'Good' },
  ];

  it('marks the chosen value and reports a new choice', async () => {
    const onChange = jest.fn();
    await render(<ChoiceChips label="Condition" choices={choices} value="GOOD" onChange={onChange} />);

    expect(screen.getByRole('radio', { name: 'Good' }).props.accessibilityState).toMatchObject({ selected: true });
    expect(screen.getByRole('radio', { name: 'New' }).props.accessibilityState).toMatchObject({ selected: false });
    await fireEvent.press(screen.getByRole('radio', { name: 'New' }));
    expect(onChange).toHaveBeenCalledWith('NEW');
  });

  it('shows a validation message', async () => {
    await render(
      <ChoiceChips label="Condition" choices={choices} value={undefined} onChange={jest.fn()} error="Choose the condition" />,
    );
    expect(screen.getByText('Choose the condition')).toBeTruthy();
  });
});

describe('Segmented', () => {
  it('switches between the two lists', async () => {
    const onChange = jest.fn();
    await render(
      <Segmented
        choices={[
          { value: 'donations', label: 'My donations' },
          { value: 'claims', label: 'My claims' },
        ]}
        value="donations"
        onChange={onChange}
      />,
    );
    await fireEvent.press(screen.getByRole('tab', { name: 'My claims' }));
    expect(onChange).toHaveBeenCalledWith('claims');
  });
});
