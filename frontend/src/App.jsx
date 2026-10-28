import { useState } from 'react';
import { getAccessToken, logout } from './api/client.js';
import LoginForm from './components/LoginForm.jsx';
import PostFeed from './components/PostFeed.jsx';
import UserList from './components/UserList.jsx';

export default function App() {
  const [token, setToken] = useState(getAccessToken());
  const [tab, setTab] = useState('posts');

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 서버 로그아웃이 실패해도 로컬 토큰은 삭제된다.
    }
    setToken(null);
  };

  return (
    <main style={{ maxWidth: 640, margin: '2rem auto', fontFamily: 'sans-serif' }}>
      <h1>Kombatzzang</h1>
      {token ? (
        <>
          <nav>
            <button onClick={() => setTab('posts')} disabled={tab === 'posts'}>
              게시글
            </button>
            <button onClick={() => setTab('users')} disabled={tab === 'users'}>
              유저
            </button>
            {tab === 'posts' && <button onClick={handleLogout}>로그아웃</button>}
          </nav>
          {tab === 'posts' ? <PostFeed /> : <UserList onLogout={() => setToken(null)} />}
        </>
      ) : (
        <LoginForm onLogin={() => setToken(getAccessToken())} />
      )}
    </main>
  );
}
