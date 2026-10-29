import { useState } from 'react';
import { login, signup } from '../api/client.js';

export default function SignupForm({ onSignup }) {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    try {
      await signup({ username, email, password });
      await login(email, password);
      onSignup();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <div>
        <input type="text" placeholder="사용자 이름" value={username}
          minLength={2} maxLength={20}
          onChange={(e) => setUsername(e.target.value)} required />
      </div>
      <div>
        <input type="email" placeholder="이메일" value={email}
          onChange={(e) => setEmail(e.target.value)} required />
      </div>
      <div>
        <input type="password" placeholder="비밀번호" value={password}
          minLength={8} maxLength={64}
          onChange={(e) => setPassword(e.target.value)} required />
      </div>
      <button type="submit">회원가입</button>
      {error && <p style={{ color: 'crimson' }}>{error}</p>}
    </form>
  );
}
