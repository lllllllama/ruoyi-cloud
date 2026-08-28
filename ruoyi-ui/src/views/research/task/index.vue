<template>
  <div class="app-container task-tree-page">
    <div class="task-toolbar">
      <el-select v-model="frameworkId" filterable placeholder="请选择年度任务" style="width:360px" @change="loadTasks">
        <el-option v-for="item in frameworks" :key="item.frameworkId" :label="item.year + ' · ' + item.groupName + ' · ' + item.frameworkName" :value="item.frameworkId" />
      </el-select>
      <el-button v-hasPermi="['task:info:add']" type="primary" icon="el-icon-plus" :disabled="!frameworkId" @click="openTask()">添加一级任务</el-button>
      <el-button v-hasPermi="['task:info:edit']" icon="el-icon-circle-check" :disabled="!frameworkId" @click="validateStructure">校验任务结构</el-button>
    </div>

    <el-empty v-if="!frameworkId" description="选择年度任务后维护任务树" />
    <el-table v-else v-loading="loading" :data="taskTree" row-key="taskId" default-expand-all :tree-props="{ children: 'children' }">
      <el-table-column label="任务名称" prop="taskName" min-width="280" show-overflow-tooltip>
        <template slot-scope="scope"><span class="level-mark">L{{ scope.row.level }}</span>{{ scope.row.taskName }}</template>
      </el-table-column>
      <el-table-column label="类型" prop="taskType" width="120"><template slot-scope="scope">{{ scope.row.taskType || '—' }}</template></el-table-column>
      <el-table-column label="起止时间" min-width="210"><template slot-scope="scope">{{ scope.row.startDate || '—' }} 至 {{ scope.row.deadline || '—' }}</template></el-table-column>
      <el-table-column label="状态" width="90" align="center"><template slot-scope="scope"><el-tag :type="statusType(scope.row.status)">{{ statusName(scope.row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="排序" prop="sort" width="70" align="center" />
      <el-table-column label="操作" width="300" fixed="right">
        <template slot-scope="scope">
          <div class="business-table-actions">
            <el-button v-if="scope.row.level < 3" v-hasPermi="['task:info:add']" type="text" icon="el-icon-plus" @click="openTask(null, scope.row)">子任务</el-button>
            <el-button v-hasPermi="['task:info:edit']" type="text" icon="el-icon-edit" @click="openTask(scope.row)">编辑</el-button>
            <el-button v-hasPermi="['task:deliverable:add']" type="text" icon="el-icon-document" @click="openDeliverables(scope.row)">成果</el-button>
            <el-button v-hasPermi="['task:info:remove']" type="text" icon="el-icon-delete" @click="removeTask(scope.row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="form.taskId ? '编辑任务' : '新增任务'" :visible.sync="open" width="660px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级任务"><el-input :value="parentName(form.parentId)" disabled /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="16"><el-form-item label="任务名称" prop="taskName"><el-input v-model="form.taskName" maxlength="200" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="层级" prop="level"><el-select v-model="form.level" :disabled="!!form.taskId" style="width:100%"><el-option v-for="level in allowedLevels" :key="level" :label="'第 ' + level + ' 级'" :value="level" /></el-select></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="任务类型"><el-input v-model="form.taskType" maxlength="32" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" :max="9999" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="yyyy-MM-dd" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="截止日期"><el-date-picker v-model="form.deadline" type="date" value-format="yyyy-MM-dd" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="任务说明"><el-input v-model="form.description" type="textarea" :rows="4" maxlength="2000" show-word-limit /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" maxlength="500" /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="open = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveTask">确定</el-button></div>
    </el-dialog>

    <el-drawer :title="deliverableTaskName + ' · 交付成果'" :visible.sync="deliverableOpen" size="760px" append-to-body>
      <div class="drawer-body">
        <el-button v-hasPermi="['task:deliverable:add']" type="primary" plain size="mini" icon="el-icon-plus" class="mb8" @click="openDeliverableForm()">添加成果</el-button>
        <el-table v-loading="deliverableLoading" :data="deliverables">
          <el-table-column label="成果名称" prop="deliverableName" min-width="170" show-overflow-tooltip />
          <el-table-column label="进度" width="95" align="center"><template slot-scope="scope">{{ scope.row.archivedNum || 0 }} / {{ scope.row.requiredNum }}</template></el-table-column>
          <el-table-column label="截止日期" prop="deadline" width="110" />
          <el-table-column label="必交" width="65" align="center"><template slot-scope="scope">{{ scope.row.isRequired === '1' ? '是' : '否' }}</template></el-table-column>
          <el-table-column label="状态" width="90"><template slot-scope="scope">{{ deliverableStatus(scope.row.status) }}</template></el-table-column>
          <el-table-column label="操作" width="120"><template slot-scope="scope"><div class="business-table-actions"><el-button type="text" @click="openDeliverableForm(scope.row)">编辑</el-button><el-button type="text" @click="removeDeliverable(scope.row)">删除</el-button></div></template></el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-dialog :title="deliverableForm.deliverableId ? '编辑交付成果' : '添加交付成果'" :visible.sync="deliverableFormOpen" width="620px" append-to-body>
      <el-form ref="deliverableForm" :model="deliverableForm" :rules="deliverableRules" label-width="96px">
        <el-form-item label="成果名称" prop="deliverableName"><el-input v-model="deliverableForm.deliverableName" maxlength="200" /></el-form-item>
        <el-form-item label="成果要求"><el-input v-model="deliverableForm.requirement" type="textarea" :rows="3" maxlength="2000" show-word-limit /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="要求数量" prop="requiredNum"><el-input-number v-model="deliverableForm.requiredNum" :min="1" :max="9999" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="截止日期"><el-date-picker v-model="deliverableForm.deadline" type="date" value-format="yyyy-MM-dd" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="是否必交"><el-radio-group v-model="deliverableForm.isRequired"><el-radio label="1">必交</el-radio><el-radio label="0">选交</el-radio></el-radio-group></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="deliverableForm.sort" :min="0" :max="9999" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="责任人"><el-select v-model="assigneeIds" multiple filterable clearable style="width:100%" placeholder="不指定责任人时，由除课题负责人外的其他有效课题成员提交"><el-option v-for="member in groupMemberOptions" :key="member.userId" :label="member.nickName || member.userName" :value="member.userId" /></el-select></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="deliverableFormOpen = false">取消</el-button><el-button type="primary" :loading="deliverableSaving" @click="saveDeliverable">确定</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import { listFrameworks } from '@/api/research/framework'
import { groupMembers } from '@/api/research/group'
import { listTasks, getTask, validateTaskFramework, addTask, updateTask, deleteTask } from '@/api/research/task'
import { listDeliverables, getDeliverable, getDeliverableAssignees, assignDeliverable, addDeliverable, updateDeliverable, deleteDeliverable } from '@/api/research/deliverable'

export default {
  name: 'ResearchTask',
  data() {
    return {
      loading: false, saving: false, open: false, frameworkId: null,
      frameworks: [], tasks: [], taskTree: [], form: {}, allowedLevels: [1],
      deliverableOpen: false, deliverableLoading: false, deliverableFormOpen: false, deliverableSaving: false,
      deliverableTaskId: null, deliverableTaskName: '', deliverables: [], groupMemberOptions: [], deliverableForm: {}, assigneeIds: [],
      rules: {
        taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
        level: [{ required: true, message: '请选择任务层级', trigger: 'change' }]
      },
      deliverableRules: {
        deliverableName: [{ required: true, message: '请输入成果名称', trigger: 'blur' }],
        requiredNum: [{ required: true, message: '请输入要求数量', trigger: 'change' }]
      }
    }
  },
  computed: {
    framework() { return this.frameworks.find(item => item.frameworkId === this.frameworkId) }
  },
  created() {
    listFrameworks({ pageNum: 1, pageSize: 1000, status: '0' }).then(res => {
      this.frameworks = res.rows || []
      if (this.frameworks.length) {
        this.frameworkId = this.frameworks[0].frameworkId
        this.loadTasks()
      }
    })
  },
  methods: {
    loadTasks() {
      if (!this.frameworkId) { this.tasks = []; this.taskTree = []; return }
      this.loading = true
      listTasks({ frameworkId: this.frameworkId }).then(res => {
        this.tasks = res.data || []
        this.taskTree = this.handleTree(this.tasks.map(item => Object.assign({}, item)), 'taskId', 'parentId')
      }).finally(() => { this.loading = false })
    },
    parentName(parentId) { if (!parentId) return '无（一级任务）'; const parent = this.tasks.find(item => item.taskId === parentId); return parent ? parent.taskName : '无（一级任务）' },
    statusName(status) { return { '0': '草稿', '1': '进行中', '2': '已完成', '3': '已关闭' }[status] || status },
    statusType(status) { return { '0': 'info', '1': 'primary', '2': 'success', '3': 'warning' }[status] || '' },
    openTask(row, parent) {
      const show = data => {
        const parentId = parent ? parent.taskId : (data.parentId || 0)
        const parentLevel = parent ? parent.level : 0
        this.allowedLevels = data.taskId
          ? [data.level]
          : (parent ? Array.from({ length: 3 - parentLevel }, (item, index) => parentLevel + index + 1) : [1])
        this.form = Object.assign({ frameworkId: this.frameworkId, groupId: this.framework.groupId, parentId, level: parent ? parentLevel + 1 : 1, taskName: '', taskType: '', startDate: null, deadline: null, description: '', sort: 0, remark: '' }, data)
        this.open = true
      }
      row ? getTask(row.taskId).then(res => show(res.data)) : show({})
    },
    saveTask() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.saving = true
        const request = this.form.taskId ? updateTask : addTask
        request(this.form).then(() => { this.$modal.msgSuccess('保存成功'); this.open = false; this.loadTasks() }).finally(() => { this.saving = false })
      })
    },
    removeTask(row) { this.$modal.confirm('确认删除任务“' + row.taskName + '”吗？').then(() => deleteTask(row.taskId)).then(() => { this.$modal.msgSuccess('删除成功'); this.loadTasks() }).catch(() => {}) },
    validateStructure() { validateTaskFramework(this.frameworkId).then(() => this.$modal.msgSuccess('任务结构校验通过')) },
    deliverableStatus(status) { return { '0': '未开始', '1': '进行中', '2': '已完成' }[status] || status },
    openDeliverables(task) {
      this.deliverableTaskId = task.taskId
      this.deliverableTaskName = task.taskName
      this.deliverableOpen = true
      Promise.all([groupMembers(task.groupId), this.loadDeliverables()]).then(([members]) => {
        this.groupMemberOptions = (members.data || []).filter(member => member.status === '0' && member.memberRole !== 'LEADER')
      })
    },
    loadDeliverables() {
      this.deliverableLoading = true
      return listDeliverables({ taskId: this.deliverableTaskId }).then(res => { this.deliverables = res.data || [] }).finally(() => { this.deliverableLoading = false })
    },
    openDeliverableForm(row) {
      const show = (data, assignees) => {
        const task = this.tasks.find(item => item.taskId === this.deliverableTaskId)
        this.deliverableForm = Object.assign({ groupId: task.groupId, taskId: task.taskId, deliverableName: '', requirement: '', requiredNum: 1, deadline: null, isRequired: '1', sort: 0 }, data)
        this.assigneeIds = (assignees || []).map(item => item.userId)
        this.deliverableFormOpen = true
      }
      row
        ? Promise.all([getDeliverable(row.deliverableId), getDeliverableAssignees(row.deliverableId)]).then(([detail, assignees]) => show(detail.data, assignees.data))
        : show({}, [])
    },
    saveDeliverable() {
      this.$refs.deliverableForm.validate(valid => {
        if (!valid) return
        this.deliverableSaving = true
        const request = this.deliverableForm.deliverableId ? updateDeliverable : addDeliverable
        request(this.deliverableForm).then(res => {
          const id = this.deliverableForm.deliverableId || (res.data && res.data.deliverableId)
          if (!id) throw new Error('未取得成果ID')
          return assignDeliverable(id, this.assigneeIds)
        }).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.deliverableFormOpen = false
          this.loadDeliverables()
        }).finally(() => { this.deliverableSaving = false })
      })
    },
    removeDeliverable(row) { this.$modal.confirm('确认删除成果“' + row.deliverableName + '”吗？').then(() => deleteDeliverable(row.deliverableId)).then(() => { this.$modal.msgSuccess('删除成功'); this.loadDeliverables() }).catch(() => {}) }
  }
}
</script>

<style scoped>
.task-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.level-mark { display: inline-block; margin-right: 8px; padding: 1px 6px; border-radius: 3px; color: #3b82f6; background: #eff6ff; font-size: 12px; }
.drawer-body { padding: 0 24px 24px; }
</style>
