import { useEffect, useState } from 'react';
import { fetchUsers, logout } from '../api/client.js';

export default function UserList({ onLogout }) {
  const [users, setUsers] = useState([]);
  const [nextCursor, setNextCursor] = useState(null);
  const [hasNext, setHasNext] = useState(false);
  const [error, setError] = useState(null);

  const load = async (cursor) => {
    try {
      const page = await fetchUsers(cursor);
      setUsers((prev) => [...prev, ...page.content]);
      setNextCursor(page.nextCursor);
      setHasNext(page.hasNext);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    load(null);
  }, []);

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 서버 로그아웃이 실패해도 로컬 토큰은 삭제된다.
    }
    onLogout();
  };

  return (
    <section>
      <button onClick={handleLogout}>로그아웃</button>
      {error && <p style={{ color: 'crimson' }}>{error}</p>}
      <ul>
        {users.map((u) => (
          <li key={u.id}>
            {u.username} ({u.email}) - {u.wins}승 {u.losses}패
          </li>
        ))}
      </ul>
      {hasNext && <button onClick={() => load(nextCursor)}>더 보기</button>}
    </section>
  );
}
