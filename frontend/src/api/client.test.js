import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchUsers, signup } from './client';

function okResponse(body, status = 200) {
  return { ok: true, status, json: () => Promise.resolve(body) };
}

describe('api client', () => {
  beforeEach(() => {
    localStorage.clear();
    global.fetch = vi.fn();
  });

  it('signup 은 POST /api/users 로 JSON 본문을 보낸다', async () => {
    global.fetch.mockResolvedValue(okResponse({ id: 1 }, 201));

    await signup({ username: 'tester', email: 'a@b.com', password: 'password1' });

    expect(global.fetch).toHaveBeenCalledTimes(1);
    const [url, init] = global.fetch.mock.calls[0];
    expect(url).toBe('/api/users');
    expect(init.method).toBe('POST');
    expect(init.headers['Content-Type']).toBe('application/json');
    expect(JSON.parse(init.body)).toEqual({
      username: 'tester',
      email: 'a@b.com',
      password: 'password1',
    });
  });

  it('signup 은 accessToken 이 있어도 Authorization 헤더를 보내지 않는다', async () => {
    localStorage.setItem('accessToken', 'stale-token');
    global.fetch.mockResolvedValue(okResponse({ id: 1 }, 201));

    await signup({ username: 'tester', email: 'a@b.com', password: 'password1' });

    const [, init] = global.fetch.mock.calls[0];
    expect(init.headers.Authorization).toBeUndefined();
    expect(init).not.toHaveProperty('auth');
  });

  it('오류 응답의 message 를 status/serverMessage 와 함께 던진다', async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 409,
      json: () => Promise.resolve({ status: 409, message: '이미 존재하는 이메일입니다.' }),
    });

    await expect(
      signup({ username: 'tester', email: 'a@b.com', password: 'password1' }),
    ).rejects.toMatchObject({
      status: 409,
      serverMessage: '이미 존재하는 이메일입니다.',
      message: '이미 존재하는 이메일입니다.',
    });
  });

  it('JSON 이 아닌 오류 응답은 기본 메시지를 사용한다', async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.reject(new Error('not json')),
    });

    const err = await signup({ username: 'tester', email: 'a@b.com', password: 'password1' }).catch(
      (e) => e,
    );

    expect(err.status).toBe(500);
    expect(err.serverMessage).toBeUndefined();
    expect(err.message).toBe('요청에 실패했습니다. (500)');
  });

  it('fetchUsers 는 토큰이 있으면 Authorization 헤더를 보낸다', async () => {
    localStorage.setItem('accessToken', 'abc');
    global.fetch.mockResolvedValue(okResponse({ users: [] }));

    await fetchUsers();

    const [, init] = global.fetch.mock.calls[0];
    expect(init.headers.Authorization).toBe('Bearer abc');
  });
});
