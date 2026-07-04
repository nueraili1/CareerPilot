<template>
  <el-dialog :model-value="modelValue" width="480px" class="auth-dialog" @update:model-value="emit('update:modelValue', $event)">
    <template #header>
      <div class="auth-header">
        <div class="auth-logo">CP</div>
        <div>
          <h2>{{ title }}</h2>
          <p>{{ subtitle }}</p>
        </div>
      </div>
    </template>
    <el-alert class="auth-tip" :title="tip" type="info" :closable="false" />
    <el-form label-position="top" class="settings-form auth-form" @submit.prevent>
      <el-form-item v-if="mode === 'login'" label="用户名 / 手机号">
        <el-input
          :model-value="form.identifier"
          size="large"
          placeholder="输入用户名或手机号"
          maxlength="20"
          @update:model-value="updateForm('identifier', $event)"
        />
      </el-form-item>
      <el-form-item v-if="mode === 'register'" label="用户名">
        <el-input
          :model-value="form.username"
          size="large"
          placeholder="3-20 位用户名"
          maxlength="20"
          @update:model-value="updateForm('username', $event)"
        />
      </el-form-item>
      <el-form-item v-if="mode !== 'login'" label="手机号">
        <el-input
          :model-value="form.phone"
          size="large"
          placeholder="请输入 11 位手机号"
          maxlength="11"
          @update:model-value="updateForm('phone', $event)"
        />
      </el-form-item>
      <el-form-item v-if="mode !== 'login'" label="验证码">
        <div class="code-row">
          <el-input
            :model-value="form.code"
            size="large"
            placeholder="输入 4 位验证码"
            maxlength="4"
            @update:model-value="updateForm('code', $event)"
          />
          <el-button size="large" :disabled="codeCountdown > 0" :loading="sendingCode" @click="emit('send-code')">
            {{ codeButtonText }}
          </el-button>
          <el-tag v-if="visibleCode" type="success" effect="light">{{ visibleCode }}</el-tag>
        </div>
      </el-form-item>
      <el-form-item label="密码">
        <el-input
          :model-value="form.password"
          size="large"
          type="password"
          show-password
          :placeholder="mode === 'reset' ? '输入新密码' : '至少 6 位密码'"
          maxlength="32"
          @update:model-value="updateForm('password', $event)"
        />
      </el-form-item>
      <el-form-item v-if="mode !== 'login'" label="确认密码">
        <el-input
          :model-value="form.confirmPassword"
          size="large"
          type="password"
          show-password
          placeholder="再次输入密码"
          maxlength="32"
          @update:model-value="updateForm('confirmPassword', $event)"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="auth-footer">
        <el-button v-if="mode === 'login'" text class="auth-link" @click="emit('switch-mode', 'reset')">忘记密码</el-button>
        <span v-else></span>
        <div class="auth-actions">
          <el-button size="large" @click="emit('switch-mode', mode === 'login' ? 'register' : 'login')">
            {{ mode === 'login' ? '去注册' : '去登录' }}
          </el-button>
          <el-button size="large" type="primary" :loading="authLoading" @click="emit('submit')">
            {{ submitText }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  },
  mode: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    required: true
  },
  tip: {
    type: String,
    required: true
  },
  submitText: {
    type: String,
    required: true
  },
  codeButtonText: {
    type: String,
    required: true
  },
  form: {
    type: Object,
    required: true
  },
  authLoading: {
    type: Boolean,
    required: true
  },
  sendingCode: {
    type: Boolean,
    required: true
  },
  codeCountdown: {
    type: Number,
    required: true
  },
  visibleCode: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'update:modelValue',
  'update:form',
  'switch-mode',
  'send-code',
  'submit'
])

function updateForm(field, value) {
  emit('update:form', {
    ...props.form,
    [field]: value
  })
}
</script>

<style scoped>
.settings-form {
  margin-top: 16px;
}

.auth-dialog :deep(.el-dialog) {
  border-radius: 18px;
  overflow: hidden;
}

.auth-dialog :deep(.el-dialog__header) {
  margin: 0;
  padding: 24px 26px 10px;
}

.auth-dialog :deep(.el-dialog__body) {
  padding: 10px 26px 8px;
}

.auth-dialog :deep(.el-dialog__footer) {
  padding: 10px 26px 24px;
}

.auth-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.auth-logo {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(135deg, #111827 0%, #2563eb 100%);
  color: #ffffff;
  font-size: 14px;
  font-weight: 900;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.22);
}

.auth-header h2 {
  margin: 0;
  color: #172033;
  font-size: 22px;
  line-height: 1.2;
}

.auth-header p {
  margin: 6px 0 0;
  color: #7a8699;
  font-size: 13px;
}

.auth-tip {
  border: 0;
  border-radius: 14px;
  background: #f5f7fb;
}

.auth-tip :deep(.el-alert__title) {
  color: #7c8798;
  font-size: 13px;
  line-height: 1.7;
}

.auth-form {
  display: grid;
  gap: 2px;
}

.auth-form :deep(.el-form-item__label) {
  color: #344258;
  font-size: 13px;
  font-weight: 800;
}

.auth-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px #d8e1ee inset;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #409eff inset, 0 0 0 4px rgba(64, 158, 255, 0.1);
}

.code-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 126px auto;
  gap: 10px;
  align-items: center;
}

.code-row .el-tag {
  height: 38px;
  padding: 0 12px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 1px;
}

.auth-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.auth-link {
  color: #64748b;
  font-weight: 700;
}

.auth-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.auth-actions .el-button {
  min-width: 88px;
  border-radius: 12px;
  font-weight: 800;
}
</style>
