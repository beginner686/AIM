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
    paymentChannel: 'WECHAT_PAY',
  };
}

export async function createOrderApi(params) {
  const payload = buildPayload(params);
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

export function getOrderDetailApi(orderId) {
  return request({
    url: `/order/${orderId}/detail`,
    method: 'get',
  });
}

export function getMyOrderListApi() {
  return request({
    url: '/order/my',
    method: 'get',
  });
}

export function payServiceFeeApi(orderId, paymentChannel = 'WECHAT_PAY') {
  return request({
    url: `/order/${orderId}/pay-service-fee`,
    method: 'post',
    data: { paymentChannel },
  });
}

export function startWorkApi(orderId) {
  return request({
    url: `/order/${orderId}/start`,
    method: 'post',
  });
}

export function completeOrderApi(orderId) {
  return request({
    url: `/order/${orderId}/complete`,
    method: 'post',
  });
}

export function disputeOrderApi(orderId, reason, evidenceUrl = '') {
  return request({
    url: `/order/${orderId}/dispute`,
    method: 'post',
    data: { reason, evidenceUrl },
  });
}

export function getOrderReviewsApi(orderId) {
  return request({
    url: `/order/${orderId}/review`,
    method: 'get',
  });
}

export function createOrderReviewApi(orderId, score, content) {
  return request({
    url: `/order/${orderId}/review`,
    method: 'post',
    data: { score, content },
  });
}

export function getOrderChatMessagesApi(orderId) {
  return request({
    url: `/order/${orderId}/chat/messages`,
    method: 'get',
  });
}

export function sendOrderChatMessageApi(orderId, content) {
  return request({
    url: `/order/${orderId}/chat/messages`,
    method: 'post',
    data: { content },
  });
}
