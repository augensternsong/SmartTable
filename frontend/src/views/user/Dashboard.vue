<template>
  <div>
    <n-grid :cols="3" :x-gap="16" :y-gap="16">
      <n-gi>
        <n-card>
          <n-statistic label="可见模板" :value="templateCount" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card>
          <n-statistic label="待填写" :value="pendingCount">
            <template #suffix>
              <n-text depth="3" style="font-size: 14px">项</n-text>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card>
          <n-statistic label="已超期" :value="expiredCount" :value-style="{ color: '#d03050' }">
            <template #suffix>
              <n-text depth="3" style="font-size: 14px">项</n-text>
            </template>
          </n-statistic>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card class="mt" title="待办提醒" :bordered="false">
      <n-spin :show="loading">
        <n-empty v-if="!reminders.length" description="暂无待办, 一切正常" />
        <n-list v-else hoverable clickable>
          <n-list-item v-for="r in reminders" :key="r.templateId + r.fieldId" @click="goFill(r)">
            <n-thing>
              <template #header>
                <n-space align="center">
                  <n-tag :type="r.type === 'REMINDER' ? 'warning' : 'info'" size="small" round>
                    {{ r.type === 'REMINDER' ? '已超期' : '未填写' }}
                  </n-tag>
                  <span>{{ r.templateName }} / {{ r.fieldName }}</span>
                </n-space>
              </template>
              <template #description>
                <n-text v-if="r.type === 'REMINDER'" depth="3">
                  上次填写: {{ formatTime(r.filledAt) }} · 已过去 {{ r.daysSinceFilled }} 天 (周期 {{ r.fillCycleDays }} 天)
                </n-text>
                <n-text v-else depth="3">尚未填写{{ r.fillCycleDays ? `, 周期 ${r.fillCycleDays} 天` : '' }}</n-text>
              </template>
            </n-thing>
          </n-list-item>
        </n-list>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { myReminders } from '@/api/form'
import { myTemplates } from '@/api/template'

const router = useRouter()
const reminders = ref([])
const templateCount = ref(0)
const loading = ref(false)

const pendingCount = computed(() => reminders.value.filter((r) => r.type === 'PENDING').length)
const expiredCount = computed(() => reminders.value.filter((r) => r.type === 'REMINDER').length)

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

function goFill(r) {
  router.push({ name: 'FormFill', params: { templateId: r.templateId } })
}

async function load() {
  loading.value = true
  try {
    const [rem, tpls] = await Promise.all([myReminders(), myTemplates()])
    reminders.value = rem || []
    templateCount.value = (tpls || []).length
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.mt {
  margin-top: 16px;
}
</style>
