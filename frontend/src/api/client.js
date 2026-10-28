const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

export function getAccessToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export async function request(path, options = {}) {
  const headers = { ...options.headers };
  const token = getAccessToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  let body = options.body;
  if (body !== undefined && typeof body !== 'string') {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }

  const response = await fetch(path, { ...options, headers, body });

  if (!response.ok) {
    let message = `요청에 실패했습니다. (${response.status})`;
    try {
      const error = await response.json();
      if (error.message) message = error.message;
    } catch {
      // 본문이 JSON 이 아니면 기본 메시지를 사용한다.
    }
    throw new Error(message);
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

export function getCurrentUser() {
  const token = getAccessToken();
  if (!token) return null;
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(atob(base64));
    return { id: Number(payload.sub), role: payload.role };
  } catch {
    return null;
  }
}

export function fetchUser(userId) {
  return request(`/api/users/${userId}`);
}

export function updateUser(userId, body) {
  return request(`/api/users/${userId}`, { method: 'PATCH', body });
}

export function updateUserRecord(userId, body) {
  return request(`/api/users/${userId}/record`, { method: 'PATCH', body });
}

export function updateUserRegion(userId, region) {
  return request(`/api/users/${userId}/region`, { method: 'PATCH', body: { region } });
}

export function follow(followeeId) {
  return request('/api/users/follow', { method: 'POST', body: { followeeId } });
}

export function unfollow(userId) {
  return request(`/api/users/${userId}/follow`, { method: 'DELETE' });
}

export function fetchFollowers(userId) {
  return request(`/api/users/${userId}/followers`);
}

export function fetchFollowings(userId) {
  return request(`/api/users/${userId}/followings`);
}
