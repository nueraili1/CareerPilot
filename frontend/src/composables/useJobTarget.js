import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { resolveJobTarget } from '../api/client'

export function useJobTarget({ jobDescription, runtimeAiConfig }) {
  const targetCompanyName = ref('')
  const targetRoleTitle = ref('')
  const targetImageName = ref('')
  const targetImageDataUrl = ref('')
  const targetImageExtractedText = ref('')
  const targetSearchQuery = ref('')
  const targetEnrichmentSummary = ref('')
  const targetSearchHint = ref('')
  const targetSearchResults = ref([])
  const targetResolving = ref(false)

  function hasTargetInput() {
    return Boolean(
      jobDescription.value.trim()
        || targetCompanyName.value.trim()
        || targetRoleTitle.value.trim()
        || targetImageDataUrl.value
        || targetImageExtractedText.value.trim()
        || targetEnrichmentSummary.value.trim()
    )
  }

  async function handleTargetImageChange(uploadFile) {
    if (!uploadFile?.raw) {
      return
    }

    targetImageName.value = uploadFile.name || uploadFile.raw.name || ''
    targetImageDataUrl.value = await readFileAsDataUrl(uploadFile.raw)
    targetImageExtractedText.value = ''
    targetSearchQuery.value = ''
    targetEnrichmentSummary.value = ''
    targetSearchHint.value = ''
    targetSearchResults.value = []
    ElMessage.success('\u5c97\u4f4d\u622a\u56fe\u5df2\u52a0\u8f7d\uff0c\u53ef\u70b9\u51fb\u201c\u8bc6\u522b\u5e76\u8054\u7f51\u8865\u5168\u201d')
  }

  function clearTargetImage() {
    targetImageName.value = ''
    targetImageDataUrl.value = ''
    targetImageExtractedText.value = ''
  }

  async function resolveTargetContext({ silent = false } = {}) {
    if (!hasTargetInput()) {
      if (!silent) {
        ElMessage.warning('\u8bf7\u5148\u586b\u5199\u5c97\u4f4d\u63cf\u8ff0\u3001\u516c\u53f8\u5c97\u4f4d\u4fe1\u606f\u6216\u4e0a\u4f20\u5c97\u4f4d\u622a\u56fe')
      }
      return buildRequestTargetContext()
    }

    targetResolving.value = true
    try {
      const response = await resolveJobTarget({
        companyName: targetCompanyName.value.trim(),
        roleTitle: targetRoleTitle.value.trim(),
        description: jobDescription.value.trim(),
        imageFileName: targetImageName.value.trim(),
        imageDataUrl: targetImageDataUrl.value,
        aiConfig: runtimeAiConfig()
      })
      applyResolvedTargetContext(response.data)
      targetImageDataUrl.value = ''
      if (!silent) {
        if (targetEnrichmentSummary.value.trim()) {
          ElMessage.success('\u5df2\u8bc6\u522b\u5c97\u4f4d\u4fe1\u606f\u5e76\u8865\u5145\u9ad8\u76f8\u5173\u62db\u8058\u9700\u6c42')
        } else if (targetSearchHint.value.trim()) {
          ElMessage.warning(targetSearchHint.value.trim())
        } else if (targetImageExtractedText.value.trim() || targetCompanyName.value.trim() || targetRoleTitle.value.trim()) {
          ElMessage.success('\u5df2\u8bc6\u522b\u5c97\u4f4d\u4fe1\u606f')
        } else {
          ElMessage.warning('\u6ca1\u6709\u8bc6\u522b\u51fa\u660e\u786e\u7684\u516c\u53f8\u548c\u5c97\u4f4d\uff0c\u53ef\u624b\u52a8\u8865\u5145\u540e\u518d\u8bd5')
        }
      }
      return buildRequestTargetContext()
    } catch (error) {
      if (!silent) {
        ElMessage.error(error.message || '\u5c97\u4f4d\u4fe1\u606f\u8865\u5168\u5931\u8d25')
      }
      throw error
    } finally {
      targetResolving.value = false
    }
  }

  function applyResolvedTargetContext(context) {
    targetCompanyName.value = context?.companyName || targetCompanyName.value
    targetRoleTitle.value = context?.roleTitle || targetRoleTitle.value
    targetImageExtractedText.value = context?.imageExtractedText || ''
    targetSearchQuery.value = context?.searchQuery || ''
    targetEnrichmentSummary.value = context?.enrichmentSummary || ''
    targetSearchHint.value = context?.searchHint || ''
    targetSearchResults.value = Array.isArray(context?.webSearchResults) ? context.webSearchResults : []
    if (context?.description && !jobDescription.value.trim()) {
      jobDescription.value = context.description
    }
  }

  function buildRequestTargetContext() {
    return {
      companyName: targetCompanyName.value.trim(),
      roleTitle: targetRoleTitle.value.trim(),
      description: jobDescription.value.trim(),
      imageFileName: targetImageName.value.trim(),
      imageExtractedText: targetImageExtractedText.value.trim(),
      searchQuery: targetSearchQuery.value.trim(),
      enrichmentSummary: targetEnrichmentSummary.value.trim(),
      searchHint: targetSearchHint.value.trim(),
      searchPerformed: Boolean(targetEnrichmentSummary.value.trim() || targetSearchResults.value.length),
      webSearchResults: targetSearchResults.value.slice(0, 6)
    }
  }

  function restoreTargetContext(context, fallbackJobDescription = '') {
    if (!context) {
      targetCompanyName.value = ''
      targetRoleTitle.value = ''
      targetImageName.value = ''
      targetImageDataUrl.value = ''
      targetImageExtractedText.value = ''
      targetSearchQuery.value = ''
      targetEnrichmentSummary.value = ''
      targetSearchHint.value = ''
      targetSearchResults.value = []
      jobDescription.value = fallbackJobDescription || ''
      return
    }

    targetCompanyName.value = context.companyName || ''
    targetRoleTitle.value = context.roleTitle || ''
    targetImageName.value = context.imageFileName || ''
    targetImageDataUrl.value = ''
    targetImageExtractedText.value = context.imageExtractedText || ''
    targetSearchQuery.value = context.searchQuery || ''
    targetEnrichmentSummary.value = context.enrichmentSummary || ''
    targetSearchHint.value = context.searchHint || ''
    targetSearchResults.value = Array.isArray(context.webSearchResults) ? context.webSearchResults : []
    jobDescription.value = context.description || fallbackJobDescription || ''
  }

  return {
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
  }
}

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('\u8bfb\u53d6\u5c97\u4f4d\u622a\u56fe\u5931\u8d25'))
    reader.readAsDataURL(file)
  })
}
