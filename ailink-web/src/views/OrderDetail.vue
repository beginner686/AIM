<template>
  <el-space direction="vertical" fill>
    <el-card shadow="never">
      <template #header>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span>{{ t('orderDetail.pageTitle') }}</span>
          <el-tag type="success" effect="dark" round style="display: flex; align-items: center; gap: 4px;">
            🛡️ {{ t('orderDetail.aiEscrowProtected') }}
          </el-tag>
        </div>
      </template>
      <el-alert v-if="invalidOrderId" :title="t('orderDetail.invalidOrderId')" type="error" :closable="false" show-icon />
      <el-alert v-else-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div v-else v-loading="loading">
        <el-descriptions :title="t('orderDetail.basicInfo')" :column="2" border>
          <el-descriptions-item :label="t('orderDetail.orderId')">{{ order.id || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('orderDetail.orderNo')">{{ order.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('orderDetail.demandId')">{{ order.demandId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('orderDetail.workerId')">{{ order.workerUserId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="t('orderDetail.createdTime')">{{ formatDateTime(order.createdTime) }}</el-descriptions-item>
          <el-descriptions-item :label="t('orderDetail.currentStatus')">
            <el-tag :type="orderStatusTag.type">{{ orderStatusTag.label }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>{{ t('orderDetail.feeInfo') }}</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('orderDetail.orderAmount')">{{ formatMoney(order.amount) }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.serviceFeeAmount')">{{ formatMoney(order.serviceFeeAmount) }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.serviceFeeStatus')">
          <el-tag :type="serviceFeeStatusTag">{{ order.serviceFeeStatus || 'REQUIRED' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.paidTime')">{{ formatDateTime(order.serviceFeePaidTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="action-panel">
      <template #header>{{ t('orderDetail.actionsTitle') }}</template>
      <div v-if="order.status === ORDER_STATUS.SERVICE_FEE_REQUIRED" class="action-row">
        <template v-if="isEmployer && canClientOperate">
          <el-button
            type="primary"
            :loading="actionLoading"
            @click="handlePayServiceFee"
          >
            {{ t('orderDetail.payAndUnlock') }}
          </el-button>
          <el-button
            type="danger"
            plain
            :loading="actionLoading"
            @click="handleCancelOrder"
          >
            {{ t('orderDetail.cancelOrder') }}
          </el-button>
        </template>
        <span class="action-tip">{{ serviceFeeRequiredTip }}</span>
      </div>

      <div v-else-if="order.status === ORDER_STATUS.WAIT_WORKER_ACCEPT || order.status === ORDER_STATUS.SERVICE_FEE_PAID" class="action-row">
        <template v-if="isWorker">
          <el-button type="success" :loading="actionLoading" @click="handleAccept">{{ t('orderDetail.acceptOrder') }}</el-button>
          <el-button type="danger" :loading="actionLoading" @click="handleReject">{{ t('orderDetail.rejectOrder') }}</el-button>
        </template>
        <el-button
          v-if="isEmployer && canClientOperate"
          type="danger"
          plain
          :loading="actionLoading"
          @click="handleCancelOrder"
        >
          {{ t('orderDetail.cancelOrder') }}
        </el-button>
        <span class="action-tip">{{ waitAcceptTip }}</span>
      </div>

      <div v-else-if="order.status === ORDER_STATUS.MATCH_UNLOCKED" class="action-row">
        <el-button
          v-if="isEmployer && canClientOperate && !employerDeclaredPaid"
          type="primary"
          :loading="actionLoading"
          @click="handleDeclarePaid"
        >
          {{ t('orderDetail.declarePaid') }}
        </el-button>
        <el-tag v-if="isEmployer && employerDeclaredPaid" type="success">{{ t('orderDetail.declaredPaidTag') }}</el-tag>
        <el-button
          v-if="isEmployer && canClientOperate"
          type="danger"
          plain
          :loading="actionLoading"
          @click="handleCancelOrder"
        >
          {{ t('orderDetail.cancelOrder') }}
        </el-button>
        <el-button
          v-if="(isEmployer && canClientOperate) || isWorker"
          type="warning"
          plain
          :loading="actionLoading"
          @click="handleDispute"
        >
          {{ t('orderDetail.raiseAiDispute') }}
        </el-button>
        <el-button
          v-if="isWorker"
          type="success"
          :loading="actionLoading"
          :disabled="!employerDeclaredPaid"
          @click="handleStart"
        >
          {{ t('orderDetail.startWork') }}
        </el-button>
        <span class="action-tip">{{ matchUnlockedTip }}</span>
      </div>

      <div v-else-if="order.status === ORDER_STATUS.IN_PROGRESS" class="action-row">
        <el-button v-if="isEmployer && canClientOperate" type="primary" :loading="actionLoading" @click="handleComplete">{{ t('orderDetail.completeOrder') }}</el-button>
        <el-button v-if="(isEmployer && canClientOperate) || isWorker" type="danger" :loading="actionLoading" @click="handleDispute">{{ t('orderDetail.raiseAiDispute') }}</el-button>
        <span class="action-tip">{{ inProgressTip }}</span>
      </div>

      <div v-else-if="order.status === ORDER_STATUS.COMPLETED" class="action-row">
        <el-tag type="success" size="large">{{ t('orderDetail.completedTag') }}</el-tag>
        <span class="action-tip">{{ t('orderDetail.completedTip') }}</span>
      </div>

      <div v-else-if="isDisputeStatus" class="action-row">
        <el-tag type="danger" size="large">{{ t('orderDetail.disputeTag') }}</el-tag>
        <span class="action-tip">{{ t('orderDetail.disputeTip') }}</span>
      </div>

      <div v-else class="action-row">
        <span class="action-tip">{{ t('orderDetail.noAction') }}</span>
      </div>
    </el-card>

    <el-card v-if="disputes.length > 0" shadow="never">
      <template #header><el-icon><Warning /></el-icon> {{ t('orderDetail.aiArbitrationReport') }}</template>
      <div v-for="d in disputes" :key="d.id" class="dispute-wrap">
        <p><strong>{{ t('orderDetail.reason') }}:</strong> {{ d.reason }}</p>
        <p v-if="d.evidenceUrl"><strong>{{ t('orderDetail.evidenceUrl') }}:</strong> <a :href="d.evidenceUrl" target="_blank">{{ d.evidenceUrl }}</a></p>
        <div v-if="d.aiAnalysisReport" class="ai-report-box">
          <p class="ai-sender">🤖 {{ t('orderDetail.aiSystemName') }}</p>
          <pre class="ai-report-text">{{ d.aiAnalysisReport }}</pre>
        </div>
        <el-alert v-else :title="t('orderDetail.aiAnalyzing')" type="info" :closable="false" show-icon />
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>{{ t('orderDetail.contactTitle') }}</template>
      <el-alert
        v-if="!detail.showContact"
        :title="t('orderDetail.contactLocked')"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item :label="t('orderDetail.runnerName')">{{ detail.runnerDisplayName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.contact')">{{ detail.runnerContact || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.paypalEmail')">
          {{ detail.runnerPaymentProfile?.paypalEmail || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.wiseId')">
          {{ detail.runnerPaymentProfile?.wiseId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.wiseLink')">
          <el-link v-if="detail.runnerPaymentProfile?.wiseLink" type="primary" @click.prevent="openExternalPaymentLink(detail.runnerPaymentProfile.wiseLink)">
            {{ t('orderDetail.openWise') }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.payoneerLink')">
          <el-link v-if="detail.runnerPaymentProfile?.payoneerLink" type="primary" @click.prevent="openExternalPaymentLink(detail.runnerPaymentProfile.payoneerLink)">
            {{ t('orderDetail.openPayoneer') }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.cryptoWallet')">
          {{ detail.runnerPaymentProfile?.cryptoWallet || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.paymentLink')">
          <el-link v-if="detail.runnerPaymentProfile?.paymentUrl" type="primary" @click.prevent="openExternalPaymentLink(detail.runnerPaymentProfile.paymentUrl)">
            {{ t('orderDetail.openPaymentLink') }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderDetail.currency')">{{ detail.runnerPaymentProfile?.currency || 'CNY' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="detail.showChat" shadow="never">
      <template #header>{{ t('orderDetail.chatTitle') }}</template>
      <div class="chat-wrap" v-loading="chatLoading">
        <div class="chat-list">
          <el-empty v-if="chatMessages.length === 0" :description="t('orderDetail.noMessages')" :image-size="72" />
          <div v-for="item in chatMessages" :key="item.id" :class="['chat-item', { 'ai-system-msg': item.senderId === 0 }]">
            <p class="chat-meta">
              <span v-if="item.senderId === 0" class="ai-sender">🤖 {{ t('orderDetail.aiSystemName') }}</span>
              <span v-else>{{ t('orderDetail.senderWithTime', { senderId: item.senderId, time: formatDateTime(item.createdTime) }) }}</span>
            </p>
            <p class="chat-content" v-html="renderChatContent(item.content)"></p>
            <el-alert
              v-if="item.warning"
              :title="item.warningMessage || t('orderDetail.chatWarning')"
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
            :placeholder="t('orderDetail.chatInput')"
          />
          <el-button
            type="primary"
            :loading="chatSending"
            :disabled="!chatInput.trim()"
            @click="handleSendMessage"
          >
            {{ t('orderDetail.sendMessage') }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card v-if="order.status === ORDER_STATUS.COMPLETED" shadow="never">
      <template #header>{{ t('orderDetail.reviewTitle') }}</template>
      <div class="review-wrap" v-loading="reviewLoading">
        <el-form v-if="canReview" :model="reviewForm" label-width="72px" class="review-form">
          <el-form-item :label="t('orderDetail.reviewScore')">
            <el-rate v-model="reviewForm.score" />
          </el-form-item>
          <el-form-item :label="t('orderDetail.reviewContent')">
            <el-input v-model="reviewForm.content" type="textarea" :rows="3" maxlength="300" show-word-limit />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">{{ t('orderDetail.submitReview') }}</el-button>
          </el-form-item>
        </el-form>
        <el-alert
          v-else
          :title="t('orderDetail.noReviewPermission')"
          type="info"
          :closable="false"
          show-icon
        />

        <div class="review-list">
          <el-empty v-if="reviews.length === 0" :description="t('orderDetail.noReviews')" :image-size="72" />
          <div v-for="item in reviews" :key="item.id" class="review-item">
            <div class="review-head">
              <span>{{ t('orderDetail.reviewerFlow', { reviewerId: item.reviewerId, revieweeId: item.revieweeId }) }}</span>
              <el-rate :model-value="item.score" disabled />
            </div>
            <p>{{ item.content || t('orderDetail.reviewEmptyContent') }}</p>
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
import { useI18n } from 'vue-i18n';
import {
  cancelOrderApi,
  completeOrderApi,
  declarePaidApi,
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
  getOrderDisputesApi,
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
const { t } = useI18n();

const loading = ref(false);
const actionLoading = ref(false);
const loadError = ref('');
const invalidOrderId = ref(false);
const disputes = ref([]);

function renderChatContent(rawContent) {
  if (!rawContent) return '';
  return rawContent.replace(/\\n/g, '<br/>');
}

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
const employerDeclaredPaid = computed(() => {
  const raw = order.value?.employerDeclaredPaid;
  if (typeof raw === 'boolean') return raw;
  if (typeof raw === 'number') return raw === 1;
  if (typeof raw === 'string') return raw.toLowerCase() === 'true' || raw === '1';
  return false;
});
const canReview = computed(() => {
  if (order.value.status !== ORDER_STATUS.COMPLETED) return false;
  return !reviews.value.some((item) => Number(item?.reviewerId || 0) === currentUserId.value);
});
const serviceFeeRequiredTip = computed(() => {
  if (isEmployer.value) {
    if (canClientOperate.value) return t('orderDetail.tipServiceFeeEmployer');
    return t('orderDetail.tipNoPayPermission');
  }
  return t('orderDetail.tipWaitEmployerPay');
});
const waitAcceptTip = computed(() => {
  if (isWorker.value) return t('orderDetail.tipWaitAcceptWorker');
  if (isEmployer.value && canClientOperate.value) return t('orderDetail.tipWaitAcceptEmployer');
  return t('orderDetail.tipWaitAcceptOther');
});
const matchUnlockedTip = computed(() => {
  if (isWorker.value) {
    return employerDeclaredPaid.value
      ? t('orderDetail.tipMatchUnlockedWorker')
      : t('orderDetail.tipNeedEmployerDeclare');
  }
  if (isEmployer.value && canClientOperate.value) {
    return employerDeclaredPaid.value
      ? t('orderDetail.tipMatchUnlockedEmployer')
      : t('orderDetail.tipEmployerDeclareAfterTransfer');
  }
  return t('orderDetail.tipMatchUnlockedOther');
});
const inProgressTip = computed(() => {
  if (isEmployer.value) {
    if (canClientOperate.value) return t('orderDetail.tipInProgressEmployer');
    return t('orderDetail.tipNoClientPermission');
  }
  return t('orderDetail.tipInProgressOther');
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
    
    if (isDisputeStatus.value || data.order.status === 'COMPLETED' || data.order.status === 'CLOSED') {
      try {
        const d = await getOrderDisputesApi(orderId);
        disputes.value = Array.isArray(d) ? d : [];
      } catch(e) {}
    } else {
      disputes.value = [];
    }
  } catch (error) {
    const status = error?.response?.status;
    loadError.value = status === 404 ? t('orderDetail.notFound') : t('orderDetail.loadFailed');
  } finally {
    loading.value = false;
  }
}

function isCancelError(error) {
  return error === 'cancel' || error === 'close';
}

async function openExternalPaymentLink(url) {
  const target = String(url || '').trim();
  if (!target) return;
  try {
    await ElMessageBox.confirm(t('orderDetail.externalPaymentRiskMessage'), t('orderDetail.externalPaymentRiskTitle'), {
      type: 'warning',
      confirmButtonText: t('orderDetail.continueOpen'),
      cancelButtonText: t('orderDetail.cancel'),
    });
    window.open(target, '_blank', 'noopener,noreferrer');
  } catch (error) {
    if (!isCancelError(error)) {}
  }
}

async function handlePayServiceFee() {
  if (actionLoading.value || !isEmployer.value || !canClientOperate.value) return;
  try {
    await ElMessageBox.confirm(t('orderDetail.payConfirmMessage'), t('orderDetail.payConfirmTitle'), {
      confirmButtonText: t('orderDetail.confirmPay'),
      cancelButtonText: t('orderDetail.cancel'),
      type: 'warning',
    });
    actionLoading.value = true;
    const paymentData = await payServiceFeeApi(order.value.id, 'WECHAT_PAY');
    const codeUrl = String(paymentData?.codeUrl || '');
    if (codeUrl.startsWith('mock://auto-success/')) {
      await loadDetail();
      ElMessage.success(t('orderDetail.payMockSuccess'));
    } else if (codeUrl) {
      window.open(paymentData.codeUrl, '_blank');
      ElMessage.success(t('orderDetail.payQrCreated'));
    } else {
      ElMessage.success(t('orderDetail.payOrderCreated'));
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
    await ElMessageBox.confirm(t('orderDetail.acceptConfirmMessage'), t('orderDetail.acceptConfirmTitle'), { type: 'warning' });
    actionLoading.value = true;
    await acceptOrderApi(order.value.id);
    ElMessage.success(t('orderDetail.acceptSuccess'));
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
    const { value: reason } = await ElMessageBox.prompt(t('orderDetail.rejectPromptMessage'), t('orderDetail.rejectPromptTitle'), {
      confirmButtonText: t('orderDetail.confirmReject'),
      cancelButtonText: t('orderDetail.cancel'),
      inputPlaceholder: t('orderDetail.rejectPlaceholder'),
    });
    actionLoading.value = true;
    await rejectOrderApi(order.value.id, String(reason || '').trim());
    ElMessage.success(t('orderDetail.rejectSuccess'));
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleStart() {
  if (actionLoading.value || !isWorker.value) return;
  if (!employerDeclaredPaid.value) {
    ElMessage.warning(t('orderDetail.tipNeedEmployerDeclare'));
    return;
  }
  try {
    await ElMessageBox.confirm(t('orderDetail.startConfirmMessage'), t('orderDetail.tipTitle'), { type: 'warning' });
    actionLoading.value = true;
    await startWorkApi(order.value.id);
    ElMessage.success(t('orderDetail.startSuccess'));
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleDeclarePaid() {
  if (actionLoading.value || !isEmployer.value || !canClientOperate.value || employerDeclaredPaid.value) return;
  try {
    await ElMessageBox.confirm(t('orderDetail.declarePaidConfirmMessage'), t('orderDetail.declarePaidConfirmTitle'), {
      type: 'warning',
      confirmButtonText: t('orderDetail.submit'),
      cancelButtonText: t('orderDetail.cancel'),
    });
    actionLoading.value = true;
    await declarePaidApi(order.value.id, 'EMPLOYER_DECLARED_PAID');
    ElMessage.success(t('orderDetail.declarePaidDone'));
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
    await ElMessageBox.confirm(t('orderDetail.completeConfirmMessage'), t('orderDetail.tipTitle'), { type: 'warning' });
    actionLoading.value = true;
    await completeOrderApi(order.value.id);
    ElMessage.success(t('orderDetail.completeSuccess'));
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
    const { value: reason } = await ElMessageBox.prompt(t('orderDetail.cancelPromptMessage'), t('orderDetail.cancelPromptTitle'), {
      confirmButtonText: t('orderDetail.confirmCancel'),
      cancelButtonText: t('orderDetail.back'),
      inputPlaceholder: t('orderDetail.cancelPlaceholder'),
    });
    actionLoading.value = true;
    await cancelOrderApi(order.value.id, String(reason || '').trim());
    ElMessage.success(t('orderDetail.cancelSuccess'));
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleDispute() {
  if (actionLoading.value) return;
  try {
    const { value: reason } = await ElMessageBox.prompt(t('orderDetail.disputePromptMessage'), t('orderDetail.disputePromptTitle'), {
      confirmButtonText: t('orderDetail.submit'),
      cancelButtonText: t('orderDetail.cancel'),
      inputPattern: /\S+/,
      inputErrorMessage: t('orderDetail.disputeReasonRequired'),
    });
    actionLoading.value = true;
    await disputeOrderApi(order.value.id, reason);
    ElMessage.success(t('orderDetail.disputeSuccess'));
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
    ElMessage.error(t('orderDetail.sendFailed'));
  } finally {
    chatSending.value = false;
  }
}

async function submitReview() {
  if (!canReview.value || reviewSubmitting.value) return;
  reviewSubmitting.value = true;
  try {
    await createOrderReviewApi(order.value.id, reviewForm.score, reviewForm.content.trim());
    ElMessage.success(t('orderDetail.reviewSubmitSuccess'));
    reviewForm.score = 5;
    reviewForm.content = '';
    await loadReviews();
  } catch {
    ElMessage.error(t('orderDetail.reviewSubmitFailed'));
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
  white-space: pre-wrap;
  word-break: break-all;
}

.ai-system-msg {
  background-color: var(--el-color-success-light-9);
  border-left: 4px solid var(--el-color-success);
}

.ai-sender {
  color: var(--el-color-success);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
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

.dispute-wrap {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}

.ai-report-box {
  background-color: var(--el-color-success-light-9);
  border-left: 4px solid var(--el-color-success);
  padding: 12px;
  margin-top: 12px;
}

.ai-report-text {
  font-family: inherit;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 8px 0 0 0;
  color: var(--el-text-color-regular);
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
