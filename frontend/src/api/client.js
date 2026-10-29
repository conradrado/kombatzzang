const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export async function request(path, options = {}) {
  const { auth = true, ...fetchOptions } = options;
  const headers = { ...fetchOptions.headers };
  const token = auth ? getAccessToken() : null;
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let body = fetchOptions.body;
  if (body !== undefined && typeof body !== 'string') {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }

  const response = await fetch(path, { ...fetchOptions, headers, body });

  if (!response.ok) {
    let message = `요청에 실패했습니다. (${response.status})`;
    let serverMessage;
    try {
      const error = await response.json();
      if (error.message) {
        message = error.message;
        serverMessage = error.message;
      }
    } catch {
      // 본문이 JSON 이 아니면 기본 메시지를 사용한다.
    }
    const err = new Error(message);
    err.status = response.status;
    err.serverMessage = serverMessage;
    throw err;
  }

  if (response.status === 204) return null;
  return response.json();
}

export async function login(email, password) {
  const data = await request('/api/auth/login', {
    method: 'POST',
    body: { email, password },
  });
  localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken);
  return data;
}

export function signup({ username, email, password }) {
  return request('/api/users', {
    method: 'POST',
    body: { username, email, password },
    auth: false,
  });
}

export async function logout() {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
  try {
    await request('/api/auth/logout', {
      method: 'POST',
      body: { refreshToken },
    });
  } finally {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }
}

export function fetchUsers(cursor, size = 10) {
  const params = new URLSearchParams();
  if (cursor != null) params.set('cursor', cursor);
  params.set('size', size);
  return request(`/api/users?${params}`);
}
