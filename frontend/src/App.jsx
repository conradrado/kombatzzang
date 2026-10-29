import { useState } from 'react';
import { getAccessToken } from './api/client.js';
import LoginForm from './components/LoginForm.jsx';
import SignupForm from './components/SignupForm.jsx';
import UserList from './components/UserList.jsx';

export default function App() {
  const [token, setToken] = useState(getAccessToken());
  const [view, setView] = useState('login');
  const [notice, setNotice] = useState(null);

  const renderGuest = () => {
    if (view === 'signup') {
      return (
        <SignupForm
          onSignup={() => {
            setNotice('회원가입이 완료되었습니다. 로그인해 주세요.');
            setView('login');
          }}
          onCancel={() => setView('login')}
        />
      );
    }
    return (
      <>
        {notice && <p>{notice}</p>}
        <LoginForm onLogin={() => setToken(getAccessToken())} />
        <button type="button" onClick={() => { setNotice(null); setView('signup'); }}>
          회원가입
        </button>
      </>
    );
  };

  return (
    <main style={{ maxWidth: 640, margin: '2rem auto', fontFamily: 'sans-serif' }}>
      <h1>Kombatzzang</h1>
      {token ? <UserList onLogout={() => setToken(null)} /> : renderGuest()}
    </main>
  );
}
