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
    const error = new Error(message);
    error.status = response.status;
    throw error;
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
    const segment = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = segment + '='.repeat((4 - (segment.length % 4)) % 4);
    const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0));
    const payload = JSON.parse(new TextDecoder().decode(bytes));
    return { userId: Number(payload.sub), role: payload.role ?? null };
  } catch {
    return null;
  }
}

export function fetchPosts(cursor, size = 10) {
  const params = new URLSearchParams();
  if (cursor != null) params.set('cursor', cursor);
  params.set('size', size);
  return request(`/api/posts?${params}`);
}

export function fetchPost(postId) {
  return request(`/api/posts/${postId}`);
}

export function createPost({ title, body }) {
  return request('/api/posts', { method: 'POST', body: { title, body } });
}

export function updatePost(postId, { title, body }) {
  return request(`/api/posts/${postId}`, { method: 'PATCH', body: { title, body } });
}

export function deletePost(postId) {
  return request(`/api/posts/${postId}`, { method: 'DELETE' });
}
