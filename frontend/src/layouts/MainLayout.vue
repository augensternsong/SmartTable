<template>
  <n-layout has-sider style="height: 100vh">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="220"
      :collapsed="collapsed"
      show-trigger
      @collapse="collapsed = true"
      @expand="collapsed = false"
    >
      <div class="logo">
        <span v-if="!collapsed">个人情况表单</span>
        <span v-else>表</span>
      </div>
      <n-menu
        :collapsed="collapsed"
        :collapsed-width="64"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :value="activeKey"
        @update:value="handleMenuClick"
      />
    </n-layout-sider>

    <n-layout>
      <n-layout-header bordered class="header">
        <div class="header-left">
          <n-breadcrumb>
            <n-breadcrumb-item>首页</n-breadcrumb-item>
            <n-breadcrumb-item v-if="route.meta.title">
              {{ route.meta.title }}
            </n-breadcrumb-item>
          </n-breadcrumb>
        </div>
        <div class="header-right">
          <n-dropdown :options="userDropdown" @select="handleUserAction">
            <n-button quaternary>
              <template #icon>
                <n-icon><PersonCircleOutline /></n-icon>
              </template>
              {{ auth.userInfo?.nickname || auth.userInfo?.username || '用户' }}
            </n-button>
          </n-dropdown>
        </div>
      </n-layout-header>

      <n-layout-content class="content" content-style="padding: 16px;">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </n-layout-content>
    </n-layout>
  </n-layout>
</template>

<script setup>
import { computed, ref, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { NIcon } from 'naive-ui'
import {
  PersonCircleOutline,
  LogOutOutline,
  SettingsOutline,
  CreateOutline,
  GridOutline,
  SpeedometerOutline,
  ListOutline,
  PeopleOutline
} from '@vicons/ionicons5'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)

const activeKey = computed(() => route.name)

// 把后端返回的菜单树转为 naive-ui 的 menu options
function renderIcon(icon) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

const ICON_MAP = {
  dashboard: SpeedometerOutline,
  'form-fill': CreateOutline,
  template: ListOutline,
  system: SettingsOutline,
  'system:user': PeopleOutline,
  'system:role': PeopleOutline,
  'system:group': PeopleOutline,
  'system:perm': GridOutline
}

function buildMenuOptions(nodes) {
  const result = []
  for (const node of nodes || []) {
    if (node.visible === 0) continue
    // 权限过滤: 超管或拥有该 permCode 才显示
    if (node.permCode && !auth.isSuperAdmin && !auth.permissions.includes(node.permCode)) {
      continue
    }
    const opt = {
      label: node.name,
      key: node.permCode || node.id,
      icon: node.permCode && ICON_MAP[node.permCode] ? renderIcon(ICON_MAP[node.permCode]) : undefined
    }
    if (node.children && node.children.length > 0) {
      const children = buildMenuOptions(node.children)
      if (children.length > 0) {
        opt.children = children
      }
    }
    // 叶子菜单点击跳转
    if ((!opt.children || opt.children.length === 0) && node.path) {
      opt.label = () => h('span', { onClick: () => router.push(node.path) }, node.name)
    }
    result.push(opt)
  }
  return result
}

const menuOptions = computed(() => buildMenuOptions(auth.menus))

function handleMenuClick(key) {
  // 已通过 label onClick 跳转; 这里仅用于 collapsed 时的图标点击
}

const userDropdown = [
  { label: '个人中心', key: 'profile', icon: () => h(NIcon, null, { default: () => h(SettingsOutline) }) },
  { type: 'divider', key: 'd1' },
  { label: '退出登录', key: 'logout', icon: () => h(NIcon, null, { default: () => h(LogOutOutline) }) }
]

async function handleUserAction(key) {
  if (key === 'logout') {
    await auth.logout()
    router.replace({ name: 'login' })
  } else if (key === 'profile') {
    router.push({ name: 'Profile' })
  }
}
</script>

<style scoped>
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  color: #2080f0;
  border-bottom: 1px solid var(--n-border-color);
}
.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: var(--n-color);
}
.header-right {
  display: flex;
  align-items: center;
}
.content {
  height: calc(100vh - 56px);
  background: #f5f7fa;
  overflow: auto;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
