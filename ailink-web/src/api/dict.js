import request from '@/api/request';

export function getPublicDictItemsApi(options = {}) {
  const { types, version, ...config } = options;
  return request({
    url: '/dict/public/items',
    method: 'get',
    params: {
      types,
      version,
    },
    ...config,
  });
}

export function searchDictItemsApi({ type, keyword } = {}) {
  return request({
    url: '/dict/public/search',
    method: 'get',
    params: { type, keyword },
  });
}

export function getDictByTypeApi(type) {
  return request({
    url: `/dict/${type}`,
    method: 'get',
    silentBusinessError: true,
    silentHttpError: true,
    ignoreErrorStatuses: [404],
  });
}

export async function getCountryDictApi() {
  try {
    return await getDictByTypeApi('COUNTRY');
  } catch (error) {
    const status = error?.response?.status;
    if (status && status !== 404) {
      throw error;
    }
  }

  const bundle = await getPublicDictItemsApi({
    types: 'COUNTRY',
    silentBusinessError: true,
    silentHttpError: true,
    ignoreErrorStatuses: [404],
  });
  const rows = Array.isArray(bundle?.items?.COUNTRY) ? bundle.items.COUNTRY : [];
  return rows.map((item) => ({
    dict_code: item?.code || '',
    dict_label: item?.label || '',
    sort_no: Number(item?.sort || 0),
    extra_json: item?.extra || '',
  }));
}
