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
      url: '/demand/create',
      method: 'post',
      data: payload,
    });
  } catch (error) {
    if (error?.response?.status !== 404) {
      throw error;
    }
    return request({
      url: '/demand',
      method: 'post',
      data: payload,
    });
  }
}
