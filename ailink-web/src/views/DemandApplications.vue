<template>
  <div class="apply-page">
    <section class="hero">
      <div>
        <p class="kicker">Marketplace Application Hub</p>
        <h1>需求申请管理</h1>
        <p class="sub">执行者可主动申请需求，需求方可从申请池中审核并选人创建订单。</p>
      </div>
    </section>

    <el-card shadow="never">
      <template #header>
        <div class="section-head">
          <h2>收到的申请（需求方）</h2>
          <el-button size="small" :loading="ownerLoading" @click="loadOwnerApplications">刷新</el-button>
        </div>
      </template>
      <div class="toolbar">
        <el-select v-model="selectedDemandId" placeholder="请选择需求" clearable filterable class="toolbar-item" @change="loadOwnerApplications">
          <el-option v-for="item in myDemands" :key="item.id" :label="formatDemandLabel(item)" :value="item.id" />
        </el-select>
        <el-select v-model="ownerStatusFilter" placeholder="申请状态" clearable class="toolbar-item" @change="loadOwnerApplications">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已接受" value="ACCEPTED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已取消" value="CANCELED" />
        </el-select>
      </div>
      <el-empty v-if="!selectedDemandId" description="请先选择你的需求" :image-size="88" />
      <el-table v-else v-loading="ownerLoading" :data="ownerApplications" stripe>
        <el-table-column prop="id" label="申请ID" min-width="90" />
        <el-table-column label="执行者" min-width="120">
          <template #default="{ row }">{{ row.workerName || `用户#${row.workerUserId}` }}</template>
        </el-table-column>
        <el-table-column prop="workerCountry" label="国家" min-width="100" />
        <el-table-column prop="workerSkillTags" label="技能" min-width="180" show-overflow-tooltip />
        <el-table-column label="报价" min-width="120">
          <template #default="{ row }">¥{{ formatMoney(row.quoteAmount) }}</template>
        </el-table-column>
        <el-table-column prop="applyNote" label="申请说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              size="small"
              type="success"
              plain
              :disabled="row.status !== 'PENDING'"
              :loading="Boolean(actionLoadingMap[`accept-${row.id}`])"
              @click="handleAccept(row)"
            >
              接受并建单
            </el-button>
            <el-button
              size="small"
              type="danger"
              plain
              :disabled="row.status !== 'PENDING'"
              :loading="Boolean(actionLoadingMap[`reject-${row.id}`])"
              @click="handleReject(row)"
            >
              拒绝
            </el-button>
            <el-button
              v-if="row.orderId"
              size="small"
              type="primary"
              text
              @click="router.push(`/order/${row.orderId}`)"
            >
              查看订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-head">
          <h2>我的申请（执行者）</h2>
          <el-button size="small" :loading="myLoading" @click="loadMyApplications">刷新</el-button>
        </div>
      </template>
      <el-table v-loading="myLoading" :data="myApplications" stripe>
        <el-table-column prop="id" label="申请ID" min-width="90" />
        <el-table-column label="需求" min-width="200">
          <template #default="{ row }">#{{ row.demandId }} · {{ row.demandCategory || '未分类' }} · {{ row.demandCountry || '-' }}</template>
        </el-table-column>
        <el-table-column label="报价" min-width="120">
          <template #default="{ row }">¥{{ formatMoney(row.quoteAmount) }}</template>
        </el-table-column>
        <el-table-column prop="applyNote" label="申请说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" min-width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理意见" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reviewNote || '-' }}</template>
        </el-table-column>
        <el-table-column label="时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              size="small"
              type="danger"
              plain
              :loading="Boolean(actionLoadingMap[`cancel-${row.id}`])"
              @click="handleCancel(row)"
            >
              取消申请
            </el-button>
            <el-button
              v-if="row.orderId"
              size="small"
              type="primary"
              text
              @click="router.push(`/order/${row.orderId}`)"
            >
              查看订单
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
import { getMyDemandListApi } from '@/api/demand';
import {
  acceptDemandApplyApi,
  cancelDemandApplyApi,
  getDemandApplyListApi,
  getMyDemandAppliesApi,
  rejectDemandApplyApi,
} from '@/api/demandApply';

const router = useRouter();

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
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 });
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
  if (key === 'PENDING') return '待处理';
  if (key === 'ACCEPTED') return '已接受';
  if (key === 'REJECTED') return '已拒绝';
  if (key === 'CANCELED') return '已取消';
  return key || '-';
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
  return `#${item.id} ${item.category || '未分类'} / ${item.targetCountry || '-'}`;
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
      '可填写成交金额（选填，留空默认使用执行者报价）',
      '接受申请并创建订单',
      {
        inputPlaceholder: `默认报价：${formatMoney(row.quoteAmount)}`,
        confirmButtonText: '确认创建',
        cancelButtonText: '取消',
      },
    );
    const amount = Number(String(value || '').trim());
    actionLoadingMap[`accept-${applyId}`] = true;
    const data = await acceptDemandApplyApi(applyId, {
      amount: Number.isFinite(amount) && amount > 0 ? amount : undefined,
    });
    ElMessage.success('已创建订单');
    await Promise.all([loadOwnerApplications(), loadMyApplications()]);
    const orderId = Number(data?.orderId || 0);
    if (orderId) {
      router.push(`/order/${orderId}`);
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '操作失败');
    }
  } finally {
    actionLoadingMap[`accept-${applyId}`] = false;
  }
}

async function handleReject(row) {
  const applyId = Number(row?.id || 0);
  if (!applyId || actionLoadingMap[`reject-${applyId}`]) return;
  try {
    const { value } = await ElMessageBox.prompt('可填写拒绝原因（选填）', '拒绝申请', {
      inputPlaceholder: '例如：当前需求方向不匹配',
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
    });
    actionLoadingMap[`reject-${applyId}`] = true;
    await rejectDemandApplyApi(applyId, String(value || '').trim());
    ElMessage.success('已拒绝该申请');
    await Promise.all([loadOwnerApplications(), loadMyApplications()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '操作失败');
    }
  } finally {
    actionLoadingMap[`reject-${applyId}`] = false;
  }
}

async function handleCancel(row) {
  const applyId = Number(row?.id || 0);
  if (!applyId || actionLoadingMap[`cancel-${applyId}`]) return;
  try {
    await ElMessageBox.confirm('确认取消该申请？', '取消申请', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      type: 'warning',
    });
    actionLoadingMap[`cancel-${applyId}`] = true;
    await cancelDemandApplyApi(applyId);
    ElMessage.success('申请已取消');
    await Promise.all([loadOwnerApplications(), loadMyApplications()]);
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || '操作失败');
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
