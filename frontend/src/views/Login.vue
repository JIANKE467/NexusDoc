<template>
  <section class="page auth-page">
    <div class="panel auth-panel">
      <h1 class="page-title">登录</h1>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="full" @click="handleLogin">登录</el-button>
        <p class="muted">
          还没有账号？
          <router-link class="link" to="/register">去注册</router-link>
        </p>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { login } from '../api/user';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const form = reactive({ username: '', password: '' });

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码');
    return;
  }
  loading.value = true;
  try {
    const user = await login(form);
    userStore.setUser(user);
    ElMessage.success('登录成功');
    router.push('/create');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding-top: 56px;
}

.auth-panel {
  width: min(420px, 100%);
}

.full {
  width: 100%;
}

.link {
  color: #1d4ed8;
}
</style>
