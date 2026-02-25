<template>
  <el-card shadow="never" class="page-card">
    <template #header>发布需求</template>

    <el-form ref="formRef" :model="formData" :rules="rules" label-position="top">
      <el-form-item label="标题" prop="title">
        <el-input v-model="formData.title" maxlength="50" placeholder="请输入需求标题" />
      </el-form-item>

      <el-form-item label="类目" prop="category">
        <el-input v-model="formData.category" maxlength="50" placeholder="例如：翻译、远程助理、视频剪辑" />
      </el-form-item>

      <el-form-item label="预算（USD）" prop="budget">
        <el-input-number v-model="formData.budget" :min="0.01" :step="10" :precision="2" controls-position="right" />
        <div class="money-preview">预算预览：{{ formatMoney(formData.budget) }}</div>
      </el-form-item>

      <el-form-item label="国家" prop="country">
        <el-input v-model="formData.country" maxlength="50" placeholder="例如：Singapore" />
      </el-form-item>

      <el-form-item label="描述" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="5"
          maxlength="500"
          show-word-limit
          placeholder="请写清交付内容、时效和验收标准"
        />
      </el-form-item>

      <el-space>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">发布需求</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-space>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref } from 'vue';
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
.page-card {
  max-width: 760px;
}

.money-preview {
  margin-top: 8px;
  color: #4b5563;
  font-size: 12px;
}
</style>
