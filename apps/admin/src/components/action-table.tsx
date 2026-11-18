import { TargetLink } from '@/components/ui';
import { actionLabels, formatDate } from '@/lib/format';
import type { ActionRow } from '@/lib/types';

/** Rows of the admin log. */
export function ActionTable({ actions }: { actions: ActionRow[] }) {
  if (actions.length === 0) {
    return <p className="muted">Nothing yet.</p>;
  }
  return (
    <table>
      <thead>
        <tr>
          <th>When</th>
          <th>Admin</th>
          <th>Step</th>
          <th>Reason</th>
        </tr>
      </thead>
      <tbody>
        {actions.map((action) => (
          <tr key={action.id}>
            <td>{formatDate(action.createdAt)}</td>
            <td>{action.adminName}</td>
            <td>
              <TargetLink type={action.targetType} id={action.targetId}>
                {actionLabels[action.action] ?? action.action}
              </TargetLink>
            </td>
            <td>{action.note ?? '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
