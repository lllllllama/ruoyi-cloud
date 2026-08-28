<template>
  <div class="app-container">
    <el-form :inline="true" :model="filters" label-width="72px">
      <el-form-item label="课题"><el-input v-model="filters.group" clearable size="small" placeholder="筛选课题" /></el-form-item>
      <el-form-item label="任务/成果"><el-input v-model="filters.keyword" clearable size="small" placeholder="筛选任务或成果" /></el-form-item>
      <el-form-item label="时间状态"><el-select v-model="filters.timeStatus" clearable size="small" placeholder="全部"><el-option label="正常" value="NORMAL" /><el-option label="临近截止" value="NEAR_DUE" /><el-option label="已逾期" value="OVERDUE" /></el-select></el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="filteredRows">
      <el-table-column label="课题" prop="groupName" min-width="150" show-overflow-tooltip />
      <el-table-column label="任务" prop="taskName" min-width="180" show-overflow-tooltip />
      <el-table-column label="交付成果" prop="deliverableName" min-width="190" show-overflow-tooltip />
      <el-table-column label="归档进度" width="100" align="center"><template slot-scope="scope">{{ scope.row.archivedNum || 0 }} / {{ scope.row.requiredNum }}</template></el-table-column>
      <el-table-column label="待审核" prop="pendingNum" width="80" align="center" />
      <el-table-column label="截止时间" prop="deadline" width="110" />
      <el-table-column label="业务状态" width="90"><template slot-scope="scope"><el-tag :type="businessType(scope.row.status)">{{ businessName(scope.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="时间状态" width="100"><template slot-scope="scope"><el-tag effect="plain" :type="timeType(scope.row.timeStatus)">{{ timeName(scope.row.timeStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="190" fixed="right"><template slot-scope="scope"><div class="business-table-actions"><el-button v-if="scope.row.canSubmit" v-hasPermi="['task:submission:add']" type="text" icon="el-icon-upload2" @click="submit(scope.row)">提交成果</el-button><el-button v-hasPermi="['task:submission:add']" type="text" icon="el-icon-document" @click="openMine(scope.row)">我的提交</el-button></div></template></el-table-column>
    </el-table>

    <el-dialog :title="(mineDeliverable.deliverableName || '成果') + ' · 我的提交'" :visible.sync="mineOpen" width="860px" append-to-body>
      <el-alert v-if="unfinishedWarning" title="当前成果已有未完成提交，可继续处理已有记录，也可以仍然新建成果。" type="warning" :closable="false" show-icon class="mb12" />
      <el-table v-loading="mineLoading" :data="mineRows" empty-text="当前成果暂无我的提交">
        <el-table-column label="成果名称" prop="submissionName" min-width="190" show-overflow-tooltip />
        <el-table-column label="状态" width="90"><template slot-scope="scope"><el-tag :type="submissionStatusType(scope.row.status)">{{ submissionStatusName(scope.row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="提交时间" width="165"><template slot-scope="scope">{{ formatDateTime(scope.row.submitTime) }}</template></el-table-column>
        <el-table-column label="更新时间" width="165"><template slot-scope="scope">{{ formatDateTime(scope.row.updateTime || scope.row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right"><template slot-scope="scope"><div class="business-table-actions"><el-button type="text" @click="openSubmission(scope.row)">{{ submissionActionName(scope.row.status) }}</el-button><el-button v-if="scope.row.status === '0' || scope.row.status === '2'" type="text" class="delete-link" @click="removeSubmission(scope.row)">删除</el-button></div></template></el-table-column>
      </el-table>
      <div slot="footer"><el-button @click="mineOpen = false">关闭</el-button><el-button v-if="unfinishedWarning" type="primary" @click="createSubmission(mineDeliverable)">仍然新建成果</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listMyTasks } from '@/api/research/task'
import { listMySubmissions, deleteSubmission } from '@/api/research/submission'

export default {
  name: 'MyResearchTask',
  data() {
    return { loading: false, rows: [], filters: { group: '', keyword: '', timeStatus: '' }, mineOpen: false, mineLoading: false, mineRows: [], mineDeliverable: {}, unfinishedWarning: false }
  },
  computed: {
    filteredRows() {
      const group = this.filters.group.trim().toLowerCase()
      const keyword = this.filters.keyword.trim().toLowerCase()
      return this.rows.filter(item => {
        const matchesGroup = !group || (item.groupName || '').toLowerCase().indexOf(group) >= 0
        const text = ((item.taskName || '') + ' ' + (item.deliverableName || '')).toLowerCase()
        return matchesGroup && (!keyword || text.indexOf(keyword) >= 0) && (!this.filters.timeStatus || item.timeStatus === this.filters.timeStatus)
      })
    }
  },
  created() { this.load() },
  methods: {
    load() { this.loading = true; listMyTasks().then(res => { this.rows = res.data || [] }).finally(() => { this.loading = false }) },
    businessName(status) { return { '0': '未开始', '1': '进行中', '2': '已完成' }[status] || status },
    businessType(status) { return { '0': 'info', '1': 'primary', '2': 'success' }[status] || '' },
    timeName(status) { return { NORMAL: '正常', NEAR_DUE: '临近截止', OVERDUE: '已逾期' }[status] || status },
    timeType(status) { return { NORMAL: 'info', NEAR_DUE: 'warning', OVERDUE: 'danger' }[status] || '' },
    submissionStatusName(status) { return { '0': '草稿', '1': '待审核', '2': '已退回', '3': '已归档' }[status] || status },
    submissionStatusType(status) { return { '0': 'info', '1': 'warning', '2': 'danger', '3': 'success' }[status] || '' },
    submissionActionName(status) { return status === '0' ? '继续编辑' : (status === '2' ? '继续修改' : '查看') },
    formatDateTime(value) { return value ? this.parseTime(value) : '—' },
    loadMine(row, warnIfUnfinished) {
      this.mineDeliverable = row
      this.mineLoading = true
      return listMySubmissions(row.deliverableId).then(res => {
        this.mineRows = res.data || []
        this.unfinishedWarning = !!warnIfUnfinished && this.mineRows.some(item => ['0', '1', '2'].indexOf(item.status) >= 0)
        this.mineOpen = true
        return this.mineRows
      }).finally(() => { this.mineLoading = false })
    },
    submit(row) {
      this.loadMine(row, true).then(rows => {
        if (!rows.some(item => ['0', '1', '2'].indexOf(item.status) >= 0)) this.createSubmission(row)
      })
    },
    openMine(row) { this.loadMine(row, false) },
    createSubmission(row) { this.mineOpen = false; this.$router.push({ path: '/research/submission', query: { deliverableId: row.deliverableId } }) },
    openSubmission(row) { this.mineOpen = false; this.$router.push({ path: '/research/submission', query: { submissionId: row.submissionId } }) },
    removeSubmission(row) {
      this.$modal.confirm('确认删除成果“' + row.submissionName + '”吗？').then(() => deleteSubmission(row.submissionId)).then(() => {
        this.$modal.msgSuccess('删除成功')
        return this.loadMine(this.mineDeliverable, false)
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.delete-link { color: #f56c6c; }
</style>
