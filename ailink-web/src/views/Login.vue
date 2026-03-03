<template>
  <div class="auth-page">
    <div class="orb orb-a" />
    <div class="orb orb-b" />
    <div class="orb orb-c" />

    <div class="auth-shell">
      <aside class="auth-aside">
        <div class="brand-line">
          <img src="/brand-logo.png" alt="AI-Link" />
          <span>AI-Link</span>
        </div>
        <h2>跨境任务协作账户中心</h2>
        <p>登录后可进入工作台完成需求发布、执行者筛选、订单托管与验收。</p>
        <ul>
          <li>支付后进入平台托管流程</li>
          <li>执行者接单后才开启履约节点</li>
          <li>状态全程可追踪、可回溯</li>
        </ul>
        <RouterLink to="/" class="back-link">返回前台首页</RouterLink>
      </aside>

      <section class="auth-main">
        <header class="main-head">
          <p>ACCOUNT CENTER</p>
          <h1>登录 / 注册</h1>
        </header>

        <el-tabs v-model="activeTab" stretch>
          <el-tab-pane label="登录" name="login">
            <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @keyup.enter="handleLogin">
              <el-form-item prop="username" label="用户名">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" />
              </el-form-item>
              <el-form-item prop="password" label="密码">
                <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" size="large" />
              </el-form-item>
              <el-button type="primary" size="large" :loading="loginLoading" class="full-btn" @click="handleLogin">
                登录并进入工作台
              </el-button>
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
                <el-input v-model="registerForm.username" placeholder="请输入用户名" size="large" />
              </el-form-item>
              <el-form-item prop="password" label="密码">
                <el-input v-model="registerForm.password" type="password" show-password placeholder="请输入密码" size="large" />
              </el-form-item>
              <el-form-item prop="confirmPassword" label="确认密码">
                <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  show-password
                  placeholder="请再次输入密码"
                  size="large"
                />
              </el-form-item>
              <el-button type="primary" size="large" :loading="registerLoading" class="full-btn" @click="handleRegister">
                创建账户
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { loginApi, registerApi } from '@/api/auth';
import { useUserStore } from '@/store/modules/user';
import { USER_ROLE } from '@/dicts';

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

function syncTabFromQuery() {
  const tab = String(route.query?.tab || '').trim().toLowerCase();
  activeTab.value = tab === 'register' ? 'register' : 'login';
}

async function handleLogin() {
  if (!loginFormRef.value || loginLoading.value) {
    return;
  }
  try {
    await loginFormRef.value.validate();
    loginLoading.value = true;
    const data = await loginApi(loginForm);
    userStore.setLogin({
      token: data?.token || '',
      user: data?.user || null,
    });
    const role = data?.user?.role || '';
    const defaultRedirect = role === USER_ROLE.ADMIN ? '/admin' : '/home';
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : defaultRedirect;
    await router.replace(redirect);
    ElMessage.success('登录成功');
  } catch (_error) {
    // 请求层与表单层已处理提示
  } finally {
    loginLoading.value = false;
  }
}

async function handleRegister() {
  if (!registerFormRef.value || registerLoading.value) {
    return;
  }
  try {
    await registerFormRef.value.validate();
    registerLoading.value = true;
    await registerApi(registerForm);
    ElMessage.success('注册成功，请登录后到个人中心提交执行者申请');
    activeTab.value = 'login';
    loginForm.username = registerForm.username;
    loginForm.password = '';
    registerForm.password = '';
    registerForm.confirmPassword = '';
  } catch (_error) {
    // 请求层与表单层已处理提示
  } finally {
    registerLoading.value = false;
  }
}

watch(
  () => route.query.tab,
  () => {
    syncTabFromQuery();
  },
);

onMounted(() => {
  syncTabFromQuery();
});
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100vh;
  padding: 32px 20px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background:
    radial-gradient(circle at 2% 0%, rgba(95, 134, 255, 0.2), transparent 36%),
    radial-gradient(circle at 100% 80%, rgba(66, 227, 201, 0.2), transparent 35%),
    linear-gradient(145deg, #eaf2ff 0%, #f8fbff 100%);
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(10px);
  pointer-events: none;
}

.orb-a {
  width: 240px;
  height: 240px;
  top: -80px;
  left: -70px;
  background: rgba(92, 142, 255, 0.35);
}

.orb-b {
  width: 220px;
  height: 220px;
  right: -70px;
  top: 8%;
  background: rgba(58, 225, 209, 0.28);
}

.orb-c {
  width: 180px;
  height: 180px;
  right: 8%;
  bottom: -60px;
  background: rgba(127, 154, 255, 0.26);
}

.auth-shell {
  position: relative;
  z-index: 1;
  width: min(960px, 100%);
  border-radius: 24px;
  overflow: hidden;
  display: grid;
  grid-template-columns: minmax(300px, 0.92fr) minmax(0, 1.08fr);
  box-shadow: 0 28px 56px rgba(15, 40, 88, 0.2);
}

.auth-aside {
  padding: 34px 28px;
  color: #ecf4ff;
  background:
    radial-gradient(circle at 8% 12%, rgba(107, 162, 255, 0.34), transparent 32%),
    radial-gradient(circle at 96% 90%, rgba(94, 227, 208, 0.3), transparent 32%),
    linear-gradient(136deg, #0b234d 0%, #173a73 56%, #1f4d8b 100%);
}

.brand-line {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 700;
}

.brand-line img {
  width: 128px;
  height: 128px;
  object-fit: cover;
  border-radius: 16px;
}

.auth-aside h2 {
  margin: 24px 0 10px;
  font-size: 30px;
  line-height: 1.16;
  letter-spacing: -0.01em;
}

.auth-aside p {
  margin: 0;
  line-height: 1.72;
  color: rgba(222, 233, 253, 0.94);
}

.auth-aside ul {
  margin: 20px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
  font-size: 13px;
  color: rgba(233, 241, 255, 0.95);
}

.back-link {
  margin-top: 22px;
  display: inline-block;
  color: #bfe0ff;
  font-weight: 600;
  border-bottom: 1px solid rgba(191, 224, 255, 0.5);
}

.auth-main {
  padding: 30px 32px;
  background:
    radial-gradient(circle at 100% 0%, rgba(111, 156, 255, 0.15), transparent 30%),
    radial-gradient(circle at 0% 100%, rgba(52, 211, 193, 0.12), transparent 35%),
    rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(18px);
}

.main-head p {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #4d75b0;
}

.main-head h1 {
  margin: 8px 0 14px;
  font-size: 32px;
  letter-spacing: -0.01em;
  color: #10274d;
}

.full-btn {
  width: 100%;
  border-radius: 12px;
}

:deep(.el-tabs__item) {
  height: 42px;
  font-size: 14px;
  font-weight: 700;
}

:deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 2px;
}

:deep(.el-form-item__label) {
  font-weight: 600;
  color: #35517b;
}

@media (max-width: 860px) {
  .auth-page {
    padding: 14px;
  }

  .auth-shell {
    grid-template-columns: 1fr;
  }

  .auth-aside {
    padding: 24px 22px;
  }

  .auth-main {
    padding: 24px 22px 26px;
  }

  .auth-aside h2,
  .main-head h1 {
    font-size: 26px;
  }
}
</style>
