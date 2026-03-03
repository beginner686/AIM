import request from '@/api/request';

function normalizePayload(data) {
  const title = (data?.title || '').trim();
  const category = (data?.category || '').trim();
  const country = (data?.country || '').trim();
  const description = (data?.description || '').trim();
  const budget = Number(data?.budget || 0);

  return {
    title,
    category,
    budget,
    country,
    targetCountry: country,
    description: title ? `[${title}] ${description}` : description,
  };
}

export async function createDemandApi(data) {
  const payload = normalizePayload(data);
  try {
    return await request({
      url: '/demand',
      method: 'post',
      data: payload,
      ignoreErrorStatuses: [404, 405],
      silentBusinessError: true,
    });
  } catch (error) {
    const status = error?.response?.status;
    const message = String(error?.response?.data?.message || error?.message || '').toLowerCase();
    const canFallback = status === 404
      || status === 405
      || message.includes("request method 'post' is not supported")
      || message.includes('method not allowed');
    if (!canFallback) {
      throw error;
    }
    return request({
      url: '/demand/create',
      method: 'post',
      data: payload,
    });
  }
}

export function getDemandListApi(params) {
  return request({
    url: '/demand',
    method: 'get',
    params,
  });
}

export function getMyDemandListApi() {
  return request({
    url: '/demand/my',
    method: 'get',
  });
}

export function getDemandDetailApi(demandId) {
  return request({
    url: `/demand/${demandId}`,
    method: 'get',
  });
}

export function cancelDemandApi(demandId) {
  return request({
    url: `/demand/${demandId}/cancel`,
    method: 'post',
  });
}
