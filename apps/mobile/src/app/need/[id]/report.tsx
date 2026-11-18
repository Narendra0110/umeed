import { useLocalSearchParams } from 'expo-router';

import { ReportForm } from '@/components/report-form';

/** Report a need (F06). The person who asked is never told who reported it. */
export default function ReportNeedScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <ReportForm target={{ kind: 'need', id }} />;
}
