import { useLocalSearchParams } from 'expo-router';

import { ReportForm } from '@/components/report-form';

/** Report a listing. The donor is never told who reported it. */
export default function ReportListingScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <ReportForm target={{ kind: 'donation', id }} />;
}
