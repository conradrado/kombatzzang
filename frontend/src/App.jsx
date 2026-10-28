import { useState } from 'react';
import { getAccessToken } from './api/client.js';
import HomePage from './components/HomePage.jsx';

export default function App() {
  const [token, setToken] = useState(getAccessToken());

  return (
    <main style={{ maxWidth: 640, margin: '2rem auto', fontFamily: 'sans-serif' }}>
      <HomePage
        token={token}
        onLogin={() => setToken(getAccessToken())}
        onLogout={() => setToken(null)}
      />
    </main>
  );
}
