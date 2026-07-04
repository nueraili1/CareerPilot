<template>
  <section class="studio-panel analysis-panel">
    <PanelHeader title="分析结果" subtitle="匹配度、短板和简历优化建议" />
    <div class="panel-scroll">
      <div v-if="!analysis" class="empty-card">
        <strong>等待分析</strong>
        <span>填写简历和 JD 后，点击左侧“开始分析”。</span>
      </div>

      <template v-else>
        <section class="score-card">
          <div class="score-number">
            <strong>{{ analysis.matchScore }}</strong>
            <span>分</span>
          </div>
          <div class="score-copy">
            <div class="score-line">
              <el-progress :percentage="analysis.matchScore" :show-text="false" :stroke-width="8" />
              <el-tag :type="scoreTagType" effect="light">{{ scoreLabel }}</el-tag>
            </div>
            <p>{{ analysis.summary }}</p>
          </div>
        </section>

        <section class="stat-grid">
          <div class="stat-card">
            <strong>{{ analysis.strengths?.length || 0 }}</strong>
            <span>匹配优势</span>
          </div>
          <div class="stat-card">
            <strong>{{ analysis.gaps?.length || 0 }}</strong>
            <span>能力短板</span>
          </div>
          <div class="stat-card">
            <strong>{{ analysis.suggestions?.length || 0 }}</strong>
            <span>优化建议</span>
          </div>
        </section>

        <section v-if="prioritySuggestions.length" class="priority-card">
          <div class="block-title">
            <strong>优先处理</strong>
            <el-tag type="danger" effect="plain">招聘会前</el-tag>
          </div>
          <ol>
            <li v-for="item in prioritySuggestions" :key="item">{{ item }}</li>
          </ol>
        </section>

        <el-collapse :model-value="openSections" class="clean-collapse" @update:model-value="emit('update:openSections', $event)">
          <el-collapse-item title="匹配优势" name="strengths">
            <ResultList :items="analysis.strengths" type="success" />
          </el-collapse-item>
          <el-collapse-item title="能力短板" name="gaps">
            <ResultList :items="analysis.gaps" type="warning" />
          </el-collapse-item>
          <el-collapse-item title="优化建议" name="suggestions">
            <ResultList :items="analysis.suggestions" type="primary" />
          </el-collapse-item>
          <el-collapse-item title="项目写法" name="projectRewrite">
            <ResultList :items="analysis.projectRewrite" type="info" />
          </el-collapse-item>
        </el-collapse>
      </template>
    </div>
  </section>
</template>

<script setup>
import PanelHeader from './PanelHeader.vue'
import ResultList from './ResultList.vue'

defineProps({
  analysis: {
    type: Object,
    default: null
  },
  prioritySuggestions: {
    type: Array,
    default: () => []
  },
  openSections: {
    type: Array,
    default: () => []
  },
  scoreTagType: {
    type: String,
    required: true
  },
  scoreLabel: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['update:openSections'])
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

.score-card {
  display: grid;
  grid-template-columns: 106px 1fr;
  gap: 18px;
  padding: 16px;
  border: 1px solid #dce6f3;
  border-radius: 12px;
  background: #f8fbff;
}

.score-number {
  min-height: 104px;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e4ebf5;
}

.score-number strong {
  color: #172033;
  font-size: 34px;
  line-height: 1;
}

.score-number span {
  margin-top: 4px;
  color: #6b778c;
  font-size: 13px;
}

.score-copy {
  min-width: 0;
}

.score-line {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px;
}

.score-copy p {
  max-height: 116px;
  margin: 0;
  padding-right: 8px;
  overflow: auto;
  color: #3d4d63;
  font-size: 14px;
  line-height: 1.8;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 12px;
}

.stat-card {
  padding: 12px;
  border: 1px solid #dfe7f2;
  border-radius: 10px;
  background: #ffffff;
}

.stat-card strong {
  display: block;
  color: #172033;
  font-size: 24px;
  line-height: 1;
}

.stat-card span {
  display: block;
  margin-top: 8px;
  color: #6d7890;
  font-size: 12px;
}

.priority-card {
  margin-top: 12px;
  padding: 14px 16px;
  border: 1px solid #f0d7a8;
  border-radius: 12px;
  background: #fffaf1;
}

.block-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.priority-card ol {
  margin: 0;
  padding-left: 18px;
  color: #4a3920;
  font-size: 13px;
  line-height: 1.8;
}

.clean-collapse {
  margin-top: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.clean-collapse :deep(.el-collapse-item__header) {
  padding: 0 16px;
  color: #172033;
  font-weight: 800;
}

.clean-collapse :deep(.el-collapse-item__content) {
  padding: 0 16px 16px;
}
</style>
