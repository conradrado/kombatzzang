import { useState } from 'react';
import { getAccessToken } from './api/client.js';
import LoginForm from './components/LoginForm.jsx';
import SignupForm from './components/SignupForm.jsx';
import UserList from './components/UserList.jsx';

export default function App() {
  const [token, setToken] = useState(getAccessToken());
  const [mode, setMode] = useState('login');
  const [notice, setNotice] = useState(null);

  const showSignup = () => {
    setNotice(null);
    setMode('signup');
  };

  const handleSignup = () => {
    setNotice('회원가입이 완료되었습니다. 로그인해 주세요.');
    setMode('login');
  };

  return (
    <main style={{ maxWidth: 640, margin: '2rem auto', fontFamily: 'sans-serif' }}>
      <h1>Kombatzzang</h1>
      {token ? (
        <UserList onLogout={() => setToken(null)} />
      ) : mode === 'signup' ? (
        <SignupForm onSignup={handleSignup} onSwitchToLogin={() => setMode('login')} />
      ) : (
        <>
          {notice && <p style={{ color: 'seagreen' }}>{notice}</p>}
          <LoginForm onLogin={() => setToken(getAccessToken())} />
          <button type="button" onClick={showSignup}>회원가입</button>
        </>
      )}
    </main>
  );
}
