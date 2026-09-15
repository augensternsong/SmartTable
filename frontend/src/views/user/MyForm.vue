<template>
  <n-card title="我的表单" :bordered="false">
    <n-spin :show="loading">
      <n-empty v-if="!templates.length" description="暂无可填写的表单" />
      <n-grid v-else :cols="2" :x-gap="16" :y-gap="16">
        <n-gi v-for="t in templates" :key="t.id">
          <n-card hoverable class="tpl-card" @click="goFill(t)">
            <div class="tpl-title">
              <span class="tpl-name">{{ t.templateName }}</span>
              <n-tag size="small" type="success" round>v{{ t.version }}</n-tag>
            </div>
            <div class="tpl-code">{{ t.templateCode }}</div>
            <div class="tpl-desc">{{ t.description || '无描述' }}</div>
            <div class="tpl-meta">
              <span>栏位 {{ t.fieldCount }} 个</span>
              <n-divider vertical />
              <span>{{ formatTime(t.updatedAt) }}</span>
            </div>
          </n-card>
        </n-gi>
      </n-grid>
    </n-spin>
  </n-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { myTemplates } from '@/api/template'

const router = useRouter()
const templates = ref([])
const loading = ref(false)

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

function goFill(t) {
  router.push({ name: 'FormFill', params: { templateId: t.id } })
}

async function load() {
  loading.value = true
  try {
    templates.value = (await myTemplates()) || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.tpl-card {
  cursor: pointer;
  transition: all 0.2s;
}
.tpl-card:hover {
  box-shadow: 0 4px 16px rgba(32, 128, 240, 0.15);
}
.tpl-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.tpl-name {
  font-size: 16px;
  font-weight: 600;
}
.tpl-code {
  color: #999;
  font-size: 12px;
  margin-bottom: 6px;
}
.tpl-desc {
  color: #666;
  font-size: 13px;
  margin-bottom: 8px;
  min-height: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.tpl-meta {
  color: #999;
  font-size: 12px;
}
</style>
