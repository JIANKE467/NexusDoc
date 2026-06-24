import { defineStore } from 'pinia';

const STORAGE_KEY = 'nexusdoc_user';

export const useUserStore = defineStore('user', {
  state: () => ({
    user: JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  }),
  actions: {
    setUser(user) {
      this.user = user;
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    },
    logout() {
      this.user = null;
      localStorage.removeItem(STORAGE_KEY);
    }
  }
});
