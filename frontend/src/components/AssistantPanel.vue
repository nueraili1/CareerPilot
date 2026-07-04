<template>
  <aside class="studio-panel interview-panel">
    <PanelHeader title="AI 助手" subtitle="模拟面试与上下文对话" />
    <el-tabs :model-value="rightTab" class="right-tabs" @update:model-value="emit('update:rightTab', $event)">
      <el-tab-pane label="模拟面试" name="interview">
        <div class="assistant-pane">
          <div class="tab-scroll">
            <div v-if="questions.length === 0" class="empty-card">
              <strong>暂无面试题</strong>
              <span>点击“生成面试题”，这里会显示可练习的问题。</span>
            </div>

            <ol v-else class="interview-list">
              <li v-for="(question, index) in questions" :key="question">
                <span>{{ index + 1 }}</span>
                <p>{{ question }}</p>
              </li>
            </ol>

            <el-alert v-if="interviewFeedback" :title="interviewFeedback" type="info" :closable="false" />
          </div>
          <div class="panel-footer">
            <el-button type="primary" plain :loading="interviewing" @click="emit('regenerate-interview')">重新生成</el-button>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="AI 对话" name="chat">
        <div class="chat-layout">
          <div class="chat-history-panel">
            <div class="chat-history-actions">
              <el-button size="small" type="primary" plain @click="emit('start-new-chat')">新对话</el-button>
              <el-button size="small" :loading="chatSessionsLoading" @click="emit('refresh-chat-sessions')">刷新历史</el-button>
              <span class="chat-history-tip">
                {{ currentUser ? '登录后自动保存 AI 对话' : '登录后可保存和恢复对话' }}
              </span>
            </div>
            <div v-if="currentUser && chatSessions.length > 0" class="chat-session-list">
              <button
                v-for="session in chatSessions"
                :key="session.id"
                class="chat-session-item"
                :class="{ 'chat-session-item--active': session.id === chatSessionId }"
                type="button"
                @click="emit('restore-chat-session', session.id)"
              >
                <span>{{ session.title || '新的 AI 对话' }}</span>
                <small>{{ formatTime(session.updatedAt) }}</small>
                <el-button
                  class="chat-session-delete"
                  size="small"
                  text
                  type="danger"
                  @click.stop="emit('remove-chat-session', session.id)"
                >
                  删除
                </el-button>
              </button>
            </div>
          </div>
          <div class="chat-messages">
            <div v-if="chatMessages.length === 0" class="empty-card">
              <strong>问问 CareerPilot</strong>
              <span>可以追问简历优化、面试回答、项目描述和岗位准备。</span>
            </div>
            <article
              v-for="(message, index) in chatMessages"
              :key="index"
              class="chat-bubble"
              :class="`chat-bubble--${message.role}`"
            >
              <p>{{ message.content }}</p>
            </article>
          </div>
          <div class="chat-input">
            <el-input
              :model-value="chatInput"
              type="textarea"
              :rows="3"
              resize="none"
              placeholder="例如：帮我把 CareerPilot 项目经历写得更适合 Java 后端实习"
              @update:model-value="emit('update:chatInput', $event)"
              @keydown.enter.exact.prevent="emit('send-chat')"
            />
            <el-button class="send-button" type="primary" :loading="chatting" @click="emit('send-chat')">发送</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </aside>
</template>

<script setup>
import PanelHeader from './PanelHeader.vue'

defineProps({
  rightTab: {
    type: String,
    required: true
  },
  questions: {
    type: Array,
    default: () => []
  },
  interviewFeedback: {
    type: String,
    default: ''
  },
  interviewing: {
    type: Boolean,
    required: true
  },
  currentUser: {
    type: Object,
    default: null
  },
  chatSessionsLoading: {
    type: Boolean,
    required: true
  },
  chatSessions: {
    type: Array,
    default: () => []
  },
  chatSessionId: {
    type: Number,
    default: null
  },
  chatMessages: {
    type: Array,
    default: () => []
  },
  chatInput: {
    type: String,
    default: ''
  },
  chatting: {
    type: Boolean,
    required: true
  },
  formatTime: {
    type: Function,
    required: true
  }
})

const emit = defineEmits([
  'update:rightTab',
  'regenerate-interview',
  'start-new-chat',
  'refresh-chat-sessions',
  'restore-chat-session',
  'remove-chat-session',
  'update:chatInput',
  'send-chat'
])
</script>

<style scoped>
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

.right-tabs {
  min-height: 0;
  height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
}

.right-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 18px;
}

.right-tabs :deep(.el-tabs__content),
.right-tabs :deep(.el-tab-pane) {
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.assistant-pane {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
}

.tab-scroll {
  min-height: 0;
  height: auto;
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

.interview-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.interview-list li {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 10px;
  padding: 12px;
  border: 1px solid #dfe7f2;
  border-radius: 10px;
  background: #ffffff;
}

.interview-list span {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #eef5ff;
  color: #2f6fbb;
  font-size: 12px;
  font-weight: 800;
}

.interview-list p {
  margin: 0;
  color: #344258;
  font-size: 14px;
  line-height: 1.7;
}

.chat-layout {
  height: 100%;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-height: 0;
}

.chat-history-panel {
  padding: 12px 16px 10px;
  border-bottom: 1px solid #e4eaf3;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.chat-history-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-history-tip {
  margin-left: auto;
  color: #8492a6;
  font-size: 12px;
}

.chat-session-list {
  display: grid;
  gap: 8px;
  max-height: 132px;
  overflow-y: auto;
  margin-top: 10px;
  padding-right: 4px;
}

.chat-session-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 4px 8px;
  align-items: center;
  width: 100%;
  padding: 9px 10px;
  border: 1px solid #e2e9f4;
  border-radius: 12px;
  background: #ffffff;
  color: #26364d;
  text-align: left;
  cursor: pointer;
}

.chat-session-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 700;
}

.chat-session-item small {
  grid-column: 1;
  color: #93a0b3;
  font-size: 11px;
}

.chat-session-item--active {
  border-color: #2f7df6;
  background: #eef5ff;
}

.chat-session-delete {
  grid-column: 2;
  grid-row: 1 / span 2;
}

.chat-messages {
  display: grid;
  align-content: start;
  gap: 10px;
  overflow-y: auto;
  padding: 16px 18px;
}

.chat-bubble {
  max-width: 92%;
  padding: 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.75;
}

.chat-bubble p {
  margin: 0;
  white-space: pre-wrap;
}

.chat-bubble--user {
  justify-self: end;
  background: #2f7df6;
  color: #ffffff;
}

.chat-bubble--assistant {
  justify-self: start;
  border: 1px solid #dfe7f2;
  background: #ffffff;
  color: #344258;
}

.chat-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 74px;
  align-items: end;
  gap: 10px;
  padding: 14px 18px;
  border-top: 1px solid #e4eaf3;
  background: #fbfcfe;
}

.chat-input :deep(.el-textarea__inner) {
  min-height: 72px !important;
  max-height: 92px !important;
}

.send-button {
  height: 72px;
}
</style>
