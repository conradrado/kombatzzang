import { useState } from 'react';
import { getAccessToken, getCurrentUser } from './api/client.js';
import FollowList from './components/FollowList.jsx';
import HomePage from './components/HomePage.jsx';
import UserList from './components/UserList.jsx';
import UserProfile from './components/UserProfile.jsx';

const HOME_SCREEN = { name: 'home' };
const LIST_SCREEN = { name: 'list' };

export default function App() {
  const [token, setToken] = useState(getAccessToken());
  const [screen, setScreen] = useState(HOME_SCREEN);

  const openProfile = (userId) => setScreen({ name: 'profile', userId });

  const renderScreen = () => {
    if (token && screen.name === 'profile') {
      return (
        <UserProfile
          userId={screen.userId}
          onBack={() => setScreen(LIST_SCREEN)}
          onOpenFollows={(userId, type) => setScreen({ name: 'follows', userId, type })}
        />
      );
    }
    if (token && screen.name === 'follows') {
      return (
        <FollowList
          userId={screen.userId}
          type={screen.type}
          onBack={() => openProfile(screen.userId)}
          onSelectUser={openProfile}
        />
      );
    }
    if (token && screen.name === 'list') {
      return (
        <>
          <button onClick={() => setScreen(HOME_SCREEN)}>홈</button>
          <UserList
            onLogout={() => {
              setScreen(HOME_SCREEN);
              setToken(null);
            }}
            onSelectUser={openProfile}
            onOpenMyProfile={() => {
              const me = getCurrentUser();
              if (me) openProfile(me.id);
            }}
          />
        </>
      );
    }
    return (
      <>
        <HomePage
          token={token}
          onLogin={() => setToken(getAccessToken())}
          onLogout={() => setToken(null)}
        />
        {token && <button onClick={() => setScreen(LIST_SCREEN)}>유저 목록</button>}
      </>
    );
  };

  return (
    <main style={{ maxWidth: 640, margin: '2rem auto', fontFamily: 'sans-serif' }}>
      {renderScreen()}
    </main>
  );
}
