<template>
  <div class="app-container submission-page">
    <div class="page-heading">
      <div><h2>成果提交</h2><p>{{ contextText }}</p></div>
      <el-tag v-if="form.status != null" :type="statusType(form.status)">{{ statusName(form.status) }}</el-tag>
    </div>
    <el-alert v-if="!editable" title="该成果已提交或归档，当前为只读状态" type="info" :closable="false" show-icon class="mb16" />

    <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="submission-form">
      <el-form-item label="成果名称" prop="submissionName"><el-input v-model="form.submissionName" :disabled="!editable" maxlength="200" /></el-form-item>
      <el-form-item label="成果说明"><el-input v-model="form.submissionDesc" :disabled="!editable" type="textarea" :rows="6" maxlength="2000" show-word-limit /></el-form-item>
      <el-form-item v-if="attachments.length" label="已有附件">
        <div v-for="item in attachments" :key="item.attachmentId" class="attachment-row">
          <el-button type="text" icon="el-icon-document" @click="download(item)">{{ item.originalName }}</el-button>
          <el-button v-if="editable" type="text" class="delete-link" @click="removeAttachment(item)">删除</el-button>
        </div>
      </el-form-item>
      <el-form-item v-if="editable" label="新增附件"><file-upload v-model="newFileUrls" :limit="8" :file-size="20" :file-type="fileTypes" /></el-form-item>
    </el-form>

    <div class="action-bar">
      <el-button @click="$router.back()">返回</el-button>
      <el-button v-if="editable" :loading="saving" @click="save(false)">保存草稿</el-button>
      <el-button v-if="editable" type="primary" :loading="saving" @click="save(true)">{{ form.status === '2' ? '重新提交' : '提交审核' }}</el-button>
    </div>
  </div>
</template>

<script>
import { getDeliverable } from '@/api/research/deliverable'
import { getSubmission, addSubmission, updateSubmission, submitSubmission, resubmitSubmission, listSubmissionAttachments, addSubmissionAttachment, deleteSubmissionAttachment, downloadSubmissionAttachment } from '@/api/research/submission'

export default {
  name: 'ResearchSubmission',
  data() {
    return {
      saving: false, deliverable: {}, form: {}, attachments: [], newFileUrls: '',
      fileTypes: ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf', 'txt', 'zip', 'rar', 'png', 'jpg', 'jpeg'],
      rules: { submissionName: [{ required: true, message: '请输入成果名称', trigger: 'blur' }] }
    }
  },
  computed: {
    editable() { return this.form.status == null || this.form.status === '0' || this.form.status === '2' },
    contextText() {
      const parts = [this.form.groupName, this.form.taskName, this.form.deliverableName || this.deliverable.deliverableName]
      return parts.filter(Boolean).join(' · ') || '填写成果资料并上传附件'
    }
  },
  created() {
    const submissionId = Number(this.$route.query.submissionId || 0)
    const deliverableId = Number(this.$route.query.deliverableId || 0)
    if (submissionId) {
      Promise.all([getSubmission(submissionId), listSubmissionAttachments(submissionId)]).then(([detail, files]) => {
        this.form = detail.data || {}
        this.attachments = files.data || []
      })
    } else if (deliverableId) {
      getDeliverable(deliverableId).then(res => {
        this.deliverable = res.data || {}
        this.form = { deliverableId, submissionName: '', submissionDesc: '', status: null }
      })
    } else {
      this.$modal.msgError('缺少交付成果参数')
    }
  },
  methods: {
    statusName(status) { return { '0': '草稿', '1': '待审核', '2': '已退回', '3': '已归档' }[status] || status },
    statusType(status) { return { '0': 'info', '1': 'warning', '2': 'danger', '3': 'success' }[status] || '' },
    save(shouldSubmit) {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.saving = true
        const request = this.form.submissionId ? updateSubmission : addSubmission
        request(this.form).then(res => {
          if (!this.form.submissionId) this.$set(this.form, 'submissionId', res.data.submissionId)
          return this.saveNewAttachments()
        }).then(() => {
          if (!shouldSubmit) return null
          return this.form.status === '2' ? resubmitSubmission(this.form.submissionId) : submitSubmission(this.form.submissionId)
        }).then(() => {
          this.$modal.msgSuccess(shouldSubmit ? '提交成功' : '草稿已保存')
          return this.reload()
        }).finally(() => { this.saving = false })
      })
    },
    saveNewAttachments() {
      const urls = (this.newFileUrls || '').split(',').filter(Boolean)
      return Promise.all(urls.map(url => {
        const name = decodeURIComponent(url.substring(url.lastIndexOf('/') + 1)) || 'attachment'
        const type = name.indexOf('.') >= 0 ? name.substring(name.lastIndexOf('.') + 1) : ''
        return addSubmissionAttachment(this.form.submissionId, { fileName: name, originalName: name, fileUrl: url, fileType: type })
      })).then(() => { this.newFileUrls = '' })
    },
    reload() {
      return Promise.all([getSubmission(this.form.submissionId), listSubmissionAttachments(this.form.submissionId)]).then(([detail, files]) => {
        this.form = detail.data || {}
        this.attachments = files.data || []
      })
    },
    removeAttachment(item) { this.$modal.confirm('确认删除附件“' + item.originalName + '”吗？').then(() => deleteSubmissionAttachment(item.attachmentId)).then(() => this.reload()).catch(() => {}) },
    download(item) { downloadSubmissionAttachment(item.attachmentId).then(blob => { const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = item.originalName || 'attachment'; link.click(); URL.revokeObjectURL(link.href) }) }
  }
}
</script>

<style scoped>
.submission-page { max-width: 920px; }
.page-heading { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; border-bottom: 1px solid #ebeef5; }
.page-heading h2 { margin: 0 0 8px; font-size: 22px; }
.page-heading p { margin: 0 0 20px; color: #909399; }
.submission-form { max-width: 820px; }
.attachment-row { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #f0f2f5; }
.delete-link { color: #f56c6c; }
.action-bar { margin: 24px 0 0 100px; }
.mb16 { margin-bottom: 16px; }
</style>
