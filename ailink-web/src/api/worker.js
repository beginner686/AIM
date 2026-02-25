import request from '@/api/request';

function buildQuery(params) {
  const country = (params?.country || '').trim();
  const category = (params?.category || '').trim();

  return {
    country,
    category,
    skillKeyword: category,
    verified: 1,
  };
}

function normalizeWorkers(list) {
  if (!Array.isArray(list)) {
    return [];
  }
  return list.map((item) => ({
    id: item?.id,
    workerId: item?.id,
    userId: item?.userId,
    name: item?.realName || item?.username || `执行者#${item?.id || '-'}`,
    country: item?.country || '',
    category: item?.category || item?.skillTags || '',
    dealCount: Number(item?.dealCount || item?.orderCount || 0),
    rating: Number(item?.rating || 0),
    priceMin: Number(item?.priceMin || 0),
    priceMax: Number(item?.priceMax || 0),
    raw: item,
  }));
}

export async function getWorkerListApi(params) {
  const data = await request({
    url: '/worker/list',
    method: 'get',
    params: buildQuery(params),
  });
  return normalizeWorkers(data);
}
