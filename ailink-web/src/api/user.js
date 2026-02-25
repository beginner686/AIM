import request from '@/api/request';

export function getCurrentUserApi() {
  return request({
    url: '/user/me',
    method: 'get',
  });
}

export function updateUserProfileApi(data) {
  return request({
    url: '/user/me',
    method: 'put',
    data,
  });
}
