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
      </nav>
      <button
        class="mobile-nav-toggle"
        type="button"
        :aria-expanded="mobileNavOpen"
        aria-label="打开主导航"
        @click="mobileNavOpen = !mobileNavOpen"
      >
        <span></span>
        <span></span>
      </button>
      <div v-if="mobileNavOpen" class="mobile-nav-mask" @click="mobileNavOpen = false">
        <nav class="mobile-nav-panel" aria-label="移动端主导航" @click.stop>
          <button
            :class="['mobile-nav-item', { active: activeNav === 'home' }]"
            type="button"
            @click="navigateHome('home')"
          >
            主页
          </button>
          <button
            :class="['mobile-nav-item', { active: activeNav === 'chat' }]"
            type="button"
            @click="navigateHome('chat')"
          >
            工作台
          </button>
          <button
            :class="['mobile-nav-item', { active: activeNav === 'folders' }]"
            type="button"
            @click="navigateHome('folders')"
          >
            档案夹
          </button>
          <button
            :class="['mobile-nav-item', { active: activeNav === 'history' }]"
            type="button"
            @click="navigateHistory"
          >
            历史记录
          </button>
        </nav>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute();
const router = useRouter();
const mobileNavOpen = ref(false);

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
  mobileNavOpen.value = false;
  router.push({ path: '/', query });
}

function navigateHistory() {
  mobileNavOpen.value = false;
  router.push('/history');
}

</script>
