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
    silentBusinessError: true,
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

export function getOrderFeeConfigApi() {
  return request({
    url: '/order/config/fees',
    method: 'get',
  });
}

export function deleteMyOrdersApi(orderIds = []) {
  return request({
    url: '/order/my/delete',
    method: 'post',
    data: {
      orderIds,
    },
    silentBusinessError: true,
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

export function declarePaidApi(orderId, remark = '') {
  return request({
    url: `/order/${orderId}/declare-paid`,
    method: 'post',
    data: { remark },
  });
}

export function acceptOrderApi(orderId, remark = '') {
  return request({
    url: `/order/${orderId}/accept`,
    method: 'post',
    data: { remark },
  });
}

export function rejectOrderApi(orderId, remark = '') {
  return request({
    url: `/order/${orderId}/reject`,
    method: 'post',
    data: { remark },
  });
}

export function completeOrderApi(orderId) {
  return request({
    url: `/order/${orderId}/complete`,
    method: 'post',
  });
}

export function cancelOrderApi(orderId, remark = '') {
  return request({
    url: `/order/${orderId}/cancel`,
    method: 'post',
    data: { remark },
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

export function getOrderDisputesApi(orderId) {
  return request({
    url: `/order/${orderId}/dispute`,
    method: 'get',
  });
}

export function aiAutoMatchApi(demandId) {
  return request({
    url: `/ai-match/demand/${demandId}/auto`,
    method: 'post',
    silentBusinessError: true,
  });
}
