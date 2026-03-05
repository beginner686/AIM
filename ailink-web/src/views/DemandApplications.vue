<template>
  <div class="apply-page">
    <section class="hero-strip reveal-up">
      <div class="orb orb-a" />
      <div class="orb orb-b" />

      <div class="hero-content">
        <p class="kicker">{{ t('demandApplications.kicker') }}</p>
        <h1>
          <span class="gradient-text">{{ t('demandApplications.title') }}</span>
        </h1>
        <p class="sub">{{ t('demandApplications.subtitle') }}</p>
      </div>
    </section>

    <el-card class="list-card reveal-up delay-1" shadow="never">
      <template #header>
          <div class="section-head">
            <h2>{{ t('demandApplications.ownerSectionTitle') }}</h2>
            <div class="head-actions">
              <el-button size="small" :loading="ownerLoading" @click="loadOwnerApplications">{{ t('demandApplications.refresh') }}</el-button>
            </div>
          </div>
      </template>
      <div class="toolbar">
        <el-select v-model="selectedDemandId" :placeholder="t('demandApplications.selectDemand')" clearable filterable class="toolbar-item" @change="loadOwnerApplications">
          <el-option v-for="item in myDemands" :key="item.id" :label="formatDemandLabel(item)" :value="item.id" />
        </el-select>
        <el-select v-model="ownerStatusFilter" :placeholder="t('demandApplications.statusFilter')" clearable class="toolbar-item" @change="loadOwnerApplications">
          <el-option :label="statusText('PENDING')" value="PENDING" />
          <el-option :label="statusText('ACCEPTED')" value="ACCEPTED" />
          <el-option :label="statusText('REJECTED')" value="REJECTED" />
          <el-option :label="statusText('CANCELED')" value="CANCELED" />
        </el-select>
      </div>
      <el-empty v-if="!selectedDemandId" :description="t('demandApplications.emptySelectDemand')" :image-size="88" />
      <el-table v-else v-loading="ownerLoading" :data="ownerApplications" stripe>
        <el-table-column prop="id" :label="t('demandApplications.colApplyId')" min-width="90" />
        <el-table-column :label="t('demandApplications.colWorker')" min-width="120">
          <template #default="{ row }">{{ row.workerName || t('demandApplications.userWithId', { id: row.workerUserId }) }}</template>
        </el-table-column>
        <el-table-column prop="workerCountry" :label="t('demandApplications.colCountry')" min-width="100" />
        <el-table-column prop="workerSkillTags" :label="t('demandApplications.colSkills')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('demandApplications.colQuote')" min-width="120">
          <template #default="{ row }">¥{{ formatMoney(row.quoteAmount) }}</template>
        </el-table-column>
        <el-table-column prop="applyNote" :label="t('demandApplications.colApplyNote')" min-width="180" show-overflow-tooltip />
        <el-table-column :label="t('demandApplications.colStatus')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('demandApplications.colApplyTime')" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('demandApplications.colActions')" min-width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="success"
              plain
              :disabled="row.status !== 'PENDING'"
              :loading="Boolean(actionLoadingMap[`accept-${row.id}`])"
              @click="handleAccept(row)"
            >
              {{ t('demandApplications.acceptAndCreate') }}
            </el-button>
            <el-button
              size="small"
              type="danger"
              plain
              :disabled="row.status !== 'PENDING'"
              :loading="Boolean(actionLoadingMap[`reject-${row.id}`])"
              @click="handleReject(row)"
            >
              {{ t('demandApplications.reject') }}
            </el-button>
            <el-button
              v-if="row.orderId"
              size="small"
              type="primary"
              text
              @click="router.push(`/order/${row.orderId}`)"
            >
              {{ t('demandApplications.viewOrder') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="list-card reveal-up delay-1" shadow="never">
      <template #header>
        <div class="section-head">
          <h2>{{ t('demandApplications.mySectionTitle') }}</h2>
          <el-button size="small" :loading="myLoading" @click="loadMyApplications">{{ t('demandApplications.refresh') }}</el-button>
        </div>
      </template>

      <el-table v-loading="myLoading" :data="myApplications" stripe>
        <el-table-column prop="id" :label="t('demandApplications.colApplyId')" min-width="90" />
        <el-table-column :label="t('demandApplications.colDemand')" min-width="200">
          <template #default="{ row }">#{{ row.demandId }} · {{ row.demandCategory || t('demandApplications.uncategorized') }} · {{ row.demandCountry || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('demandApplications.colQuote')" min-width="120">
          <template #default="{ row }">¥{{ formatMoney(row.quoteAmount) }}</template>
        </el-table-column>
        <el-table-column prop="applyNote" :label="t('demandApplications.colApplyNote')" min-width="220" show-overflow-tooltip />
        <el-table-column :label="t('demandApplications.colStatus')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('demandApplications.colReviewNote')" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reviewNote || t('demandApplications.dash') }}</template>
        </el-table-column>
        <el-table-column :label="t('demandApplications.colTime')" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column :label="t('demandApplications.colActions')" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              type="danger"
              plain
              :loading="Boolean(actionLoadingMap[`cancel-${row.id}`])"
              @click="handleCancel(row)"
            >
              {{ t('demandApplications.cancelApply') }}
            </el-button>
            <el-button
              v-if="row.orderId"
              size="small"
              type="primary"
              text
              @click="router.push(`/order/${row.orderId}`)"
            >
              {{ t('demandApplications.viewOrder') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { getMyDemandListApi } from '@/api/demand';
import {
  acceptDemandApplyApi,
  cancelDemandApplyApi,
  getDemandApplyListApi,
  getMyDemandAppliesApi,
  rejectDemandApplyApi,
} from '@/api/demandApply';

const router = useRouter();
const { t, locale } = useI18n();

const myDemands = ref([]);
const selectedDemandId = ref(null);
const ownerStatusFilter = ref('PENDING');
const ownerLoading = ref(false);
const myLoading = ref(false);
const ownerApplications = ref([]);
const myApplications = ref([]);
const actionLoadingMap = reactive({});

function formatMoney(value) {
  const n = Number(value || 0);
  return n.toLocaleString(locale.value, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

function formatDateTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${hh}:${mm}`;
}

function statusText(status) {
  const key = String(status || '').toUpperCase();
  if (key === 'PENDING') return t('demandApplications.statusPending');
  if (key === 'ACCEPTED') return t('demandApplications.statusAccepted');
  if (key === 'REJECTED') return t('demandApplications.statusRejected');
  if (key === 'CANCELED') return t('demandApplications.statusCanceled');
  return key || t('demandApplications.dash');
}

function statusTagType(status) {
  const key = String(status || '').toUpperCase();
  if (key === 'PENDING') return 'warning';
  if (key === 'ACCEPTED') return 'success';
  if (key === 'REJECTED') return 'danger';
  if (key === 'CANCELED') return 'info';
  return 'info';
}

function formatDemandLabel(item) {
  return `#${item.id} ${item.category || t('demandApplications.uncategorized')} / ${item.targetCountry || '-'}`;
}

async function loadMyDemands() {
  try {
    const rows = await getMyDemandListApi();
    myDemands.value = Array.isArray(rows) ? rows : [];
    if (!selectedDemandId.value && myDemands.value.length > 0) {
      selectedDemandId.value = myDemands.value[0].id;
    }
  } catch {
    myDemands.value = [];
  }
}

async function loadOwnerApplications() {
  const demandId = Number(selectedDemandId.value || 0);
  if (!demandId) {
    ownerApplications.value = [];
    return;
  }
  ownerLoading.value = true;
  try {
    const rows = await getDemandApplyListApi(demandId, ownerStatusFilter.value || '');
    ownerApplications.value = Array.isArray(rows) ? rows : [];
  } catch {
    ownerApplications.value = [];
  } finally {
    ownerLoading.value = false;
  }
}

async function loadMyApplications() {
  myLoading.value = true;
  try {
    const rows = await getMyDemandAppliesApi();
    myApplications.value = Array.isArray(rows) ? rows : [];
  } catch {
    myApplications.value = [];
  } finally {
    myLoading.value = false;
  }
}

async function handleAccept(row) {
  const applyId = Number(row?.id || 0);
  if (!applyId || actionLoadingMap[`accept-${applyId}`]) return;
  try {
    const { value } = await ElMessageBox.prompt(
      t('demandApplications.acceptPromptMessage'),
      t('demandApplications.acceptPromptTitle'),
      {
        inputPlaceholder: t('demandApplications.defaultQuote', { amount: formatMoney(row.quoteAmount) }),
        confirmButtonText: t('demandApplications.confirmCreate'),
        cancelButtonText: t('demandApplications.cancel'),
      },
    );
    const amount = Number(String(value || '').trim());
    actionLoadingMap[`accept-${applyId}`] = true;
    const data = await acceptDemandApplyApi(applyId, {
      amount: Number.isFinite(amount) && amount > 0 ? amount : undefined,
    });
    ElMessage.success(t('demandApplications.createSuccess'));
    await Promise.all([loadOwnerApplications(), loadMyApplications()]);
    const orderId = Number(data?.orderId || 0);
    if (orderId) {
      router.push(`/order/checkout/${orderId}`);
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || t('demandApplications.actionFailed'));
    }
  } finally {
    actionLoadingMap[`accept-${applyId}`] = false;
  }
}

async function handleReject(row) {
  const applyId = Number(row?.id || 0);
  if (!applyId || actionLoadingMap[`reject-${applyId}`]) return;
  try {
    const { value } = await ElMessageBox.prompt(t('demandApplications.rejectPromptMessage'), t('demandApplications.rejectPromptTitle'), {
      inputPlaceholder: t('demandApplications.rejectPlaceholder'),
      confirmButtonText: t('demandApplications.confirmReject'),
      cancelButtonText: t('demandApplications.cancel'),
    });
    actionLoadingMap[`reject-${applyId}`] = true;
    await rejectDemandApplyApi(applyId, String(value || '').trim());
    ElMessage.success(t('demandApplications.rejectSuccess'));
    await Promise.all([loadOwnerApplications(), loadMyApplications()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || t('demandApplications.actionFailed'));
    }
  } finally {
    actionLoadingMap[`reject-${applyId}`] = false;
  }
}

async function handleCancel(row) {
  const applyId = Number(row?.id || 0);
  if (!applyId || actionLoadingMap[`cancel-${applyId}`]) return;
  try {
    await ElMessageBox.confirm(t('demandApplications.cancelConfirmMessage'), t('demandApplications.cancelConfirmTitle'), {
      confirmButtonText: t('demandApplications.confirmCancel'),
      cancelButtonText: t('demandApplications.back'),
      type: 'warning',
    });
    actionLoadingMap[`cancel-${applyId}`] = true;
    await cancelDemandApplyApi(applyId);
    ElMessage.success(t('demandApplications.cancelSuccess'));
    await Promise.all([loadOwnerApplications(), loadMyApplications()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || t('demandApplications.actionFailed'));
    }
  } finally {
    actionLoadingMap[`cancel-${applyId}`] = false;
  }
}

onMounted(async () => {
  await loadMyDemands();
  await Promise.all([loadOwnerApplications(), loadMyApplications()]);
});
</script>

<style scoped>
.apply-page {
  --surface: rgba(255, 255, 255, 0.76);
  --border-light: rgba(216, 228, 246, 0.85);
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 24px;
}

.hero-strip {
  position: relative;
  overflow: hidden;
  border-radius: 20px;
  padding: 28px 30px;
  background:
    radial-gradient(circle at 18% -6%, rgba(68, 125, 255, 0.58), transparent 45%),
    radial-gradient(circle at 88% 10%, rgba(61, 225, 226, 0.4), transparent 40%),
    linear-gradient(135deg, #08132f 0%, #0f2451 46%, #11386e 100%);
  color: #f6f9ff;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  box-shadow: 0 16px 32px rgba(9, 23, 56, 0.12);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(12px);
  pointer-events: none;
}

.orb-a {
  width: 200px;
  height: 200px;
  left: -40px;
  top: -40px;
  background: rgba(86, 140, 255, 0.45);
  animation: floatY 6s ease-in-out infinite;
}

.orb-b {
  width: 160px;
  height: 160px;
  right: 18%;
  top: 10%;
  background: rgba(65, 234, 203, 0.3);
  animation: floatY 8s ease-in-out infinite reverse;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.kicker {
  margin: 0 0 10px;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: #cad5f3;
}

.hero-strip h1 {
  margin: 0;
  font-size: clamp(24px, 3vw, 32px);
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: -0.01em;
}

.gradient-text {
  background: linear-gradient(92deg, #36c9f7, #73f2ba);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sub {
  margin: 10px 0 0;
  color: rgba(229, 238, 255, 0.9);
  font-size: 14.5px;
  line-height: 1.6;
}

.list-card {
  border-radius: 18px;
  border: 1px solid var(--border-light);
  background: var(--surface);
  backdrop-filter: blur(16px);
  box-shadow: 0 16px 36px rgba(27, 49, 90, 0.04);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-head h2 {
  margin: 0;
  font-size: 19px;
  font-weight: 700;
  color: #102449;
  letter-spacing: -0.01em;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.toolbar-item {
  width: 300px;
  max-width: 100%;
}

:deep(.el-button--small) {
  border-radius: 999px;
  padding: 8px 16px;
}

/* 深度定制表单输入框样式 */
:deep(.el-select__wrapper) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(216, 228, 246, 0.9) inset;
  background: #fdfeff;
  transition: all 0.25s ease;
}

:deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #a4c5f4 inset;
  background: #fff;
}

:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 2px #5a9cf8 inset !important;
  background: #fff;
}

@keyframes floatY {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-14px); }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: translateY(0); }
}

.reveal-up {
  animation: fadeInUp 0.55s ease both;
}

.delay-1 {
  animation-delay: 0.1s;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
