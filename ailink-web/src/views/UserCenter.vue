<template>
  <div class="ucx-page">
    <section class="ucx-hero">
      <div class="ucx-hero-left">
        <div class="ucx-avatar-wrap">
          <div class="ucx-avatar">{{ avatarLetter }}</div>
        </div>
        <div class="ucx-user-copy">
          <p class="ucx-kicker">Account Center</p>
          <h1>{{ userInfo?.username || '—' }}</h1>
          <div class="ucx-meta">
            <span>邮箱：{{ userInfo?.email || '—' }}</span>
            <span>地区：{{ locationText }}</span>
            <span>注册时间：{{ formatDate(userInfo?.createdTime) }}</span>
          </div>
          <el-tag size="small" class="ucx-role-tag" :type="roleTagType">{{ roleText }}</el-tag>
        </div>
      </div>

      <div class="ucx-hero-actions">
        <el-button plain @click="showEditDialog = true">
          <el-icon><Edit /></el-icon>
          编辑资料
        </el-button>
        <el-button type="primary" @click="router.push('/orders')">查看我的订单</el-button>
      </div>
    </section>

    <section class="ucx-kpi-grid">
      <article class="ucx-kpi-card">
        <p>已发布需求</p>
        <strong>{{ myDemands.length }}</strong>
      </article>
      <article class="ucx-kpi-card">
        <p>开放中</p>
        <strong>{{ openCount }}</strong>
      </article>
      <article class="ucx-kpi-card">
        <p>履约中</p>
        <strong>{{ activeCount }}</strong>
      </article>
      <article class="ucx-kpi-card">
        <p>累计预算</p>
        <strong>¥{{ formatMoney(totalBudget) }}</strong>
      </article>
    </section>

    <el-card shadow="never" class="ucx-demand-panel">
      <template #header>
        <div class="ucx-panel-header">
          <div>
            <h2>我发布的需求</h2>
            <p>个人中心只管理资料和需求，订单请在“我的订单”页查看。</p>
          </div>
          <div class="ucx-filter-actions">
            <el-select v-model="statusFilter" clearable placeholder="状态筛选" style="width: 160px;">
              <el-option label="全部状态" value="" />
              <el-option
                v-for="(label, key) in demandStatusMap"
                :key="key"
                :label="label"
                :value="key"
              />
            </el-select>
            <el-button @click="loadDemands">刷新</el-button>
            <el-button type="primary" @click="router.push('/publish-demand')">发布新需求</el-button>
          </div>
        </div>
      </template>

      <div v-if="loadingDemands">
        <el-skeleton :rows="4" animated />
      </div>
      <el-empty v-else-if="filteredDemands.length === 0" description="暂无需求记录" :image-size="90" />
      <div v-else class="ucx-demand-grid">
        <article
          v-for="item in filteredDemands"
          :key="item.id"
          class="ucx-demand-card"
          @click="goWorkerPool(item.id)"
        >
          <div class="ucx-demand-top">
            <h3>{{ item.title || item.category || `需求 #${item.id}` }}</h3>
            <el-tag size="small" :type="demandStatusType(item.status)">
              {{ demandStatusText(item.status) }}
            </el-tag>
          </div>
          <p class="ucx-demand-desc">{{ item.description || '暂无描述' }}</p>
          <div class="ucx-demand-meta">
            <span>国家：{{ item.targetCountry || item.country || '—' }}</span>
            <span>预算：¥{{ formatMoney(item.budget) }}</span>
            <span>发布时间：{{ formatDate(item.createdTime) }}</span>
          </div>
        </article>
      </div>
    </el-card>

    <el-dialog v-model="showEditDialog" title="编辑个人资料" width="460px" destroy-on-close>
      <el-form :model="editForm" label-width="80px" label-position="left">
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="输入邮箱" />
        </el-form-item>
        <el-form-item label="国家">
          <el-input v-model="editForm.country" placeholder="输入国家" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="editForm.city" placeholder="输入城市" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Edit } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/modules/user';
import { getCurrentUserApi, updateUserProfileApi } from '@/api/user';
import { getMyDemandListApi } from '@/api/demand';
import { formatMoney } from '@/utils/format';

const router = useRouter();
const userStore = useUserStore();

const userInfo = ref(null);
const myDemands = ref([]);
const loadingDemands = ref(true);
const statusFilter = ref('');

const showEditDialog = ref(false);
const saving = ref(false);
const editForm = ref({
  email: '',
  country: '',
  city: '',
});

const roleMap = {
  ADMIN: '管理员',
  USER: '用户',
  EMPLOYER: '雇主',
  WORKER: '执行者',
};

const demandStatusMap = {
  OPEN: '开放中',
  MATCHED: '已匹配',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
  CLOSED: '已关闭',
};

const avatarLetter = computed(() => {
  const name = userInfo.value?.username || '';
  return name.charAt(0).toUpperCase() || 'U';
});

const roleText = computed(() => roleMap[userInfo.value?.role] || userInfo.value?.role || '用户');

const roleTagType = computed(() => {
  if (userInfo.value?.role === 'ADMIN') return 'danger';
  if (userInfo.value?.role === 'WORKER') return 'success';
  return 'info';
});

const locationText = computed(() => {
  const country = userInfo.value?.country || '';
  const city = userInfo.value?.city || '';
  if (country && city) return `${country} · ${city}`;
  return country || city || '—';
});

const filteredDemands = computed(() => {
  if (!statusFilter.value) {
    return myDemands.value;
  }
  return myDemands.value.filter((item) => item?.status === statusFilter.value);
});

const openCount = computed(() => myDemands.value.filter((item) => item?.status === 'OPEN').length);

const activeCount = computed(
  () => myDemands.value.filter((item) => ['MATCHED', 'IN_PROGRESS'].includes(item?.status)).length,
);

const totalBudget = computed(
  () => myDemands.value.reduce((sum, item) => sum + Number(item?.budget || 0), 0),
);

watch(showEditDialog, (visible) => {
  if (!visible || !userInfo.value) return;
  editForm.value = {
    email: userInfo.value.email || '',
    country: userInfo.value.country || '',
    city: userInfo.value.city || '',
  };
});

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

function demandStatusText(status) {
  return demandStatusMap[status] || status || '未知';
}

function demandStatusType(status) {
  if (status === 'OPEN') return 'success';
  if (status === 'MATCHED' || status === 'IN_PROGRESS') return 'warning';
  if (status === 'DONE') return 'info';
  if (status === 'CLOSED') return 'danger';
  return '';
}

async function loadUserProfile() {
  try {
    const data = await getCurrentUserApi();
    userInfo.value = data || {};
    userStore.setUserInfo(data || {});
  } catch {
    userInfo.value = userStore.userInfo || {};
  }
}

async function loadDemands() {
  loadingDemands.value = true;
  try {
    const data = await getMyDemandListApi();
    myDemands.value = Array.isArray(data) ? data : [];
  } catch {
    myDemands.value = [];
  } finally {
    loadingDemands.value = false;
  }
}

function goWorkerPool(demandId) {
  if (!demandId) {
    router.push('/worker-pool');
    return;
  }
  router.push({
    path: '/worker-pool',
    query: { demandId: String(demandId) },
  });
}

async function handleSave() {
  saving.value = true;
  try {
    await updateUserProfileApi(editForm.value);
    await loadUserProfile();
    showEditDialog.value = false;
    ElMessage.success('资料已更新');
  } catch {
    ElMessage.error('更新失败，请稍后重试');
  } finally {
    saving.value = false;
  }
}

onMounted(async () => {
  await Promise.allSettled([loadUserProfile(), loadDemands()]);
});
</script>

<style>
.ucx-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.ucx-hero {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  padding: 22px 24px;
  background:
    radial-gradient(circle at 12% 12%, rgba(56, 189, 248, 0.22), transparent 34%),
    radial-gradient(circle at 92% 82%, rgba(59, 130, 246, 0.22), transparent 38%),
    linear-gradient(120deg, #06243d 0%, #0a3657 52%, #0d4f66 100%);
  color: #eaf3fb;
  box-shadow: 0 12px 30px rgba(2, 41, 83, 0.18);
}

.ucx-hero-left {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.ucx-avatar-wrap {
  width: 66px;
  height: 66px;
  padding: 2px;
  border-radius: 50%;
  background: linear-gradient(140deg, #7dd3fc 0%, #38bdf8 40%, #0891b2 100%);
}

.ucx-avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(4, 33, 66, 0.9);
  color: #ffffff;
  font-size: 30px;
  font-weight: 800;
}

.ucx-user-copy {
  min-width: 0;
}

.ucx-kicker {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(211, 236, 255, 0.8);
}

.ucx-user-copy h1 {
  margin: 0;
  font-size: 32px;
  line-height: 1.05;
  font-weight: 700;
  color: #ffffff;
}

.ucx-meta {
  margin: 10px 0 8px;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: rgba(226, 237, 248, 0.92);
}

.ucx-role-tag {
  border-color: rgba(255, 255, 255, 0.5);
}

.ucx-hero-actions {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ucx-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.ucx-kpi-card {
  background: #ffffff;
  border: 1px solid #dbe7f1;
  border-radius: 14px;
  padding: 14px 16px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.05);
}

.ucx-kpi-card p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.ucx-kpi-card strong {
  margin-top: 8px;
  display: block;
  font-size: 28px;
  line-height: 1.05;
  color: #0f172a;
}

.ucx-demand-panel {
  border-radius: 14px;
  border-color: #dbe7f1;
}

.ucx-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.ucx-panel-header h2 {
  margin: 0;
  font-size: 24px;
  line-height: 1.15;
  color: #0f172a;
}

.ucx-panel-header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
}

.ucx-filter-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ucx-demand-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.ucx-demand-card {
  border: 1px solid #dbe7f1;
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  background: #fbfdff;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s, transform 0.2s;
}

.ucx-demand-card:hover {
  border-color: #7ca8cb;
  background: #f3f9ff;
  box-shadow: 0 10px 22px rgba(2, 55, 104, 0.1);
  transform: translateY(-1px);
}

.ucx-demand-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.ucx-demand-top h3 {
  margin: 0;
  font-size: 16px;
  line-height: 1.25;
  color: #0f172a;
}

.ucx-demand-desc {
  margin: 10px 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.ucx-demand-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 1100px) {
  .ucx-kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .ucx-hero {
    padding: 18px;
  }

  .ucx-user-copy h1 {
    font-size: 26px;
  }

  .ucx-hero-actions {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .ucx-kpi-grid,
  .ucx-demand-grid {
    grid-template-columns: 1fr;
  }

  .ucx-filter-actions {
    width: 100%;
  }
}
</style>
