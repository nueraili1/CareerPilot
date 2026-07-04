import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteRecord, loadRecordDetail, loadRecords } from '../api/client'

export function useRecords({ resumeName, resumeText, jobDescription, restoreTargetContext, analysis, questions, interviewFeedback, rightTab }) {
  const records = ref([])
  const recordsVisible = ref(false)
  const recordLoadingId = ref(null)
  const recordDeletingId = ref(null)

  async function refreshRecords() {
    const response = await loadRecords()
    records.value = response.data
  }

  async function restoreRecord(id) {
    recordLoadingId.value = id
    try {
      const response = await loadRecordDetail(id)
      const record = response.data
      resumeName.value = record.resumeName || ''
      resumeText.value = record.resumeText || ''
      if (typeof restoreTargetContext === 'function') {
        restoreTargetContext(record.targetContext, record.jobDescription || '')
      } else {
        jobDescription.value = record.jobDescription || ''
      }
      analysis.value = record.result
      questions.value = record.result?.interviewQuestions || []
      interviewFeedback.value = ''
      recordsVisible.value = false
      rightTab.value = questions.value.length ? 'interview' : rightTab.value
      ElMessage.success('已恢复历史分析')
    } catch (error) {
      ElMessage.error(error.message || '加载历史记录失败')
    } finally {
      recordLoadingId.value = null
    }
  }

  async function removeRecord(record) {
    try {
      await ElMessageBox.confirm(
        `确定删除「${record.resumeName}」这条分析记录吗？`,
        '删除记录',
        {
          confirmButtonText: '删除',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch (error) {
      return
    }

    recordDeletingId.value = record.id
    try {
      await deleteRecord(record.id)
      records.value = records.value.filter((item) => item.id !== record.id)
      ElMessage.success('记录已删除')
    } catch (error) {
      ElMessage.error(error.message || '删除记录失败')
    } finally {
      recordDeletingId.value = null
    }
  }

  return {
    records,
    recordsVisible,
    recordLoadingId,
    recordDeletingId,
    refreshRecords,
    restoreRecord,
    removeRecord
  }
}
