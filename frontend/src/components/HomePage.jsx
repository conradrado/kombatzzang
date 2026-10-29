import { useEffect, useState } from 'react';
import {
  fetchActiveUsers,
  fetchPosts,
  fetchUser,
  getCurrentUserId,
  logout,
} from '../api/client.js';
import LoginForm from './LoginForm.jsx';
import SignupForm from './SignupForm.jsx';

function winRate(user) {
  const total = user.wins + user.losses;
  return total === 0 ? '-' : `${Math.round((user.wins / total) * 100)}%`;
}

export default function HomePage({ token, onLogin, onLogout, onSelectUser }) {
  const [mode, setMode] = useState(null);
  const [me, setMe] = useState(null);
  const [meLoading, setMeLoading] = useState(!!token);
  const [meError, setMeError] = useState(null);
  const [activeUsers, setActiveUsers] = useState([]);
  const [activeLoading, setActiveLoading] = useState(false);
  const [activeError, setActiveError] = useState(null);
  const [recentPosts, setRecentPosts] = useState([]);
  const [postsLoading, setPostsLoading] = useState(false);
  const [postsError, setPostsError] = useState(null);

  useEffect(() => {
    if (!token) {
      setRecentPosts([]);
      return;
    }

    let cancelled = false;
    const loadPosts = async () => {
      setPostsLoading(true);
      setPostsError(null);
      try {
        const page = await fetchPosts(null, 5);
        if (!cancelled) setRecentPosts(Array.isArray(page) ? page : page.content ?? []);
      } catch (err) {
        if (!cancelled) setPostsError(err.message);
      } finally {
        if (!cancelled) setPostsLoading(false);
      }
    };
    loadPosts();
    return () => {
      cancelled = true;
    };
  }, [token]);

  useEffect(() => {
    if (!token) {
      setMe(null);
      setActiveUsers([]);
      return;
    }

    let cancelled = false;
    const load = async () => {
      setMeLoading(true);
      setMeError(null);
      setActiveError(null);
      let user;
      try {
        const userId = getCurrentUserId();
        if (userId == null) throw new Error('사용자 정보를 확인할 수 없습니다.');
        user = await fetchUser(userId);
        if (cancelled) return;
        setMe(user);
      } catch (err) {
        if (!cancelled) setMeError(err.message);
        return;
      } finally {
        if (!cancelled) setMeLoading(false);
      }

      const region = !user.activityRegion || user.activityRegion === 'UNKNOWN'
        ? user.region
        : user.activityRegion;
      if (!region) {
        setActiveUsers([]);
        return;
      }
      setActiveLoading(true);
      try {
        const page = await fetchActiveUsers(region, null);
        if (!cancelled) setActiveUsers(Array.isArray(page) ? page : page.content ?? []);
      } catch (err) {
        if (!cancelled) setActiveError(err.message);
      } finally {
        if (!cancelled) setActiveLoading(false);
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, [token]);

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 서버 로그아웃이 실패해도 로컬 토큰은 삭제된다.
    }
    onLogout();
  };

  return (
    <div>
      <section>
        <h1>Kombatzzang</h1>
        <p>동네 사람들과 겨루고, 기록하고, 이야기하는 대결 커뮤니티입니다.</p>
        <p>배틀에 도전하고 전적을 쌓아 보세요.</p>
      </section>

      {!token ? (
        <section>
          <button onClick={() => setMode('login')}>로그인</button>
          <button onClick={() => setMode('signup')}>회원가입</button>
          {mode === 'login' && <LoginForm onLogin={onLogin} />}
          {mode === 'signup' && <SignupForm onSignup={onLogin} />}
          <p>로그인하면 내 전적과 활동 유저를 볼 수 있습니다.</p>
        </section>
      ) : (
        <>
          <button onClick={handleLogout}>로그아웃</button>

          <section>
            <h2>내 전적</h2>
            {meLoading && <p>불러오는 중...</p>}
            {meError && <p style={{ color: 'crimson' }}>{meError}</p>}
            {!meLoading && !meError && !me && <p>전적 정보가 없습니다.</p>}
            {me && !meLoading && (
              <p>
                {me.username} - {me.wins}승 {me.losses}패 (승률 {winRate(me)})
              </p>
            )}
          </section>

          <section>
            <h2>진행 중인 배틀</h2>
            <p>진행 중인 배틀이 없습니다.</p>
          </section>

          <section>
            <h2>최근 게시글</h2>
            {postsLoading && <p>불러오는 중...</p>}
            {postsError && <p style={{ color: 'crimson' }}>{postsError}</p>}
            {!postsLoading && !postsError && recentPosts.length === 0 && (
              <p>최근 게시글이 없습니다.</p>
            )}
            <ul>
              {recentPosts.map((post) => (
                <li key={post.id}>{post.title}</li>
              ))}
            </ul>
          </section>

          <section>
            <h2>활동 유저</h2>
            {activeLoading && <p>불러오는 중...</p>}
            {activeError && <p style={{ color: 'crimson' }}>{activeError}</p>}
            {!activeLoading && !activeError && !meLoading && !meError && me && activeUsers.length === 0 && (
              <p>활동 중인 유저가 없습니다.</p>
            )}
            <ul>
              {activeUsers.map((u) => (
                <li key={u.id}>
                  <button type="button" onClick={() => onSelectUser?.(u.id)}>
                    {u.username}
                  </button>{' '}
                  - {u.wins}승 {u.losses}패
                </li>
              ))}
            </ul>
          </section>
        </>
      )}
    </div>
  );
}
