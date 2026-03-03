<template>
  <el-dialog
    v-model="authModalState.visible"
    width="960px"
    align-center
    append-to-body
    :show-close="false"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    class="auth-modal"
  >
    <div class="auth-shell">
      <aside class="auth-aside">
        <div class="aside-brand">
          <img src="/brand-logo.png" alt="AI-Link" />
          <span>AI-Link</span>
        </div>
        <h3>全球任务协作入口</h3>
        <p>登录后可发布任务、筛选执行者、创建订单并推进跨境履约。</p>
        <ul>
          <li>平台托管，流程透明</li>
          <li>执行者审核机制，降低履约风险</li>
          <li>跨国协作节点可追踪</li>
        </ul>
      </aside>

      <section class="auth-main">
        <button class="close-btn" type="button" @click="closeAuthModal">x</button>
        <header class="main-head">
          <p class="head-tag">WELCOME</p>
          <h2>登录 / 注册账户</h2>
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
              <el-button type="primary" class="full-btn" size="large" :loading="loginLoading" @click="handleLogin">
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
              <el-button type="primary" class="full-btn" size="large" :loading="registerLoading" @click="handleRegister">
                创建账户
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </section>
    </div>
  </el-dialog>
</template>

<script setup>
import { reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { loginApi, registerApi } from '@/api/auth';
import { useUserStore } from '@/store/modules/user';
import { authModalState, closeAuthModal } from '@/composables/useAuthModal';
import { USER_ROLE } from '@/dicts';

const router = useRouter();
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

watch(
  () => authModalState.visible,
  (visible) => {
    if (!visible) {
      return;
    }
    activeTab.value = authModalState.tab === 'register' ? 'register' : 'login';
  },
);

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
    closeAuthModal();
    await router.replace(authModalState.redirect || defaultRedirect);
    ElMessage.success('登录成功');
  } catch (_error) {
    // 表单与请求层已处理提示
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
    ElMessage.success('注册成功，请登录后进入工作台');
    activeTab.value = 'login';
    loginForm.username = registerForm.username;
    loginForm.password = '';
    registerForm.password = '';
    registerForm.confirmPassword = '';
  } catch (_error) {
    // 表单与请求层已处理提示
  } finally {
    registerLoading.value = false;
  }
}
</script>

<style scoped>
:deep(.auth-modal) {
  border-radius: 24px;
  overflow: hidden;
  background: transparent;
  box-shadow: 0 24px 60px rgba(8, 23, 53, 0.34);
}

:deep(.auth-modal .el-dialog__header) {
  display: none;
}

:deep(.auth-modal .el-dialog__body) {
  padding: 0;
}

.auth-shell {
  display: grid;
  grid-template-columns: minmax(280px, 0.95fr) minmax(0, 1.05fr);
  min-height: 600px;
  background: linear-gradient(132deg, #f8fbff 0%, #f5faff 100%);
}

.auth-aside {
  padding: 34px 28px;
  background:
    radial-gradient(circle at 10% 10%, rgba(96, 149, 255, 0.36), transparent 38%),
    radial-gradient(circle at 95% 90%, rgba(86, 229, 213, 0.28), transparent 36%),
    linear-gradient(138deg, #0c234d 0%, #163771 58%, #1d4a88 100%);
  color: #ebf3ff;
}

.aside-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.aside-brand img {
  width: 128px;
  height: 128px;
  border-radius: 8px;
  object-fit: cover;
}

.aside-brand span {
  font-size: 18px;
  font-weight: 700;
}

.auth-aside h3 {
  margin: 26px 0 10px;
  font-size: 30px;
  line-height: 1.16;
  letter-spacing: -0.01em;
}

.auth-aside p {
  margin: 0;
  color: rgba(222, 233, 253, 0.94);
  line-height: 1.72;
}

.auth-aside ul {
  margin: 22px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
  color: rgba(231, 239, 255, 0.95);
  font-size: 13px;
}

.auth-main {
  position: relative;
  padding: 30px 32px;
  background:
    radial-gradient(circle at 100% 0%, rgba(111, 156, 255, 0.15), transparent 30%),
    radial-gradient(circle at 0% 100%, rgba(52, 211, 193, 0.12), transparent 35%),
    rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(18px);
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 28px;
  height: 28px;
  border: 1px solid #d8e5fb;
  background: #f8fbff;
  border-radius: 8px;
  color: #56739d;
  cursor: pointer;
}

.close-btn:hover {
  background: #eef5ff;
}

.main-head {
  margin-bottom: 10px;
}

.head-tag {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.14em;
  color: #5881bd;
}

.main-head h2 {
  margin: 8px 0 0;
  font-size: 30px;
  letter-spacing: -0.01em;
  color: #10274d;
}

.full-btn {
  width: 100%;
  margin-top: 6px;
  border-radius: 12px;
}

:deep(.auth-main .el-tabs__item) {
  height: 42px;
  font-size: 14px;
  font-weight: 700;
}

:deep(.auth-main .el-tabs__active-bar) {
  height: 3px;
  border-radius: 2px;
}

:deep(.auth-main .el-form-item__label) {
  font-weight: 600;
  color: #35517b;
}

@media (max-width: 860px) {
  :deep(.auth-modal) {
    width: calc(100vw - 24px) !important;
    margin: 0 12px;
  }

  .auth-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .auth-aside {
    padding: 24px 22px;
  }

  .auth-main {
    padding: 24px 22px 26px;
  }

  .main-head h2 {
    font-size: 26px;
  }
}
</style>
