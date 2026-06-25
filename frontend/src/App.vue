<template>
  <el-container class="app-shell">
    <el-header class="topbar">
      <router-link class="brand" to="/">文枢 NexusDoc</router-link>
      <nav class="nav">
        <router-link to="/">AI 对话</router-link>
        <router-link to="/history">历史记录</router-link>
      </nav>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>
