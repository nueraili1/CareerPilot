<template>
  <el-drawer
    :model-value="modelValue"
    title="最近记录"
    direction="rtl"
    size="420px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-if="records.length === 0" class="empty-card">
      <strong>暂无记录</strong>
      <span>完成一次分析后会自动保存到这里。</span>
    </div>
    <div v-else class="record-list">
      <article v-for="record in records" :key="record.id" class="record-card">
        <div class="record-card__head">
          <button class="record-title" type="button" @click="emit('restore', record.id)">
            {{ record.resumeName }}
          </button>
          <span>{{ formatTime(record.createdAt) }}</span>
        </div>
        <el-tag type="primary" effect="light">{{ record.matchScore }} 分</el-tag>
        <p>{{ record.summary }}</p>
        <div class="record-card__actions">
          <el-button size="small" type="primary" plain :loading="loadingId === record.id" @click="emit('restore', record.id)">
            查看详情
          </el-button>
          <el-button size="small" type="danger" plain :loading="deletingId === record.id" @click="emit('remove', record)">
            删除
          </el-button>
        </div>
      </article>
    </div>
  </el-drawer>
</template>

<script setup>
defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  records: {
    type: Array,
    default: () => []
  },
  loadingId: {
    type: Number,
    default: null
  },
  deletingId: {
    type: Number,
    default: null
  },
  formatTime: {
    type: Function,
    required: true
  }
})

const emit = defineEmits(['update:modelValue', 'restore', 'remove'])
</script>

<style scoped>
.empty-card {
  display: grid;
  gap: 6px;
  padding: 18px;
  border: 1px dashed #ccd7e6;
  border-radius: 12px;
  background: #fbfdff;
  color: #7b8798;
}

.empty-card strong {
  color: #344258;
}

.record-list {
  display: grid;
  gap: 12px;
}

.record-card {
  padding: 14px;
  border: 1px solid #dfe7f2;
  border-radius: 12px;
  background: #ffffff;
}

.record-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.record-title {
  min-width: 0;
  padding: 0;
  overflow: hidden;
  border: 0;
  background: transparent;
  color: #172033;
  font: inherit;
  font-weight: 800;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.record-title:hover {
  color: #2f7df6;
}

.record-card span {
  flex: 0 0 auto;
  color: #8a95a8;
  font-size: 12px;
}

.record-card p {
  margin: 10px 0 0;
  color: #526176;
  font-size: 13px;
  line-height: 1.6;
}

.record-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
