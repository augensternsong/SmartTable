<template>
  <n-card :bordered="false">
    <n-space align="center" class="mb" justify="space-between">
      <n-text depth="3">权限为系统预置, 仅支持查看, 不可新增或修改</n-text>
      <n-space align="center">
        <n-input v-model:value="filterText" placeholder="筛选权限名称/编码" clearable style="width: 240px" />
        <n-button @click="toggleExpand">{{ expanded ? '全部收起' : '全部展开' }}</n-button>
      </n-space>
    </n-space>

    <n-spin :show="loading">
      <n-tree
        block-line
        :data="filteredTreeData"
        :expanded-keys="expandedKeys"
        :render-label="renderLabel"
        :render-prefix="renderPrefix"
      />
      <n-empty v-if="!loading && !filteredTreeData.length" description="暂无权限数据" class="mt" />
    </n-spin>
  </n-card>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { useMessage, NTag } from 'naive-ui'
import { permissionTree } from '@/api/permission'

const message = useMessage()
const loading = ref(false)
const filterText = ref('')
const expanded = ref(true)
const treeData = ref([])
const expandedKeys = ref([])

const TYPE_TAG = {
  MENU: { text: '菜单', type: 'info' },
  BUTTON: { text: '按钮', type: 'success' },
  API: { text: '接口', type: 'warning' }
}

function buildTree(nodes) {
  return (nodes || []).map((n) => ({
    key: n.id,
    name: n.permName,
    permCode: n.permCode || '',
    permType: n.permType,
    children: buildTree(n.children)
  }))
}

function collectKeys(nodes, acc = []) {
  for (const n of nodes || []) {
    acc.push(n.key)
    if (n.children?.length) collectKeys(n.children, acc)
  }
  return acc
}

function filterNodes(nodes, kw) {
  const result = []
  for (const n of nodes) {
    const selfMatch = n.name.includes(kw) || n.permCode.toLowerCase().includes(kw)
    const children = n.children?.length ? filterNodes(n.children, kw) : []
    if (selfMatch || children.length) {
      result.push({ ...n, children: selfMatch ? n.children : children })
    }
  }
  return result
}

const filteredTreeData = computed(() => {
  const kw = filterText.value.trim().toLowerCase()
  if (!kw) return treeData.value
  return filterNodes(treeData.value, kw)
})

function renderLabel(node) {
  return h('span', { style: 'display: inline-flex; align-items: center; gap: 8px' }, [
    h('span', null, node.name),
    node.permCode ? h('span', { style: 'color: var(--n-text-color-3, #999); font-size: 12px' }, node.permCode) : null
  ])
}

function renderPrefix(node) {
  if (!node.permType) return null
  const t = TYPE_TAG[node.permType] || { text: node.permType, type: 'default' }
  return h(NTag, { size: 'small', type: t.type, round: true, bordered: false }, { default: () => t.text })
}

function toggleExpand() {
  expanded.value = !expanded.value
  expandedKeys.value = expanded.value ? collectKeys(treeData.value) : []
}

async function load() {
  loading.value = true
  try {
    const data = await permissionTree()
    treeData.value = buildTree(data)
    expandedKeys.value = collectKeys(treeData.value)
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.mb {
  margin-bottom: 16px;
}
.mt {
  margin-top: 24px;
}
</style>
