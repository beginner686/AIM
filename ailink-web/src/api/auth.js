import request from '@/api/request';

export function loginApi(data) {
  const username = (data?.username || '').trim();
  return request({
    url: '/auth/login',
    method: 'post',
    data: {
      username,
      password: data?.password || '',
    },
  });
}

export function registerApi(data) {
  const username = (data?.username || '').trim();
  const safeName = username
    .toLowerCase()
    .replace(/[^a-z0-9._-]/g, '')
    .replace(/^[-_.]+|[-_.]+$/g, '');
  const emailLocal = safeName || `user${Date.now()}`;

  return request({
    url: '/auth/register',
    method: 'post',
    data: {
      username,
      password: data?.password || '',
      role: 'USER',
      email: `${emailLocal}@ailink.local`,
    },
  });
}
