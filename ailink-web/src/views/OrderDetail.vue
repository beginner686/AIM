<template>
  <el-space direction="vertical" fill>
    <el-card shadow="never">
      <template #header>订单详情</template>
      <el-alert v-if="invalidOrderId" title="订单ID无效" type="error" :closable="false" show-icon />
      <el-alert v-else-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div v-else v-loading="loading">
        <el-descriptions title="订单基本信息" :column="2" border>
          <el-descriptions-item label="订单ID">{{ order.id || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ order.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需求ID">{{ order.demandId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="执行者ID">{{ order.workerUserId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(order.createdTime) }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="orderStatusTag.type">{{ orderStatusTag.label }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>费用信息</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单总金额">{{ formatMoney(order.amount) }}</el-descriptions-item>
        <el-descriptions-item label="服务费金额">{{ formatMoney(order.serviceFeeAmount) }}</el-descriptions-item>
        <el-descriptions-item label="平台抽成">{{ formatMoney(order.platformFee) }}</el-descriptions-item>
        <el-descriptions-item label="执行者收入">{{ formatMoney(order.workerIncome) }}</el-descriptions-item>
        <el-descriptions-item label="服务费状态">
          <el-tag :type="serviceFeeStatusTag">{{ order.serviceFeeStatus || 'REQUIRED' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDateTime(order.serviceFeePaidTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="action-panel">
      <template #header>订单操作</template>
      <div v-if="order.status === ORDER_STATUS.SERVICE_FEE_REQUIRED" class="action-row">
        <template v-if="isEmployer && canClientOperate">
          <el-button
            type="primary"
            :loading="actionLoading"
            @click="handlePayServiceFee"
          >
            支付服务费并解锁
          </el-button>
          <el-button
            type="danger"
            plain
            :loading="actionLoading"
            @click="handleCancelOrder"
          >
            取消订单
          </el-button>
        </template>
        <span class="action-tip">
          {{ isEmployer
            ? (canClientOperate
              ? '支付后进入待执行者接单，接单后解锁联系方式、收款方式和聊天入口。'
              : '当前账号角色无支付权限，请使用需求方账号（USER/EMPLOYER/CLIENT）。')
            : '等待需求方支付服务费后解锁。' }}
        </span>
      </div>

      <div v-else-if="order.status === ORDER_STATUS.WAIT_WORKER_ACCEPT || order.status === ORDER_STATUS.SERVICE_FEE_PAID" class="action-row">
        <template v-if="isWorker">
          <el-button type="success" :loading="actionLoading" @click="handleAccept">确认接单</el-button>
          <el-button type="danger" :loading="actionLoading" @click="handleReject">拒绝接单</el-button>
        </template>
        <el-button
          v-if="isEmployer && canClientOperate"
          type="danger"
          plain
          :loading="actionLoading"
          @click="handleCancelOrder"
        >
          取消订单
        </el-button>
        <span class="action-tip">
          {{ isWorker
            ? '请确认是否接单，接单后可开始履约。'
            : (isEmployer && canClientOperate ? '可继续等待执行者接单，或主动取消订单。' : '等待执行者确认接单。') }}
        </span>
      </div>

      <div v-else-if="order.status === ORDER_STATUS.MATCH_UNLOCKED" class="action-row">
        <el-button
          v-if="isEmployer && canClientOperate"
          type="danger"
          plain
          :loading="actionLoading"
          @click="handleCancelOrder"
        >
          取消订单
        </el-button>
        <el-button v-if="isWorker" type="success" :loading="actionLoading" @click="handleStart">开始工作</el-button>
        <span class="action-tip">
          {{ isWorker
            ? '执行者确认开始后进入履约阶段。'
            : (isEmployer && canClientOperate ? '执行者尚未开始工作，你可以选择取消订单。' : '等待执行者开始工作。') }}
        </span>
      </div>

      <div v-else-if="order.status === ORDER_STATUS.IN_PROGRESS" class="action-row">
        <template v-if="isEmployer && canClientOperate">
          <el-button type="primary" :loading="actionLoading" @click="handleComplete">确认完成</el-button>
          <el-button type="danger" :loading="actionLoading" @click="handleDispute">发起争议</el-button>
        </template>
        <span class="action-tip">
          {{ isEmployer
            ? (canClientOperate
              ? '确认完成后可进入评价。'
              : '当前账号角色无需求方操作权限，请使用需求方账号（USER/EMPLOYER/CLIENT）。')
            : '履约进行中，等待需求方确认完成。' }}
        </span>
      </div>

      <div v-else-if="order.status === ORDER_STATUS.COMPLETED" class="action-row">
        <el-tag type="success" size="large">订单已完成</el-tag>
        <span class="action-tip">双方可在下方提交评价。</span>
      </div>

      <div v-else-if="isDisputeStatus" class="action-row">
        <el-tag type="danger" size="large">争议处理中</el-tag>
        <span class="action-tip">平台介入处理中，请关注后续状态。</span>
      </div>

      <div v-else class="action-row">
        <span class="action-tip">当前状态无可用操作。</span>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>执行者联系方式与收款方式</template>
      <el-alert
        v-if="!detail.showContact"
        title="执行者接单后可解锁联系方式与收款方式"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="执行者姓名">{{ detail.runnerDisplayName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">{{ detail.runnerContact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="PayPal">
          {{ detail.runnerPaymentProfile?.paypalEmail || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="Wise">
          <el-link v-if="detail.runnerPaymentProfile?.wiseLink" :href="detail.runnerPaymentProfile.wiseLink" target="_blank" type="primary">
            打开 Wise 链接
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="收款链接">
          <el-link v-if="detail.runnerPaymentProfile?.paymentUrl" :href="detail.runnerPaymentProfile.paymentUrl" target="_blank" type="primary">
            打开收款链接
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="币种">{{ detail.runnerPaymentProfile?.currency || 'CNY' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="detail.showChat" shadow="never">
      <template #header>站内沟通</template>
      <div class="chat-wrap" v-loading="chatLoading">
        <div class="chat-list">
          <el-empty v-if="chatMessages.length === 0" description="暂无消息" :image-size="72" />
          <div v-for="item in chatMessages" :key="item.id" class="chat-item">
            <p class="chat-meta">发送者 {{ item.senderId }} · {{ formatDateTime(item.createdTime) }}</p>
            <p class="chat-content">{{ item.content }}</p>
            <el-alert
              v-if="item.warning"
              :title="item.warningMessage || '为保障权益，请在平台内完成交易'"
              type="warning"
              :closable="false"
              show-icon
            />
          </div>
        </div>
        <div class="chat-editor">
          <el-input
            v-model="chatInput"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="输入消息内容"
          />
          <el-button
            type="primary"
            :loading="chatSending"
            :disabled="!chatInput.trim()"
            @click="handleSendMessage"
          >
            发送消息
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card v-if="order.status === ORDER_STATUS.COMPLETED" shadow="never">
      <template #header>信用评价</template>
      <div class="review-wrap" v-loading="reviewLoading">
        <el-form v-if="canReview" :model="reviewForm" label-width="72px" class="review-form">
          <el-form-item label="评分">
            <el-rate v-model="reviewForm.score" />
          </el-form-item>
          <el-form-item label="评价">
            <el-input v-model="reviewForm.content" type="textarea" :rows="3" maxlength="300" show-word-limit />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
          </el-form-item>
        </el-form>
        <el-alert
          v-else
          title="你已评价或无评价权限"
          type="info"
          :closable="false"
          show-icon
        />

        <div class="review-list">
          <el-empty v-if="reviews.length === 0" description="暂无评价" :image-size="72" />
          <div v-for="item in reviews" :key="item.id" class="review-item">
            <div class="review-head">
              <span>评价人 {{ item.reviewerId }} → {{ item.revieweeId }}</span>
              <el-rate :model-value="item.score" disabled />
            </div>
            <p>{{ item.content || '未填写评价内容' }}</p>
          </div>
        </div>
      </div>
    </el-card>
  </el-space>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  cancelOrderApi,
  completeOrderApi,
  createOrderReviewApi,
  disputeOrderApi,
  getOrderChatMessagesApi,
  getOrderDetailApi,
  getOrderReviewsApi,
  payServiceFeeApi,
  acceptOrderApi,
  rejectOrderApi,
  sendOrderChatMessageApi,
  startWorkApi,
} from '@/api/order';
import {
  ORDER_DISPUTE_STATUSES,
  ORDER_STATUS,
  getOrderStatusLabel,
  getOrderStatusTag,
} from '@/dicts';
import { formatMoney } from '@/utils/format';
import { useUserStore } from '@/store/modules/user';

const route = useRoute();
const userStore = useUserStore();

const loading = ref(false);
const actionLoading = ref(false);
const loadError = ref('');
const invalidOrderId = ref(false);

const detail = reactive({
  order: {},
  showContact: false,
  showChat: false,
  runnerContact: '',
  runnerDisplayName: '',
  runnerPaymentProfile: null,
});

const chatLoading = ref(false);
const chatSending = ref(false);
const chatMessages = ref([]);
const chatInput = ref('');

const reviewLoading = ref(false);
const reviewSubmitting = ref(false);
const reviews = ref([]);
const reviewForm = reactive({
  score: 5,
  content: '',
});

const order = computed(() => detail.order || {});
const orderStatusTag = computed(() => ({
  type: getOrderStatusTag(order.value.status),
  label: getOrderStatusLabel(order.value.status),
}));
const serviceFeeStatusTag = computed(() => (detail.order?.serviceFeeStatus === 'PAID' ? 'success' : 'warning'));
const currentUserId = computed(() => Number(userStore.userInfo?.id || 0));
const currentRole = computed(() => String(userStore.userInfo?.role || '').toUpperCase());
const isEmployer = computed(() => currentUserId.value > 0 && currentUserId.value === Number(order.value.employerId || 0));
const isWorker = computed(() => currentUserId.value > 0 && currentUserId.value === Number(order.value.workerUserId || 0));
const canClientOperate = computed(() =>
  currentRole.value === 'USER' || currentRole.value === 'EMPLOYER' || currentRole.value === 'CLIENT');
const isDisputeStatus = computed(() => ORDER_DISPUTE_STATUSES.includes(order.value.status));
const canReview = computed(() => {
  if (order.value.status !== ORDER_STATUS.COMPLETED) return false;
  return !reviews.value.some((item) => Number(item?.reviewerId || 0) === currentUserId.value);
});

function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  const ss = String(date.getSeconds()).padStart(2, '0');
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
}

async function loadChat() {
  if (!detail.showChat || !order.value.id) {
    chatMessages.value = [];
    return;
  }
  chatLoading.value = true;
  try {
    const data = await getOrderChatMessagesApi(order.value.id);
    chatMessages.value = Array.isArray(data) ? data : [];
  } catch {
    chatMessages.value = [];
  } finally {
    chatLoading.value = false;
  }
}

async function loadReviews() {
  if (!order.value.id || order.value.status !== ORDER_STATUS.COMPLETED) {
    reviews.value = [];
    return;
  }
  reviewLoading.value = true;
  try {
    const data = await getOrderReviewsApi(order.value.id);
    reviews.value = Array.isArray(data) ? data : [];
  } catch {
    reviews.value = [];
  } finally {
    reviewLoading.value = false;
  }
}

async function loadDetail() {
  const orderId = Number(route.params.id || 0);
  invalidOrderId.value = !orderId;
  loadError.value = '';
  if (invalidOrderId.value) return;

  loading.value = true;
  try {
    const data = await getOrderDetailApi(orderId);
    detail.order = data?.order || {};
    detail.showContact = Boolean(data?.showContact);
    detail.showChat = Boolean(data?.showChat);
    detail.runnerContact = data?.runnerContact || '';
    detail.runnerDisplayName = data?.runnerDisplayName || '';
    detail.runnerPaymentProfile = data?.runnerPaymentProfile || null;

    await Promise.allSettled([loadChat(), loadReviews()]);
  } catch (error) {
    const status = error?.response?.status;
    loadError.value = status === 404 ? '订单不存在' : '订单详情加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function isCancelError(error) {
  return error === 'cancel' || error === 'close';
}

async function handlePayServiceFee() {
  if (actionLoading.value || !isEmployer.value || !canClientOperate.value) return;
  try {
    await ElMessageBox.confirm('确认支付服务费并解锁执行者信息？', '服务费支付', {
      confirmButtonText: '确认支付',
      cancelButtonText: '取消',
      type: 'warning',
    });
    actionLoading.value = true;
    const paymentData = await payServiceFeeApi(order.value.id, 'WECHAT_PAY');
    const codeUrl = String(paymentData?.codeUrl || '');
    if (codeUrl.startsWith('mock://auto-success/')) {
      await loadDetail();
      ElMessage.success('测试模式已自动支付成功，等待执行者接单');
    } else if (codeUrl) {
      window.open(paymentData.codeUrl, '_blank');
      ElMessage.success('已生成支付二维码，请完成支付，支付后将进入待执行者接单');
    } else {
      ElMessage.success('支付单已创建，请完成支付，支付后将进入待执行者接单');
    }
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleAccept() {
  if (actionLoading.value || !isWorker.value) return;
  try {
    await ElMessageBox.confirm('确认接单并解锁联系方式？', '接单确认', { type: 'warning' });
    actionLoading.value = true;
    await acceptOrderApi(order.value.id);
    ElMessage.success('已接单，订单已解锁');
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleReject() {
  if (actionLoading.value || !isWorker.value) return;
  try {
    const { value: reason } = await ElMessageBox.prompt('可填写拒单原因（选填）', '拒绝接单', {
      confirmButtonText: '确认拒单',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：当前排期冲突',
    });
    actionLoading.value = true;
    await rejectOrderApi(order.value.id, String(reason || '').trim());
    ElMessage.success('已拒单，订单已关闭');
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleStart() {
  if (actionLoading.value || !isWorker.value) return;
  try {
    await ElMessageBox.confirm('确认开始工作？', '提示', { type: 'warning' });
    actionLoading.value = true;
    await startWorkApi(order.value.id);
    ElMessage.success('已进入执行中');
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleComplete() {
  if (actionLoading.value || !isEmployer.value || !canClientOperate.value) return;
  try {
    await ElMessageBox.confirm('确认订单已完成？', '提示', { type: 'warning' });
    actionLoading.value = true;
    await completeOrderApi(order.value.id);
    ElMessage.success('订单已完成');
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleCancelOrder() {
  if (actionLoading.value || !isEmployer.value || !canClientOperate.value) return;
  try {
    const { value: reason } = await ElMessageBox.prompt('可填写取消原因（选填）', '取消订单', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      inputPlaceholder: '例如：需求变更',
    });
    actionLoading.value = true;
    await cancelOrderApi(order.value.id, String(reason || '').trim());
    ElMessage.success('订单已取消');
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleDispute() {
  if (actionLoading.value || !isEmployer.value || !canClientOperate.value) return;
  try {
    const { value: reason } = await ElMessageBox.prompt('请输入争议原因：', '发起争议', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '原因不能为空',
    });
    actionLoading.value = true;
    await disputeOrderApi(order.value.id, reason);
    ElMessage.success('争议已提交');
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleSendMessage() {
  const content = chatInput.value.trim();
  if (!content || chatSending.value) return;
  chatSending.value = true;
  try {
    const message = await sendOrderChatMessageApi(order.value.id, content);
    chatMessages.value.push(message);
    chatInput.value = '';
  } catch {
    ElMessage.error('发送失败，请稍后重试');
  } finally {
    chatSending.value = false;
  }
}

async function submitReview() {
  if (!canReview.value || reviewSubmitting.value) return;
  reviewSubmitting.value = true;
  try {
    await createOrderReviewApi(order.value.id, reviewForm.score, reviewForm.content.trim());
    ElMessage.success('评价已提交');
    reviewForm.score = 5;
    reviewForm.content = '';
    await loadReviews();
  } catch {
    ElMessage.error('评价提交失败');
  } finally {
    reviewSubmitting.value = false;
  }
}

watch(() => route.params.id, () => {
  loadDetail();
}, { immediate: true });
</script>

<style scoped>
.action-panel {
  margin-top: 16px;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-tip {
  font-size: 13px;
  color: #6a8396;
}

.chat-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-item {
  border: 1px solid #dbe7f1;
  border-radius: 8px;
  padding: 10px;
}

.chat-meta {
  margin: 0;
  font-size: 12px;
  color: #6a8396;
}

.chat-content {
  margin: 8px 0;
}

.chat-editor {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-item {
  border: 1px solid #dbe7f1;
  border-radius: 8px;
  padding: 10px;
}

.review-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
