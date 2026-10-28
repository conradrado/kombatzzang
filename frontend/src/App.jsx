import { useState } from 'react';
import { getAccessToken, getCurrentUser } from './api/client.js';
import FollowList from './components/FollowList.jsx';
import LoginForm from './components/LoginForm.jsx';
import UserList from './components/UserList.jsx';
import UserProfile from './components/UserProfile.jsx';

const LIST_SCREEN = { name: 'list' };

export default function App() {
  const [token, setToken] = useState(getAccessToken());
  const [screen, setScreen] = useState(LIST_SCREEN);

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
      {token ? renderScreen() : <LoginForm onLogin={() => setToken(getAccessToken())} />}
    </main>
  );
}
