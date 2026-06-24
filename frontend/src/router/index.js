import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import Login from '../views/Login.vue';
import Register from '../views/Register.vue';
import DocumentCreate from '../views/DocumentCreate.vue';
import DocumentResult from '../views/DocumentResult.vue';
import DocumentHistory from '../views/DocumentHistory.vue';
import DocumentChat from '../views/DocumentChat.vue';
import { useUserStore } from '../stores/user';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Home },
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/create', component: DocumentCreate, meta: { requiresAuth: true } },
    { path: '/document/:id', component: DocumentResult, meta: { requiresAuth: true } },
    { path: '/history', component: DocumentHistory, meta: { requiresAuth: true } },
    { path: '/chat/:id', component: DocumentChat, meta: { requiresAuth: true } }
  ]
});

router.beforeEach((to) => {
  const userStore = useUserStore();
  if (to.meta.requiresAuth && !userStore.user) {
    return '/login';
  }
  return true;
});

export default router;
