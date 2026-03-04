import request from '@/api/request';

export function submitWorkerApplyApi(data) {
  return request({
    url: '/worker/apply',
    method: 'post',
    data,
  });
}

export function uploadWorkerApplyAttachmentApi(formData) {
  return request({
    url: '/worker/apply/attachment',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

export function getMyWorkerApplyApi() {
  return request({
    url: '/worker/apply/me',
    method: 'get',
  });
}

export function getAdminWorkerApplyListApi(params = {}) {
  return request({
    url: '/admin/worker/apply/list',
    method: 'get',
    params,
  });
}

export function approveWorkerApplyApi(applyId, reviewNote = '') {
  return request({
    url: `/admin/worker/apply/${applyId}/approve`,
    method: 'post',
    data: { reviewNote },
  });
}

export function rejectWorkerApplyApi(applyId, reviewNote = '') {
  return request({
    url: `/admin/worker/apply/${applyId}/reject`,
    method: 'post',
    data: { reviewNote },
  });
}
