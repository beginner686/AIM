import request from '@/api/request';

export function getPublicDemandsApi(params) {
    return request({
        url: '/public/demands',
        method: 'get',
        params,
        silentHttpError: true,
        ignoreErrorStatuses: [401, 403],
    });
}

export function getPublicWorkersApi(params) {
    return request({
        url: '/public/workers',
        method: 'get',
        params,
        silentHttpError: true,
        ignoreErrorStatuses: [401, 403],
    });
}

export function getPublicStatsApi() {
    return request({
        url: '/public/stats',
        method: 'get',
        silentHttpError: true,
        ignoreErrorStatuses: [401, 403],
    });
}
