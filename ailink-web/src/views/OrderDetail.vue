<template>
  <el-space direction="vertical" fill>
    <el-card shadow="never">
      <template #header>订单详情</template>

      <el-alert v-if="invalidOrderId" title="订单ID无效" type="error" :closable="false" show-icon />
      <el-alert v-else-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

      <div v-else v-loading="loading">
        <el-descriptions title="订单基本信息" :column="2" border>
          <el-descriptions-item label="订单ID">{{ detail.id || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需求ID">{{ detail.demandId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="执行者ID">{{ detail.workerProfileId || detail.workerUserId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(detail.createdTime) }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="orderStatusTag.type">{{ orderStatusTag.label }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>金额拆分（盈利模型）</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单总金额">{{ formatMoney(detail.amount) }}</el-descriptions-item>
        <el-descriptions-item label="平台抽成金额">{{ formatMoney(detail.platformFee) }}</el-descriptions-item>
        <el-descriptions-item label="执行者实际收入">{{ formatMoney(detail.workerIncome) }}</el-descriptions-item>
        <el-descriptions-item label="抽成比例">{{ platformRateText }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never">
      <template #header>托管状态（保障）</template>
      <el-space>
        <span>是否已冻结：</span>
        <el-tag :type="escrowStatus.frozen ? 'warning' : 'info'">{{ escrowStatus.frozen ? '是' : '否' }}</el-tag>
        <span>是否已释放：</span>
        <el-tag :type="escrowStatus.released ? 'success' : 'info'">{{ escrowStatus.released ? '是' : '否' }}</el-tag>
        <span>是否已退款：</span>
        <el-tag :type="escrowStatus.refunded ? 'danger' : 'info'">{{ escrowStatus.refunded ? '是' : '否' }}</el-tag>
      </el-space>
    </el-card>
  </el-space>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { getOrderDetailApi } from '@/api/order';
import { formatMoney } from '@/utils/format';

const route = useRoute();
const loading = ref(false);
const loadError = ref('');
const invalidOrderId = ref(false);
const detail = reactive({
  id: '',
  demandId: '',
  workerProfileId: '',
  workerUserId: '',
  createdTime: '',
  status: '',
  payStatus: '',
  amount: 0,
  platformFee: 0,
  platformFeeRate: 0,
  workerIncome: 0,
  riskStatus: '',
});

const orderStatusMap = {
  CREATED: { type: 'info', label: '已创建' },
  ESCROWED: { type: 'warning', label: '已托管冻结' },
  IN_PROGRESS: { type: 'primary', label: '执行中' },
  COMPLETED: { type: 'success', label: '已完成' },
  DISPUTED: { type: 'danger', label: '争议中' },
  CLOSED: { type: 'info', label: '已关闭' },
};

const orderStatusTag = computed(() => orderStatusMap[detail.status] || { type: 'info', label: detail.status || '-' });

const platformRateText = computed(() => {
  let ratio = Number(detail.platformFeeRate || 0);
  if (!ratio && Number(detail.amount) > 0) {
    ratio = Number(detail.platformFee || 0) / Number(detail.amount);
  }
  return `${(ratio * 100).toFixed(2)}%`;
});

const escrowStatus = computed(() => {
  const status = detail.status;
  const paid = detail.payStatus === 'PAID';
  const frozen = paid && ['ESCROWED', 'IN_PROGRESS', 'DISPUTED'].includes(status);
  const released = status === 'COMPLETED';
  const refunded = paid && status === 'CLOSED' && detail.riskStatus === 'ABNORMAL';
  return {
    frozen,
    released,
    refunded,
  };
});

function formatDateTime(value) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  const ss = String(date.getSeconds()).padStart(2, '0');
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
}

function patchDetail(data) {
  Object.assign(detail, {
    id: data?.id || '',
    demandId: data?.demandId || '',
    workerProfileId: data?.workerProfileId || '',
    workerUserId: data?.workerUserId || '',
    createdTime: data?.createdTime || '',
    status: data?.status || '',
    payStatus: data?.payStatus || '',
    amount: Number(data?.amount || 0),
    platformFee: Number(data?.platformFee || 0),
    platformFeeRate: Number(data?.platformFeeRate || 0),
    workerIncome: Number(data?.workerIncome || 0),
    riskStatus: data?.riskStatus || '',
  });
}

async function loadDetail() {
  const orderId = Number(route.params.id || 0);
  invalidOrderId.value = !orderId;
  loadError.value = '';
  if (invalidOrderId.value) {
    return;
  }

  loading.value = true;
  try {
    const data = await getOrderDetailApi(orderId);
    patchDetail(data || {});
  } catch (error) {
    const status = error?.response?.status;
    if (status === 404) {
      loadError.value = '订单不存在';
    } else {
      loadError.value = '订单详情加载失败，请稍后重试';
    }
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadDetail();
});
</script>
