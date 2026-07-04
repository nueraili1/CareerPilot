<template>
  <aside class="studio-panel source-panel">
    <PanelHeader title="简历与岗位" subtitle="上传简历，补全目标岗位信息" />
    <div class="panel-scroll">
      <el-upload
        class="upload-box"
        drag
        :auto-upload="false"
        :show-file-list="false"
        accept=".pdf,.docx,.txt"
        :on-change="(file) => emit('file-change', file)"
      >
        <div class="upload-mark">PDF</div>
        <div class="upload-title">拖拽或点击上传简历</div>
        <div class="upload-subtitle">{{ resumeName || '支持 PDF / DOCX / TXT，也可以直接粘贴文本' }}</div>
      </el-upload>

      <el-button class="resume-builder-button" type="primary" plain @click="emit('open-resume-builder')">
        打开简历编辑器
      </el-button>

      <label class="field-label">简历文本</label>
      <el-input
        :model-value="resumeText"
        type="textarea"
        :rows="12"
        resize="none"
        placeholder="粘贴你的简历内容，或上传文件后自动解析"
        @update:model-value="emit('update:resumeText', $event)"
      />

      <div class="target-header">
        <label class="field-label">目标岗位 JD</label>
        <span class="target-tip">支持文字、截图、公司名和岗位名组合补全</span>
      </div>

      <div class="target-grid">
        <el-input
          :model-value="targetCompanyName"
          placeholder="公司名称，例如：字节跳动"
          @update:model-value="emit('update:targetCompanyName', $event)"
        />
        <el-input
          :model-value="targetRoleTitle"
          placeholder="岗位名称，例如：Java 后端开发实习生"
          @update:model-value="emit('update:targetRoleTitle', $event)"
        />
      </div>

      <el-upload
        class="target-upload"
        :auto-upload="false"
        :show-file-list="false"
        accept="image/png,image/jpeg,image/webp"
        :on-change="(file) => emit('target-image-change', file)"
      >
        <el-button plain>上传岗位截图</el-button>
      </el-upload>

      <div v-if="targetImageName" class="target-image-row">
        <span class="target-image-name">{{ targetImageName }}</span>
        <el-button text type="danger" @click="emit('clear-target-image')">移除</el-button>
      </div>

      <el-input
        :model-value="jobDescription"
        type="textarea"
        :rows="7"
        resize="none"
        placeholder="可粘贴岗位描述、招聘要求，或写一段你对目标岗位的补充说明"
        @update:model-value="emit('update:jobDescription', $event)"
      />

      <el-button class="resolve-target-button" plain :loading="targetResolving" @click="emit('resolve-target')">
        识别并联网补全
      </el-button>

      <div v-if="targetResolvedPreview" class="resolved-block">
        <label class="field-label">识别与搜索补充</label>
        <el-input
          :model-value="targetResolvedPreview"
          type="textarea"
          :rows="8"
          resize="none"
          readonly
        />
      </div>
    </div>

    <div class="panel-footer">
      <el-button type="primary" :loading="analyzing" @click="emit('analyze')">开始分析</el-button>
      <el-button :loading="interviewing" @click="emit('interview')">生成面试题</el-button>
    </div>
  </aside>
</template>

<script setup>
import PanelHeader from './PanelHeader.vue'

defineProps({
  resumeName: {
    type: String,
    default: ''
  },
  resumeText: {
    type: String,
    default: ''
  },
  jobDescription: {
    type: String,
    default: ''
  },
  targetCompanyName: {
    type: String,
    default: ''
  },
  targetRoleTitle: {
    type: String,
    default: ''
  },
  targetImageName: {
    type: String,
    default: ''
  },
  targetResolvedPreview: {
    type: String,
    default: ''
  },
  targetResolving: {
    type: Boolean,
    required: true
  },
  analyzing: {
    type: Boolean,
    required: true
  },
  interviewing: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits([
  'file-change',
  'target-image-change',
  'clear-target-image',
  'resolve-target',
  'open-resume-builder',
  'update:resumeText',
  'update:jobDescription',
  'update:targetCompanyName',
  'update:targetRoleTitle',
  'analyze',
  'interview'
])
</script>

<style scoped>
.studio-panel {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  border: 1px solid #d9e1ed;
  border-radius: 14px;
  background: #ffffff;
}

.panel-scroll {
  min-height: 0;
  padding: 16px 18px;
  overflow: auto;
}

.panel-footer {
  display: flex;
  gap: 10px;
  padding: 14px 18px;
  border-top: 1px solid #e4eaf3;
  background: #fbfcfe;
}

.panel-footer .el-button {
  flex: 1;
}

.upload-box {
  margin-bottom: 18px;
}

.resume-builder-button {
  width: 100%;
  margin-bottom: 4px;
}

.upload-mark {
  width: 42px;
  height: 30px;
  display: grid;
  place-items: center;
  margin: 0 auto 10px;
  border: 1px solid #bdd2f2;
  border-radius: 8px;
  background: #edf5ff;
  color: #2c6fbb;
  font-size: 12px;
  font-weight: 800;
}

.upload-title {
  color: #172033;
  font-size: 15px;
  font-weight: 800;
}

.upload-subtitle {
  margin-top: 6px;
  color: #8792a3;
  font-size: 12px;
}

.field-label {
  display: block;
  margin: 16px 0 8px;
  color: #344258;
  font-size: 13px;
  font-weight: 800;
}

.target-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.target-header .field-label {
  margin-bottom: 6px;
}

.target-tip {
  color: #7b8799;
  font-size: 12px;
}

.target-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 10px;
}

.target-upload {
  margin-bottom: 8px;
}

.target-image-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  padding: 10px 12px;
  border: 1px solid #dbe4f0;
  border-radius: 8px;
  background: #f8fbff;
}

.target-image-name {
  min-width: 0;
  color: #344258;
  font-size: 12px;
  word-break: break-all;
}

.resolve-target-button {
  width: 100%;
  margin-top: 10px;
}

.resolved-block {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .target-grid {
    grid-template-columns: 1fr;
  }

  .target-header {
    display: block;
  }
}
</style>
