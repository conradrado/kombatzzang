import { describe, it, expect } from 'vitest';
import { validateSignup } from './signupValidation.js';

const valid = {
  username: 'tester',
  email: 'a@b.com',
  password: 'password1',
  passwordConfirm: 'password1',
};
const run = (override) => validateSignup({ ...valid, ...override });

describe('validateSignup', () => {
  it('모두 유효하면 빈 객체를 반환한다', () => {
    expect(run({})).toEqual({});
  });

  it.each([
    ['1자', 'a', true],
    ['2자', 'ab', false],
    ['20자', 'a'.repeat(20), false],
    ['21자', 'a'.repeat(21), true],
    ['공백', '   ', true],
  ])('username %s', (_, username, hasError) => {
    expect('username' in run({ username })).toBe(hasError);
  });

  it('email 형식 오류', () => {
    expect(run({ email: 'abc' })).toHaveProperty('email');
    expect(run({ email: 'a@b' })).toHaveProperty('email');
    expect(run({ email: '' })).toHaveProperty('email');
  });

  it.each([
    [7, true],
    [8, false],
    [64, false],
    [65, true],
  ])('password %i자', (len, hasError) => {
    const password = 'p'.repeat(len);
    const errors = run({ password, passwordConfirm: password });
    expect('password' in errors).toBe(hasError);
  });

  it('비밀번호 확인 불일치', () => {
    expect(run({ passwordConfirm: 'different1' })).toEqual({
      passwordConfirm: expect.any(String),
    });
  });
});
