<template>
  <el-container class="app-shell">
    <el-header class="topbar">
      <router-link class="brand" to="/">文枢 NexusDoc</router-link>
      <nav class="nav" aria-label="主导航">
        <button
          :class="['nav-item', { active: activeNav === 'home' }]"
          type="button"
          @click="navigateHome('home')"
        >
          主页
        </button>
        <button
          :class="['nav-item', { active: activeNav === 'chat' }]"
          type="button"
          @click="navigateHome('chat')"
        >
          工作台
        </button>
        <button
          :class="['nav-item', { active: activeNav === 'folders' }]"
          type="button"
          @click="navigateHome('folders')"
        >
          档案夹
        </button>
        <button
          :class="['nav-item', { active: activeNav === 'history' }]"
          type="button"
          @click="$router.push('/history')"
        >
          历史记录
        </button>
        <button class="command-trigger" type="button" @click="openCommandCenter">
          Ctrl K
        </button>
        <button class="user-chip" type="button" aria-label="用户菜单">
          <span>F</span>
          <i>⌄</i>
        </button>
      </nav>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();

const activeNav = computed(() => {
  if (route.path === '/history') {
    return 'history';
  }
  if (route.path !== '/') {
    return 'chat';
  }
  const view = route.query.view;
  return ['chat', 'folders'].includes(view) ? view : 'home';
});

function navigateHome(view) {
  const query = view === 'home' ? {} : { view };
  router.push({ path: '/', query });
}

function openCommandCenter() {
  window.dispatchEvent(new CustomEvent('nexusdoc:open-command-center'));
}
</script>
