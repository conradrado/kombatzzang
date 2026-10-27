import { useState } from 'react';
import { signup } from '../api/client.js';

export default function SignupForm({ onSignup, onSwitchToLogin }) {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await signup(username, email, password);
      onSignup();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <div>
        <input type="text" placeholder="아이디 (2~20자)" value={username}
          onChange={(e) => setUsername(e.target.value)}
          required minLength={2} maxLength={20} />
      </div>
      <div>
        <input type="email" placeholder="이메일" value={email}
          onChange={(e) => setEmail(e.target.value)} required />
      </div>
      <div>
        <input type="password" placeholder="비밀번호 (8~64자)" value={password}
          onChange={(e) => setPassword(e.target.value)}
          required minLength={8} maxLength={64} />
      </div>
      <button type="submit">회원가입</button>
      <button type="button" onClick={onSwitchToLogin}>로그인으로 돌아가기</button>
      {error && <p style={{ color: 'crimson' }}>{error}</p>}
    </form>
  );
}
