import { useState } from 'react';
import { login } from '../api/client.js';

export default function LoginForm({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await login(email, password);
      onLogin();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <div>
        <input type="email" placeholder="이메일" value={email}
          onChange={(e) => setEmail(e.target.value)} required />
      </div>
      <div>
        <input type="password" placeholder="비밀번호" value={password}
          onChange={(e) => setPassword(e.target.value)} required />
      </div>
      <button type="submit">로그인</button>
      {error && <p style={{ color: 'crimson' }}>{error}</p>}
    </form>
  );
}
