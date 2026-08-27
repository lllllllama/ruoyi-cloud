<template>
  <div class="app-container">
    <el-form :inline="true" :model="query" label-width="72px">
      <el-form-item label="课题"><el-select v-model="query.groupId" clearable filterable size="small" placeholder="全部课题"><el-option v-for="group in groups" :key="group.groupId" :label="group.groupName" :value="group.groupId" /></el-select></el-form-item>
      <el-form-item label="成果名称"><el-input v-model="query.submissionName" clearable size="small" @keyup.enter.native="search" /></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="search">搜索</el-button></el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="rows">
      <el-table-column label="课题" prop="groupName" min-width="140" show-overflow-tooltip />
      <el-table-column label="任务" prop="taskName" min-width="150" show-overflow-tooltip />
      <el-table-column label="交付成果" prop="deliverableName" min-width="160" show-overflow-tooltip />
      <el-table-column label="提交名称" prop="submissionName" min-width="170" show-overflow-tooltip />
      <el-table-column label="提交人" prop="submitUserName" width="110"><template slot-scope="scope">{{ scope.row.submitUserName || scope.row.submitUserId }}</template></el-table-column>
      <el-table-column label="归档人" prop="archiveUserName" width="110"><template slot-scope="scope">{{ scope.row.archiveUserName || scope.row.archiveUserId }}</template></el-table-column>
      <el-table-column label="归档时间" prop="archiveTime" width="160" />
      <el-table-column label="操作" width="150" fixed="right"><template slot-scope="scope"><el-button type="text" icon="el-icon-view" @click="view(scope.row)">资料</el-button><el-button v-hasPermi="['task:submission:cancelAudit']" type="text" icon="el-icon-refresh-left" @click="cancel(scope.row)">取消审核</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />

    <el-dialog title="归档资料" :visible.sync="detailOpen" width="720px" append-to-body>
      <el-descriptions v-if="detail.submissionId" :column="2" border>
        <el-descriptions-item label="课题">{{ detail.groupName }}</el-descriptions-item><el-descriptions-item label="任务">{{ detail.taskName }}</el-descriptions-item>
        <el-descriptions-item label="交付成果">{{ detail.deliverableName }}</el-descriptions-item><el-descriptions-item label="提交人">{{ detail.submitUserName || detail.submitUserId }}</el-descriptions-item>
        <el-descriptions-item label="提交名称" :span="2">{{ detail.submissionName }}</el-descriptions-item><el-descriptions-item label="成果说明" :span="2">{{ detail.submissionDesc || '—' }}</el-descriptions-item>
      </el-descriptions>
      <h4>附件</h4><div v-if="attachments.length"><el-button v-for="file in attachments" :key="file.attachmentId" type="text" icon="el-icon-download" @click="download(file)">{{ file.originalName }}</el-button></div><el-empty v-else :image-size="50" description="无附件" />
      <h4>审核轨迹</h4><el-timeline v-if="audits.length"><el-timeline-item v-for="item in audits" :key="item.auditId" :timestamp="item.auditTime" placement="top">{{ actionName(item.action) }}<span v-if="item.auditOpinion">：{{ item.auditOpinion }}</span></el-timeline-item></el-timeline><el-empty v-else :image-size="50" description="暂无审核记录" />
    </el-dialog>
  </div>
</template>

<script>
import { accessibleGroups } from '@/api/research/group'
import { listSubmissions, getSubmission, listSubmissionAttachments, listSubmissionAudits, cancelSubmissionApproval, downloadSubmissionAttachment } from '@/api/research/submission'

export default {
  name: 'ResearchArchive',
  data() { return { loading: false, rows: [], total: 0, groups: [], detailOpen: false, detail: {}, attachments: [], audits: [], query: { pageNum: 1, pageSize: 10, groupId: undefined, submissionName: undefined, status: '3' } } },
  created() { accessibleGroups().then(res => { this.groups = res.data || [] }); this.load() },
  methods: {
    load() { this.loading = true; listSubmissions(this.query).then(res => { this.rows = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    search() { this.query.pageNum = 1; this.load() },
    view(row) { Promise.all([getSubmission(row.submissionId), listSubmissionAttachments(row.submissionId), listSubmissionAudits(row.submissionId)]).then(([detail, files, audits]) => { this.detail = detail.data || {}; this.attachments = files.data || []; this.audits = audits.data || []; this.detailOpen = true }) },
    cancel(row) { this.$prompt('可填写取消审核原因', '取消审核', { inputType: 'textarea' }).then(({ value }) => cancelSubmissionApproval(row.submissionId, value)).then(() => { this.$modal.msgSuccess('已取消审核'); this.load() }).catch(() => {}) },
    actionName(action) { return { SUBMIT: '提交', APPROVE: '审核通过', REJECT: '退回', RESUBMIT: '重新提交', CANCEL_APPROVE: '取消审核' }[action] || action },
    download(file) { downloadSubmissionAttachment(file.attachmentId).then(blob => { const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = file.originalName || 'attachment'; link.click(); URL.revokeObjectURL(link.href) }) }
  }
}
</script>
