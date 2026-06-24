<template>
  <el-container class="app-shell">
    <el-header class="topbar">
      <router-link class="brand" to="/">文枢 NexusDoc</router-link>
      <nav class="nav">
        <router-link to="/create">处理文档</router-link>
        <router-link to="/history">历史记录</router-link>
        <span v-if="userStore.user" class="user-name">{{ userStore.user.username }}</span>
        <el-button v-if="userStore.user" text @click="logout">退出</el-button>
        <router-link v-else to="/login">登录</router-link>
      </nav>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useUserStore } from './stores/user';

const router = useRouter();
const userStore = useUserStore();

function logout() {
  userStore.logout();
  router.push('/login');
}
</script>
