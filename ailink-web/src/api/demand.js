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
    });
  } catch (error) {
    const status = error?.response?.status;
    if (status !== 404 && status !== 405) {
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
