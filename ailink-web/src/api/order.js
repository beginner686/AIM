import request from '@/api/request';

function buildPayload(params) {
  const demandId = Number(params?.demandId || 0);
  const workerId = Number(params?.workerId || 0);
  const amount = Number(params?.amount || 0);

  return {
    demandId,
    workerId,
    workerProfileId: workerId,
    amount,
    paymentChannel: 'MANUAL',
  };
}

export async function createOrderApi(params) {
  const payload = buildPayload(params);
  try {
    return await request({
      url: '/order/create',
      method: 'post',
      data: {
        demandId: payload.demandId,
        workerId: payload.workerId,
      },
    });
  } catch (error) {
    return request({
      url: '/order',
      method: 'post',
      data: {
        demandId: payload.demandId,
        workerProfileId: payload.workerProfileId,
        amount: payload.amount,
        paymentChannel: payload.paymentChannel,
      },
    });
  }
}

export function getOrderDetailApi(orderId) {
  return request({
    url: `/order/${orderId}`,
    method: 'get',
  });
}
