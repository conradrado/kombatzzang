import { useEffect, useState } from 'react';
import { fetchFollowers, fetchFollowings, fetchUser } from '../api/client.js';

export default function FollowList({ userId, type, onBack, onSelectUser }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const isFollowers = type === 'followers';

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    (async () => {
      try {
        const follows = await (isFollowers ? fetchFollowers(userId) : fetchFollowings(userId));
        const result = await Promise.all(
          follows.map(async (f) => {
            const id = isFollowers ? f.followerId : f.followingId;
            try {
              const u = await fetchUser(id);
              return { id, name: u.username };
            } catch {
              return { id, name: `ID ${id}` };
            }
          }),
        );
        if (!cancelled) setItems(result);
      } catch (err) {
        if (!cancelled) setError(err.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [userId, type]);

  return (
    <section>
      <button onClick={onBack}>뒤로</button>
      <h2>{isFollowers ? '팔로워' : '팔로잉'}</h2>
      {loading && <p>불러오는 중...</p>}
      {error && <p style={{ color: 'crimson' }}>{error}</p>}
      {!loading && !error && items.length === 0 && (
        <p>{isFollowers ? '팔로워가 없습니다.' : '팔로잉한 유저가 없습니다.'}</p>
      )}
      <ul>
        {items.map((i) => (
          <li key={i.id}>
            <a href="#" onClick={(e) => { e.preventDefault(); onSelectUser(i.id); }}>{i.name}</a>
          </li>
        ))}
      </ul>
    </section>
  );
}
