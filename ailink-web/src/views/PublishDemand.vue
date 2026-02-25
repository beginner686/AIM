<template>
  <div class="publish-page">
    <section class="hero-strip">
      <div>
        <p class="kicker">Demand Creation Workspace</p>
        <h1>发布跨国需求</h1>
        <p class="sub">
          一次提交，进入平台匹配、托管交易、履约结算的完整链路。需求写得越清晰，成交效率越高。
        </p>
      </div>
      <div class="hero-badges">
        <span class="badge">托管保障</span>
        <span class="badge">风控审计</span>
        <span class="badge">跨国履约</span>
      </div>
    </section>

    <section class="content-grid">
      <el-card class="form-card" shadow="never">
        <template #header>
          <div class="section-head">
            <h2>需求信息</h2>
            <span class="section-tip">* 为必填项</span>
          </div>
        </template>

        <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
          <div class="two-col">
            <el-form-item label="标题" prop="title">
              <el-input v-model="formData.title" maxlength="50" placeholder="例如：新加坡市场产品文案本地化" />
            </el-form-item>
            <el-form-item label="类目" prop="category">
              <el-input v-model="formData.category" maxlength="50" placeholder="例如：翻译、远程助理、视频剪辑" />
            </el-form-item>
          </div>

          <div class="preset-row">
            <span class="preset-label">常用类目：</span>
            <button
              v-for="item in categoryPresets"
              :key="item"
              type="button"
              class="preset-btn"
              @click="formData.category = item"
            >
              {{ item }}
            </button>
          </div>

          <div class="two-col">
            <el-form-item label="预算（USD）" prop="budget">
              <el-input-number
                v-model="formData.budget"
                :min="0.01"
                :step="50"
                :precision="2"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="国家" prop="country">
              <el-input v-model="formData.country" maxlength="50" placeholder="例如：Singapore / Japan / UAE" />
            </el-form-item>
          </div>

          <div class="preset-row">
            <span class="preset-label">常用国家：</span>
            <button
              v-for="item in countryPresets"
              :key="item"
              type="button"
              class="preset-btn"
              @click="formData.country = item"
            >
              {{ item }}
            </button>
          </div>

          <el-form-item label="描述" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="6"
              maxlength="500"
              show-word-limit
              placeholder="请写清交付内容、时效、验收标准和协作方式。"
            />
          </el-form-item>

          <div class="action-row">
            <el-button type="primary" :loading="submitLoading" @click="handleSubmit">发布需求</el-button>
            <el-button @click="resetForm">重置</el-button>
          </div>
        </el-form>
      </el-card>

      <div class="right-stack">
        <el-card class="side-card" shadow="never">
          <template #header>
            <div class="section-head">
              <h2>金额拆分预估</h2>
            </div>
          </template>

          <div class="split-item">
            <span>订单预算</span>
            <strong>{{ formatMoney(budgetAmount) }}</strong>
          </div>
          <div class="split-item">
            <span>平台抽成 (6%)</span>
            <strong>{{ formatMoney(platformFeeAmount) }}</strong>
          </div>
          <div class="split-item">
            <span>托管手续费 (0.5%)</span>
            <strong>{{ formatMoney(escrowFeeAmount) }}</strong>
          </div>
          <div class="split-item highlight">
            <span>执行者预估到手</span>
            <strong>{{ formatMoney(workerIncomeAmount) }}</strong>
          </div>
        </el-card>

        <el-card class="side-card" shadow="never">
          <template #header>
            <div class="section-head">
              <h2>成交建议</h2>
            </div>
          </template>
          <ul class="tips">
            <li>标题建议包含国家 + 类目 + 交付目标，提升匹配速度。</li>
            <li>预算越明确，执行者报价越稳定，减少议价时间。</li>
            <li>验收标准写进描述，能显著降低争议率。</li>
            <li>使用平台托管交易，避免线下支付风险。</li>
          </ul>
        </el-card>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { createDemandApi } from '@/api/demand';
import { formatMoney } from '@/utils/format';

const router = useRouter();
const formRef = ref(null);
const submitLoading = ref(false);

const formData = reactive({
  title: '',
  category: '',
  budget: null,
  country: '',
  description: '',
});

const categoryPresets = ['翻译本地化', '远程助理', '视频剪辑', '海外投放', '客服支持'];
const countryPresets = ['Singapore', 'Japan', 'UAE', 'Germany', 'Australia'];

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请输入类目', trigger: 'blur' }],
  budget: [
    { required: true, message: '请输入预算', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (value === null || value === undefined || Number(value) <= 0) {
          callback(new Error('预算必须为正数'));
          return;
        }
        callback();
      },
      trigger: 'change',
    },
  ],
  country: [{ required: true, message: '请输入国家', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
};

const budgetAmount = computed(() => Number(formData.budget || 0));
const platformFeeAmount = computed(() => round2(budgetAmount.value * 0.06));
const escrowFeeAmount = computed(() => round2(budgetAmount.value * 0.005));
const workerIncomeAmount = computed(() => round2(Math.max(0, budgetAmount.value - platformFeeAmount.value - escrowFeeAmount.value)));

function round2(value) {
  return Math.round((Number(value || 0) + Number.EPSILON) * 100) / 100;
}

async function handleSubmit() {
  if (!formRef.value || submitLoading.value) {
    return;
  }

  await formRef.value.validate();
  submitLoading.value = true;
  try {
    const data = await createDemandApi(formData);
    const demandId = data?.demandId || data?.id || data || '';
    ElMessage.success('需求发布成功');
    router.push({
      path: '/worker-pool',
      query: demandId ? { demandId: String(demandId) } : undefined,
    });
  } finally {
    submitLoading.value = false;
  }
}

function resetForm() {
  formRef.value?.resetFields();
}
</script>

<style scoped>
.publish-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-strip {
  border-radius: 16px;
  padding: 20px 22px;
  background:
    radial-gradient(circle at 20% 0%, rgba(67, 156, 211, 0.18), transparent 35%),
    linear-gradient(120deg, #0a3a56 0%, #0f4f63 60%, #0f6b63 100%);
  color: #e9f4ff;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.kicker {
  margin: 0 0 6px;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #9dd6ff;
}

.hero-strip h1 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  color: #f7fcff;
}

.sub {
  margin: 8px 0 0;
  color: #c5deef;
  font-size: 14px;
  max-width: 760px;
}

.hero-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.badge {
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 12px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.content-grid {
  display: grid;
  grid-template-columns: 1.7fr 1fr;
  gap: 16px;
}

.form-card,
.side-card {
  border-radius: 14px;
  border: 1px solid #dbe7f1;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-head h2 {
  margin: 0;
  font-size: 18px;
  color: #0f2c42;
}

.section-tip {
  font-size: 12px;
  color: #7a90a2;
}

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.preset-row {
  margin: 0 0 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.preset-label {
  font-size: 12px;
  color: #6e8799;
}

.preset-btn {
  border: 1px solid #d0dfec;
  background: #f8fbff;
  color: #214962;
  border-radius: 999px;
  font-size: 12px;
  padding: 4px 10px;
  cursor: pointer;
}

.preset-btn:hover {
  border-color: #9fc4df;
  background: #edf5fc;
}

.action-row {
  display: flex;
  gap: 10px;
}

.right-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.split-item {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 10px 0;
  border-bottom: 1px dashed #e1ebf3;
  color: #3d5a71;
  font-size: 13px;
}

.split-item strong {
  color: #102b40;
  font-size: 16px;
}

.split-item.highlight {
  border-bottom: none;
}

.split-item.highlight strong {
  color: #0f766e;
}

.tips {
  margin: 0;
  padding-left: 18px;
  color: #3c596f;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  line-height: 1.65;
}

@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .two-col {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .hero-strip h1 {
    font-size: 22px;
  }
}
</style>
