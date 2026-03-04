<template>
  <div class="checkout-page">
    <section class="hero-strip">
      <p class="kicker">{{ t('orderCheckout.kicker') }}</p>
      <h1>{{ t('orderCheckout.title') }}</h1>
      <p class="sub">{{ t('orderCheckout.subtitle') }}</p>
    </section>

    <el-alert v-if="invalidOrderId" :title="t('orderCheckout.invalidOrderId')" type="error" :closable="false" show-icon />
    <el-alert v-else-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

    <el-card v-else shadow="never" class="panel" v-loading="loading">
      <el-steps :active="stepIndex" finish-status="success" align-center class="steps">
        <el-step :title="t('orderCheckout.stepCreate')" />
        <el-step :title="t('orderCheckout.stepPayFee')" />
        <el-step :title="t('orderCheckout.stepUnlock')" />
      </el-steps>

      <el-descriptions :title="t('orderCheckout.orderSummary')" :column="2" border>
        <el-descriptions-item :label="t('orderCheckout.orderId')">{{ order.id || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderCheckout.orderNo')">{{ order.orderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderCheckout.status')">
          <el-tag :type="orderStatusTag.type">{{ orderStatusTag.label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item :label="t('orderCheckout.serviceFeeStatus')">{{ order.serviceFeeStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderCheckout.amount')">¥{{ formatMoney(order.amount) }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderCheckout.serviceFeeAmount')">¥{{ formatMoney(order.serviceFeeAmount) }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderCheckout.createdTime')">{{ formatDateTime(order.createdTime) }}</el-descriptions-item>
        <el-descriptions-item :label="t('orderCheckout.paidTime')">{{ formatDateTime(order.serviceFeePaidTime) }}</el-descriptions-item>
      </el-descriptions>

      <div class="action-wrap">
        <el-alert
          :title="checkoutTip"
          :type="tipType"
          :closable="false"
          show-icon
        />
        <div class="actions">
          <template v-if="canPayServiceFee">
            <el-button type="primary" :loading="actionLoading" @click="handlePay('WECHAT_PAY')">
              {{ t('orderCheckout.payWechat') }}
            </el-button>
            <el-button type="primary" plain :loading="actionLoading" @click="handlePay('ALIPAY')">
              {{ t('orderCheckout.payAlipay') }}
            </el-button>
            <el-button :loading="loading" @click="handleCheckPayment">{{ t('orderCheckout.checkResult') }}</el-button>
          </template>
          <template v-else>
            <el-button v-if="canGoMatch" type="success" @click="goMatch">{{ t('orderCheckout.goMatch') }}</el-button>
            <el-button :loading="loading" @click="loadDetail">{{ t('orderCheckout.refresh') }}</el-button>
          </template>
          <el-button @click="goDetail">{{ t('orderCheckout.viewDetail') }}</el-button>
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
import { getOrderStatusLabel, getOrderStatusTag, ORDER_STATUS } from '@/dicts';
import { getOrderDetailApi, payServiceFeeApi } from '@/api/order';
import { formatMoney } from '@/utils/format';
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
});

const order = computed(() => detail.order || {});
const orderId = computed(() => Number(route.params.id || 0));
const orderStatusTag = computed(() => ({
  type: getOrderStatusTag(order.value.status),
  label: getOrderStatusLabel(order.value.status),
}));
const currentUserId = computed(() => Number(userStore.userInfo?.id || 0));
const currentRole = computed(() => String(userStore.userInfo?.role || '').toUpperCase());
const isEmployer = computed(() => currentUserId.value > 0 && currentUserId.value === Number(order.value.employerId || 0));
const canClientOperate = computed(() =>
  currentRole.value === 'USER' || currentRole.value === 'EMPLOYER' || currentRole.value === 'CLIENT');

const canPayServiceFee = computed(() =>
  isEmployer.value
  && canClientOperate.value
  && (order.value.status === ORDER_STATUS.CREATED || order.value.status === ORDER_STATUS.SERVICE_FEE_REQUIRED));

const canGoMatch = computed(() => [
  ORDER_STATUS.SERVICE_FEE_PAID,
  ORDER_STATUS.WAIT_WORKER_ACCEPT,
  ORDER_STATUS.MATCH_UNLOCKED,
  ORDER_STATUS.IN_PROGRESS,
  ORDER_STATUS.COMPLETED,
  ORDER_STATUS.DISPUTE,
  ORDER_STATUS.ARBITRATION,
].includes(order.value.status));

const stepIndex = computed(() => {
  if (canGoMatch.value) return 2;
  if (canPayServiceFee.value) return 1;
  return 0;
});

const tipType = computed(() => {
  if (canPayServiceFee.value) return 'warning';
  if (canGoMatch.value) return 'success';
  return 'info';
});

const checkoutTip = computed(() => {
  if (canPayServiceFee.value) return t('orderCheckout.tipNeedPay');
  if (canGoMatch.value) return t('orderCheckout.tipPaidDone');
  if (!isEmployer.value) return t('orderCheckout.tipNoPermission');
  return t('orderCheckout.tipUnavailable');
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

async function loadDetail() {
  invalidOrderId.value = !orderId.value;
  loadError.value = '';
  if (invalidOrderId.value) return;
  loading.value = true;
  try {
    const data = await getOrderDetailApi(orderId.value);
    detail.order = data?.order || {};
  } catch (error) {
    const status = error?.response?.status;
    loadError.value = status === 404 ? t('orderCheckout.notFound') : t('orderCheckout.loadFailed');
  } finally {
    loading.value = false;
  }
}

function isCancelError(error) {
  return error === 'cancel' || error === 'close';
}

async function handlePay(channel) {
  if (!canPayServiceFee.value || actionLoading.value) return;
  const channelLabel = channel === 'ALIPAY' ? t('orderCheckout.channelAlipay') : t('orderCheckout.channelWechat');
  try {
    await ElMessageBox.confirm(
      t('orderCheckout.payConfirmMessage', { channel: channelLabel }),
      t('orderCheckout.payConfirmTitle'),
      {
        type: 'warning',
        confirmButtonText: t('orderCheckout.confirmPay'),
        cancelButtonText: t('orderCheckout.cancel'),
      },
    );
    actionLoading.value = true;
    const paymentData = await payServiceFeeApi(order.value.id, channel);
    const codeUrl = String(paymentData?.codeUrl || '');
    if (codeUrl.startsWith('mock://auto-success/')) {
      await loadDetail();
      ElMessage.success(t('orderCheckout.paySuccessMock'));
      goMatch();
      return;
    }
    if (codeUrl) {
      window.open(codeUrl, '_blank', 'noopener,noreferrer');
      ElMessage.success(t('orderCheckout.payQrCreated'));
    } else {
      ElMessage.success(t('orderCheckout.payCreated'));
    }
    await loadDetail();
    if (canGoMatch.value) {
      goMatch();
    }
  } catch (error) {
    if (!isCancelError(error)) {}
  } finally {
    actionLoading.value = false;
  }
}

async function handleCheckPayment() {
  await loadDetail();
  if (canGoMatch.value) {
    goMatch();
    return;
  }
  ElMessage.info(t('orderCheckout.checkPending'));
}

function goMatch() {
  if (!order.value.id) return;
  router.push(`/order/match/${order.value.id}`);
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
.checkout-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-strip {
  border-radius: 18px;
  padding: 24px 28px;
  background: linear-gradient(135deg, #0f2f57 0%, #126f78 100%);
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
