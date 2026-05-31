<script setup>
import TabBar from './TabBar.vue';
import SideNav from './SideNav.vue';
import TopBar from './TopBar.vue';
import CompanionWidget from '@/components/companion/CompanionWidget.vue';
</script>

<template>
  <SideNav />
  <TopBar />
  <main class="app-main">
    <slot />
  </main>
  <TabBar />
  <CompanionWidget />
</template>
