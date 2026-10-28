import { useState } from 'react';

export default function ConfirmModal({ open, message, onConfirm, onCancel }) {
  const [busy, setBusy] = useState(false);

  if (!open) return null;

  const handleConfirm = async () => {
    setBusy(true);
    try {
      await onConfirm();
    } finally {
      setBusy(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0, 0, 0, 0.4)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      <div role="dialog" style={{ background: '#fff', padding: '1.5rem', borderRadius: 8 }}>
        <p>{message}</p>
        <button onClick={handleConfirm} disabled={busy}>
          삭제
        </button>
        <button onClick={onCancel} disabled={busy}>
          취소
        </button>
      </div>
    </div>
  );
}
