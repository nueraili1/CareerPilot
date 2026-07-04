import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  AUTH_TOKEN_KEY,
  loadCurrentUser,
  login,
  register,
  resetPassword,
  sendAuthCode
} from '../api/client'

export function useAuth({ onAuthSuccess, onLogout }) {
  const authVisible = ref(false)
  const authMode = ref('login')
  const authLoading = ref(false)
  const sendingCode = ref(false)
  const codeCountdown = ref(0)
  const visibleCode = ref('')
  const currentUser = ref(null)
  const authForm = ref(createEmptyAuthForm())

  let codeTimer = null

  const authTitle = computed(() => {
    if (authMode.value === 'login') return '登录 CareerPilot'
    if (authMode.value === 'register') return '注册 CareerPilot'
    return '忘记密码'
  })
  const authSubtitle = computed(() => {
    if (authMode.value === 'login') return '继续管理你的简历分析记录'
    if (authMode.value === 'register') return '创建账号，保存专属求职进度'
    return '用手机号验证码重置登录密码'
  })
  const authTip = computed(() => {
    if (authMode.value === 'login') {
      return '登录后，分析记录会保存到你的账号下；不登录也可以继续匿名体验。'
    }
    if (authMode.value === 'register') {
      return '注册需要手机号验证码。当前为演示模式，验证码会直接显示在发送按钮旁边。'
    }
    return '忘记密码时，使用手机号验证码重置密码。验证码 5 分钟内有效。'
  })
  const authSubmitText = computed(() => {
    if (authMode.value === 'login') return '登录'
    if (authMode.value === 'register') return '注册并登录'
    return '修改密码'
  })
  const codeButtonText = computed(() => {
    if (codeCountdown.value > 0) {
      return `已发送 ${codeCountdown.value}s`
    }
    return visibleCode.value ? '重新发送' : '发送验证码'
  })

  function openAuth(mode) {
    switchAuthMode(mode)
    authVisible.value = true
  }

  function switchAuthMode(mode) {
    authMode.value = mode
    authForm.value = createEmptyAuthForm()
    visibleCode.value = ''
    stopCodeCountdown()
  }

  async function sendCode() {
    if (!/^1\d{10}$/.test(authForm.value.phone.trim())) {
      ElMessage.warning('请先填写正确的手机号')
      return
    }

    sendingCode.value = true
    try {
      const response = await sendAuthCode({
        phone: authForm.value.phone.trim(),
        purpose: authMode.value === 'register' ? 'REGISTER' : 'RESET_PASSWORD'
      })
      visibleCode.value = response.data.code
      authForm.value.code = response.data.code
      startCodeCountdown(response.data.resendSeconds || 10)
      ElMessage.success('验证码已发送')
    } catch (error) {
      ElMessage.error(error.message || '验证码发送失败')
    } finally {
      sendingCode.value = false
    }
  }

  async function submitAuth() {
    if (!authForm.value.password.trim()) {
      ElMessage.warning('请填写密码')
      return
    }
    if (authMode.value === 'login' && !authForm.value.identifier.trim()) {
      ElMessage.warning('请填写用户名或手机号')
      return
    }
    if (authMode.value !== 'login' && !authForm.value.code.trim()) {
      ElMessage.warning('请填写验证码')
      return
    }
    if (authMode.value !== 'login' && authForm.value.password !== authForm.value.confirmPassword) {
      ElMessage.warning('两次输入的密码不一致')
      return
    }

    authLoading.value = true
    try {
      if (authMode.value === 'reset') {
        await resetPassword({
          phone: authForm.value.phone,
          code: authForm.value.code,
          password: authForm.value.password,
          confirmPassword: authForm.value.confirmPassword
        })
        ElMessage.success('密码已修改，请重新登录')
        switchAuthMode('login')
        return
      }

      const response = authMode.value === 'login'
        ? await login({
          identifier: authForm.value.identifier,
          password: authForm.value.password
        })
        : await register({
          username: authForm.value.username,
          phone: authForm.value.phone,
          code: authForm.value.code,
          password: authForm.value.password,
          confirmPassword: authForm.value.confirmPassword
        })
      localStorage.setItem(AUTH_TOKEN_KEY, response.data.token)
      currentUser.value = {
        userId: response.data.userId,
        username: response.data.username,
        phone: response.data.phone
      }
      authVisible.value = false
      await onAuthSuccess?.()
      ElMessage.success(authMode.value === 'login' ? '登录成功' : '注册成功，已自动登录')
    } catch (error) {
      ElMessage.error(error.message || '认证失败')
    } finally {
      authLoading.value = false
    }
  }

  async function refreshCurrentUser() {
    if (!localStorage.getItem(AUTH_TOKEN_KEY)) {
      currentUser.value = null
      return
    }

    try {
      const response = await loadCurrentUser()
      currentUser.value = response.data
      await onAuthSuccess?.()
    } catch (error) {
      localStorage.removeItem(AUTH_TOKEN_KEY)
      currentUser.value = null
    }
  }

  async function logout() {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    currentUser.value = null
    stopCodeCountdown()
    await onLogout?.()
    ElMessage.success('已退出登录')
  }

  function startCodeCountdown(seconds) {
    stopCodeCountdown()
    codeCountdown.value = seconds
    codeTimer = window.setInterval(() => {
      codeCountdown.value -= 1
      if (codeCountdown.value <= 0) {
        stopCodeCountdown()
      }
    }, 1000)
  }

  function stopCodeCountdown() {
    if (codeTimer) {
      window.clearInterval(codeTimer)
      codeTimer = null
    }
    codeCountdown.value = 0
  }

  return {
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
  }
}

function createEmptyAuthForm() {
  return {
    identifier: '',
    username: '',
    phone: '',
    code: '',
    password: '',
    confirmPassword: ''
  }
}
