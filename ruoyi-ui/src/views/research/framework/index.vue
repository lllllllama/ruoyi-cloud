<template>
  <div class="app-container">
    <el-form v-show="showSearch" ref="queryForm" :model="query" :inline="true" label-width="72px">
      <el-form-item label="课题" prop="groupId"><el-select v-model="query.groupId" clearable filterable size="small" placeholder="全部课题"><el-option v-for="group in groups" :key="group.groupId" :label="group.groupName" :value="group.groupId" /></el-select></el-form-item>
      <el-form-item label="年度" prop="year"><el-date-picker v-model="query.year" type="year" value-format="yyyy" placeholder="全部年度" size="small" /></el-form-item>
      <el-form-item label="框架名称" prop="frameworkName"><el-input v-model="query.frameworkName" clearable size="small" placeholder="请输入框架名称" @keyup.enter.native="search" /></el-form-item>
      <el-form-item><el-button type="primary" icon="el-icon-search" size="mini" @click="search">搜索</el-button><el-button icon="el-icon-refresh" size="mini" @click="resetSearch">重置</el-button></el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button v-hasPermi="['task:framework:add']" type="primary" plain icon="el-icon-plus" size="mini" @click="openForm()">新增年度任务</el-button></el-col>
      <right-toolbar :show-search.sync="showSearch" @queryTable="load" />
    </el-row>

    <el-table v-loading="loading" :data="rows">
      <el-table-column label="年度" prop="year" width="90" align="center" />
      <el-table-column label="课题" prop="groupName" min-width="170" show-overflow-tooltip />
      <el-table-column label="框架名称" prop="frameworkName" min-width="220" show-overflow-tooltip />
      <el-table-column label="牵头单位" prop="leadDeptName" min-width="150"><template slot-scope="scope">{{ scope.row.leadDeptName || deptName(scope.row.leadDeptId) }}</template></el-table-column>
      <el-table-column label="协同单位" min-width="220" show-overflow-tooltip><template slot-scope="scope">{{ unitNames(scope.row.units) || '—' }}</template></el-table-column>
      <el-table-column label="状态" width="80" align="center"><template slot-scope="scope"><el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="150" fixed="right"><template slot-scope="scope"><el-button v-hasPermi="['task:framework:add']" type="text" icon="el-icon-edit" @click="openForm(scope.row)">编辑</el-button><el-button v-hasPermi="['task:framework:add']" type="text" icon="el-icon-delete" @click="remove(scope.row)">删除</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load" />

    <el-dialog :title="form.frameworkId ? '编辑年度任务' : '新增年度任务'" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="96px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="课题" prop="groupId"><el-select v-model="form.groupId" :disabled="!!form.frameworkId" filterable style="width:100%" @change="groupChanged"><el-option v-for="group in groups" :key="group.groupId" :label="group.groupName" :value="group.groupId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="年度" prop="year"><el-date-picker v-model="formYear" type="year" value-format="yyyy" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="框架名称" prop="frameworkName"><el-input v-model="form.frameworkName" maxlength="200" /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="牵头单位" prop="leadDeptId"><el-select v-model="form.leadDeptId" style="width:100%" @change="leadChanged"><el-option v-for="unit in groupUnits" :key="unit.deptId" :label="deptName(unit.deptId)" :value="unit.deptId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="9999" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="协同单位"><el-select v-model="collaboratingDeptIds" multiple filterable style="width:100%"><el-option v-for="unit in groupUnits" :key="unit.deptId" :label="deptName(unit.deptId)" :value="unit.deptId" :disabled="unit.deptId === form.leadDeptId" /></el-select></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio label="0">正常</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="总体目标"><el-input v-model="form.overallGoal" type="textarea" :rows="5" maxlength="2000" show-word-limit /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="open = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">确定</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listDept } from '@/api/system/dept'
import { accessibleGroups } from '@/api/research/group'
import { listFrameworks, getFramework, addFramework, updateFramework, deleteFramework } from '@/api/research/framework'

export default {
  name: 'TaskFramework',
  data() {
    return {
      loading: false, saving: false, showSearch: true, open: false,
      rows: [], total: 0, groups: [], depts: [], collaboratingDeptIds: [],
      query: { pageNum: 1, pageSize: 10, groupId: undefined, year: undefined, frameworkName: undefined },
      form: {},
      rules: {
        groupId: [{ required: true, message: '请选择课题', trigger: 'change' }],
        year: [{ required: true, message: '请选择年度', trigger: 'change' }],
        frameworkName: [{ required: true, message: '请输入框架名称', trigger: 'blur' }],
        leadDeptId: [{ required: true, message: '请选择牵头单位', trigger: 'change' }]
      }
    }
  },
  computed: {
    formYear: {
      get() { return this.form.year == null ? null : String(this.form.year) },
      set(value) { this.$set(this.form, 'year', value ? Number(value) : null) }
    },
    selectedGroup() { return this.groups.find(item => item.groupId === this.form.groupId) },
    groupUnits() { return this.selectedGroup ? (this.selectedGroup.units || []).filter(item => item.status === '0') : [] }
  },
  created() {
    Promise.all([accessibleGroups(), listDept({ status: '0' })]).then(([groups, depts]) => {
      this.groups = groups.data || []
      this.depts = depts.data || []
      this.load()
    })
  },
  methods: {
    deptName(id) { const dept = this.depts.find(item => item.deptId === id); return dept ? dept.deptName : (id || '—') },
    unitNames(units) { return (units || []).map(item => item.deptName || this.deptName(item.deptId)).join('、') },
    load() { this.loading = true; const query = Object.assign({}, this.query, { year: this.query.year ? Number(this.query.year) : undefined }); listFrameworks(query).then(res => { this.rows = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    search() { this.query.pageNum = 1; this.load() },
    resetSearch() { this.resetForm('queryForm'); this.search() },
    openForm(row) {
      const show = data => {
        this.form = Object.assign({ groupId: null, frameworkName: '', year: new Date().getFullYear(), leadDeptId: null, overallGoal: '', status: '0', sort: 0 }, data)
        this.collaboratingDeptIds = (data.units || []).map(item => item.deptId)
        this.open = true
      }
      row ? getFramework(row.frameworkId).then(res => show(res.data)) : show({})
    },
    groupChanged() { const lead = this.groupUnits.find(item => item.unitType === 'LEAD'); this.form.leadDeptId = lead ? lead.deptId : null; this.collaboratingDeptIds = [] },
    leadChanged() { this.collaboratingDeptIds = this.collaboratingDeptIds.filter(id => id !== this.form.leadDeptId) },
    save() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const data = Object.assign({}, this.form, { units: this.collaboratingDeptIds.map(deptId => ({ deptId })) })
        this.saving = true
        const request = data.frameworkId ? updateFramework : addFramework
        request(data).then(() => { this.$modal.msgSuccess('保存成功'); this.open = false; this.load() }).finally(() => { this.saving = false })
      })
    },
    remove(row) { this.$modal.confirm('确认删除年度任务“' + row.frameworkName + '”吗？').then(() => deleteFramework(row.frameworkId)).then(() => { this.$modal.msgSuccess('删除成功'); this.load() }).catch(() => {}) }
  }
}
</script>
