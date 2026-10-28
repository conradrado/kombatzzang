import { useEffect, useState } from 'react';
import { createPost, deletePost, fetchPosts, getCurrentUser, updatePost } from '../api/client.js';
import ConfirmModal from './ConfirmModal.jsx';
import PostForm from './PostForm.jsx';

const toMessage = (err) => (err.status === 403 ? '권한이 없습니다.' : err.message);

export default function PostFeed() {
  const [posts, setPosts] = useState([]);
  const [nextCursor, setNextCursor] = useState(null);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [deleteError, setDeleteError] = useState(null);
  const [user] = useState(() => getCurrentUser());

  const load = async (cursor) => {
    setLoading(true);
    try {
      const page = await fetchPosts(cursor);
      setPosts((prev) => [...prev, ...page.content]);
      setNextCursor(page.nextCursor);
      setHasNext(page.hasNext);
      setError(null);
    } catch (err) {
      setError(toMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load(null);
  }, []);

  const handleCreate = async (values) => {
    const created = await createPost(values);
    setPosts((prev) => [created, ...prev]);
  };

  const handleUpdate = async (postId, values) => {
    const updated = await updatePost(postId, values);
    setPosts((prev) => prev.map((p) => (p.id === postId ? updated : p)));
    setEditingId(null);
  };

  const handleDelete = async () => {
    try {
      await deletePost(deleting.id);
      setPosts((prev) => prev.filter((p) => p.id !== deleting.id));
      setDeleteError(null);
    } catch (err) {
      setDeleteError(toMessage(err));
    }
    setDeleting(null);
  };

  const isAdmin = user?.role === 'ADMIN';

  return (
    <section>
      <PostForm onSubmit={handleCreate} />
      {error && <p style={{ color: 'crimson' }}>{error}</p>}
      {deleteError && <p style={{ color: 'crimson' }}>{deleteError}</p>}
      {loading && <p>불러오는 중...</p>}
      {!loading && !error && posts.length === 0 && <p>게시글이 없습니다.</p>}
      {posts.map((post) => (
        <article
          key={post.id}
          style={{ border: '1px solid #ccc', borderRadius: 8, padding: '1rem', margin: '0.5rem 0' }}
        >
          {editingId === post.id ? (
            <PostForm
              initialPost={post}
              onSubmit={(values) => handleUpdate(post.id, values)}
              onCancel={() => setEditingId(null)}
            />
          ) : (
            <>
              <h3>{post.title}</h3>
              <p>{post.body}</p>
              <small>
                작성자 {post.userId} · {post.createdAt}
              </small>
              <div>
                {(user?.role === 'ADMIN' || user?.userId === post.userId) && (
                  <button onClick={() => setEditingId(post.id)}>수정</button>
                )}
                {isAdmin && (
                  <button
                    onClick={() => {
                      setDeleteError(null);
                      setDeleting(post);
                    }}
                  >
                    삭제
                  </button>
                )}
              </div>
            </>
          )}
        </article>
      ))}
      {hasNext && !loading && <button onClick={() => load(nextCursor)}>더 보기</button>}
      <ConfirmModal
        open={deleting !== null}
        message="게시글을 삭제하시겠습니까?"
        onConfirm={handleDelete}
        onCancel={() => setDeleting(null)}
      />
    </section>
  );
}
