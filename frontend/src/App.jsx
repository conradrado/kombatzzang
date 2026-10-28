import { useState } from 'react';
import { getAccessToken, getCurrentUser, logout } from './api/client.js';
import FollowList from './components/FollowList.jsx';
import LoginForm from './components/LoginForm.jsx';
import PostFeed from './components/PostFeed.jsx';
import UserList from './components/UserList.jsx';
import UserProfile from './components/UserProfile.jsx';

const LIST_SCREEN = { name: 'list' };

export default function App() {
  const [token, setToken] = useState(getAccessToken());
  const [tab, setTab] = useState('posts');
  const [screen, setScreen] = useState(LIST_SCREEN);

  const handleLogout = async () => {
    try {
      await logout();
    } catch {
      // 서버 로그아웃이 실패해도 로컬 토큰은 삭제된다.
    }
    setScreen(LIST_SCREEN);
    setToken(null);
  };

  const changeTab = (next) => {
    setScreen(LIST_SCREEN);
    setTab(next);
  };

  const openProfile = (userId) => setScreen({ name: 'profile', userId });

  const renderScreen = () => {
    if (screen.name === 'profile') {
      return (
        <UserProfile
          userId={screen.userId}
          onBack={() => setScreen(LIST_SCREEN)}
          onOpenFollows={(userId, type) => setScreen({ name: 'follows', userId, type })}
        />
      );
    }
    if (screen.name === 'follows') {
      return (
        <FollowList
          userId={screen.userId}
          type={screen.type}
          onBack={() => openProfile(screen.userId)}
          onSelectUser={openProfile}
        />
      );
    }
    return (
      <UserList
        onLogout={() => {
          setScreen(LIST_SCREEN);
          setToken(null);
        }}
        onSelectUser={openProfile}
        onOpenMyProfile={() => {
          const me = getCurrentUser();
          if (me) openProfile(me.id);
        }}
      />
    );
  };

  return (
    <main style={{ maxWidth: 640, margin: '2rem auto', fontFamily: 'sans-serif' }}>
      <h1>Kombatzzang</h1>
      {token ? (
        <>
          <nav>
            <button onClick={() => changeTab('posts')} disabled={tab === 'posts'}>
              게시글
            </button>
            <button onClick={() => changeTab('users')} disabled={tab === 'users'}>
              유저
            </button>
            {tab === 'posts' && <button onClick={handleLogout}>로그아웃</button>}
          </nav>
          {tab === 'posts' ? <PostFeed /> : renderScreen()}
        </>
      ) : (
        <LoginForm onLogin={() => setToken(getAccessToken())} />
      )}
    </main>
  );
}
