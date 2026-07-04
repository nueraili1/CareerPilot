<template>
  <main class="app-shell">
    <header class="app-header">
      <div class="brand">
        <div class="brand-mark">CP</div>
        <div>
          <h1>CareerPilot</h1>
          <p>AI 简历分析与模拟面试平台</p>
        </div>
      </div>

      <div class="header-actions">
        <el-tag type="success" effect="light">Spring Boot + Vue3</el-tag>
        <el-tag v-if="currentUser" type="info" effect="light">已登录：{{ currentUser.username }}</el-tag>
        <el-button v-if="currentUser" plain @click="logout">退出</el-button>
        <el-button v-else type="primary" plain @click="openAuth('login')">登录 / 注册</el-button>
        <el-button :type="aiStatusButtonType" plain @click="settingsVisible = true">
          {{ aiStatusLabel }}
        </el-button>
        <el-button plain @click="recordsVisible = true">最近记录</el-button>
      </div>
    </header>

    <section class="studio-grid">
      <SourcePanel
        :resume-name="resumeName"
        :resume-text="resumeText"
        :job-description="jobDescription"
        :target-company-name="targetCompanyName"
        :target-role-title="targetRoleTitle"
        :target-image-name="targetImageName"
        :target-resolved-preview="targetResolvedPreview"
        :target-resolving="targetResolving"
        :analyzing="analyzing"
        :interviewing="interviewing"
        @file-change="handleFileChange"
        @target-image-change="handleTargetImageChange"
        @clear-target-image="clearTargetImage"
        @resolve-target="handleResolveTarget"
        @open-resume-builder="resumeBuilderVisible = true"
        @update:resumeText="resumeText = $event"
        @update:jobDescription="jobDescription = $event"
        @update:targetCompanyName="targetCompanyName = $event"
        @update:targetRoleTitle="targetRoleTitle = $event"
        @analyze="handleAnalyze"
        @interview="handleInterview"
      />

      <AnalysisPanel
        :analysis="analysis"
        :priority-suggestions="prioritySuggestions"
        :open-sections="openSections"
        :score-tag-type="scoreTagType"
        :score-label="scoreLabel"
        @update:openSections="openSections = $event"
      />

      <AssistantPanel
        :right-tab="rightTab"
        :questions="questions"
        :interview-feedback="interviewFeedback"
        :interviewing="interviewing"
        :current-user="currentUser"
        :chat-sessions-loading="chatSessionsLoading"
        :chat-sessions="chatSessions"
        :chat-session-id="chatSessionId"
        :chat-messages="chatMessages"
        :chat-input="chatInput"
        :chatting="chatting"
        :format-time="formatTime"
        @update:rightTab="rightTab = $event"
        @regenerate-interview="handleInterview"
        @start-new-chat="startNewChatSession"
        @refresh-chat-sessions="refreshChatSessions"
        @restore-chat-session="restoreChatSession"
        @remove-chat-session="removeChatSession"
        @update:chatInput="chatInput = $event"
        @send-chat="handleChat"
      />
    </section>

    <AiSettingsDialog
      v-model="settingsVisible"
      :settings-tip="settingsTip"
      :draft="aiConfigDraft"
      @update:draft="aiConfigDraft = $event"
      @clear="clearAiConfig"
      @save="saveAiConfig"
    />

    <AuthDialog
      v-model="authVisible"
      :mode="authMode"
      :title="authTitle"
      :subtitle="authSubtitle"
      :tip="authTip"
      :submit-text="authSubmitText"
      :code-button-text="codeButtonText"
      :form="authForm"
      :auth-loading="authLoading"
      :sending-code="sendingCode"
      :code-countdown="codeCountdown"
      :visible-code="visibleCode"
      @update:form="authForm = $event"
      @switch-mode="switchAuthMode"
      @send-code="sendCode"
      @submit="submitAuth"
    />

    <ResumeBuilderDialog
      v-model="resumeBuilderVisible"
      :resume-templates="resumeTemplates"
      :resume-template-id="resumeTemplateId"
      :resume-theme-presets="resumeThemePresets"
      :resume-theme-preset-id="resumeThemePresetId"
      :resume-theme-color="resumeThemeColor"
      :resume-font-options="resumeFontOptions"
      :resume-font-family="resumeFontFamily"
      :resume-profile="resumeProfile"
      :resume-sections="resumeSections"
      :built-resume-ai-enhanced="builtResumeAiEnhanced"
      :resume-preview-html="resumePreviewHtml"
      :resume-text="resumeText"
      :resume-building="resumeBuilding"
      :section-defaults="resumeSectionDefaults"
      @update:resumeTemplateId="resumeTemplateId = $event"
      @update:resumeThemePresetId="resumeThemePresetId = $event"
      @update:resumeThemeColor="resumeThemeColor = $event"
      @update:resumeFontFamily="resumeFontFamily = $event"
      @build-resume="handleBuildResume"
      @import-current-resume="importCurrentResumeToBuilder"
      @apply-built-resume="applyBuiltResume"
      @download-markdown="downloadMarkdown"
      @download-html="downloadHtml"
      @download-word="downloadWord"
      @download-json="downloadJson"
      @print-pdf="printPdf"
    />

    <RecordsDrawer
      v-model="recordsVisible"
      :records="records"
      :loading-id="recordLoadingId"
      :deleting-id="recordDeletingId"
      :format-time="formatTime"
      @restore="restoreRecord"
      @remove="removeRecord"
    />
  </main>
</template>

<script setup>
import { computed, defineAsyncComponent, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AnalysisPanel from './components/AnalysisPanel.vue'
import SourcePanel from './components/SourcePanel.vue'
import { useAiConfig } from './composables/useAiConfig'
import { useAuth } from './composables/useAuth'
import { useJobTarget } from './composables/useJobTarget'
import { useRecords } from './composables/useRecords'
import {
  analyzeResume,
  buildResume,
  deleteChatSession,
  generateQuestions,
  loadChatSessionDetail,
  loadChatSessions,
  parseResume,
  sendChatMessage
} from './api/client'

const AiSettingsDialog = defineAsyncComponent(() => import('./components/AiSettingsDialog.vue'))
const AssistantPanel = defineAsyncComponent(() => import('./components/AssistantPanel.vue'))
const AuthDialog = defineAsyncComponent(() => import('./components/AuthDialog.vue'))
const RecordsDrawer = defineAsyncComponent(() => import('./components/RecordsDrawer.vue'))
const ResumeBuilderDialog = defineAsyncComponent(() => import('./components/ResumeBuilderDialog.vue'))

const resumeName = ref('')
const resumeText = ref('')
const jobDescription = ref('')
const analysis = ref(null)
const questions = ref([])
const interviewFeedback = ref('')
const analyzing = ref(false)
const interviewing = ref(false)
const resumeBuilderVisible = ref(false)
const rightTab = ref('interview')
const openSections = ref(['strengths', 'gaps', 'suggestions'])
const chatInput = ref('')
const chatting = ref(false)
const chatMessages = ref([])
const chatSessionId = ref(null)
const chatSessions = ref([])
const chatSessionsLoading = ref(false)
const resumeBuilding = ref(false)
const builtResumeAiEnhanced = ref(false)
const resumeTemplateId = ref('techBlue')
const resumeTemplates = [
  { id: 'techBlue', name: '\u84dd\u8272\u79d1\u6280\u98ce', description: '\u540e\u7aef / AI \u5e94\u7528', accent: '#2f7df6' },
  { id: 'purpleSidebar', name: '\u7d2b\u8272\u4fa7\u680f\u98ce', description: '\u9192\u76ee\u4e14\u6709\u8bbe\u8ba1\u611f', accent: '#7c3aed' },
  { id: 'freshGreen', name: '\u7eff\u8272\u6e05\u65b0\u98ce', description: '\u6821\u56ed\u6c42\u804c', accent: '#10b981' },
  { id: 'classic', name: '\u9ed1\u767d\u6781\u7b80\u98ce', description: '\u6b63\u5f0f\u6295\u9012', accent: '#111827' }
]
resumeTemplates.push(
  { id: 'navyBusiness', name: '\u6df1\u84dd\u5546\u52a1\u98ce', description: '\u7a33\u91cd\u540e\u7aef\u5c97', accent: '#1d4ed8' },
  { id: 'orangeModern', name: '\u6a59\u8272\u6d3b\u529b\u98ce', description: '\u5e74\u8f7b\u6709\u51b2\u52b2', accent: '#f97316' },
  { id: 'slateCard', name: '\u5361\u7247\u5206\u533a\u98ce', description: '\u6a21\u5757\u6e05\u6670', accent: '#475569' },
  { id: 'redLine', name: '\u7ea2\u9ed1\u7ebf\u6761\u98ce', description: '\u6b63\u5f0f\u4e14\u9192\u76ee', accent: '#dc2626' }
)
const resumeSectionDefaults = {
  education: '\u6559\u80b2\u80cc\u666f',
  skills: '\u6280\u80fd\u6808',
  projects: '\u9879\u76ee\u7ecf\u5386',
  experience: '\u5b9e\u4e60 / \u6821\u56ed\u7ecf\u5386',
  evaluation: '\u81ea\u6211\u8bc4\u4ef7',
  extra: '\u5176\u4ed6\u4fe1\u606f'
}
const resumeThemePresets = [
  { id: 'ocean', name: '\u6d77\u84dd\u4e13\u4e1a', accent: '#2f7df6' },
  { id: 'violet', name: '\u6c89\u7a33\u7d2b', accent: '#7c3aed' },
  { id: 'mint', name: '\u6e05\u65b0\u7eff', accent: '#10b981' },
  { id: 'amber', name: '\u6696\u6a59', accent: '#f97316' },
  { id: 'slate', name: '\u77f3\u58a8\u7070', accent: '#475569' },
  { id: 'crimson', name: '\u7ea2\u9ed1\u98ce', accent: '#dc2626' }
]
const resumeFontOptions = [
  { label: '\u7cfb\u7edf\u65e0\u886c\u7ebf', value: '-apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif' },
  { label: '\u5fae\u8f6f\u96c5\u9ed1', value: '"Microsoft YaHei", "PingFang SC", sans-serif' },
  { label: '\u82f9\u65b9', value: '"PingFang SC", "Microsoft YaHei", sans-serif' },
  { label: '\u601d\u6e90\u9ed1\u4f53', value: '"Source Han Sans SC", "Noto Sans SC", "Microsoft YaHei", sans-serif' },
  { label: 'HarmonyOS Sans', value: '"HarmonyOS Sans", "Microsoft YaHei", sans-serif' },
  { label: '\u5546\u52a1\u886c\u7ebf', value: 'Georgia, "Times New Roman", serif' }
]
const resumeThemePresetId = ref('ocean')
const resumeThemeColor = ref('#2f7df6')
const resumeFontFamily = ref('"Microsoft YaHei", "PingFang SC", sans-serif')
const resumeProfile = ref({
  name: '',
  targetRole: 'Java \u540e\u7aef\u5f00\u53d1\u5b9e\u4e60\u751f',
  phone: '',
  email: '',
  location: '',
  photoUrl: '',
  photoVisible: true,
  photoShape: 'portrait',
  education: '',
  skills: 'Java\u3001Spring Boot\u3001MySQL\u3001RESTful API\u3001Vue3\u3001AI API \u8c03\u7528',
  projects: 'CareerPilot AI \u7b80\u5386\u5206\u6790\u4e0e\u6a21\u62df\u9762\u8bd5\u5e73\u53f0\uff1a\u57fa\u4e8e Spring Boot + Vue3 \u5f00\u53d1\uff0c\u652f\u6301\u7b80\u5386\u89e3\u6790\u3001\u5c97\u4f4d\u5339\u914d\u5206\u6790\u3001\u6a21\u62df\u9762\u8bd5\u9898\u751f\u6210\u3001AI \u5bf9\u8bdd\u548c\u5386\u53f2\u8bb0\u5f55\u7ba1\u7406\u3002',
  experience: '',
  evaluation: '\u719f\u6089 Java / Spring Boot \u540e\u7aef\u5f00\u53d1\uff0c\u5177\u5907\u5b8c\u6574 AI Web \u9879\u76ee\u5f00\u53d1\u7ecf\u9a8c\uff0c\u4e86\u89e3\u524d\u540e\u7aef\u8054\u8c03\u3001\u63a5\u53e3\u8bbe\u8ba1\u548c\u6570\u636e\u6301\u4e45\u5316\u3002',
  extra: ''
})
const resumeSections = ref([
  { key: 'education', label: '\u6559\u80b2\u80cc\u666f', visible: true },
  { key: 'skills', label: '\u6280\u80fd\u6808', visible: true },
  { key: 'projects', label: '\u9879\u76ee\u7ecf\u5386', visible: true },
  { key: 'experience', label: '\u5b9e\u4e60 / \u6821\u56ed\u7ecf\u5386', visible: true },
  { key: 'evaluation', label: '\u81ea\u6211\u8bc4\u4ef7', visible: true },
  { key: 'extra', label: '\u5176\u4ed6\u4fe1\u606f', visible: false }
])

const {
  settingsVisible,
  aiConfigDraft,
  localAiConfigured,
  aiStatusLabel,
  aiStatusButtonType,
  settingsTip,
  refreshAiStatus,
  saveAiConfig: persistAiConfig,
  clearAiConfig: resetAiConfig,
  runtimeAiConfig
} = useAiConfig()

const {
  targetCompanyName,
  targetRoleTitle,
  targetImageName,
  targetImageDataUrl,
  targetImageExtractedText,
  targetSearchQuery,
  targetEnrichmentSummary,
  targetSearchHint,
  targetSearchResults,
  targetResolving,
  hasTargetInput,
  handleTargetImageChange,
  clearTargetImage,
  resolveTargetContext,
  buildRequestTargetContext,
  restoreTargetContext
} = useJobTarget({
  jobDescription,
  runtimeAiConfig
})

const {
  records,
  recordsVisible,
  recordLoadingId,
  recordDeletingId,
  refreshRecords,
  restoreRecord,
  removeRecord
} = useRecords({
  resumeName,
  resumeText,
  jobDescription,
  restoreTargetContext,
  analysis,
  questions,
  interviewFeedback,
  rightTab
})

const {
  authVisible,
  authMode,
  authLoading,
  sendingCode,
  codeCountdown,
  visibleCode,
  authForm,
  currentUser,
  authTitle,
  authSubtitle,
  authTip,
  authSubmitText,
  codeButtonText,
  openAuth,
  switchAuthMode,
  sendCode,
  submitAuth,
  refreshCurrentUser,
  logout
} = useAuth({
  onAuthSuccess: async () => {
    await refreshRecords()
    await refreshChatSessions(false)
  },
  onLogout: async () => {
    analysis.value = null
    questions.value = []
    interviewFeedback.value = ''
    chatSessions.value = []
    startNewChatSession()
    await refreshRecords()
  }
})

function saveAiConfig() {
  persistAiConfig()
  ElMessage.success('\u0041\u0049 \u914d\u7f6e\u5df2\u4fdd\u5b58')
}

function clearAiConfig() {
  resetAiConfig()
  ElMessage.success('\u0041\u0049 \u914d\u7f6e\u5df2\u6e05\u7a7a')
}

const scoreLabel = computed(() => {
  const score = analysis.value?.matchScore || 0
  if (score >= 85) return '\u5f3a\u5339\u914d'
  if (score >= 70) return '\u8f83\u5339\u914d'
  if (score >= 50) return '\u5f85\u4f18\u5316'
  return '宸窛杈冨ぇ'
})

const scoreTagType = computed(() => {
  const score = analysis.value?.matchScore || 0
  if (score >= 85) return 'success'
  if (score >= 70) return 'primary'
  if (score >= 50) return 'warning'
  return 'danger'
})

const prioritySuggestions = computed(() => (analysis.value?.suggestions || []).slice(0, 3))
const currentResumeTemplate = computed(() => resumeTemplates.find((template) => template.id === resumeTemplateId.value) || resumeTemplates[0])
const currentResumeAccent = computed(() => resumeThemeColor.value || currentResumeTemplate.value.accent)
const visibleResumeSections = computed(() => resumeSections.value.filter((section) => section.visible && sectionContent(section.key).trim()))
const resumePreviewHtml = computed(() => renderResumeHtml())
const targetResolvedPreview = computed(() => {
  const blocks = []
  if (targetCompanyName.value.trim() || targetRoleTitle.value.trim()) {
    blocks.push([
      targetCompanyName.value.trim() ? `公司：${targetCompanyName.value.trim()}` : '',
      targetRoleTitle.value.trim() ? `岗位：${targetRoleTitle.value.trim()}` : ''
    ].filter(Boolean).join('\n'))
  }
  if (targetImageExtractedText.value.trim()) {
    blocks.push(`图片识别文本：\n${targetImageExtractedText.value.trim()}`)
  }
  if (targetEnrichmentSummary.value.trim()) {
    const header = targetSearchQuery.value.trim()
      ? `联网搜索补充（${targetSearchQuery.value.trim()}）：`
      : '联网搜索补充：'
    blocks.push(`${header}\n${targetEnrichmentSummary.value.trim()}`)
  } else if (targetSearchHint.value.trim()) {
    blocks.push(targetSearchHint.value.trim())
  }
  return blocks.filter(Boolean).join('\n\n')
})

watch(resumeThemePresetId, (presetId) => {
  applyThemePreset(presetId)
}, { immediate: true })

async function handleFileChange(uploadFile) {
  try {
    const response = await parseResume(uploadFile.raw)
    resumeName.value = response.data.fileName
    resumeText.value = response.data.text
    ElMessage.success('\u7b80\u5386\u89e3\u6790\u5b8c\u6210')
  } catch (error) {
    ElMessage.error(error.message || '\u7b80\u5386\u89e3\u6790\u5931\u8d25')
  }
}

async function handleResolveTarget() {
  await resolveTargetContext()
}

async function ensureResolvedTargetContext() {
  if (!hasTargetInput()) {
    return buildRequestTargetContext()
  }

  const currentSearchQuery = [targetCompanyName.value.trim(), targetRoleTitle.value.trim()].filter(Boolean).join(' ')
  const shouldResolve = Boolean(
    targetImageDataUrl.value
      || (currentSearchQuery && targetSearchQuery.value.trim() !== currentSearchQuery)
      || (!targetEnrichmentSummary.value.trim()
        && !targetSearchHint.value.trim()
        && (targetCompanyName.value.trim() || targetRoleTitle.value.trim()))
  )

  if (shouldResolve) {
    return await resolveTargetContext({ silent: true })
  }
  return buildRequestTargetContext()
}

async function handleAnalyze() {
  if (!resumeText.value.trim() || !hasTargetInput()) {
    ElMessage.warning('请先填写简历文本和目标岗位信息')
    return
  }

  analyzing.value = true
  try {
    const targetContext = await ensureResolvedTargetContext()
    const response = await analyzeResume({
      resumeName: resumeName.value || '\u624b\u52a8\u8f93\u5165\u7b80\u5386',
      resumeText: resumeText.value,
      jobDescription: jobDescription.value,
      targetContext,
      aiConfig: runtimeAiConfig()
    })
    analysis.value = response.data
    questions.value = response.data.interviewQuestions || []
    await refreshRecords()
    ElMessage.success('鍒嗘瀽瀹屾垚')
  } catch (error) {
    ElMessage.error(error.message || '\u5206\u6790\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u540e\u7aef\u670d\u52a1')
  } finally {
    analyzing.value = false
  }
}

async function handleInterview() {
  if (!resumeText.value.trim() || !hasTargetInput()) {
    ElMessage.warning('请先填写简历文本和目标岗位信息')
    return
  }

  interviewing.value = true
  try {
    const targetContext = await ensureResolvedTargetContext()
    const response = await generateQuestions({
      resumeText: resumeText.value,
      jobDescription: jobDescription.value,
      targetContext,
      answer: '',
      aiConfig: runtimeAiConfig()
    })
    questions.value = response.data.questions || []
    interviewFeedback.value = response.data.feedback
    rightTab.value = 'interview'
    ElMessage.success('闈㈣瘯棰樺凡鐢熸垚')
  } catch (error) {
    ElMessage.error(error.message || '\u751f\u6210\u9762\u8bd5\u9898\u5931\u8d25')
  } finally {
    interviewing.value = false
  }
}

async function handleChat() {
  const content = chatInput.value.trim()
  if (!content) {
    return
  }
  if (!localAiConfigured.value) {
    ElMessage.warning('璇峰厛鍦ㄥ彸涓婅 AI 璁剧疆涓～鍐?API Key銆丅ase URL 鍜屾ā鍨嬪悕')
    settingsVisible.value = true
    return
  }

  const nextMessages = [...chatMessages.value, { role: 'user', content }]
  chatMessages.value = nextMessages
  chatInput.value = ''
  chatting.value = true

  try {
    const targetContext = await ensureResolvedTargetContext()
    const response = await sendChatMessage({
      resumeText: resumeText.value,
      jobDescription: jobDescription.value,
      targetContext,
      sessionId: chatSessionId.value,
      messages: nextMessages,
      aiConfig: runtimeAiConfig()
    })
    if (response.data.sessionId) {
      chatSessionId.value = response.data.sessionId
    }
    chatMessages.value.push({ role: 'assistant', content: response.data.answer })
    await refreshChatSessions(false)
  } catch (error) {
    ElMessage.error(error.message || 'AI 瀵硅瘽澶辫触')
  } finally {
    chatting.value = false
  }
}

function startNewChatSession() {
  chatSessionId.value = null
  chatMessages.value = []
  chatInput.value = ''
}

async function refreshChatSessions(showError = true) {
  if (!currentUser.value) {
    chatSessions.value = []
    return
  }
  chatSessionsLoading.value = true
  try {
    const response = await loadChatSessions()
    chatSessions.value = response.data || []
  } catch (error) {
    if (showError) {
      ElMessage.error(error.message || '鍔犺浇瀵硅瘽鍘嗗彶澶辫触')
    }
  } finally {
    chatSessionsLoading.value = false
  }
}

async function restoreChatSession(id) {
  try {
    const response = await loadChatSessionDetail(id)
    const session = response.data
    chatSessionId.value = session.id
    resumeText.value = session.resumeText || resumeText.value
    restoreTargetContext(session.targetContext, session.jobDescription || jobDescription.value)
    chatMessages.value = (session.messages || []).map((message) => ({
      role: message.role,
      content: message.content
    }))
    rightTab.value = 'chat'
    ElMessage.success('宸叉仮澶?AI 瀵硅瘽')
  } catch (error) {
    ElMessage.error(error.message || '鎭㈠瀵硅瘽澶辫触')
  }
}

async function removeChatSession(id) {
  try {
    await ElMessageBox.confirm('纭畾鍒犻櫎杩欐 AI 瀵硅瘽鍘嗗彶鍚楋紵', '鍒犻櫎纭', {
      confirmButtonText: '鍒犻櫎',
      cancelButtonText: '鍙栨秷',
      type: 'warning'
    })
    await deleteChatSession(id)
    if (chatSessionId.value === id) {
      startNewChatSession()
    }
    await refreshChatSessions(false)
    ElMessage.success('AI \u5bf9\u8bdd\u5df2\u5220\u9664')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '鍒犻櫎瀵硅瘽澶辫触')
    }
  }
}

async function handleBuildResume() {
  if (!resumeProfile.value.name.trim() || !resumeProfile.value.targetRole.trim()) {
    ElMessage.warning('璇疯嚦灏戝～鍐欏鍚嶅拰姹傝亴鎰忓悜')
    return
  }

  resumeBuilding.value = true
  try {
    const response = await buildResume({
      name: resumeProfile.value.name,
      targetRole: resumeProfile.value.targetRole,
      education: resumeProfile.value.education,
      skills: resumeProfile.value.skills,
      projects: resumeProfile.value.projects,
      experience: [resumeProfile.value.experience, resumeProfile.value.evaluation, resumeProfile.value.extra].filter(Boolean).join('\n\n'),
      aiConfig: runtimeAiConfig()
    })
    builtResumeAiEnhanced.value = response.data.aiEnhanced
    ElMessage.success(response.data.aiEnhanced ? 'AI \u5df2\u751f\u6210\u7b80\u5386' : '\u5df2\u751f\u6210\u672c\u5730\u6a21\u677f\u7b80\u5386')
  } catch (error) {
    ElMessage.error(error.message || '\u751f\u6210\u7b80\u5386\u5931\u8d25')
  } finally {
    resumeBuilding.value = false
  }
}

function applyBuiltResume() {
  resumeName.value = `${resumeProfile.value.name || '\u6211\u7684'}-\u6a21\u677f\u7b80\u5386`
  resumeText.value = buildResumeMarkdownFromProfile()
  resumeBuilderVisible.value = false
  ElMessage.success('\u5df2\u586b\u5165\u5de6\u4fa7\u7b80\u5386\u6587\u672c')
}

function importCurrentResumeToBuilder() {
  if (!resumeText.value.trim()) {
    ElMessage.warning('\u5f53\u524d\u6ca1\u6709\u53ef\u5bfc\u5165\u7684\u7b80\u5386\u6587\u672c')
    return
  }
  const parsed = parseResumeTextToProfile(resumeText.value)
  resumeProfile.value = {
    ...resumeProfile.value,
    ...parsed
  }
  builtResumeAiEnhanced.value = false
  ElMessage.success('\u5df2\u5bfc\u5165\u5f53\u524d\u7b80\u5386\uff0c\u53ef\u5728\u5b57\u6bb5\u4e2d\u7ee7\u7eed\u4fee\u6539')
}

function downloadMarkdown() {
  downloadBlob(`${resumeFileBaseName()}.md`, buildResumeMarkdownFromProfile(), 'text/markdown;charset=utf-8')
}

function downloadHtml() {
  downloadBlob(`${resumeFileBaseName()}.html`, buildResumeHtmlDocument(), 'text/html;charset=utf-8')
}

function downloadWord() {
  downloadBlob(`${resumeFileBaseName()}-WPS鍙紪杈戠増.doc`, buildEditableWordDocument(), 'application/msword;charset=utf-8')
}

function downloadJson() {
  const payload = {
    exportedAt: new Date().toISOString(),
    resumeName: resumeFileBaseName(),
    templateId: resumeTemplateId.value,
    theme: {
      presetId: resumeThemePresetId.value,
      accent: currentResumeAccent.value,
      fontFamily: resumeFontFamily.value
    },
    profile: resumeProfile.value,
    sections: resumeSections.value,
    markdown: buildResumeMarkdownFromProfile()
  }
  downloadBlob(`${resumeFileBaseName()}.json`, JSON.stringify(payload, null, 2), 'application/json;charset=utf-8')
}

function printPdf() {
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.warning('\u6d4f\u89c8\u5668\u62e6\u622a\u4e86\u6253\u5370\u7a97\u53e3\uff0c\u8bf7\u5141\u8bb8\u5f39\u7a97\u540e\u91cd\u8bd5')
    return
  }
  printWindow.document.open()
  printWindow.document.write(buildResumeHtmlDocument())
  printWindow.document.close()
  printWindow.focus()
  window.setTimeout(() => {
    printWindow.print()
  }, 300)
}

function applyThemePreset(presetId) {
  const preset = resumeThemePresets.find((item) => item.id === presetId)
  if (preset) {
    resumeThemeColor.value = preset.accent
  }
}

function parseResumeTextToProfile(text) {
  const normalized = text.replace(/\r/g, '').trim()
  const lines = normalized.split('\n').map((line) => line.trim()).filter(Boolean)
  const lowerText = normalized.toLowerCase()
  return {
    name: resumeProfile.value.name || inferName(lines),
    targetRole: inferTargetRole(normalized) || resumeProfile.value.targetRole,
    phone: normalized.match(/1\d{10}/)?.[0] || resumeProfile.value.phone,
    email: normalized.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i)?.[0] || resumeProfile.value.email,
    location: resumeProfile.value.location,
    education: extractSection(normalized, ['\u6559\u80b2\u80cc\u666f', '\u6559\u80b2\u7ecf\u5386', 'education']) || resumeProfile.value.education,
    skills: extractSection(normalized, ['\u6280\u80fd\u6808', '\u4e13\u4e1a\u6280\u80fd', '\u6280\u80fd', 'skills']) || resumeProfile.value.skills,
    projects: extractSection(normalized, ['\u9879\u76ee\u7ecf\u5386', '\u9879\u76ee\u7ecf\u9a8c', 'projects']) || resumeProfile.value.projects,
    experience: extractSection(normalized, ['\u5b9e\u4e60\u7ecf\u5386', '\u6821\u56ed\u7ecf\u5386', '\u793e\u4f1a\u7ecf\u5386', '\u5de5\u4f5c\u7ecf\u5386', 'experience']) || resumeProfile.value.experience,
    evaluation: extractSection(normalized, ['\u81ea\u6211\u8bc4\u4ef7', '\u4e2a\u4eba\u8bc4\u4ef7', 'self-evaluation']) || resumeProfile.value.evaluation,
    extra: lowerText.includes('education') || lowerText.includes('experience') ? normalized : resumeProfile.value.extra || normalized
  }
}

function inferName(lines) {
  const candidate = lines.find((line) => line.length >= 2 && line.length <= 12 && !/[\uff0c,]/.test(line))
  return candidate || ''
}

function inferTargetRole(text) {
  const match = text.match(/(?:\u6c42\u804c\u610f\u5411|\u5e94\u8058\u610f\u5411|\u76ee\u6807\u5c97\u4f4d)[:\uff1a\s]*(.+)/)
  return match?.[1]?.trim() || ''
}

function extractSection(text, titles) {
  const lines = text.split('\n')
  const titleIndex = lines.findIndex((line) => titles.some((title) => line.toLowerCase().includes(title.toLowerCase())))
  if (titleIndex < 0) return ''

  const stopWords = ['\u6559\u80b2\u80cc\u666f', '\u6559\u80b2\u7ecf\u5386', '\u6280\u80fd\u6808', '\u4e13\u4e1a\u6280\u80fd', '\u6280\u80fd', '\u9879\u76ee\u7ecf\u5386', '\u9879\u76ee\u7ecf\u9a8c', '\u5b9e\u4e60\u7ecf\u5386', '\u6821\u56ed\u7ecf\u5386', '\u793e\u4f1a\u7ecf\u5386', '\u5de5\u4f5c\u7ecf\u5386', '\u81ea\u6211\u8bc4\u4ef7', '\u4e2a\u4eba\u8bc4\u4ef7', '\u4e2a\u4eba\u4fe1\u606f', '\u6c42\u804c\u610f\u5411']
  const content = []
  for (let index = titleIndex + 1; index < lines.length; index++) {
    const line = lines[index].trim()
    if (!line) continue
    if (content.length > 0 && stopWords.some((word) => line.includes(word))) {
      break
    }
    content.push(line)
  }
  return content.join('\n').trim()
}


function formatTime(value) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

function resumeFileBaseName() {
  return safeFileName(resumeProfile.value.name || resumeName.value || 'CareerPilot\u7b80\u5386')
}

function safeFileName(value) {
  return (value || 'CareerPilot\u7b80\u5386')
    .replace(/[\\/:*?"<>|]/g, '-')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .slice(0, 40)
}

function downloadBlob(fileName, content, mimeType) {
  const blob = new Blob(['\ufeff', content], { type: mimeType })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

function buildResumeHtmlDocument() {
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title>${escapeHtml(resumeFileBaseName())}</title>
  <style>
    html, body, .resume-export-shell, .resume-template, .resume-template * {
      -webkit-print-color-adjust: exact !important;
      print-color-adjust: exact !important;
    }
    @page {
      size: A4;
      margin: 0;
    }
    body {
      margin: 0;
      padding: 24px;
      background: #eef2f8;
      -webkit-print-color-adjust: exact !important;
      print-color-adjust: exact !important;
    }
    @media print {
      body { padding: 0; background: #ffffff !important; }
      .resume-export-shell {
        margin: 0 !important;
        box-shadow: none !important;
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
      }
      .resume-template,
      .resume-template * {
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
      }
    }
  </style>
</head>
<body>
  <main class="resume-export-shell">
    ${renderResumeHtml()}
  </main>
</body>
</html>`
}

function buildEditableWordDocument() {
  const accent = currentResumeAccent.value
  const profile = resumeProfile.value
  const contactItems = [
    profile.phone ? `鐢佃瘽锛?{profile.phone}` : '',
    profile.email ? `閭锛?{profile.email}` : '',
    profile.location ? `鎵€鍦ㄥ湴锛?{profile.location}` : ''
  ].filter(Boolean)
  const photoHtml = profile.photoVisible && profile.photoUrl
    ? `<td style="width:96px;text-align:right;vertical-align:top;"><img src="${escapeHtml(profile.photoUrl)}" width="86" height="${profile.photoShape === 'square' ? '86' : '108'}" style="display:block;border:2px solid #ffffff;object-fit:cover;" /></td>`
    : ''
  const sectionHtml = visibleResumeSections.value.map((section) => `
    <tr>
      <td colspan="${photoHtml ? 2 : 1}" style="padding:14px 0 4px;border-bottom:1.5pt solid ${accent};">
        <h2 style="margin:0;color:${accent};font-size:14pt;font-weight:bold;">${escapeHtml(section.label)}</h2>
      </td>
    </tr>
    <tr>
      <td colspan="${photoHtml ? 2 : 1}" style="padding:7px 0 2px;color:#263244;font-size:10.5pt;line-height:1.55;">
        ${wordSectionContent(sectionContent(section.key))}
      </td>
    </tr>
  `).join('')

  return `<!DOCTYPE html>
<html xmlns:o="urn:schemas-microsoft-com:office:office"
      xmlns:w="urn:schemas-microsoft-com:office:word"
      xmlns="http://www.w3.org/TR/REC-html40">
<head>
  <meta charset="utf-8">
  <meta name="ProgId" content="Word.Document">
  <meta name="Generator" content="CareerPilot">
  <meta name="Originator" content="CareerPilot">
  <title>${escapeHtml(resumeFileBaseName())}</title>
  <!--[if gte mso 9]>
  <xml>
    <w:WordDocument>
      <w:View>Print</w:View>
      <w:Zoom>100</w:Zoom>
      <w:DoNotOptimizeForBrowser/>
    </w:WordDocument>
  </xml>
  <![endif]-->
  <style>
    @page Section1 { size: 595.3pt 841.9pt; margin: 36pt 42pt 36pt 42pt; }
    div.Section1 { page: Section1; }
    body { margin: 0; font-family: "Microsoft YaHei", "SimSun", Arial, sans-serif; color: #172033; }
    table { border-collapse: collapse; mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
    p { margin: 0 0 5px; }
    ul { margin: 0 0 0 18px; padding: 0; }
    li { margin: 0 0 4px; }
  </style>
</head>
<body>
  <div class="Section1">
    <table width="100%" cellpadding="0" cellspacing="0" style="width:100%;background:${accent};color:#ffffff;">
      <tr>
        <td style="padding:18px 20px;vertical-align:top;">
          <h1 style="margin:0 0 7px;font-size:24pt;line-height:1.1;color:#ffffff;">${escapeHtml(profile.name || '浣犵殑濮撳悕')}</h1>
          <p style="margin:0 0 9px;font-size:12pt;font-weight:bold;color:#ffffff;">${escapeHtml(profile.targetRole || 'Java 鍚庣寮€鍙戝疄涔犵敓')}</p>
          <p style="margin:0;font-size:9.5pt;color:#ffffff;">${escapeHtml(contactItems.join('  锝? '))}</p>
        </td>
        ${photoHtml}
      </tr>
    </table>
    <table width="100%" cellpadding="0" cellspacing="0" style="width:100%;">
      ${sectionHtml}
    </table>
  </div>
</body>
</html>`
}

function renderResumeHtml() {
  if (resumeTemplateId.value === 'purpleSidebar') return renderTemplatePurpleSidebar()
  if (resumeTemplateId.value === 'freshGreen') return renderTemplateFreshGreen()
  if (resumeTemplateId.value === 'classic') return renderTemplateClassic()
  if (resumeTemplateId.value === 'navyBusiness') return renderTemplateNavyBusiness()
  if (resumeTemplateId.value === 'orangeModern') return renderTemplateOrangeModern()
  if (resumeTemplateId.value === 'slateCard') return renderTemplateSlateCard()
  if (resumeTemplateId.value === 'redLine') return renderTemplateRedLine()
  return renderTemplateTechBlue()
}

function editableFieldAttrs(field) {
  return `contenteditable="true" spellcheck="false" title="鐐瑰嚮鐩存帴缂栬緫" data-edit-field="${field}"`
}

function editableSectionTitleAttrs(key) {
  return `contenteditable="true" spellcheck="false" title="鐐瑰嚮鐩存帴缂栬緫鏍囬" data-edit-section-title="${key}"`
}

function editableSectionAttrs(key) {
  return `contenteditable="true" spellcheck="false" title="鐐瑰嚮鐩存帴缂栬緫杩欎竴娈? data-edit-section="${key}"`
}

function renderTopRightBlock(layoutClass = '') {
  const parts = []
  if (resumeProfile.value.photoVisible) {
    parts.push(renderPhotoBlock(layoutClass))
  }
  parts.push(`<div class="resume-contact">${renderContactItems()}</div>`)
  return `<div class="resume-top-right ${layoutClass ? `resume-top-right--${layoutClass}` : ''}">${parts.join('')}</div>`
}

function renderPhotoBlock(layoutClass = '') {
  if (!resumeProfile.value.photoVisible) {
    return ''
  }
  const photoClass = `${layoutClass ? `resume-avatar--${layoutClass}` : ''} resume-avatar--${resumeProfile.value.photoShape === 'square' ? 'square' : 'portrait'}`
  if (resumeProfile.value.photoUrl) {
    return `<div class="resume-avatar ${photoClass}"><img src="${escapeHtml(resumeProfile.value.photoUrl)}" alt="鐓х墖" /></div>`
  }
  return `<div class="resume-avatar resume-avatar--placeholder ${photoClass}"><span>${escapeHtml(getResumePhotoFallbackText())}</span><small>鐐瑰嚮涓婁紶</small></div>`
}

function getResumePhotoFallbackText() {
  const name = resumeProfile.value.name.trim()
  if (name) {
    return name.slice(0, 2)
  }
  return '鐓х墖'
}

function renderTemplateTechBlue() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--tech">
      <header class="resume-hero">
        <div class="resume-hero__left">
          <div class="resume-hero__title">
          <h1 contenteditable="true" data-edit-field="name">${escapeHtml(resumeProfile.value.name || '浣犵殑濮撳悕')}</h1>
          <p contenteditable="true" data-edit-field="targetRole">${escapeHtml(resumeProfile.value.targetRole || 'Java 鍚庣寮€鍙戝疄涔犵敓')}</p>
          </div>
          <div class="resume-hero__meta">${renderContactItems()}</div>
        </div>
        <div class="resume-hero__photo">${renderPhotoBlock('hero')}</div>
      </header>
      <div class="resume-body">${renderSections()}</div>
    </section>`
}

function renderTemplatePurpleSidebar() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--purple">
      <aside class="resume-side">
        ${renderTopRightBlock('side')}
        <h1 contenteditable="true" data-edit-field="name">${escapeHtml(resumeProfile.value.name || '浣犵殑濮撳悕')}</h1>
        <p contenteditable="true" data-edit-field="targetRole">${escapeHtml(resumeProfile.value.targetRole || 'Java 鍚庣寮€鍙戝疄涔犵敓')}</p>
      </aside>
      <main class="resume-main">${renderSections()}</main>
    </section>`
}

function renderTemplateFreshGreen() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--fresh">
      <header class="resume-fresh-head">
        <div>
          <h1 contenteditable="true" data-edit-field="name">${escapeHtml(resumeProfile.value.name || '浣犵殑濮撳悕')}</h1>
          <p contenteditable="true" data-edit-field="targetRole">${escapeHtml(resumeProfile.value.targetRole || 'Java 鍚庣寮€鍙戝疄涔犵敓')}</p>
        </div>
        ${renderTopRightBlock('fresh')}
      </header>
      <div class="resume-body">${renderSections()}</div>
    </section>`
}

function renderTemplateClassic() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--classic">
      <header class="resume-classic-head">
        <h1 contenteditable="true" data-edit-field="name">${escapeHtml(resumeProfile.value.name || '浣犵殑濮撳悕')}</h1>
        <p contenteditable="true" data-edit-field="targetRole">${escapeHtml(resumeProfile.value.targetRole || 'Java 鍚庣寮€鍙戝疄涔犵敓')}</p>
        ${renderTopRightBlock('classic')}
      </header>
      <div class="resume-body">${renderSections()}</div>
    </section>`
}

function renderTemplateNavyBusiness() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--navy">
      <header class="resume-navy-head">
        <div class="resume-navy-title">
          <span>Career Resume</span>
          <h1 ${editableFieldAttrs('name')}>${escapeHtml(resumeProfile.value.name || '浣犵殑濮撳悕')}</h1>
          <p ${editableFieldAttrs('targetRole')}>${escapeHtml(resumeProfile.value.targetRole || 'Java 鍚庣寮€鍙戝疄涔犵敓')}</p>
        </div>
        ${renderTopRightBlock('navy')}
      </header>
      <div class="resume-navy-body">${renderSections()}</div>
    </section>`
}

function renderTemplateOrangeModern() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--orange">
      <div class="resume-orange-rail"></div>
      <header class="resume-orange-head">
        <div>
          <h1 ${editableFieldAttrs('name')}>${escapeHtml(resumeProfile.value.name || '浣犵殑濮撳悕')}</h1>
          <p ${editableFieldAttrs('targetRole')}>${escapeHtml(resumeProfile.value.targetRole || 'Java 鍚庣寮€鍙戝疄涔犵敓')}</p>
        </div>
        ${renderTopRightBlock('orange')}
      </header>
      <div class="resume-orange-body">${renderSections()}</div>
    </section>`
}

function renderTemplateSlateCard() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--card">
      <header class="resume-card-head">
        <div>
          <h1 ${editableFieldAttrs('name')}>${escapeHtml(resumeProfile.value.name || '浣犵殑濮撳悕')}</h1>
          <p ${editableFieldAttrs('targetRole')}>${escapeHtml(resumeProfile.value.targetRole || 'Java 鍚庣寮€鍙戝疄涔犵敓')}</p>
        </div>
        ${renderTopRightBlock('card')}
      </header>
      <div class="resume-card-body">${renderSections()}</div>
    </section>`
}

function renderTemplateRedLine() {
  return `${baseResumeStyle(currentResumeAccent.value, resumeFontFamily.value)}
    <section class="resume-template resume-template--redline">
      <header class="resume-redline-head">
        <div>
          <h1 ${editableFieldAttrs('name')}>${escapeHtml(resumeProfile.value.name || '浣犵殑濮撳悕')}</h1>
          <p ${editableFieldAttrs('targetRole')}>${escapeHtml(resumeProfile.value.targetRole || 'Java 鍚庣寮€鍙戝疄涔犵敓')}</p>
        </div>
        ${renderTopRightBlock('redline')}
      </header>
      <div class="resume-redline-body">${renderSections()}</div>
    </section>`
}

function baseResumeStyle(accent, fontFamily) {
  return `<style>
    .resume-template {
      width: 794px;
      min-height: 1123px;
      margin: 0 auto;
      overflow: hidden;
      background: #ffffff;
      color: #172033;
      font-family: ${fontFamily};
      line-height: 1.62;
      box-shadow: 0 18px 46px rgba(15, 23, 42, 0.12);
    }
    .resume-template * { box-sizing: border-box; }
    .resume-template,
    .resume-template * {
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
    .resume-template [contenteditable="true"] {
      outline: 0;
      border-radius: 6px;
      transition: box-shadow .15s ease, background .15s ease, transform .15s ease;
      cursor: text;
    }
    .resume-template [contenteditable="true"]:hover {
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.28), 0 8px 22px rgba(15, 23, 42, 0.08);
      background: rgba(255, 255, 255, 0.32);
    }
    .resume-template [contenteditable="true"]:focus {
      box-shadow: 0 0 0 2px #409eff;
      background: rgba(255, 255, 255, 0.46);
    }
    .resume-hero {
      display: grid;
      grid-template-columns: minmax(0, 1fr) auto;
      align-items: start;
      gap: 24px;
      padding: 26px 34px 22px;
      background-color: ${accent};
      min-height: 180px;
      background: linear-gradient(135deg, ${accent}, #172033);
      color: #ffffff;
    }
    .resume-template--tech .resume-hero__left {
      display: grid;
      gap: 12px;
      align-content: start;
      min-width: 0;
    }
    .resume-template--tech .resume-hero__title {
      display: grid;
      gap: 8px;
      min-width: 0;
    }
    .resume-template--tech .resume-hero h1 {
      margin: 0;
      font-size: 34px;
      line-height: 1.05;
      letter-spacing: 1px;
    }
    .resume-template--tech .resume-hero p {
      margin: 0;
      font-size: 17px;
      font-weight: 700;
      opacity: 0.96;
    }
    .resume-template--tech .resume-hero__meta {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 14px;
      max-width: 100%;
    }
    .resume-template--tech .resume-hero__meta .resume-contact {
      display: flex;
      flex-wrap: wrap;
      gap: 8px 14px;
    }
    .resume-template--tech .resume-hero__meta .resume-contact span {
      display: inline-flex;
      align-items: center;
      min-height: 28px;
      padding: 4px 10px;
      border-radius: 999px;
      background: rgba(255, 255, 255, 0.16);
      color: rgba(255, 255, 255, 0.96);
      backdrop-filter: blur(4px);
    }
    .resume-template--tech .resume-hero__photo {
      display: grid;
      justify-items: end;
      align-content: start;
      padding-top: 2px;
    }
    .resume-template--tech .resume-avatar--hero {
      width: 104px;
      height: 132px;
    }
    .resume-template--tech .resume-avatar--hero.resume-avatar--placeholder {
      padding: 10px;
    }
    .resume-template h1 { margin: 0; font-size: 30px; line-height: 1.2; letter-spacing: 1px; }
    .resume-template h2 { margin: 0 0 10px; color: ${accent}; font-size: 16px; letter-spacing: 1px; }
    .resume-template p { margin: 0; }
    .resume-template ul { margin: 0; padding-left: 18px; }
    .resume-template li { margin: 4px 0; }
    .resume-body { padding: 22px 42px 36px; }
    .resume-template--tech .resume-body { padding: 22px 34px 34px; }
    .resume-template--tech .resume-section { margin-bottom: 16px; }
    .resume-template:not(.resume-template--card) .resume-section { margin-bottom: 15px; }
    .resume-section-title {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;
      font-weight: 900;
    }
    .resume-section-title[contenteditable="true"] {
      padding: 2px 6px;
      margin-left: -6px;
      min-height: 22px;
    }
    .resume-section-title::before {
      content: "";
      width: 5px;
      height: 18px;
      border-radius: 999px;
      background: ${accent};
    }
    .resume-section-content {
      color: #344258;
      font-size: 13px;
      line-height: 1.65;
      min-height: 28px;
      padding: 4px 6px;
      margin: -4px -6px 0;
      white-space: pre-line;
    }
    .resume-contact {
      display: grid;
      gap: 6px;
      font-size: 12px;
      opacity: 0.92;
    }
    .resume-top-right {
      display: grid;
      justify-items: end;
      align-content: start;
      gap: 10px;
      min-width: 180px;
    }
    .resume-avatar {
      width: 94px;
      height: 118px;
      overflow: hidden;
      border: 2px solid rgba(255, 255, 255, 0.82);
      border-radius: 18px;
      background: rgba(255, 255, 255, 0.12);
      box-shadow: 0 10px 26px rgba(15, 23, 42, 0.14);
    }
    .resume-avatar img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
    .resume-avatar--placeholder {
      display: grid;
      place-items: center;
      gap: 3px;
      padding: 12px;
      color: #ffffff;
      text-align: center;
    }
    .resume-avatar--placeholder span {
      display: block;
      font-size: 22px;
      font-weight: 900;
      line-height: 1.1;
    }
    .resume-avatar--placeholder small {
      font-size: 11px;
      opacity: 0.88;
    }
    .resume-avatar--hero,
    .resume-avatar--fresh {
      width: 92px;
      height: 116px;
    }
    .resume-avatar--portrait {
      width: 94px;
      height: 118px;
      border-radius: 18px;
    }
    .resume-avatar--square {
      width: 118px;
      height: 118px;
      border-radius: 18px;
    }
    .resume-avatar--side {
      width: 116px;
      height: 144px;
      border-radius: 20px;
    }
    .resume-avatar--classic,
    .resume-avatar--card,
    .resume-avatar--redline {
      width: 88px;
      height: 110px;
    }
    .resume-avatar--navy {
      width: 100px;
      height: 124px;
      border-radius: 16px;
    }
    .resume-avatar--orange {
      width: 92px;
      height: 118px;
      border-radius: 22px;
    }
    .resume-avatar--fresh,
    .resume-avatar--orange,
    .resume-avatar--card,
    .resume-avatar--classic {
      border-color: rgba(15, 23, 42, 0.18);
      background: rgba(255, 255, 255, 0.92);
      color: #172033;
    }
    .resume-avatar--fresh.resume-avatar--placeholder,
    .resume-avatar--orange.resume-avatar--placeholder,
    .resume-avatar--card.resume-avatar--placeholder,
    .resume-avatar--classic.resume-avatar--placeholder {
      color: #172033;
    }
    .resume-top-right--fresh .resume-contact,
    .resume-top-right--card .resume-contact,
    .resume-top-right--classic .resume-contact {
      color: #475569;
    }
    .resume-top-right--side {
      justify-items: center;
      gap: 12px;
      width: 100%;
      min-width: 0;
    }
    .resume-top-right--side .resume-avatar {
      margin-bottom: 2px;
    }
    .resume-top-right--side .resume-contact {
      justify-items: center;
      text-align: center;
    }
    .resume-template--purple {
      display: grid;
      grid-template-columns: 218px 1fr;
    }
    .resume-template--purple .resume-side {
      padding: 28px 22px;
      background-color: ${accent};
      background: linear-gradient(180deg, ${accent}, #3b0764);
      color: #ffffff;
      display: grid;
      align-content: start;
      gap: 12px;
    }
    .resume-template--purple .resume-side h1 { font-size: 28px; line-height: 1.08; }
    .resume-template--purple .resume-side p { margin: 4px 0 12px; font-weight: 800; opacity: 0.94; }
    .resume-template--purple .resume-main { padding: 28px 34px 36px; }
    .resume-template--fresh { border-top: 10px solid ${accent}; }
    .resume-fresh-head {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      margin: 22px 42px 0;
      padding: 18px 20px;
      border-radius: 20px;
      background-color: #ecfdf5;
      background: linear-gradient(135deg, #ecfdf5, #ffffff);
      border: 1px solid rgba(16, 185, 129, 0.18);
    }
    .resume-template--classic { box-shadow: 0 18px 46px rgba(15, 23, 42, 0.08); }
    .resume-classic-head {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 220px;
      gap: 20px;
      margin: 26px 42px 0;
      padding-bottom: 14px;
      border-bottom: 2px solid #111827;
      text-align: left;
    }
    .resume-classic-head .resume-contact {
      margin-top: 4px;
      color: #475569;
    }
    .resume-template--navy {
      background: linear-gradient(180deg, #f8fafc 0%, #ffffff 36%);
    }
    .resume-navy-head {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 220px;
      gap: 28px;
      padding: 30px 44px 24px;
      border-bottom: 1px solid #dbeafe;
      background-color: #0f172a;
      background: linear-gradient(135deg, #0f172a, ${accent});
      color: #ffffff;
    }
    .resume-navy-title > span {
      display: inline-block;
      margin-bottom: 12px;
      color: #93c5fd;
      font-size: 12px;
      font-weight: 800;
      letter-spacing: 2px;
      text-transform: uppercase;
    }
    .resume-navy-title p { margin-top: 6px; color: #bfdbfe; font-weight: 700; }
    .resume-top-right--navy { align-self: end; padding: 14px; border-radius: 14px; background: rgba(255, 255, 255, 0.1); }
    .resume-top-right--navy .resume-avatar {
      border-color: rgba(191, 219, 254, 0.9);
      box-shadow: 0 12px 26px rgba(15, 23, 42, 0.22);
    }
    .resume-top-right--navy .resume-contact {
      color: #e0f2fe;
    }
    .resume-navy-body { padding: 24px 44px 40px; }
    .resume-template--navy .resume-section-title::before { width: 22px; height: 3px; border-radius: 0; }
    .resume-template--orange {
      position: relative;
      padding-left: 22px;
    }
    .resume-orange-rail {
      position: absolute;
      inset: 0 auto 0 0;
      width: 22px;
      background: linear-gradient(180deg, ${accent}, #fed7aa);
    }
    .resume-orange-head {
      display: flex;
      justify-content: space-between;
      gap: 20px;
      padding: 28px 42px 20px;
      background-color: #fff7ed;
      background: linear-gradient(135deg, #fff7ed, #ffffff);
      border-bottom: 1px solid #fed7aa;
    }
    .resume-orange-head h1 { color: #9a3412; }
    .resume-orange-head p { color: #c2410c; font-weight: 800; }
    .resume-top-right--orange .resume-contact { color: #7c2d12; }
    .resume-top-right--orange .resume-avatar {
      border-color: rgba(249, 115, 22, 0.28);
      box-shadow: 0 14px 28px rgba(249, 115, 22, 0.18);
    }
    .resume-orange-body { padding: 24px 42px 38px; }
    .resume-template--orange .resume-section {
      padding: 11px 0 13px;
      border-bottom: 1px dashed #fed7aa;
    }
    .resume-template--card {
      padding: 24px 34px 34px;
      background-color: #f1f5f9;
      background: linear-gradient(180deg, #f8fafc, #eef2f7);
    }
    .resume-card-head {
      display: flex;
      justify-content: space-between;
      gap: 24px;
      padding: 20px 24px;
      border-radius: 22px;
      background: #ffffff;
      box-shadow: 0 16px 34px rgba(15, 23, 42, 0.09);
    }
    .resume-card-head h1 { color: #0f172a; }
    .resume-card-head p { margin-top: 7px; color: ${accent}; font-weight: 800; }
    .resume-top-right--card .resume-avatar { border-color: rgba(71, 85, 105, 0.28); }
    .resume-top-right--card .resume-contact {
      color: #64748b;
    }
    .resume-card-body {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 14px;
      margin-top: 14px;
    }
    .resume-template--card .resume-section {
      margin: 0;
      padding: 15px 16px;
      border-radius: 18px;
      background: #ffffff;
      box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
    }
    .resume-template--card .resume-section:nth-child(1),
    .resume-template--card .resume-section:nth-child(3) {
      grid-column: 1 / -1;
    }
    .resume-template--redline {
      border-top: 12px solid ${accent};
      background: linear-gradient(90deg, #ffffff 0%, #ffffff 72%, #f8fafc 72%, #f8fafc 100%);
    }
    .resume-redline-head {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 230px;
      gap: 28px;
      margin: 26px 42px 0;
      padding-bottom: 14px;
      border-bottom: 3px solid #111827;
    }
    .resume-redline-head h1 { color: #111827; }
    .resume-redline-head p { margin-top: 8px; color: ${accent}; font-weight: 900; }
    .resume-top-right--redline .resume-contact {
      padding-left: 18px;
      border-left: 3px solid ${accent};
      color: #334155;
    }
    .resume-top-right--redline .resume-avatar {
      border-color: rgba(220, 38, 38, 0.28);
    }
    .resume-redline-body { padding: 24px 42px 38px; }
    .resume-template--redline .resume-section-title {
      padding-bottom: 6px;
      border-bottom: 1px solid #e2e8f0;
    }
    .resume-template--redline .resume-section-title::before {
      width: 12px;
      height: 12px;
      border-radius: 50%;
    }
    @media print {
      .resume-template {
        width: 210mm;
        min-height: 297mm;
        box-shadow: none;
        background-color: #ffffff !important;
      }
      .resume-template,
      .resume-template * {
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
      }
      .resume-hero {
        background-color: ${accent} !important;
        background: linear-gradient(135deg, ${accent}, #172033) !important;
      }
      .resume-template--purple .resume-side {
        background-color: ${accent} !important;
        background: linear-gradient(180deg, ${accent}, #3b0764) !important;
      }
      .resume-fresh-head {
        background-color: #ecfdf5 !important;
        background: linear-gradient(135deg, #ecfdf5, #ffffff) !important;
        border-color: rgba(16, 185, 129, 0.18) !important;
      }
      .resume-template--navy {
        background: linear-gradient(180deg, #f8fafc 0%, #ffffff 36%) !important;
      }
      .resume-navy-head {
        background-color: #0f172a !important;
        background: linear-gradient(135deg, #0f172a, ${accent}) !important;
      }
      .resume-template--orange {
        background-color: #ffffff !important;
      }
      .resume-orange-rail {
        background: linear-gradient(180deg, ${accent}, #fed7aa) !important;
      }
      .resume-orange-head {
        background-color: #fff7ed !important;
        background: linear-gradient(135deg, #fff7ed, #ffffff) !important;
        border-color: #fed7aa !important;
      }
      .resume-template--card {
        background-color: #f1f5f9 !important;
        background: linear-gradient(180deg, #f8fafc, #eef2f7) !important;
      }
      .resume-card-head,
      .resume-template--card .resume-section {
        background-color: #ffffff !important;
      }
      .resume-template--redline {
        background: linear-gradient(90deg, #ffffff 0%, #ffffff 72%, #f8fafc 72%, #f8fafc 100%) !important;
        border-top-color: ${accent} !important;
      }
      .resume-section-title::before {
        background-color: ${accent} !important;
      }
      .resume-avatar,
      .resume-top-right--navy {
        -webkit-print-color-adjust: exact !important;
        print-color-adjust: exact !important;
      }
    }
  </style>`
}

function renderContactItems() {
  return [
    ['phone', '鐢佃瘽', resumeProfile.value.phone],
    ['email', '閭', resumeProfile.value.email],
    ['location', '鎵€鍦ㄥ湴', resumeProfile.value.location]
  ]
    .filter(([, , value]) => value)
    .map(([field, label, value]) => `<span ${editableFieldAttrs(field)}>${label}锛?{escapeHtml(value)}</span>`)
    .join('')
}

function renderSections() {
  return visibleResumeSections.value.map((section) => `
    <section class="resume-section">
      <h2 class="resume-section-title" ${editableSectionTitleAttrs(section.key)}>${escapeHtml(section.label)}</h2>
      <div class="resume-section-content" ${editableSectionAttrs(section.key)}>${formatSectionContent(sectionContent(section.key))}</div>
    </section>
  `).join('')
}

function sectionContent(key) {
  return resumeProfile.value[key] || ''
}

function formatSectionContent(value) {
  const lines = value.split('\n').map((line) => line.trim()).filter(Boolean)
  if (lines.length > 1) {
    return `<ul>${lines.map((line) => `<li>${inlineMarkdown(line.replace(/^[-*]\s*/, ''))}</li>`).join('')}</ul>`
  }
  return inlineMarkdown(value)
}

function buildResumeMarkdownFromProfile() {
  const contact = [
    resumeProfile.value.phone,
    resumeProfile.value.email,
    resumeProfile.value.location
  ].filter(Boolean).join(' / ')
  const sections = visibleResumeSections.value
    .map((section) => `## ${section.label}\n\n${sectionContent(section.key)}`)
    .join('\n\n')
  return `# ${resumeProfile.value.name || '\u6211\u7684\u7b80\u5386'}\n\n**\u6c42\u804c\u610f\u5411\uff1a** ${resumeProfile.value.targetRole || ''}\n\n${contact ? `**\u8054\u7cfb\u65b9\u5f0f\uff1a** ${contact}\n\n` : ''}${sections}`.trim()
}

function markdownToHtml(markdown) {
  const lines = markdown.split(/\r?\n/)
  const html = []
  let listType = null

  const closeList = () => {
    if (listType) {
      html.push(`</${listType}>`)
      listType = null
    }
  }

  lines.forEach((line) => {
    const trimmed = line.trim()
    if (!trimmed) {
      closeList()
      return
    }

    if (trimmed.startsWith('### ')) {
      closeList()
      html.push(`<h3>${inlineMarkdown(trimmed.slice(4))}</h3>`)
      return
    }
    if (trimmed.startsWith('## ')) {
      closeList()
      html.push(`<h2>${inlineMarkdown(trimmed.slice(3))}</h2>`)
      return
    }
    if (trimmed.startsWith('# ')) {
      closeList()
      html.push(`<h1>${inlineMarkdown(trimmed.slice(2))}</h1>`)
      return
    }

    const orderedMatch = trimmed.match(/^\d+\.\s+(.+)$/)
    if (orderedMatch) {
      if (listType !== 'ol') {
        closeList()
        listType = 'ol'
        html.push('<ol>')
      }
      html.push(`<li>${inlineMarkdown(orderedMatch[1])}</li>`)
      return
    }

    const unorderedMatch = trimmed.match(/^[-*]\s+(.+)$/)
    if (unorderedMatch) {
      if (listType !== 'ul') {
        closeList()
        listType = 'ul'
        html.push('<ul>')
      }
      html.push(`<li>${inlineMarkdown(unorderedMatch[1])}</li>`)
      return
    }

    closeList()
    html.push(`<p>${inlineMarkdown(trimmed)}</p>`)
  })

  closeList()
  return html.join('\n')
}

function inlineMarkdown(value) {
  return escapeHtml(value)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
}

function wordSectionContent(value) {
  const lines = String(value || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)

  if (!lines.length) {
    return '<p style="color:#94a3b8;">鍙户缁ˉ鍏呰繖涓€閮ㄥ垎鍐呭</p>'
  }

  if (lines.length === 1) {
    return `<p>${inlineMarkdown(lines[0])}</p>`
  }

  return `<ul>${lines.map((line) => `<li>${inlineMarkdown(line.replace(/^[-*]\s*/, ''))}</li>`).join('')}</ul>`
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

onMounted(() => {
  refreshAiStatus()
  refreshCurrentUser()
  refreshRecords()
})
</script>

<style scoped>
.app-shell {
  height: 100vh;
  padding: 14px 18px 18px;
  overflow: hidden;
  background: #eef2f8;
}

.app-header {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-mark {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #111827;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
}

.brand h1 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  line-height: 1.2;
  letter-spacing: 0;
}

.brand p {
  margin: 3px 0 0;
  color: #69758a;
  font-size: 13px;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.studio-grid {
  height: calc(100vh - 90px);
  display: grid;
  grid-template-columns: minmax(320px, 0.86fr) minmax(520px, 1.18fr) minmax(320px, 0.86fr);
  gap: 14px;
}

.studio-panel {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  border: 1px solid #d9e1ed;
  border-radius: 14px;
  background: #ffffff;
}

@media (max-width: 1180px) {
  .app-shell {
    height: auto;
    overflow: auto;
  }

  .studio-grid {
    height: auto;
    grid-template-columns: 1fr;
  }

  .studio-panel {
    min-height: 520px;
  }
}
</style>
