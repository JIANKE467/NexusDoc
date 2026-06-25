import { createRouter, createWebHistory } from 'vue-router';
import Home from '../views/Home.vue';
import DocumentResult from '../views/DocumentResult.vue';
import DocumentHistory from '../views/DocumentHistory.vue';
import DocumentChat from '../views/DocumentChat.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Home },
    { path: '/create', redirect: '/' },
    { path: '/login', redirect: '/' },
    { path: '/register', redirect: '/' },
    { path: '/document/:id', component: DocumentResult },
    { path: '/history', component: DocumentHistory },
    { path: '/chat/:id', component: DocumentChat }
  ]
});

export default router;
