import { useState } from 'react';

export default function PostForm({ initialPost, onSubmit, onCancel }) {
  const isEdit = Boolean(initialPost);
  const [title, setTitle] = useState(initialPost?.title ?? '');
  const [body, setBody] = useState(initialPost?.body ?? '');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await onSubmit({ title, body });
      if (!isEdit) {
        setTitle('');
        setBody('');
      }
    } catch (err) {
      setError(err.status === 403 ? '권한이 없습니다.' : err.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'grid', gap: 8, margin: '1rem 0' }}>
      <input
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        placeholder="제목"
        maxLength={255}
        required
      />
      <textarea
        value={body}
        onChange={(e) => setBody(e.target.value)}
        placeholder="내용"
        maxLength={10000}
        rows={4}
      />
      {error && <p style={{ color: 'crimson' }}>{error}</p>}
      <div>
        <button type="submit" disabled={submitting}>
          {isEdit ? '수정' : '작성'}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} disabled={submitting}>
            취소
          </button>
        )}
      </div>
    </form>
  );
}
