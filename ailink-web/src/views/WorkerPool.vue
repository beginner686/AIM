<template>
  <el-space direction="vertical" fill>
    <el-alert
      v-if="!demandId"
      title="未检测到 demandId，请先发布需求后再选择执行者创建订单。"
      type="warning"
      :closable="false"
      show-icon
    />

    <el-card shadow="never">
      <template #header>筛选执行者</template>
      <el-form :inline="true" :model="filters">
        <el-form-item label="国家">
          <el-input v-model="filters.country" placeholder="例如：Singapore" clearable />
        </el-form-item>
        <el-form-item label="类目">
          <el-input v-model="filters.category" placeholder="例如：翻译" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="listLoading" @click="loadWorkers">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span>执行者资源池</span>
          <span class="sub">需求ID：{{ demandId || '未携带' }}</span>
        </div>
      </template>

      <el-table :data="workers" v-loading="listLoading" border>
        <el-table-column label="执行者名称" min-width="180">
          <template #default="{ row }">
            <div>{{ row.name }}</div>
            <div class="sub">ID: {{ row.workerId }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="country" label="国家" width="140" />
        <el-table-column label="类目" min-width="180">
          <template #default="{ row }">
            <span>{{ row.category || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="成交单数" width="120">
          <template #default="{ row }">
            <span>{{ row.dealCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="100">
          <template #default="{ row }">
            <span>{{ row.rating.toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="参考报价" width="180">
          <template #default="{ row }">
            <span>{{ formatMoney(getEstimatedAmount(row)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              :disabled="!demandId"
              :loading="Boolean(createLoadingMap[row.workerId])"
              @click="handleCreateOrder(row)"
            >
              选择并创建订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </el-space>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { getWorkerListApi } from '@/api/worker';
import { createOrderApi } from '@/api/order';
import { formatMoney } from '@/utils/format';

const router = useRouter();
const route = useRoute();

const demandId = ref(route.query?.demandId ? Number(route.query.demandId) : 0);
const listLoading = ref(false);
const workers = ref([]);
const createLoadingMap = reactive({});
const filters = reactive({
  country: '',
  category: '',
});

async function loadWorkers() {
  listLoading.value = true;
  try {
    workers.value = await getWorkerListApi(filters);
  } finally {
    listLoading.value = false;
  }
}

function getEstimatedAmount(worker) {
  const min = Number(worker?.priceMin || 0);
  const max = Number(worker?.priceMax || 0);
  if (min > 0 && max > 0) {
    return (min + max) / 2;
  }
  if (min > 0) {
    return min;
  }
  if (max > 0) {
    return max;
  }
  return 100;
}

async function handleCreateOrder(worker) {
  if (!demandId.value) {
    ElMessage.warning('缺少 demandId，请先发布需求');
    return;
  }
  const workerId = Number(worker?.workerId || worker?.id || 0);
  if (!workerId) {
    ElMessage.error('执行者ID无效');
    return;
  }

  createLoadingMap[workerId] = true;
  try {
    const data = await createOrderApi({
      demandId: demandId.value,
      workerId,
      amount: getEstimatedAmount(worker),
    });
    const orderId = data?.orderId || data?.id || data;
    if (!orderId) {
      throw new Error('未获取到订单ID');
    }
    ElMessage.success('订单创建成功');
    router.push(`/order/${orderId}`);
  } finally {
    createLoadingMap[workerId] = false;
  }
}

onMounted(() => {
  loadWorkers();
});
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sub {
  color: #6b7280;
  font-size: 12px;
}
</style>
