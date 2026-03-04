import request from '@/api/request';

export function submitDemandApplyApi(demandId, data) {
  return request({
    url: `/demand/${demandId}/apply`,
    method: 'post',
    silentBusinessError: true,
    data,
  });
}

export function getMyDemandAppliesApi() {
  return request({
    url: '/demand/my/applications',
    method: 'get',
  });
}

export function getDemandApplyListApi(demandId, status = '') {
  return request({
    url: `/demand/${demandId}/applications`,
    method: 'get',
    params: { status },
  });
}

export function acceptDemandApplyApi(applyId, data = {}) {
  return request({
    url: `/demand/applications/${applyId}/accept`,
    method: 'post',
    data,
  });
}

export function rejectDemandApplyApi(applyId, reviewNote = '') {
  return request({
    url: `/demand/applications/${applyId}/reject`,
    method: 'post',
    data: { reviewNote },
  });
}

export function cancelDemandApplyApi(applyId) {
  return request({
    url: `/demand/applications/${applyId}/cancel`,
    method: 'post',
  });
}
