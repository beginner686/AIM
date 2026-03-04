<template>
  <div class="apply-page">
    <section class="hero">
      <div>
        <p class="kicker">{{ t('demandApplications.kicker') }}</p>
        <h1>{{ t('demandApplications.title') }}</h1>
        <p class="sub">{{ t('demandApplications.subtitle') }}</p>
      </div>
    </section>

    <el-card shadow="never">
      <template #header>
        <div class="section-head">
          <h2>{{ t('demandApplications.ownerSectionTitle') }}</h2>
          <el-button size="small" :loading="ownerLoading" @click="loadOwnerApplications">{{ t('demandApplications.refresh') }}</el-button>
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

    <el-card shadow="never">
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
      router.push(`/order/${orderId}`);
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
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.hero {
  border-radius: 16px;
  padding: 18px 20px;
  background:
    radial-gradient(circle at 86% 0%, rgba(87, 171, 255, 0.22), transparent 34%),
    linear-gradient(125deg, #0c3658 0%, #0f5c67 55%, #0f6c5d 100%);
  color: #e7f4ff;
}

.kicker {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #9dd6ff;
}

.hero h1 {
  margin: 6px 0 0;
  font-size: 30px;
}

.sub {
  margin: 8px 0 0;
  color: #c4deee;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-head h2 {
  margin: 0;
  font-size: 17px;
  color: #10324f;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.toolbar-item {
  width: 300px;
  max-width: 100%;
}
</style>
