<template>
  <el-dialog :model-value="modelValue" title="AI 设置" width="520px" @update:model-value="emit('update:modelValue', $event)">
    <el-alert :title="settingsTip" type="info" :closable="false" />
    <el-form label-position="top" class="settings-form">
      <el-form-item label="AI_API_KEY">
        <el-input
          :model-value="draft.apiKey"
          type="password"
          show-password
          placeholder="sk-..."
          @update:model-value="updateField('apiKey', $event)"
        />
      </el-form-item>
      <el-form-item label="AI_BASE_URL">
        <el-input
          :model-value="draft.baseUrl"
          placeholder="https://api.okinto.com/v1"
          @update:model-value="updateField('baseUrl', $event)"
        />
      </el-form-item>
      <el-form-item label="AI_MODEL">
        <el-input
          :model-value="draft.model"
          placeholder="gpt-5.5"
          @update:model-value="updateField('model', $event)"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('clear')">清空配置</el-button>
      <el-button type="primary" @click="emit('save')">保存配置</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  settingsTip: {
    type: String,
    required: true
  },
  draft: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['update:modelValue', 'update:draft', 'save', 'clear'])

function updateField(field, value) {
  emit('update:draft', {
    ...props.draft,
    [field]: value
  })
}
</script>

<style scoped>
.settings-form {
  margin-top: 16px;
}
</style>
