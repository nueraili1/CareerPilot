import { computed, ref, watch } from 'vue'
import { loadAiStatus } from '../api/client'

const AI_CONFIG_KEY = 'careerpilot_ai_config'

export function useAiConfig() {
  const aiStatus = ref(null)
  const settingsVisible = ref(false)
  const aiConfig = ref(loadLocalAiConfig())
  const aiConfigDraft = ref({ ...aiConfig.value })

  const localAiConfigured = computed(() => Boolean(aiConfig.value.apiKey && aiConfig.value.baseUrl && aiConfig.value.model))
  const aiStatusLabel = computed(() => {
    if (localAiConfigured.value) {
      return `AI 已配置：${aiConfig.value.model}`
    }
    if (aiStatus.value?.configured) {
      return `AI 默认配置：${aiStatus.value.model || '后端模型'}`
    }
    return 'AI 未配置'
  })
  const aiStatusButtonType = computed(() => {
    if (localAiConfigured.value) {
      return 'primary'
    }
    if (aiStatus.value?.configured) {
      return 'success'
    }
    return 'warning'
  })
  const settingsTip = computed(() => {
    if (localAiConfigured.value) {
      return `当前使用你的前端 AI 配置：${aiConfig.value.model}。配置保存在当前浏览器，请不要在公共电脑保存 Key。`
    }
    if (aiStatus.value?.configured) {
      return `当前未填写个人 AI 配置，简历分析会使用后端默认模型：${aiStatus.value.model || '后端模型'}；AI 对话需要填写下面三项。`
    }
    return '当前没有可用 AI 配置。请填写 API Key、Base URL 和模型名后使用 AI 分析与对话。'
  })

  watch(settingsVisible, (visible) => {
    if (visible) {
      aiConfigDraft.value = { ...aiConfig.value }
    }
  })

  async function refreshAiStatus() {
    try {
      const response = await loadAiStatus()
      aiStatus.value = response.data
    } catch (error) {
      aiStatus.value = { configured: false, model: '未知' }
    }
  }

  function saveAiConfig() {
    aiConfig.value = {
      apiKey: aiConfigDraft.value.apiKey.trim(),
      baseUrl: aiConfigDraft.value.baseUrl.trim(),
      model: aiConfigDraft.value.model.trim()
    }
    localStorage.setItem(AI_CONFIG_KEY, JSON.stringify(aiConfig.value))
    settingsVisible.value = false
  }

  function clearAiConfig() {
    aiConfig.value = { apiKey: '', baseUrl: '', model: '' }
    aiConfigDraft.value = { ...aiConfig.value }
    localStorage.removeItem(AI_CONFIG_KEY)
  }

  function runtimeAiConfig() {
    return localAiConfigured.value ? aiConfig.value : null
  }

  return {
    aiStatus,
    settingsVisible,
    aiConfigDraft,
    localAiConfigured,
    aiStatusLabel,
    aiStatusButtonType,
    settingsTip,
    refreshAiStatus,
    saveAiConfig,
    clearAiConfig,
    runtimeAiConfig
  }
}

function loadLocalAiConfig() {
  try {
    return JSON.parse(localStorage.getItem(AI_CONFIG_KEY)) || { apiKey: '', baseUrl: '', model: '' }
  } catch (error) {
    return { apiKey: '', baseUrl: '', model: '' }
  }
}
