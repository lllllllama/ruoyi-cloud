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
      <el-table-column label="操作" width="90" fixed="right"><template slot-scope="scope"><el-button v-if="scope.row.canSubmit" v-hasPermi="['task:submission:add']" type="text" icon="el-icon-upload2" @click="submit(scope.row)">提交</el-button></template></el-table-column>
    </el-table>
  </div>
</template>

<script>
import { listMyTasks } from '@/api/research/task'

export default {
  name: 'MyResearchTask',
  data() {
    return { loading: false, rows: [], filters: { group: '', keyword: '', timeStatus: '' } }
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
    submit(row) { this.$router.push({ path: '/research/submission', query: { deliverableId: row.deliverableId } }) }
  }
}
</script>
