<template>
  <div class="ucx-page">
    <section class="ucx-hero">
      <div class="ucx-hero-left">
        <div class="ucx-avatar-wrap">
          <div class="ucx-avatar">{{ avatarLetter }}</div>
        </div>
        <div class="ucx-user-copy">
          <p class="ucx-kicker">{{ t('userCenter.kicker') }}</p>
          <h1>{{ userInfo?.username || '—' }}</h1>
          <div class="ucx-meta">
            <span>{{ t('userCenter.email') }}{{ userInfo?.email || '—' }}</span>
            <span>{{ t('userCenter.location') }}{{ locationText }}</span>
            <span>{{ t('userCenter.registerTime') }}{{ formatDate(userInfo?.createdTime) }}</span>
          </div>
          <el-tag size="small" class="ucx-role-tag" :type="roleTagType">{{ roleText }}</el-tag>
        </div>
      </div>

      <div class="ucx-hero-actions">
        <el-button plain @click="showEditDialog = true">
          <el-icon><Edit /></el-icon>
          {{ t('userCenter.editProfile') }}
        </el-button>
        <el-button type="primary" @click="router.push('/orders')">{{ t('userCenter.viewMyOrders') }}</el-button>
      </div>
    </section>

    <section class="ucx-kpi-grid">
      <article class="ucx-kpi-card">
        <p>{{ t('userCenter.kpiPublishedDemands') }}</p>
        <strong>{{ myDemands.length }}</strong>
      </article>
      <article class="ucx-kpi-card">
        <p>{{ t('userCenter.kpiOpen') }}</p>
        <strong>{{ openCount }}</strong>
      </article>
      <article class="ucx-kpi-card">
        <p>{{ t('userCenter.kpiActive') }}</p>
        <strong>{{ activeCount }}</strong>
      </article>
      <article class="ucx-kpi-card">
        <p>{{ t('userCenter.kpiBudget') }}</p>
        <strong>¥{{ formatMoney(totalBudget) }}</strong>
      </article>
    </section>

    <el-card v-if="!isWorker" shadow="never" class="ucx-demand-panel">
      <template #header>
        <div class="ucx-panel-header">
          <div>
            <h2>{{ t('userCenter.workerApplyTitle') }}</h2>
            <p>{{ t('userCenter.workerApplySubtitle') }}</p>
          </div>
          <el-tag :type="workerApplyTagType">{{ workerApplyStatusLabel }}</el-tag>
        </div>
      </template>

      <div v-if="workerApplyLoading">
        <el-skeleton :rows="4" animated />
      </div>
      <div v-else class="worker-apply-wrap">
        <div class="worker-apply-meta">
          <span>{{ t('userCenter.currentStatus') }}{{ workerApplyStatusLabel }}</span>
          <span v-if="workerApplyInfo?.reviewNote">{{ t('userCenter.reviewNote') }}{{ workerApplyInfo.reviewNote }}</span>
        </div>
        <el-form :model="workerApplyForm" label-width="100px" class="worker-apply-form">
          <el-form-item :label="t('userCenter.fieldCountry')">
            <el-input v-model.trim="workerApplyForm.country" :placeholder="t('userCenter.placeholderCountry')" />
          </el-form-item>
          <el-form-item :label="t('userCenter.fieldCity')">
            <el-input v-model.trim="workerApplyForm.city" :placeholder="t('userCenter.placeholderCity')" />
          </el-form-item>
          <el-form-item :label="t('userCenter.fieldSkillTags')">
            <el-input v-model.trim="workerApplyForm.skillTags" :placeholder="t('userCenter.placeholderSkillTags')" />
          </el-form-item>
          <el-form-item :label="t('userCenter.fieldPriceRange')">
            <div class="worker-apply-price-row">
              <el-input-number v-model="workerApplyForm.priceMin" :min="0" :precision="2" />
              <span>~</span>
              <el-input-number v-model="workerApplyForm.priceMax" :min="0" :precision="2" />
            </div>
          </el-form-item>
          <el-form-item :label="t('userCenter.fieldRealName')">
            <el-input v-model.trim="workerApplyForm.realName" :placeholder="t('userCenter.placeholderRealName')" />
          </el-form-item>
          <el-form-item :label="t('userCenter.fieldIdHash')">
            <el-input v-model.trim="workerApplyForm.idNoHash" :placeholder="t('userCenter.placeholderIdHash')" />
          </el-form-item>
          <el-form-item :label="t('userCenter.fieldExperience')">
            <el-input
              v-model.trim="workerApplyForm.experience"
              type="textarea"
              :rows="2"
              maxlength="300"
              show-word-limit
              :placeholder="t('userCenter.placeholderExperience')"
            />
          </el-form-item>
          <el-form-item :label="t('userCenter.fieldApplyNote')">
            <el-input
              v-model.trim="workerApplyForm.applyNote"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              :placeholder="t('userCenter.placeholderApplyNote')"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="workerApplySubmitting"
              :disabled="!canSubmitWorkerApply"
              @click="submitWorkerApply"
            >
              {{ workerApplyInfo?.status === 'REJECTED' ? t('userCenter.resubmitApply') : t('userCenter.submitApply') }}
            </el-button>
            <span class="worker-apply-tip">{{ workerApplyActionTip }}</span>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card shadow="never" class="ucx-demand-panel">
      <template #header>
        <div class="ucx-panel-header">
          <div>
            <h2>{{ t('userCenter.myDemandsTitle') }}</h2>
            <p>{{ t('userCenter.myDemandsSubtitle') }}</p>
          </div>
          <div class="ucx-filter-actions">
            <el-select v-model="statusFilter" clearable :placeholder="t('userCenter.statusFilter')" style="width: 160px;">
              <el-option :label="t('userCenter.allStatus')" value="" />
              <el-option
                v-for="item in demandStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-button @click="loadDemands">{{ t('userCenter.refresh') }}</el-button>
            <el-button type="primary" @click="router.push('/publish-demand')">{{ t('userCenter.publishNewDemand') }}</el-button>
          </div>
        </div>
      </template>

      <div v-if="loadingDemands">
        <el-skeleton :rows="4" animated />
      </div>
      <el-empty v-else-if="filteredDemands.length === 0" :description="t('userCenter.emptyDemands')" :image-size="90" />
      <div v-else class="ucx-demand-grid">
        <article
          v-for="item in filteredDemands"
          :key="item.id"
          class="ucx-demand-card"
          @click="handleDemandCardClick(item)"
        >
          <div class="ucx-demand-top">
            <h3>{{ item.title || item.category || t('userCenter.demandWithId', { id: item.id }) }}</h3>
            <el-tag size="small" :type="demandStatusType(item.status)">
              {{ demandStatusText(item.status) }}
            </el-tag>
          </div>
          <p class="ucx-demand-desc">{{ item.description || t('userCenter.noDesc') }}</p>
          <div class="ucx-demand-meta">
            <span>{{ t('userCenter.metaCountry') }}{{ item.targetCountry || item.country || '—' }}</span>
            <span>{{ t('userCenter.metaBudget') }}¥{{ formatMoney(item.budget) }}</span>
            <span>{{ t('userCenter.metaCreated') }}{{ formatDate(item.createdTime) }}</span>
          </div>
          <div class="ucx-demand-actions">
            <el-button size="small" @click.stop="goWorkerPool(item.id)">{{ t('userCenter.matchWorker') }}</el-button>
            <el-tooltip
              :content="cancelDemandDisabledReason(item)"
              placement="top"
              :disabled="!cancelDemandDisabledReason(item)"
            >
              <span class="ucx-action-wrap">
                <el-button
                  size="small"
                  type="danger"
                  plain
                  :disabled="!canCancelDemand(item)"
                  @click.stop="handleCancelDemand(item)"
                >
                  {{ t('userCenter.cancelDemand') }}
                </el-button>
              </span>
            </el-tooltip>
          </div>
        </article>
      </div>
    </el-card>

    <el-card v-if="isWorker" shadow="never" class="ucx-demand-panel">
      <template #header>
        <div class="ucx-panel-header">
          <div>
            <h2>{{ t('userCenter.paymentTitle') }}</h2>
            <p>{{ t('userCenter.paymentSubtitle') }}</p>
          </div>
        </div>
      </template>

      <el-form v-loading="runnerPaymentLoading" :model="runnerPaymentForm" label-width="110px" class="runner-payment-form">
        <el-form-item label="PayPal Email">
          <el-input v-model.trim="runnerPaymentForm.paypalEmail" placeholder="name@example.com" />
        </el-form-item>
        <el-form-item label="Wise Link">
          <el-input v-model.trim="runnerPaymentForm.wiseLink" placeholder="https://wise.com/..." />
        </el-form-item>
        <el-form-item label="Payment URL">
          <el-input v-model.trim="runnerPaymentForm.paymentUrl" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="Currency">
          <el-input v-model.trim="runnerPaymentForm.currency" placeholder="CNY / USD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="runnerPaymentSaving" @click="saveRunnerPaymentProfile">{{ t('userCenter.savePayment') }}</el-button>
          <el-link v-if="runnerPaymentForm.wiseLink" :href="runnerPaymentForm.wiseLink" target="_blank" type="primary">{{ t('userCenter.openWise') }}</el-link>
          <el-link v-if="runnerPaymentForm.paymentUrl" :href="runnerPaymentForm.paymentUrl" target="_blank" type="primary">{{ t('userCenter.openPaymentLink') }}</el-link>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="showEditDialog" :title="t('userCenter.editDialogTitle')" width="460px" destroy-on-close>
      <el-form :model="editForm" label-width="80px" label-position="left">
        <el-form-item :label="t('userCenter.fieldEmail')">
          <el-input v-model="editForm.email" :placeholder="t('userCenter.placeholderEmail')" />
        </el-form-item>
        <el-form-item :label="t('userCenter.fieldCountry')">
          <el-input v-model="editForm.country" :placeholder="t('userCenter.placeholderInputCountry')" />
        </el-form-item>
        <el-form-item :label="t('userCenter.fieldCity')">
          <el-input v-model="editForm.city" :placeholder="t('userCenter.placeholderInputCity')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">{{ t('userCenter.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">{{ t('userCenter.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Edit } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import { useUserStore } from '@/store/modules/user';
import { getCurrentUserApi, updateUserProfileApi } from '@/api/user';
import { cancelDemandApi, getMyDemandListApi } from '@/api/demand';
import { getMyRunnerPaymentProfileApi, upsertMyRunnerPaymentProfileApi } from '@/api/runner';
import { getMyWorkerApplyApi, submitWorkerApplyApi } from '@/api/workerApply';
import {
  DEMAND_ACTIVE_STATUSES,
  DEMAND_OPEN_STATUSES,
  DEMAND_STATUS_DICT,
  getDemandStatusLabel,
  getDemandStatusTag,
  getRoleLabel,
  getRoleTag,
  toSelectOptions,
} from '@/dicts';
import { formatMoney } from '@/utils/format';

const router = useRouter();
const userStore = useUserStore();
const { t } = useI18n();

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
const runnerPaymentLoading = ref(false);
const runnerPaymentSaving = ref(false);
const runnerPaymentForm = ref({
  paypalEmail: '',
  wiseLink: '',
  paymentUrl: '',
  currency: 'CNY',
});
const workerApplyLoading = ref(false);
const workerApplySubmitting = ref(false);
const workerApplyInfo = ref(null);
const workerApplyForm = ref({
  country: '',
  city: '',
  skillTags: '',
  priceMin: 0,
  priceMax: 0,
  experience: '',
  realName: '',
  idNoHash: '',
  applyNote: '',
});

const demandStatusOptions = computed(() => toSelectOptions(DEMAND_STATUS_DICT));

const avatarLetter = computed(() => {
  const name = userInfo.value?.username || '';
  return name.charAt(0).toUpperCase() || 'U';
});

const roleText = computed(() => getRoleLabel(userInfo.value?.role));
const isWorker = computed(() => String(userInfo.value?.role || '').toUpperCase() === 'WORKER');

const roleTagType = computed(() => {
  return getRoleTag(userInfo.value?.role);
});

const locationText = computed(() => {
  const country = userInfo.value?.country || '';
  const city = userInfo.value?.city || '';
  if (country && city) return `${country} · ${city}`;
  return country || city || t('userCenter.dash');
});

const workerApplyStatus = computed(() => String(workerApplyInfo.value?.status || userInfo.value?.workerApplyStatus || 'NONE').toUpperCase());
const workerApplyStatusLabel = computed(() => {
  if (workerApplyStatus.value === 'PENDING') return t('userCenter.applyPending');
  if (workerApplyStatus.value === 'APPROVED') return t('userCenter.applyApproved');
  if (workerApplyStatus.value === 'REJECTED') return t('userCenter.applyRejected');
  return t('userCenter.applyNone');
});
const workerApplyTagType = computed(() => {
  if (workerApplyStatus.value === 'PENDING') return 'warning';
  if (workerApplyStatus.value === 'APPROVED') return 'success';
  if (workerApplyStatus.value === 'REJECTED') return 'danger';
  return 'info';
});
const canSubmitWorkerApply = computed(() => {
  if (isWorker.value) return false;
  return workerApplyStatus.value === 'NONE' || workerApplyStatus.value === 'REJECTED';
});
const workerApplyActionTip = computed(() => {
  if (workerApplyStatus.value === 'PENDING') return t('userCenter.applyTipPending');
  if (workerApplyStatus.value === 'APPROVED') return t('userCenter.applyTipApproved');
  if (workerApplyStatus.value === 'REJECTED') return t('userCenter.applyTipRejected');
  return t('userCenter.applyTipDefault');
});

const filteredDemands = computed(() => {
  if (!statusFilter.value) {
    return myDemands.value;
  }
  return myDemands.value.filter((item) => item?.status === statusFilter.value);
});

const openCount = computed(
  () => myDemands.value.filter((item) => DEMAND_OPEN_STATUSES.includes(item?.status)).length,
);

const activeCount = computed(
  () => myDemands.value.filter((item) => DEMAND_ACTIVE_STATUSES.includes(item?.status)).length,
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
  return getDemandStatusLabel(status);
}

function demandStatusType(status) {
  return getDemandStatusTag(status);
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

function canCancelDemand(item) {
  return String(item?.status || '').toUpperCase() === 'OPEN';
}

function cancelDemandDisabledReason(item) {
  const status = String(item?.status || '').toUpperCase();
  if (!status || status === 'OPEN') {
    return '';
  }
  if (status === 'MATCHED' || status === 'IN_PROGRESS') {
    return t('userCenter.cancelReasonMatched');
  }
  if (status === 'DONE') {
    return t('userCenter.cancelReasonDone');
  }
  if (status === 'CLOSED') {
    return t('userCenter.cancelReasonClosed');
  }
  return t('userCenter.cancelReasonUnsupported', { status });
}

function handleDemandCardClick(item) {
  const status = String(item?.status || '').toUpperCase();
  if (status !== 'OPEN' && status !== 'MATCHED') {
    return;
  }
  goWorkerPool(item?.id);
}

async function handleCancelDemand(item) {
  const demandId = Number(item?.id || 0);
  if (!demandId || !canCancelDemand(item)) {
    const reason = cancelDemandDisabledReason(item);
    if (reason) ElMessage.warning(reason);
    return;
  }
  try {
    await ElMessageBox.confirm(t('userCenter.cancelConfirmMessage'), t('userCenter.cancelConfirmTitle'), {
      confirmButtonText: t('userCenter.confirmCancel'),
      cancelButtonText: t('userCenter.back'),
      type: 'warning',
    });
    await cancelDemandApi(demandId);
    ElMessage.success(t('userCenter.cancelSuccess'));
    await loadDemands();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error?.message || t('userCenter.cancelFailed'));
    }
  }
}

async function handleSave() {
  saving.value = true;
  try {
    await updateUserProfileApi(editForm.value);
    await loadUserProfile();
    showEditDialog.value = false;
    ElMessage.success(t('userCenter.profileUpdated'));
  } catch {
    ElMessage.error(t('userCenter.profileUpdateFailed'));
  } finally {
    saving.value = false;
  }
}

function fillWorkerApplyForm(source = {}) {
  workerApplyForm.value = {
    country: source?.country || userInfo.value?.country || '',
    city: source?.city || userInfo.value?.city || '',
    skillTags: source?.skillTags || '',
    priceMin: Number(source?.priceMin || 0),
    priceMax: Number(source?.priceMax || 0),
    experience: source?.experience || '',
    realName: source?.realName || '',
    idNoHash: source?.idNoHash || '',
    applyNote: source?.applyNote || '',
  };
}

async function loadWorkerApply() {
  if (isWorker.value) {
    workerApplyInfo.value = null;
    return;
  }
  workerApplyLoading.value = true;
  try {
    const data = await getMyWorkerApplyApi();
    workerApplyInfo.value = data || null;
    fillWorkerApplyForm(data || {});
  } catch {
    workerApplyInfo.value = null;
    fillWorkerApplyForm({});
  } finally {
    workerApplyLoading.value = false;
  }
}

async function submitWorkerApply() {
  if (!canSubmitWorkerApply.value || workerApplySubmitting.value) {
    return;
  }
  if (!workerApplyForm.value.country || !workerApplyForm.value.city || !workerApplyForm.value.skillTags
    || !workerApplyForm.value.realName || !workerApplyForm.value.idNoHash) {
    ElMessage.warning(t('userCenter.fillRequired'));
    return;
  }
  if (Number(workerApplyForm.value.priceMax || 0) < Number(workerApplyForm.value.priceMin || 0)) {
    ElMessage.warning(t('userCenter.priceRangeInvalid'));
    return;
  }
  workerApplySubmitting.value = true;
  try {
    const payload = {
      country: String(workerApplyForm.value.country || '').trim(),
      city: String(workerApplyForm.value.city || '').trim(),
      skillTags: String(workerApplyForm.value.skillTags || '').trim(),
      priceMin: Number(workerApplyForm.value.priceMin || 0),
      priceMax: Number(workerApplyForm.value.priceMax || 0),
      experience: String(workerApplyForm.value.experience || '').trim(),
      realName: String(workerApplyForm.value.realName || '').trim(),
      idNoHash: String(workerApplyForm.value.idNoHash || '').trim(),
      applyNote: String(workerApplyForm.value.applyNote || '').trim(),
    };
    await submitWorkerApplyApi(payload);
    ElMessage.success(t('userCenter.applySubmitSuccess'));
    await Promise.allSettled([loadUserProfile(), loadWorkerApply()]);
  } catch {
    ElMessage.error(t('userCenter.applySubmitFailed'));
  } finally {
    workerApplySubmitting.value = false;
  }
}

async function loadRunnerPaymentProfile() {
  if (!isWorker.value) {
    return;
  }
  runnerPaymentLoading.value = true;
  try {
    const data = await getMyRunnerPaymentProfileApi();
    runnerPaymentForm.value = {
      paypalEmail: data?.paypalEmail || '',
      wiseLink: data?.wiseLink || '',
      paymentUrl: data?.paymentUrl || '',
      currency: data?.currency || 'CNY',
    };
  } catch {
    runnerPaymentForm.value = {
      paypalEmail: '',
      wiseLink: '',
      paymentUrl: '',
      currency: 'CNY',
    };
  } finally {
    runnerPaymentLoading.value = false;
  }
}

async function saveRunnerPaymentProfile() {
  if (!isWorker.value || runnerPaymentSaving.value) {
    return;
  }
  runnerPaymentSaving.value = true;
  try {
    await upsertMyRunnerPaymentProfileApi(runnerPaymentForm.value);
    ElMessage.success(t('userCenter.paymentSaved'));
    await loadRunnerPaymentProfile();
  } catch {
    ElMessage.error(t('userCenter.paymentSaveFailed'));
  } finally {
    runnerPaymentSaving.value = false;
  }
}

onMounted(async () => {
  await loadUserProfile();
  await Promise.allSettled([loadDemands(), loadRunnerPaymentProfile(), loadWorkerApply()]);
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

.ucx-demand-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.ucx-action-wrap {
  display: inline-block;
}

.runner-payment-form {
  max-width: 720px;
}

.worker-apply-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.worker-apply-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 13px;
  color: #64748b;
}

.worker-apply-form {
  max-width: 760px;
}

.worker-apply-price-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.worker-apply-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #64748b;
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
