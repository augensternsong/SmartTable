import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// 静态路由: 不需要权限校验的基础页面
const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/user/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      // 用户填写
      {
        path: 'form/my',
        name: 'MyForm',
        component: () => import('@/views/user/MyForm.vue'),
        meta: { title: '我的表单' }
      },
      {
        path: 'form/fill/:templateId',
        name: 'FormFill',
        component: () => import('@/views/user/FormFill.vue'),
        meta: { title: '填写表单' }
      },
      // 模板管理
      {
        path: 'template/list',
        name: 'TemplateList',
        component: () => import('@/views/admin/TemplateList.vue'),
        meta: { title: '模板列表', perm: 'form:template:create' }
      },
      {
        path: 'template/field/:id',
        name: 'TemplateField',
        component: () => import('@/views/admin/TemplateField.vue'),
        meta: { title: '栏位管理', perm: 'form:field:update', hideInMenu: true }
      },
      // 系统管理
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/admin/SystemUser.vue'),
        meta: { title: '用户管理', perm: 'sys:user:update' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/admin/SystemRole.vue'),
        meta: { title: '角色管理', perm: 'sys:role:update' }
      },
      {
        path: 'system/group',
        name: 'SystemGroup',
        component: () => import('@/views/admin/SystemGroup.vue'),
        meta: { title: '用户分组', perm: 'sys:group:update' }
      },
      {
        path: 'system/perm',
        name: 'SystemPerm',
        component: () => import('@/views/admin/SystemPerm.vue'),
        meta: { title: '权限管理', perm: 'system:perm' }
      },
      // 个人中心
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心', hideInMenu: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  document.title = to.meta.title ? `${to.meta.title} - 个人情况表单系统` : '个人情况表单系统'

  if (to.meta.public) {
    return next()
  }

  if (!auth.isLoggedIn) {
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }

  // 权限校验
  if (to.meta.perm && !auth.hasPermission(to.meta.perm)) {
    return next({ name: 'Dashboard' })
  }

  next()
})

export default router
