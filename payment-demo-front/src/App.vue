<template>
  <div id="app">
    <!-- 公共头 -->
    <AppHeader />
    <!-- /公共头 -->

    <!-- 路由出口，由index.js配置的路由出口 -->
    <router-view />

    <!-- 公共底 -->
    <AppFooter />
    <!-- /公共底 -->

  </div>
</template>

<script setup>
import AppHeader from './components/AppHeader.vue'
import AppFooter from './components/AppFooter.vue'

import './assets/css/reset.css'
import './assets/css/theme.css'
import './assets/css/global.css'
</script>
