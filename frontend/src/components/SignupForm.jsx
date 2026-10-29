import { useState } from 'react';
import { signup } from '../api/client.js';
import { validateSignup } from './signupValidation.js';

const DUPLICATE_MESSAGE = '이미 사용 중인 이메일 또는 닉네임입니다.';

export default function SignupForm({ onSignup, onCancel }) {
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    const errors = validateSignup({ username, email, password, passwordConfirm });
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      await signup({ username: username.trim(), email: email.trim(), password });
      onSignup();
    } catch (err) {
      setError(err.status === 409 ? (err.serverMessage || DUPLICATE_MESSAGE) : err.message);
    } finally {
      setSubmitting(false);
    }
  };

  const fieldError = (name) =>
    fieldErrors[name] && <p style={{ color: 'crimson' }}>{fieldErrors[name]}</p>;

  return (
    <form onSubmit={handleSubmit} noValidate>
      <div>
        <input type="text" placeholder="닉네임" value={username}
          onChange={(e) => setUsername(e.target.value)} />
        {fieldError('username')}
      </div>
      <div>
        <input type="email" placeholder="이메일" value={email}
          onChange={(e) => setEmail(e.target.value)} />
        {fieldError('email')}
      </div>
      <div>
        <input type="password" placeholder="비밀번호" value={password}
          onChange={(e) => setPassword(e.target.value)} />
        {fieldError('password')}
      </div>
      <div>
        <input type="password" placeholder="비밀번호 확인" value={passwordConfirm}
          onChange={(e) => setPasswordConfirm(e.target.value)} />
        {fieldError('passwordConfirm')}
      </div>
      <button type="submit" disabled={submitting}>회원가입</button>
      <button type="button" onClick={onCancel}>로그인으로 돌아가기</button>
      {error && <p role="alert" style={{ color: 'crimson' }}>{error}</p>}
    </form>
  );
}
