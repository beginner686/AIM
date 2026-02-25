<template>
  <div class="page">
    <el-card class="card" shadow="never">
      <template #header>AI-Link 账户入口</template>
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @keyup.enter="handleLogin">
            <el-form-item prop="username" label="用户名">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item prop="password" label="密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-button type="primary" :loading="loginLoading" class="full-btn" @click="handleLogin">登录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            @keyup.enter="handleRegister"
          >
            <el-form-item prop="username" label="用户名">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item prop="password" label="密码">
              <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item prop="confirmPassword" label="确认密码">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入密码"
              />
            </el-form-item>
            <el-button type="primary" :loading="registerLoading" class="full-btn" @click="handleRegister">注册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { loginApi, registerApi } from '@/api/auth';
import { useUserStore } from '@/store/modules/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const activeTab = ref('login');
const loginLoading = ref(false);
const registerLoading = ref(false);
const loginFormRef = ref(null);
const registerFormRef = ref(null);

const loginForm = reactive({
  username: '',
  password: '',
});

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
});

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.password) {
          callback(new Error('两次密码输入不一致'));
          return;
        }
        callback();
      },
      trigger: 'blur',
    },
  ],
};

async function handleLogin() {
  if (!loginFormRef.value || loginLoading.value) {
    return;
  }
  await loginFormRef.value.validate();
  loginLoading.value = true;
  try {
    const data = await loginApi(loginForm);
    userStore.setLogin({
      token: data?.token || '',
      user: data?.user || null,
    });
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home';
    router.replace(redirect);
    ElMessage.success('登录成功');
  } finally {
    loginLoading.value = false;
  }
}

async function handleRegister() {
  if (!registerFormRef.value || registerLoading.value) {
    return;
  }
  await registerFormRef.value.validate();
  registerLoading.value = true;
  try {
    await registerApi(registerForm);
    ElMessage.success('注册成功，请登录');
    activeTab.value = 'login';
    loginForm.username = registerForm.username;
    loginForm.password = '';
    registerForm.password = '';
    registerForm.confirmPassword = '';
  } finally {
    registerLoading.value = false;
  }
}
</script>

<style scoped>
.page {
  min-height: calc(100vh - 40px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.card {
  width: 420px;
}

.full-btn {
  width: 100%;
}
</style>
