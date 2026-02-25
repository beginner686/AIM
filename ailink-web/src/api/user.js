import request from '@/api/request';

export function getCurrentUserApi() {
  return request({
    url: '/user/me',
    method: 'get',
  });
}
