<template>
  <div class="match-page">
    <section class="hero-strip">
      <p class="kicker">{{ t('orderMatch.kicker') }}</p>
      <h1>{{ t('orderMatch.title') }}</h1>
      <p class="sub">{{ t('orderMatch.subtitle') }}</p>
    </section>

    <el-alert v-if="invalidOrderId" :title="t('orderMatch.invalidOrderId')" type="error" :closable="false" show-icon />
    <el-alert v-else-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

    <el-card v-else shadow="never" class="panel" v-loading="loading">
      <el-steps :active="stepIndex" finish-status="success" align-center class="steps">
        <el-step :title="t('orderMatch.stepPayFee')" />
        <el-step :title="t('orderMatch.stepUnlock')" />
        <el-step :title="t('orderMatch.stepInProgress')" />
      </el-steps>

      <el-descriptions :title="t('orderMatch.orderSummary')" :column="2" border>
        <el-descriptions-item :label="t('orderMatch.orderId')">{{ order.id || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.orderNo')">{{ order.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.status')">
          <el-tag :type="orderStatusTag.type">{{ orderStatusTag.label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.serviceFeeStatus')">{{ order.serviceFeeStatus || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        class="blocking-alert"
        :title="blockingTip"
        :type="blockingTipType"
        :closable="false"
        show-icon
      />

      <section class="timeline-block">
        <h3 class="timeline-title">{{ t('orderMatch.progressTitle') }}</h3>
        <el-timeline>
          <el-timeline-item
            :type="order.serviceFeePaidTime ? 'success' : 'info'"
            :timestamp="formatDateTime(order.serviceFeePaidTime)"
          >
            {{ t('orderMatch.timelineServiceFeePaid') }}
          </el-timeline-item>
          <el-timeline-item
            :type="employerDeclaredPaid ? 'success' : 'warning'"
            :timestamp="formatDateTime(order.employerDeclaredPaidTime)"
          >
            {{ t('orderMatch.timelineEmployerDeclared') }}
          </el-timeline-item>
          <el-timeline-item
            :type="workerConfirmedPaid ? 'success' : 'warning'"
            :timestamp="formatDateTime(order.workerConfirmedPaidTime)"
          >
            {{ t('orderMatch.timelineWorkerConfirmed') }}
          </el-timeline-item>
        </el-timeline>
      </section>

      <el-alert
        class="risk-alert"
        :title="t('orderMatch.riskWarning')"
        type="warning"
        :closable="false"
        show-icon
      />

      <el-descriptions v-if="detail.showContact" :title="t('orderMatch.contactTitle')" :column="1" border class="contact-block">
        <el-descriptions-item :label="t('orderMatch.runnerName')">{{ detail.runnerDisplayName || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.contact')">{{ detail.runnerContact || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.paypalEmail')">{{ detail.runnerPaymentProfile?.paypalEmail || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.wiseId')">{{ detail.runnerPaymentProfile?.wiseId || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.wiseLink')">
          <el-link v-if="detail.runnerPaymentProfile?.wiseLink" type="primary" @click.prevent="openExternalPaymentLink(detail.runnerPaymentProfile.wiseLink)">
            {{ t('orderMatch.openWise') }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.payoneerLink')">
          <el-link v-if="detail.runnerPaymentProfile?.payoneerLink" type="primary" @click.prevent="openExternalPaymentLink(detail.runnerPaymentProfile.payoneerLink)">
            {{ t('orderMatch.openPayoneer') }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.cryptoWallet')">{{ detail.runnerPaymentProfile?.cryptoWallet || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.paymentLink')">
          <el-link v-if="detail.runnerPaymentProfile?.paymentUrl" type="primary" @click.prevent="openExternalPaymentLink(detail.runnerPaymentProfile.paymentUrl)">
            {{ t('orderMatch.openPaymentLink') }}
          </el-link>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderMatch.currency')">{{ detail.runnerPaymentProfile?.currency || 'CNY' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-else
        class="contact-block"
        :title="t('orderMatch.contactLocked')"
        type="warning"
        :closable="false"
        show-icon
      />

      <div class="action-wrap">
        <el-alert :title="actionTip" :type="actionTipType" :closable="false" show-icon />
        <div class="actions">
          <el-button v-if="needGoCheckout" type="primary" @click="goCheckout">{{ t('orderMatch.goCheckout') }}</el-button>
          <el-button
            v-if="canDeclarePaid"
            type="primary"
            :loading="actionLoading"
            @click="handleDeclarePaid"
          >
            {{ t('orderMatch.declarePaid') }}
          </el-button>
          <el-tag v-if="isEmployer && employerDeclaredPaid" type="success">{{ t('orderMatch.declaredPaidTag') }}</el-tag>
          <el-button
            v-if="canStartWork"
            type="success"
            :loading="actionLoading"
            @click="handleStartWork"
          >
            {{ t('orderMatch.startWork') }}
          </el-button>
          <el-button :loading="loading" @click="loadDetail">{{ t('orderMatch.refresh') }}</el-button>
          <el-button @click="goDetail">{{ t('orderMatch.viewDetail') }}</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useI18n } from 'vue-i18n';
import { declarePaidApi, getOrderDetailApi, startWorkApi } from '@/api/order';
import { getOrderStatusLabel, getOrderStatusTag, ORDER_STATUS } from '@/dicts';
import { useUserStore } from '@/store/modules/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const { t } = useI18n();

const loading = ref(false);
const actionLoading = ref(false);
const loadError = ref('');
const invalidOrderId = ref(false);
const detail = reactive({
  order: {},
  showContact: false,
  runnerDisplayName: '',
  runnerContact: '',
  runnerPaymentProfile: null,
});

const orderId = computed(() => Number(route.params.id || 0));
const order = computed(() => detail.order || {});
const orderStatusTag = computed(() => ({
  type: getOrderStatusTag(order.value.status),
  label: getOrderStatusLabel(order.value.status),
}));
const currentUserId = computed(() => Number(userStore.userInfo?.id || 0));
const currentRole = computed(() => String(userStore.userInfo?.role || '').toUpperCase());
const isEmployer = computed(() => currentUserId.value > 0 && currentUserId.value === Number(order.value.employerId || 0));
const isWorker = computed(() => currentUserId.value > 0 && currentUserId.value === Number(order.value.workerUserId || 0));
const canClientOperate = computed(() =>
  currentRole.value === 'USER' || currentRole.value === 'EMPLOYER' || currentRole.value === 'CLIENT');
const employerDeclaredPaid = computed(() => {
  const raw = order.value?.employerDeclaredPaid;
  if (typeof raw === 'boolean') return raw;
  if (typeof raw === 'number') return raw === 1;
  if (typeof raw === 'string') return raw.toLowerCase() === 'true' || raw === '1';
  return false;
});
const workerConfirmedPaid = computed(() => {
  const raw = order.value?.workerConfirmedPaid;
  if (typeof raw === 'boolean') return raw;
  if (typeof raw === 'number') return raw === 1;
  if (typeof raw === 'string') return raw.toLowerCase() === 'true' || raw === '1';
  return false;
});

const needGoCheckout = computed(() =>
  order.value.status === ORDER_STATUS.CREATED || order.value.status === ORDER_STATUS.SERVICE_FEE_REQUIRED);
const canDeclarePaid = computed(() =>
  isEmployer.value && canClientOperate.value && order.value.status === ORDER_STATUS.MATCH_UNLOCKED && !employerDeclaredPaid.value);
const canStartWork = computed(() =>
  isWorker.value && order.value.status === ORDER_STATUS.MATCH_UNLOCKED && employerDeclaredPaid.value);

const stepIndex = computed(() => {
  if (order.value.status === ORDER_STATUS.IN_PROGRESS || order.value.status === ORDER_STATUS.COMPLETED) return 2;
  if (
    order.value.status === ORDER_STATUS.SERVICE_FEE_PAID
    || order.value.status === ORDER_STATUS.WAIT_WORKER_ACCEPT
    || order.value.status === ORDER_STATUS.MATCH_UNLOCKED
  ) return 1;
  return 0;
});

const actionTipType = computed(() => {
  if (needGoCheckout.value) return 'warning';
  if (order.value.status === ORDER_STATUS.MATCH_UNLOCKED) return 'success';
  return 'info';
});

const actionTip = computed(() => {
  if (needGoCheckout.value) return t('orderMatch.tipNeedCheckout');
  if (order.value.status === ORDER_STATUS.SERVICE_FEE_PAID || order.value.status === ORDER_STATUS.WAIT_WORKER_ACCEPT) {
    return t('orderMatch.tipWaitingAccept');
  }
  if (order.value.status === ORDER_STATUS.MATCH_UNLOCKED) {
    if (isWorker.value && !employerDeclaredPaid.value) return t('orderMatch.tipNeedEmployerDeclare');
    if (isEmployer.value && !employerDeclaredPaid.value) return t('orderMatch.tipEmployerDeclare');
    if (isWorker.value && employerDeclaredPaid.value) return t('orderMatch.tipWorkerCanStart');
    return t('orderMatch.tipWaitingStart');
  }
  if (order.value.status === ORDER_STATUS.IN_PROGRESS) return t('orderMatch.tipInProgress');
  if (order.value.status === ORDER_STATUS.COMPLETED) return t('orderMatch.tipCompleted');
  return t('orderMatch.tipViewDetail');
});

const blockingTipType = computed(() => {
  if (needGoCheckout.value) return 'warning';
  if (order.value.status === ORDER_STATUS.SERVICE_FEE_PAID || order.value.status === ORDER_STATUS.WAIT_WORKER_ACCEPT) return 'warning';
  if (order.value.status === ORDER_STATUS.MATCH_UNLOCKED && !employerDeclaredPaid.value) return 'warning';
  if (order.value.status === ORDER_STATUS.MATCH_UNLOCKED && employerDeclaredPaid.value && !workerConfirmedPaid.value) return 'warning';
  if (order.value.status === ORDER_STATUS.IN_PROGRESS) return 'info';
  if (order.value.status === ORDER_STATUS.COMPLETED) return 'success';
  if (order.value.status === ORDER_STATUS.CLOSED) return 'info';
  return 'info';
});

const blockingTip = computed(() => {
  if (needGoCheckout.value) return t('orderMatch.blockingNeedPayFee');
  if (order.value.status === ORDER_STATUS.SERVICE_FEE_PAID || order.value.status === ORDER_STATUS.WAIT_WORKER_ACCEPT) {
    return t('orderMatch.blockingNeedWorkerAccept');
  }
  if (order.value.status === ORDER_STATUS.MATCH_UNLOCKED && !employerDeclaredPaid.value) {
    return t('orderMatch.blockingNeedEmployerDeclare');
  }
  if (order.value.status === ORDER_STATUS.MATCH_UNLOCKED && employerDeclaredPaid.value && !workerConfirmedPaid.value) {
    return t('orderMatch.blockingNeedWorkerStart');
  }
  if (order.value.status === ORDER_STATUS.IN_PROGRESS) return t('orderMatch.blockingInProgress');
  if (order.value.status === ORDER_STATUS.COMPLETED) return t('orderMatch.blockingCompleted');
  if (order.value.status === ORDER_STATUS.CLOSED) return t('orderMatch.blockingClosed');
  return t('orderMatch.blockingUnknown');
});

function isCancelError(error) {
  return error === 'cancel' || error === 'close';
}

function formatDateTime(value) {
  if (!value) return t('orderMatch.timelineNotYet');
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

async function loadDetail() {
  invalidOrderId.value = !orderId.value;
  loadError.value = '';
  if (invalidOrderId.value) return;
  loading.value = true;
  try {
    const data = await getOrderDetailApi(orderId.value);
    detail.order = data?.order || {};
    detail.showContact = Boolean(data?.showContact);
    detail.runnerDisplayName = data?.runnerDisplayName || '';
    detail.runnerContact = data?.runnerContact || '';
    detail.runnerPaymentProfile = data?.runnerPaymentProfile || null;
  } catch (error) {
    const status = error?.response?.status;
    loadError.value = status === 404 ? t('orderMatch.notFound') : t('orderMatch.loadFailed');
  } finally {
    loading.value = false;
  }
}

async function openExternalPaymentLink(url) {
  const target = String(url || '').trim();
  if (!target) return;
  try {
    await ElMessageBox.confirm(t('orderMatch.externalRiskMessage'), t('orderMatch.externalRiskTitle'), {
      type: 'warning',
      confirmButtonText: t('orderMatch.continueOpen'),
      cancelButtonText: t('orderMatch.cancel'),
    });
    window.open(target, '_blank', 'noopener,noreferrer');
  } catch (error) {
    if (!isCancelError(error)) {}
  }
}

async function handleDeclarePaid() {
  if (!canDeclarePaid.value || actionLoading.value) return;
  try {
    await ElMessageBox.confirm(t('orderMatch.declareConfirmMessage'), t('orderMatch.declareConfirmTitle'), {
      type: 'warning',
      confirmButtonText: t('orderMatch.confirm'),
      cancelButtonText: t('orderMatch.cancel'),
    });
    actionLoading.value = true;
    await declarePaidApi(order.value.id, 'EMPLOYER_DECLARED_PAID');
    ElMessage.success(t('orderMatch.declareDone'));
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleStartWork() {
  if (!canStartWork.value || actionLoading.value) return;
  try {
    await ElMessageBox.confirm(t('orderMatch.startConfirmMessage'), t('orderMatch.startConfirmTitle'), {
      type: 'warning',
      confirmButtonText: t('orderMatch.confirm'),
      cancelButtonText: t('orderMatch.cancel'),
    });
    actionLoading.value = true;
    await startWorkApi(order.value.id);
    ElMessage.success(t('orderMatch.startDone'));
    await loadDetail();
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

function goCheckout() {
  if (!order.value.id) return;
  router.push(`/order/checkout/${order.value.id}`);
}

function goDetail() {
  if (!order.value.id) return;
  router.push(`/order/${order.value.id}`);
}

watch(() => route.params.id, () => {
  loadDetail();
}, { immediate: true });
</script>

<style scoped>
.match-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-strip {
  border-radius: 18px;
  padding: 24px 28px;
  background: linear-gradient(135deg, #0f2f57 0%, #0d6a64 100%);
  color: #f4fbff;
}

.kicker {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(220, 241, 255, 0.85);
}

.hero-strip h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.1;
  font-weight: 800;
}

.sub {
  margin: 10px 0 0;
  font-size: 15px;
  color: rgba(230, 243, 255, 0.92);
}

.panel {
  border-radius: 16px;
}

.steps {
  margin-bottom: 18px;
}

.blocking-alert {
  margin-top: 16px;
}

.timeline-block {
  margin-top: 16px;
  border: 1px solid #dbe7f1;
  border-radius: 12px;
  padding: 12px 14px 4px;
  background: #fdfefe;
}

.timeline-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 700;
  color: #102449;
}

.risk-alert {
  margin-top: 16px;
}

.contact-block {
  margin-top: 16px;
}

.action-wrap {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
