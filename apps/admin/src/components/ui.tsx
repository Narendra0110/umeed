'use client';

import Link from 'next/link';
import { type ReactNode, useEffect, useState } from 'react';

import { errorMessage, fetchImage } from '@/lib/api';
import { targetHref } from '@/lib/format';

export function Loading() {
  return <p className="muted">Loading…</p>;
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="error" role="alert">
      <span>{message}</span>
      {onRetry ? (
        <button type="button" className="button secondary small" onClick={onRetry}>
          Try again
        </button>
      ) : null}
    </div>
  );
}

const pillTones: Record<string, string> = {
  OPEN: 'warn',
  AVAILABLE: 'ok',
  ACTIVE: 'ok',
  RESERVED: 'info',
  COMPLETED: 'info',
  FULFILLED: 'info',
  REVIEWED: 'info',
  DISMISSED: 'neutral',
  CLOSED: 'neutral',
  REMOVED: 'bad',
  SUSPENDED: 'bad',
  DELETED: 'neutral',
  ADMIN: 'info',
};

export function Pill({ value }: { value: string }) {
  return <span className={`pill ${pillTones[value] ?? 'neutral'}`}>{value.toLowerCase()}</span>;
}

export function TargetLink({ type, id, children }: { type: string; id: string; children: ReactNode }) {
  const href = targetHref(type, id);
  return href ? <Link href={href}>{children}</Link> : <>{children}</>;
}

/**
 * A button that asks for a reason before acting. The reason is saved in the admin log.
 * onConfirm should throw on failure; its message is shown under the box.
 */
export function NoteAction({
  label,
  confirmLabel,
  tone = 'danger',
  noteRequired = true,
  placeholder = 'Reason (saved in the admin log)',
  onConfirm,
}: {
  label: string;
  confirmLabel: string;
  tone?: 'danger' | 'primary' | 'secondary';
  noteRequired?: boolean;
  placeholder?: string;
  onConfirm: (note: string) => Promise<void>;
}) {
  const [open, setOpen] = useState(false);
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) {
    return (
      <button type="button" className={`button ${tone}`} onClick={() => setOpen(true)}>
        {label}
      </button>
    );
  }

  const submit = async () => {
    if (noteRequired && !note.trim()) {
      setError('Please give a reason.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await onConfirm(note.trim());
      setOpen(false);
      setNote('');
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="note-action">
      <textarea
        value={note}
        maxLength={500}
        rows={3}
        placeholder={noteRequired ? placeholder : 'Note (optional)'}
        onChange={(event) => setNote(event.target.value)}
        autoFocus
      />
      <div className="row">
        <button type="button" className={`button ${tone}`} onClick={submit} disabled={busy}>
          {busy ? 'Working…' : confirmLabel}
        </button>
        <button type="button" className="button secondary" onClick={() => setOpen(false)} disabled={busy}>
          Cancel
        </button>
      </div>
      {error ? <p className="field-error">{error}</p> : null}
    </div>
  );
}

/** A listing photo. Photos need the admin's token, so they are fetched and shown from a temporary URL. */
export function AuthImage({ path, alt }: { path: string; alt: string }) {
  const [url, setUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let created: string | null = null;
    let current = true;
    fetchImage(path)
      .then((objectUrl) => {
        created = objectUrl;
        if (current) {
          setUrl(objectUrl);
        } else {
          URL.revokeObjectURL(objectUrl);
        }
      })
      .catch(() => current && setFailed(true));
    return () => {
      current = false;
      if (created) {
        URL.revokeObjectURL(created);
      }
    };
  }, [path]);

  if (failed) {
    return <div className="photo missing">Photo not available</div>;
  }
  if (!url) {
    return <div className="photo missing">Loading photo…</div>;
  }
  // A plain img: next/image cannot load a temporary blob URL.
  return <img className="photo" src={url} alt={alt} />;
}

export function Pager({
  page,
  totalPages,
  onChange,
}: {
  page: number;
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <div className="row pager">
      <button type="button" className="button secondary small" disabled={page === 0} onClick={() => onChange(page - 1)}>
        Previous
      </button>
      <span className="muted">
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        className="button secondary small"
        disabled={page + 1 >= totalPages}
        onClick={() => onChange(page + 1)}>
        Next
      </button>
    </div>
  );
}
