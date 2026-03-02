import request from '@/api/request';

export function getMyRunnerPaymentProfileApi() {
  return request({
    url: '/runner/payment-profile/me',
    method: 'get',
  });
}

export function upsertMyRunnerPaymentProfileApi(payload) {
  return request({
    url: '/runner/payment-profile/me',
    method: 'post',
    data: payload,
  });
}
