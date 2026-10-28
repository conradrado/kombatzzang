import { useEffect, useState } from 'react';
import { fetchFollowers, fetchFollowings, fetchUser, follow, getCurrentUser, unfollow } from '../api/client.js';
import ProfileEditForm from './ProfileEditForm.jsx';

export default function UserProfile({ userId, onBack, onOpenFollows }) {
  const me = getCurrentUser();
  const isMe = me != null && me.id === userId;
  const isAdmin = me != null && me.role === 'ADMIN';

  const [user, setUser] = useState(null);
  const [followers, setFollowers] = useState([]);
  const [followingCount, setFollowingCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [pending, setPending] = useState(false);
  const [followError, setFollowError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    setFollowError(null);
    Promise.all([fetchUser(userId), fetchFollowers(userId), fetchFollowings(userId)])
      .then(([u, fs, fg]) => {
        if (cancelled) return;
        setUser(u);
        setFollowers(fs);
        setFollowingCount(fg.length);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [userId]);

  const isFollowing = me != null && followers.some((f) => f.followerId === me.id);

  const handleFollow = async () => {
    setPending(true);
    setFollowError(null);
    try {
      if (isFollowing) {
        await unfollow(userId);
      } else {
        await follow(userId);
      }
      setFollowers(await fetchFollowers(userId));
    } catch (err) {
      setFollowError(err.message);
    } finally {
      setPending(false);
    }
  };

  return (
    <section>
      <button onClick={onBack}>목록으로</button>
      {loading && <p>불러오는 중...</p>}
      {error && <p style={{ color: 'crimson' }}>{error}</p>}
      {user && !loading && (
        <>
          <h2>{user.username}</h2>
          <p>이메일: {user.email || '-'}</p>
          <p>종목: {user.sport || '-'} / 체급: {user.weightClass || '-'}</p>
          <p>소개: {user.bio || '-'}</p>
          <p>전적: {user.wins}승 {user.losses}패 ({user.level || '-'})</p>
          <p>지역: {user.region || '-'} / 활동 지역: {user.activityRegion || '-'}</p>
          <p>
            <button onClick={() => onOpenFollows(userId, 'followers')}>팔로워 {followers.length}</button>{' '}
            <button onClick={() => onOpenFollows(userId, 'followings')}>팔로잉 {followingCount}</button>
          </p>
          {!isMe && (
            <button onClick={handleFollow} disabled={pending}>
              {isFollowing ? '언팔로우' : '팔로우'}
            </button>
          )}
          {followError && <p style={{ color: 'crimson' }}>{followError}</p>}
          {(isMe || isAdmin) && <ProfileEditForm user={user} isAdmin={isAdmin} onUpdated={setUser} />}
        </>
      )}
    </section>
  );
}
