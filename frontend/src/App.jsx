import { useState } from 'react';
import { getAccessToken } from './api/client.js';
import LoginForm from './components/LoginForm.jsx';
import UserList from './components/UserList.jsx';

export default function App() {
  const [token, setToken] = useState(getAccessToken());

  return (
    <main style={{ maxWidth: 640, margin: '2rem auto', fontFamily: 'sans-serif' }}>
      <h1>Kombatzzang</h1>
      {token ? (
        <UserList onLogout={() => setToken(null)} />
      ) : (
        <LoginForm onLogin={() => setToken(getAccessToken())} />
      )}
    </main>
  );
}
