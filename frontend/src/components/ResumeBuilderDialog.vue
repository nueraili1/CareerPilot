<template>
  <el-dialog
    :model-value="modelValue"
    title="模板化简历编辑器"
    width="1180px"
    class="resume-builder-dialog"
    @update:model-value="handleDialogVisibility"
  >
    <div class="resume-studio">
      <aside class="resume-editor-panel">
        <div class="template-strip">
          <button
            v-for="template in resumeTemplates"
            :key="template.id"
            class="template-card"
            :class="{ 'template-card--active': resumeTemplateId === template.id }"
            type="button"
            @click="emit('update:resumeTemplateId', template.id)"
          >
            <span :style="{ background: template.accent }"></span>
            <strong>{{ template.name }}</strong>
            <small>{{ template.description }}</small>
          </button>
        </div>

        <div class="builder-feature-grid">
          <section class="builder-feature-card">
            <div class="builder-feature-card__head">
              <strong>主题与字体</strong>
              <span>自定义颜色方案和专业字体</span>
            </div>
            <div class="builder-mini-grid">
              <el-select
                :model-value="resumeThemePresetId"
                size="small"
                @update:model-value="emit('update:resumeThemePresetId', $event)"
              >
                <el-option
                  v-for="preset in resumeThemePresets"
                  :key="preset.id"
                  :label="preset.name"
                  :value="preset.id"
                />
              </el-select>
              <el-color-picker
                :model-value="resumeThemeColor"
                show-alpha
                @update:model-value="emit('update:resumeThemeColor', $event)"
              />
            </div>
            <el-select
              :model-value="resumeFontFamily"
              size="small"
              class="builder-font-select"
              @update:model-value="emit('update:resumeFontFamily', $event)"
            >
              <el-option
                v-for="font in resumeFontOptions"
                :key="font.value"
                :label="font.label"
                :value="font.value"
              />
            </el-select>
          </section>

          <section class="builder-feature-card">
            <div class="builder-feature-card__head">
              <strong>照片 / 头像</strong>
              <span>本地上传预览，导出时保留</span>
            </div>
            <div class="photo-preview-box">
              <div class="photo-preview-frame" :class="`photo-preview-frame--${resumeProfile.photoShape}`" @click="openResumePhotoPicker">
                <img v-if="resumeProfile.photoVisible && resumeProfile.photoUrl" :src="resumeProfile.photoUrl" alt="照片预览" />
                <div v-else class="photo-preview-placeholder">{{ resumePhotoFallbackText }}</div>
              </div>
              <div class="photo-preview-text">
                <p>支持 PNG / JPG / WEBP</p>
                <p>照片不上传服务器，只保存在当前浏览器。</p>
              </div>
            </div>
            <div class="photo-actions">
              <input ref="resumePhotoInputRef" class="photo-input" type="file" accept="image/png,image/jpeg,image/webp" @change="handleResumePhotoPick" />
              <el-button type="primary" plain @click="openResumePhotoPicker">上传照片</el-button>
              <el-switch v-model="resumeProfile.photoVisible" active-text="显示照片" inactive-text="隐藏照片" />
              <el-radio-group v-model="resumeProfile.photoShape" size="small">
                <el-radio-button label="portrait">证件照</el-radio-button>
                <el-radio-button label="square">方形头像</el-radio-button>
              </el-radio-group>
              <el-button plain @click="clearResumePhoto">清除照片</el-button>
            </div>
          </section>

          <section class="builder-feature-card builder-feature-card--sections">
            <div class="builder-feature-card__head">
              <strong>模块标题</strong>
              <span>可在这里直接改标题，也可在简历上直接编辑</span>
            </div>
            <div v-for="(section, index) in resumeSections" :key="section.key" class="section-control">
              <el-input v-model="section.label" size="small" />
              <el-switch v-model="section.visible" />
              <el-button size="small" plain @click="moveSectionUp(index)">↑</el-button>
              <el-button size="small" plain @click="moveSectionDown(index)">↓</el-button>
              <el-button size="small" plain @click="resetSectionLabel(section.key)">恢复</el-button>
            </div>
          </section>
        </div>

        <el-form label-position="top" class="resume-builder-form">
          <div class="builder-two-cols">
            <el-form-item label="姓名">
              <el-input v-model="resumeProfile.name" placeholder="例如：张三" />
            </el-form-item>
            <el-form-item label="求职意向">
              <el-input v-model="resumeProfile.targetRole" placeholder="Java 后端开发实习生" />
            </el-form-item>
          </div>
          <div class="builder-three-cols">
            <el-form-item label="手机号">
              <el-input v-model="resumeProfile.phone" placeholder="13800000000" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="resumeProfile.email" placeholder="name@example.com" />
            </el-form-item>
            <el-form-item label="所在地">
              <el-input v-model="resumeProfile.location" placeholder="上海" />
            </el-form-item>
          </div>
          <el-form-item label="教育经历">
            <el-input v-model="resumeProfile.education" type="textarea" :rows="3" resize="none" placeholder="学校、专业、时间、主修课程、绩点或荣誉" />
          </el-form-item>
          <el-form-item label="技能栈">
            <el-input v-model="resumeProfile.skills" type="textarea" :rows="4" resize="none" placeholder="Java、Spring Boot、MySQL、RESTful API、Vue3、AI API 调用..." />
          </el-form-item>
          <el-form-item label="项目经历">
            <el-input v-model="resumeProfile.projects" type="textarea" :rows="5" resize="none" placeholder="项目名称、技术栈、职责、结果" />
          </el-form-item>
          <el-form-item label="实习 / 校园经历">
            <el-input v-model="resumeProfile.experience" type="textarea" :rows="4" resize="none" placeholder="竞赛、社团、实习、兼职中能体现沟通、责任心的经历" />
          </el-form-item>
          <el-form-item label="自我评价">
            <el-input v-model="resumeProfile.evaluation" type="textarea" :rows="3" resize="none" placeholder="技术导向、学习能力、项目经验、求职态度" />
          </el-form-item>
          <el-form-item label="其他 / 原始内容">
            <el-input v-model="resumeProfile.extra" type="textarea" :rows="3" resize="none" placeholder="上传简历中暂未识别的内容会放在这里" />
          </el-form-item>
        </el-form>
      </aside>

      <section class="visual-resume-preview">
        <div class="visual-preview-toolbar">
          <div>
            <strong>A4 简历编辑预览</strong>
            <span>{{ currentResumeTemplate.name }} · 点击简历文字可直接修改</span>
          </div>
          <div class="resume-zoom-controls">
            <el-button size="small" plain @click="zoomResumeOut">-</el-button>
            <span>{{ resumeZoomPercent }}</span>
            <el-button size="small" plain @click="zoomResumeIn">+</el-button>
            <el-button size="small" plain @click="fitResumeWidth">适应宽度</el-button>
            <el-button size="small" plain @click="resetResumeZoom">100%</el-button>
            <el-tag v-if="builtResumeAiEnhanced" type="success" effect="plain">AI 已优化</el-tag>
            <el-tag v-else type="info" effect="plain">可直接编辑</el-tag>
          </div>
        </div>
        <div ref="resumePreviewScrollRef" class="a4-scroll">
          <div class="a4-stage" :style="a4StageStyle">
            <article
              ref="resumePreviewRef"
              class="a4-page editable-a4-page"
              :style="a4PageStyle"
              @blur.capture="syncResumePreviewEdits"
              v-html="resumePreviewHtml"
            ></article>
          </div>
        </div>
      </section>
    </div>
    <template #footer>
      <div class="resume-builder-footer">
        <div class="resume-builder-actions">
          <el-button :disabled="!resumeText.trim()" @click="emit('import-current-resume')">导入当前简历</el-button>
          <el-button @click="emitWithSync('apply-built-resume')">填入左侧简历文本</el-button>
          <el-button type="primary" :loading="resumeBuilding" @click="emitWithSync('build-resume')">AI 优化内容</el-button>
        </div>
        <div class="resume-export-actions">
          <el-button @click="emitWithSync('download-markdown')">Markdown</el-button>
          <el-button @click="emitWithSync('download-html')">HTML</el-button>
          <el-button @click="emitWithSync('download-word')">Word</el-button>
          <el-button @click="emitWithSync('download-json')">JSON</el-button>
          <el-button type="success" plain @click="emitWithSync('print-pdf')">PDF</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  resumeTemplates: {
    type: Array,
    required: true
  },
  resumeTemplateId: {
    type: String,
    required: true
  },
  resumeThemePresets: {
    type: Array,
    required: true
  },
  resumeThemePresetId: {
    type: String,
    required: true
  },
  resumeThemeColor: {
    type: String,
    required: true
  },
  resumeFontOptions: {
    type: Array,
    required: true
  },
  resumeFontFamily: {
    type: String,
    required: true
  },
  resumeProfile: {
    type: Object,
    required: true
  },
  resumeSections: {
    type: Array,
    required: true
  },
  builtResumeAiEnhanced: {
    type: Boolean,
    required: true
  },
  resumePreviewHtml: {
    type: String,
    required: true
  },
  resumeText: {
    type: String,
    required: true
  },
  resumeBuilding: {
    type: Boolean,
    required: true
  },
  sectionDefaults: {
    type: Object,
    required: true
  }
})

const emit = defineEmits([
  'update:modelValue',
  'update:resumeTemplateId',
  'update:resumeThemePresetId',
  'update:resumeThemeColor',
  'update:resumeFontFamily',
  'build-resume',
  'import-current-resume',
  'apply-built-resume',
  'download-markdown',
  'download-html',
  'download-word',
  'download-json',
  'print-pdf'
])

const resumePreviewRef = ref(null)
const resumePreviewScrollRef = ref(null)
const resumePhotoInputRef = ref(null)
const resumeZoom = ref(0.72)

const currentResumeTemplate = computed(() => {
  return props.resumeTemplates.find((template) => template.id === props.resumeTemplateId) || props.resumeTemplates[0]
})

const resumePhotoFallbackText = computed(() => {
  const name = props.resumeProfile.name.trim()
  if (name) {
    return name.slice(0, 2)
  }
  return '照片'
})

const resumeZoomPercent = computed(() => `${Math.round(resumeZoom.value * 100)}%`)
const a4StageStyle = computed(() => ({
  width: `${Math.ceil(794 * resumeZoom.value)}px`,
  minHeight: `${Math.ceil(1123 * resumeZoom.value)}px`
}))
const a4PageStyle = computed(() => ({
  transform: `scale(${resumeZoom.value})`
}))

function handleDialogVisibility(visible) {
  if (!visible) {
    syncResumePreviewEdits()
  }
  emit('update:modelValue', visible)
}

function emitWithSync(eventName) {
  syncResumePreviewEdits()
  emit(eventName)
}

function zoomResumeIn() {
  resumeZoom.value = clampZoom(resumeZoom.value + 0.05)
}

function zoomResumeOut() {
  resumeZoom.value = clampZoom(resumeZoom.value - 0.05)
}

function resetResumeZoom() {
  resumeZoom.value = 1
}

function fitResumeWidth() {
  const width = resumePreviewScrollRef.value?.clientWidth || 794
  resumeZoom.value = clampZoom((width - 44) / 794)
}

function clampZoom(value) {
  return Math.min(1.2, Math.max(0.45, Number(value.toFixed(2))))
}

function syncResumePreviewEdits() {
  const root = resumePreviewRef.value
  if (!root) return

  root.querySelectorAll('[data-edit-field]').forEach((element) => {
    const field = element.getAttribute('data-edit-field')
    if (field && Object.prototype.hasOwnProperty.call(props.resumeProfile, field)) {
      props.resumeProfile[field] = cleanEditableField(field, element.innerText)
    }
  })

  root.querySelectorAll('[data-edit-section-title]').forEach((element) => {
    const sectionKey = element.getAttribute('data-edit-section-title')
    if (sectionKey) {
      setResumeSectionLabel(sectionKey, element.innerText)
    }
  })

  root.querySelectorAll('[data-edit-section]').forEach((element) => {
    const section = element.getAttribute('data-edit-section')
    if (section && Object.prototype.hasOwnProperty.call(props.resumeProfile, section)) {
      props.resumeProfile[section] = element.innerText.trim()
    }
  })
}

function cleanEditableField(field, value) {
  const text = value.trim()
  if (field === 'phone') return text.replace(/^电话[：:]\s*/, '')
  if (field === 'email') return text.replace(/^邮箱[：:]\s*/, '')
  if (field === 'location') return text.replace(/^所在地[：:]\s*/, '')
  return text
}

function setResumeSectionLabel(sectionKey, value) {
  const section = props.resumeSections.find((item) => item.key === sectionKey)
  if (!section) return
  const text = value.trim()
  section.label = text || props.sectionDefaults[sectionKey] || section.label
}

function resetSectionLabel(sectionKey) {
  setResumeSectionLabel(sectionKey, props.sectionDefaults[sectionKey] || '')
}

function moveSectionUp(index) {
  if (index <= 0) return
  const sections = props.resumeSections
  const current = sections[index]
  sections[index] = sections[index - 1]
  sections[index - 1] = current
}

function moveSectionDown(index) {
  if (index >= props.resumeSections.length - 1) return
  const sections = props.resumeSections
  const current = sections[index]
  sections[index] = sections[index + 1]
  sections[index + 1] = current
}

function openResumePhotoPicker() {
  resumePhotoInputRef.value?.click()
}

function handleResumePhotoPick(event) {
  const file = event?.target?.files?.[0]
  if (!file || !file.type.startsWith('image/')) {
    ElMessage.warning('请选择 PNG / JPG / WEBP 图片')
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    props.resumeProfile.photoUrl = String(reader.result || '')
    props.resumeProfile.photoVisible = true
  }
  reader.onerror = () => ElMessage.error('照片读取失败')
  reader.readAsDataURL(file)
  event.target.value = ''
}

function clearResumePhoto() {
  props.resumeProfile.photoUrl = ''
}
</script>

<style scoped>
.resume-builder-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.resume-studio {
  height: 74vh;
  min-height: 640px;
  display: grid;
  grid-template-columns: minmax(320px, 0.72fr) minmax(520px, 1.28fr);
  gap: 14px;
  overflow: hidden;
}

.resume-editor-panel,
.resume-section-panel,
.visual-resume-preview {
  min-height: 0;
  overflow: auto;
  border: 1px solid #dfe7f2;
  border-radius: 14px;
  background: #fbfdff;
}

.resume-editor-panel {
  padding: 14px;
}

.builder-feature-grid {
  display: grid;
  gap: 12px;
  margin-bottom: 14px;
}

.builder-feature-card {
  padding: 12px;
  border: 1px solid #e0e8f3;
  border-radius: 14px;
  background: #f8fbff;
}

.builder-feature-card__head {
  display: grid;
  gap: 4px;
  margin-bottom: 10px;
}

.builder-feature-card__head strong {
  color: #172033;
  font-size: 13px;
  font-weight: 900;
}

.builder-feature-card__head span {
  color: #7b8798;
  font-size: 11px;
}

.builder-mini-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
}

.builder-font-select {
  width: 100%;
}

.photo-preview-box {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
}

.photo-preview-frame {
  width: 96px;
  height: 124px;
  overflow: hidden;
  border: 1px dashed #c9d5e4;
  border-radius: 16px;
  background: #ffffff;
  cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease, border-color .15s ease;
}

.photo-preview-frame:hover {
  transform: translateY(-1px);
  border-color: #8cb7ff;
  box-shadow: 0 8px 18px rgba(47, 125, 246, 0.12);
}

.photo-preview-frame--square {
  border-radius: 18px;
}

.photo-preview-frame--portrait {
  border-radius: 16px;
}

.photo-preview-frame img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.photo-preview-placeholder {
  display: grid;
  place-items: center;
  height: 100%;
  color: #5b6b82;
  font-size: 22px;
  font-weight: 900;
}

.photo-preview-text p {
  margin: 0;
  color: #5b6b82;
  font-size: 12px;
  line-height: 1.6;
}

.photo-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.photo-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  border: 0;
}

.template-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.template-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 3px 8px;
  align-items: center;
  padding: 10px;
  border: 1px solid #dfe7f2;
  border-radius: 12px;
  background: #ffffff;
  color: #344258;
  text-align: left;
  cursor: pointer;
}

.template-card span {
  width: 18px;
  height: 18px;
  grid-row: span 2;
  border-radius: 6px;
}

.template-card strong {
  color: #172033;
  font-size: 13px;
}

.template-card small {
  color: #7b8798;
  font-size: 11px;
}

.template-card--active {
  border-color: #409eff;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.12);
}

.resume-builder-form {
  min-width: 0;
}

.builder-two-cols,
.builder-three-cols {
  display: grid;
  gap: 12px;
}

.builder-two-cols {
  grid-template-columns: 0.7fr 1.3fr;
}

.builder-three-cols {
  grid-template-columns: repeat(3, 1fr);
}

.resume-section-panel {
  padding: 14px;
}

.section-control-list {
  display: grid;
  gap: 10px;
}

.section-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto auto;
  gap: 8px;
  align-items: center;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #ffffff;
}

.section-control .el-input {
  min-width: 0;
}

.section-control .el-button {
  margin-left: 0;
}

.source-collapse {
  margin-top: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.visual-resume-preview {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  background: #eef2f8;
}

.visual-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-bottom: 1px solid #dfe7f2;
  background: #ffffff;
}

.visual-preview-toolbar strong {
  display: block;
  color: #172033;
  font-size: 14px;
}

.visual-preview-toolbar span {
  color: #7b8798;
  font-size: 12px;
}

.resume-zoom-controls {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.resume-zoom-controls > span {
  min-width: 42px;
  color: #344258;
  font-size: 12px;
  font-weight: 800;
  text-align: center;
}

.resume-zoom-controls .el-button {
  margin-left: 0;
}

.a4-scroll {
  min-height: 0;
  overflow: auto;
  padding: 22px;
}

.a4-stage {
  position: relative;
  margin: 0;
}

.a4-page {
  position: absolute;
  top: 0;
  left: 0;
  transform-origin: top left;
}

.resume-builder-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.resume-builder-actions,
.resume-export-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.resume-export-actions {
  justify-content: flex-end;
}

.resume-builder-actions .el-button,
.resume-export-actions .el-button {
  margin-left: 0;
}

@media (max-width: 1180px) {
  .resume-studio,
  .builder-two-cols,
  .builder-three-cols {
    grid-template-columns: 1fr;
  }

  .resume-studio {
    height: auto;
    overflow: visible;
  }

  .resume-builder-footer {
    grid-template-columns: 1fr;
  }

  .resume-export-actions {
    justify-content: flex-start;
  }
}
</style>
