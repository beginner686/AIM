import request from '@/api/request';

export function getStatSummaryApi() {
    return request({ url: '/admin/stat/summary', method: 'get' });
}

export function getStatByCountryApi() {
    return request({ url: '/admin/stat/country', method: 'get' });
}

export function getStatByCategoryApi() {
    return request({ url: '/admin/stat/category', method: 'get' });
}

export function getWorkerIncomeRankApi(limit) {
    return request({ url: '/admin/stat/worker-income-rank', method: 'get', params: { limit } });
}

export function getFeeConfigApi() {
    return request({ url: '/admin/config/fees', method: 'get' });
}

export function updateFeeConfigApi(data) {
    return request({ url: '/admin/config/fees', method: 'post', data });
}
