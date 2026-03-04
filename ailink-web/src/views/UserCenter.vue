<template>
  <div class="ucx-page">
    <section class="ucx-hero reveal-up">
      <div class="orb orb-a" />
      <div class="orb orb-b" />

      <div class="ucx-hero-left hero-content">
        <div class="ucx-avatar-wrap">
          <div class="ucx-avatar">{{ avatarLetter }}</div>
        </div>
        <div class="ucx-user-copy">
          <p class="ucx-kicker">{{ t('userCenter.kicker') }}</p>
          <h1>
            <span class="gradient-text">{{ userInfo?.username || '—' }}</span>
          </h1>
          <div class="ucx-meta">
            <span>{{ t('userCenter.email') }}{{ userInfo?.email || '—' }}</span>
            <span>{{ t('userCenter.location') }}{{ locationText }}</span>
            <span>{{ t('userCenter.registerTime') }}{{ formatDate(userInfo?.createdTime) }}</span>
          </div>
          <el-tag size="small" class="ucx-role-tag" :type="roleTagType">{{ roleText }}</el-tag>
        </div>
      </div>

      <div class="ucx-hero-actions hero-content reveal-up delay-1">
        <el-button class="hero-btn-ghost" @click="showEditDialog = true">
          <el-icon><Edit /></el-icon>
          {{ t('userCenter.editProfile') }}
        </el-button>
        <el-button class="hero-btn-primary" @click="router.push('/orders')">{{ t('userCenter.viewMyOrders') }}</el-button>
      </div>
    </section>

    <section class="ucx-kpi-grid reveal-up delay-1">
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

    <el-card v-if="!isWorker" shadow="never" class="ucx-demand-panel reveal-up delay-1">
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
            <el-input
              v-model.trim="workerApplyForm.country"
              :placeholder="t('userCenter.placeholderCountry')"
              readonly
            />
            <div class="worker-apply-hint">{{ t('userCenter.workerApplyCountryAutoHint') }}</div>
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
          <el-form-item :label="t('userCenter.fieldApplyAttachment')">
            <div class="worker-apply-attachment-wrap">
              <el-upload
                class="worker-apply-upload"
                action="#"
                :show-file-list="false"
                :accept="workerApplyAttachmentAccept"
                :before-upload="beforeWorkerApplyAttachmentUpload"
                :http-request="uploadWorkerApplyAttachment"
              >
                <el-button
                  type="primary"
                  plain
                  :loading="workerApplyAttachmentUploading"
                >
                  {{ workerApplyForm.applyAttachmentName ? t('userCenter.replaceAttachment') : t('userCenter.uploadAttachment') }}
                </el-button>
              </el-upload>
              <div class="worker-apply-hint">{{ t('userCenter.applyAttachmentTip') }}</div>
              <div v-if="workerApplyForm.applyAttachmentName" class="worker-apply-attachment-item">
                <span class="attachment-name">{{ workerApplyForm.applyAttachmentName }}</span>
                <el-button link type="danger" @click="clearWorkerApplyAttachment">{{ t('userCenter.clearAttachment') }}</el-button>
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="workerApplySubmitting"
              :disabled="!canSubmitWorkerApply || workerApplyAttachmentUploading"
              @click="submitWorkerApply"
            >
              {{ workerApplyInfo?.status === 'REJECTED' ? t('userCenter.resubmitApply') : t('userCenter.submitApply') }}
            </el-button>
            <span class="worker-apply-tip">{{ workerApplyActionTip }}</span>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card shadow="never" class="ucx-demand-panel reveal-up delay-1">
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
          v-for="(item, index) in filteredDemands"
          :key="item.id"
          class="ucx-demand-card reveal-up"
          :style="{ animationDelay: `${index * 0.05}s` }"
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
                  class="action-btn"
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

    <el-card v-if="isWorker" shadow="never" class="ucx-demand-panel reveal-up delay-1">
      <template #header>
        <div class="ucx-panel-header">
          <div>
            <h2>{{ t('userCenter.paymentTitle') }}</h2>
            <p>{{ t('userCenter.paymentSubtitle') }}</p>
          </div>
        </div>
      </template>

      <el-alert
        class="runner-payment-risk-alert"
        type="warning"
        :title="t('userCenter.paymentPrivacyNotice')"
        :closable="false"
        show-icon
      />
      <el-form v-loading="runnerPaymentLoading" :model="runnerPaymentForm" label-width="140px" class="runner-payment-form">
        <el-form-item :label="t('userCenter.paymentFieldPaypalEmail')">
          <el-input v-model.trim="runnerPaymentForm.paypalEmail" :placeholder="t('userCenter.paymentPlaceholderPaypalEmail')" />
        </el-form-item>
        <el-form-item :label="t('userCenter.paymentFieldWiseId')">
          <el-input v-model.trim="runnerPaymentForm.wiseId" :placeholder="t('userCenter.paymentPlaceholderWiseId')" />
        </el-form-item>
        <el-form-item :label="t('userCenter.paymentFieldWiseLink')">
          <el-input v-model.trim="runnerPaymentForm.wiseLink" :placeholder="t('userCenter.paymentPlaceholderWiseLink')" />
        </el-form-item>
        <el-form-item :label="t('userCenter.paymentFieldPayoneerLink')">
          <el-input v-model.trim="runnerPaymentForm.payoneerLink" :placeholder="t('userCenter.paymentPlaceholderPayoneerLink')" />
        </el-form-item>
        <el-form-item :label="t('userCenter.paymentFieldCryptoWallet')">
          <el-input v-model.trim="runnerPaymentForm.cryptoWallet" :placeholder="t('userCenter.paymentPlaceholderCryptoWallet')" />
        </el-form-item>
        <el-form-item :label="t('userCenter.paymentFieldPaymentUrl')">
          <el-input v-model.trim="runnerPaymentForm.paymentUrl" :placeholder="t('userCenter.paymentPlaceholderPaymentUrl')" />
        </el-form-item>
        <el-form-item :label="t('userCenter.paymentFieldCurrency')">
          <el-input v-model.trim="runnerPaymentForm.currency" :placeholder="t('userCenter.paymentPlaceholderCurrency')" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="runnerPaymentSaving" @click="saveRunnerPaymentProfile">{{ t('userCenter.savePayment') }}</el-button>
          <el-link v-if="runnerPaymentForm.wiseLink" type="primary" @click.prevent="openExternalPayoutLink(runnerPaymentForm.wiseLink)">{{ t('userCenter.openWise') }}</el-link>
          <el-link v-if="runnerPaymentForm.payoneerLink" type="primary" @click.prevent="openExternalPayoutLink(runnerPaymentForm.payoneerLink)">{{ t('userCenter.openPayoneer') }}</el-link>
          <el-link v-if="runnerPaymentForm.paymentUrl" type="primary" @click.prevent="openExternalPayoutLink(runnerPaymentForm.paymentUrl)">{{ t('userCenter.openPaymentLink') }}</el-link>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog
      v-model="showEditDialog"
      :title="t('userCenter.editDialogTitle')"
      width="560px"
      class="ucx-edit-dialog"
      destroy-on-close
    >
      <p class="ucx-edit-subtitle">{{ t('userCenter.editDialogSubtitle') }}</p>
      <el-form :model="editForm" label-position="top" class="ucx-edit-form">
        <el-form-item :label="t('userCenter.fieldEmail')">
          <el-input v-model.trim="editForm.email" :placeholder="t('userCenter.placeholderEmail')" />
        </el-form-item>
        <el-form-item :label="t('userCenter.fieldCountry')">
          <el-select
            v-model="editForm.country"
            filterable
            clearable
            :loading="countryLoading"
            :placeholder="t('userCenter.placeholderCountry')"
            class="ucx-country-select"
          >
            <el-option
              v-for="item in countryOptions"
              :key="item.code"
              :label="item.label"
              :value="item.code"
            >
              <div class="ucx-country-option">
                <span>{{ item.label }}</span>
                <span class="ucx-country-code">{{ item.code }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item :label="t('userCenter.fieldCity')">
          <el-input v-model.trim="editForm.city" :placeholder="t('userCenter.placeholderInputCity')" />
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
import { getCountryDictApi } from '@/api/dict';
import { getCurrentUserApi, updateUserProfileApi } from '@/api/user';
import { cancelDemandApi, getMyDemandListApi } from '@/api/demand';
import { getMyRunnerPaymentProfileApi, upsertMyRunnerPaymentProfileApi } from '@/api/runner';
import { getMyWorkerApplyApi, submitWorkerApplyApi, uploadWorkerApplyAttachmentApi } from '@/api/workerApply';
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
const { t, locale } = useI18n();

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
const countryLoading = ref(false);
const countryOptions = ref([]);
const runnerPaymentLoading = ref(false);
const runnerPaymentSaving = ref(false);
const runnerPaymentForm = ref({
  paypalEmail: '',
  wiseId: '',
  wiseLink: '',
  payoneerLink: '',
  cryptoWallet: '',
  paymentUrl: '',
  currency: 'CNY',
});
const workerApplyLoading = ref(false);
const workerApplySubmitting = ref(false);
const workerApplyAttachmentUploading = ref(false);
const workerApplyInfo = ref(null);
const workerApplyAttachmentAccept = '.doc,.docx,.jpg,.jpeg,.png';
const workerApplyForm = ref({
  country: '',
  city: '',
  skillTags: '',
  priceMin: 0,
  priceMax: 0,
  experience: '',
  applyNote: '',
  applyAttachmentName: '',
  applyAttachmentUrl: '',
});
const WORKER_APPLY_ATTACHMENT_MAX_SIZE = 5 * 1024 * 1024;
const WORKER_APPLY_ATTACHMENT_EXTENSIONS = ['doc', 'docx', 'jpg', 'jpeg', 'png'];

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

const countryLabelMap = computed(() => {
  const map = new Map();
  countryOptions.value.forEach((item) => {
    const code = String(item?.code || '').trim().toUpperCase();
    const label = String(item?.label || '').trim();
    if (code) map.set(code, label);
    if (label) map.set(label.toLowerCase(), code);
  });
  return map;
});

const locationText = computed(() => {
  const country = getCountryLabel(userInfo.value?.country || '');
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

function parseCountryExtra(extraJson) {
  if (!extraJson) return {};
  try {
    const parsed = JSON.parse(extraJson);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function localizeCountryLabel(code, fallbackLabel) {
  const regionCode = String(code || '').trim().toUpperCase();
  const fallback = String(fallbackLabel || '').trim();
  if (!regionCode) return fallback;
  try {
    const displayNames = new Intl.DisplayNames([locale.value], { type: 'region' });
    return displayNames.of(regionCode) || fallback || regionCode;
  } catch {
    return fallback || regionCode;
  }
}

function getCountryLabel(rawCountry) {
  const country = String(rawCountry || '').trim();
  if (!country) return '';
  if (/^[A-Za-z]{2}$/.test(country)) {
    const code = country.toUpperCase();
    return countryLabelMap.value.get(code) || localizeCountryLabel(code, code);
  }
  const mappedCode = countryLabelMap.value.get(country.toLowerCase());
  if (mappedCode) {
    return countryLabelMap.value.get(mappedCode) || country;
  }
  return country;
}

function normalizeCountryForForm(rawCountry) {
  const country = String(rawCountry || '').trim();
  if (!country) return '';
  if (/^[A-Za-z]{2}$/.test(country)) {
    return country.toUpperCase();
  }
  const mappedCode = countryLabelMap.value.get(country.toLowerCase());
  return mappedCode || '';
}

async function loadCountryOptions() {
  countryLoading.value = true;
  try {
    const data = await getCountryDictApi();
    const list = Array.isArray(data) ? data : [];
    const mapped = list
      .map((item, index) => {
        const code = String(item?.dict_code || item?.dictCode || '').trim().toUpperCase();
        const rawLabel = String(item?.dict_label || item?.dictLabel || '').trim();
        const label = localizeCountryLabel(code, rawLabel);
        const sortNo = Number(item?.sort_no ?? item?.sortNo ?? (index + 1) * 10);
        const extra = parseCountryExtra(item?.extra_json || item?.extraJson || '');
        return {
          code,
          label,
          sortNo,
          hot: Boolean(extra?.hot),
        };
      })
      .filter((item) => /^[A-Z]{2}$/.test(item.code) && item.label);

    const dedup = new Map();
    mapped.forEach((item) => {
      if (!dedup.has(item.code)) {
        dedup.set(item.code, item);
      }
    });
    countryOptions.value = Array.from(dedup.values()).sort((a, b) => {
      if (a.hot !== b.hot) return a.hot ? -1 : 1;
      if (a.sortNo !== b.sortNo) return a.sortNo - b.sortNo;
      return a.code.localeCompare(b.code);
    });
  } catch {
    countryOptions.value = [];
    ElMessage.error(t('userCenter.countryLoadFailed'));
  } finally {
    countryLoading.value = false;
  }
}

watch(showEditDialog, (visible) => {
  if (!visible || !userInfo.value) return;
  editForm.value = {
    email: userInfo.value.email || '',
    country: normalizeCountryForForm(userInfo.value.country || ''),
    city: userInfo.value.city || '',
  };
});

watch(
  () => userInfo.value?.country,
  () => {
    syncWorkerApplyCountryFromProfile();
  },
);

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
    syncWorkerApplyCountryFromProfile();
  } catch {
    userInfo.value = userStore.userInfo || {};
    syncWorkerApplyCountryFromProfile();
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
    const payload = {
      email: String(editForm.value.email || '').trim(),
      country: String(editForm.value.country || '').trim().toUpperCase(),
      city: String(editForm.value.city || '').trim(),
    };
    await updateUserProfileApi(payload);
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
  const profileCountry = getProfileCountryForWorkerApply();
  workerApplyForm.value = {
    country: profileCountry || source?.country || '',
    city: source?.city || userInfo.value?.city || '',
    skillTags: source?.skillTags || '',
    priceMin: Number(source?.priceMin || 0),
    priceMax: Number(source?.priceMax || 0),
    experience: source?.experience || '',
    applyNote: source?.applyNote || '',
    applyAttachmentName: source?.applyAttachmentName || '',
    applyAttachmentUrl: source?.applyAttachmentUrl || '',
  };
}

function getProfileCountryForWorkerApply() {
  const normalized = normalizeCountryForForm(userInfo.value?.country || '');
  if (normalized) {
    return normalized;
  }
  return String(userInfo.value?.country || '').trim();
}

function syncWorkerApplyCountryFromProfile() {
  const profileCountry = getProfileCountryForWorkerApply();
  if (!profileCountry) {
    return;
  }
  workerApplyForm.value.country = profileCountry;
}

function getFileExtension(filename) {
  const name = String(filename || '').trim();
  const dotIndex = name.lastIndexOf('.');
  if (dotIndex < 0 || dotIndex === name.length - 1) {
    return '';
  }
  return name.slice(dotIndex + 1).toLowerCase();
}

function beforeWorkerApplyAttachmentUpload(rawFile) {
  const ext = getFileExtension(rawFile?.name || '');
  if (!WORKER_APPLY_ATTACHMENT_EXTENSIONS.includes(ext)) {
    ElMessage.warning(t('userCenter.attachmentTypeInvalid'));
    return false;
  }
  const size = Number(rawFile?.size || 0);
  if (size > WORKER_APPLY_ATTACHMENT_MAX_SIZE) {
    ElMessage.warning(t('userCenter.attachmentTooLarge'));
    return false;
  }
  return true;
}

async function uploadWorkerApplyAttachment(uploadOption) {
  const file = uploadOption?.file;
  if (!file) {
    return;
  }
  workerApplyAttachmentUploading.value = true;
  try {
    const formData = new FormData();
    formData.append('file', file);
    const data = await uploadWorkerApplyAttachmentApi(formData);
    workerApplyForm.value.applyAttachmentName = data?.fileName || file.name || '';
    workerApplyForm.value.applyAttachmentUrl = data?.fileUrl || '';
    ElMessage.success(t('userCenter.attachmentUploadSuccess'));
    if (typeof uploadOption?.onSuccess === 'function') {
      uploadOption.onSuccess(data);
    }
  } catch (error) {
    if (typeof uploadOption?.onError === 'function') {
      uploadOption.onError(error);
    }
    ElMessage.error(t('userCenter.attachmentUploadFailed'));
  } finally {
    workerApplyAttachmentUploading.value = false;
  }
}

function clearWorkerApplyAttachment() {
  workerApplyForm.value.applyAttachmentName = '';
  workerApplyForm.value.applyAttachmentUrl = '';
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
  ) {
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
      applyNote: String(workerApplyForm.value.applyNote || '').trim(),
      applyAttachmentName: String(workerApplyForm.value.applyAttachmentName || '').trim(),
      applyAttachmentUrl: String(workerApplyForm.value.applyAttachmentUrl || '').trim(),
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
      wiseId: data?.wiseId || '',
      wiseLink: data?.wiseLink || '',
      payoneerLink: data?.payoneerLink || '',
      cryptoWallet: data?.cryptoWallet || '',
      paymentUrl: data?.paymentUrl || '',
      currency: data?.currency || 'CNY',
    };
  } catch {
    runnerPaymentForm.value = {
      paypalEmail: '',
      wiseId: '',
      wiseLink: '',
      payoneerLink: '',
      cryptoWallet: '',
      paymentUrl: '',
      currency: 'CNY',
    };
  } finally {
    runnerPaymentLoading.value = false;
  }
}

async function openExternalPayoutLink(url) {
  const target = String(url || '').trim();
  if (!target) return;
  try {
    await ElMessageBox.confirm(t('userCenter.paymentRiskMessage'), t('userCenter.paymentRiskTitle'), {
      type: 'warning',
      confirmButtonText: t('userCenter.paymentContinueOpen'),
      cancelButtonText: t('userCenter.cancel'),
    });
    window.open(target, '_blank', 'noopener,noreferrer');
  } catch {}
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
  await loadCountryOptions();
  await loadUserProfile();
  await Promise.allSettled([loadDemands(), loadRunnerPaymentProfile(), loadWorkerApply()]);
});
</script>

<style>
.ucx-page {
  --surface: rgba(255, 255, 255, 0.76);
  --border-light: rgba(216, 228, 246, 0.85);
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 24px;
}

.ucx-hero {
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

.hero-btn-primary {
  border-radius: 999px;
  padding: 12px 24px;
  font-weight: 600;
  border: none;
  color: #052241;
  background: linear-gradient(120deg, #75e9ff, #8fffb7);
  box-shadow: 0 12px 24px rgba(31, 227, 208, 0.25);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.hero-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(31, 227, 208, 0.35);
  background: linear-gradient(120deg, #8cf0ff, #a6ffc6);
}

.hero-btn-ghost {
  border-radius: 999px;
  padding: 12px 24px;
  font-weight: 600;
  color: #ecf6ff;
  border: 1px solid rgba(212, 230, 255, 0.45);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
  transition: all 0.2s ease;
}

.hero-btn-ghost:hover {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  transform: translateY(-2px);
}

.ucx-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.ucx-kpi-card {
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 16px 18px;
  background: var(--surface);
  backdrop-filter: blur(8px);
  box-shadow: 0 10px 24px rgba(27, 49, 90, 0.03);
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.ucx-kpi-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(27, 49, 90, 0.05);
}

.ucx-kpi-card p {
  margin: 0;
  color: #6a8396;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.ucx-kpi-card strong {
  margin-top: 6px;
  display: block;
  font-size: 28px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.02em;
  color: #102c42;
}

.ucx-demand-panel {
  border-radius: 18px;
  border: 1px solid var(--border-light);
  background: var(--surface);
  backdrop-filter: blur(16px);
  box-shadow: 0 16px 36px rgba(27, 49, 90, 0.04);
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
  font-size: 19px;
  font-weight: 700;
  color: #102449;
  letter-spacing: -0.01em;
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
  gap: 16px;
}

.ucx-demand-card {
  border: 1px solid rgba(216, 228, 246, 0.85);
  border-radius: 16px;
  padding: 18px 20px;
  background: #fdfdff;
  box-shadow: 0 12px 24px rgba(31, 57, 107, 0.03);
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
}

.ucx-demand-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 36px rgba(31, 57, 107, 0.08);
  border-color: #bad2f9;
}

.ucx-demand-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.ucx-demand-top h3 {
  margin: 0;
  font-size: 17px;
  color: #102449;
  font-weight: 700;
  line-height: 1.25;
}

.ucx-demand-desc {
  margin: 10px 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
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
  margin-top: 12px;
  max-width: 720px;
}

.runner-payment-risk-alert {
  margin-bottom: 8px;
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

.worker-apply-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #6a8396;
}

.worker-apply-attachment-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.worker-apply-upload {
  width: fit-content;
}

.worker-apply-attachment-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.worker-apply-attachment-item .attachment-name {
  font-size: 13px;
  color: #334155;
}

.action-btn {
  border-radius: 999px;
  padding: 8px 16px;
}

.ucx-filter-actions :deep(.el-button),
.ucx-demand-actions :deep(.el-button) {
  border-radius: 999px;
  padding: 8px 16px;
}

.ucx-edit-dialog .el-dialog {
  border-radius: 18px;
  overflow: hidden;
  border: 1px solid rgba(194, 215, 244, 0.85);
  box-shadow: 0 24px 48px rgba(19, 44, 84, 0.18);
}

.ucx-edit-dialog .el-dialog__header {
  margin: 0;
  padding: 18px 22px 12px;
  background:
    radial-gradient(circle at 16% -40%, rgba(99, 181, 255, 0.45), transparent 48%),
    linear-gradient(135deg, #f5faff 0%, #eef5ff 100%);
  border-bottom: 1px solid rgba(194, 215, 244, 0.7);
}

.ucx-edit-dialog .el-dialog__title {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.01em;
  color: #123059;
}

.ucx-edit-dialog .el-dialog__body {
  padding: 16px 22px 12px;
  background: linear-gradient(180deg, #fcfeff 0%, #f7fbff 100%);
}

.ucx-edit-subtitle {
  margin: 0 0 14px;
  color: #587293;
  font-size: 13px;
  line-height: 1.6;
}

.ucx-edit-form .el-form-item {
  margin-bottom: 14px;
}

.ucx-edit-form .el-form-item__label {
  font-weight: 700;
  color: #1a3e67;
}

.ucx-country-select {
  width: 100%;
}

.ucx-country-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.ucx-country-code {
  font-size: 11px;
  color: #6e88a8;
  border: 1px solid rgba(173, 196, 229, 0.8);
  border-radius: 999px;
  padding: 2px 8px;
  line-height: 1.2;
}

.ucx-edit-dialog .el-dialog__footer {
  margin: 0;
  padding: 12px 22px 18px;
  border-top: 1px solid rgba(207, 223, 245, 0.65);
  background: linear-gradient(180deg, #f8fbff 0%, #f4f8ff 100%);
}

/* 深度定制表单输入框样式 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-textarea__inner) {
  border-radius: 10px;
  box-shadow: 0 0 0 1px rgba(216, 228, 246, 0.9) inset;
  background: #fdfeff;
  transition: all 0.25s ease;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover),
:deep(.el-textarea__inner:hover) {
  box-shadow: 0 0 0 1px #a4c5f4 inset;
  background: #fff;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused),
:deep(.el-textarea__inner:focus) {
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
