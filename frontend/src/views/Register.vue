<template>
  <section class="page auth-page">
    <div class="panel auth-panel">
      <h1 class="page-title">注册</h1>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="3 到 50 个字符" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" />
        </el-form-item>
        <el-button type="primary" :loading="loading" class="full" @click="handleRegister">注册</el-button>
        <p class="muted">
          已有账号？
          <router-link class="link" to="/login">去登录</router-link>
        </p>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { register } from '../api/user';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const form = reactive({ username: '', password: '', confirmPassword: '' });

async function handleRegister() {
  if (!form.username || !form.password || !form.confirmPassword) {
    ElMessage.warning('请完整填写注册信息');
    return;
  }
  loading.value = true;
  try {
    const user = await register(form);
    userStore.setUser(user);
    ElMessage.success('注册成功');
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
