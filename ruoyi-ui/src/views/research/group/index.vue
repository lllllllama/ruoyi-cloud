<template>
  <div class="app-container research-group-page">
    <el-form v-show="showSearch" ref="queryForm" :model="query" :inline="true" label-width="72px">
      <el-form-item label="课题编码" prop="groupCode">
        <el-input v-model="query.groupCode" placeholder="请输入课题编码" clearable size="small" @keyup.enter.native="search" />
      </el-form-item>
      <el-form-item label="课题名称" prop="groupName">
        <el-input v-model="query.groupName" placeholder="请输入课题名称" clearable size="small" @keyup.enter.native="search" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="query.status" placeholder="全部" clearable size="small">
          <el-option label="正常" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="search">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button v-hasPermi="['research:group:add']" type="primary" plain icon="el-icon-plus" size="mini" @click="openGroup()">新增课题</el-button>
      </el-col>
      <right-toolbar :show-search.sync="showSearch" @queryTable="loadGroups" />
    </el-row>

    <el-table v-loading="loading" :data="groups" row-key="groupId">
      <el-table-column label="课题编码" prop="groupCode" min-width="130" />
      <el-table-column label="课题名称" prop="groupName" min-width="220" show-overflow-tooltip />
      <el-table-column label="负责单位" min-width="180">
        <template slot-scope="scope">{{ deptName(scope.row.leadDeptId) }}</template>
      </el-table-column>
      <el-table-column label="参与单位" min-width="220" show-overflow-tooltip>
        <template slot-scope="scope">{{ participantNames(scope.row.units) || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template slot-scope="scope"><el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '正常' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="排序" prop="sort" width="70" align="center" />
      <el-table-column label="操作" width="220" fixed="right">
        <template slot-scope="scope">
          <el-button v-hasPermi="['research:group:edit']" size="mini" type="text" icon="el-icon-edit" @click="openGroup(scope.row)">编辑</el-button>
          <el-button v-hasPermi="['research:group:edit']" size="mini" type="text" icon="el-icon-user" @click="openMembers(scope.row)">成员</el-button>
          <el-button v-hasPermi="['research:group:edit']" size="mini" type="text" icon="el-icon-delete" @click="removeGroup(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="loadGroups" />

    <el-dialog :title="groupForm.groupId ? '编辑课题' : '新增课题'" :visible.sync="groupOpen" width="760px" append-to-body>
      <el-form ref="groupForm" :model="groupForm" :rules="groupRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="课题编码" prop="groupCode"><el-input v-model="groupForm.groupCode" maxlength="64" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="课题名称" prop="groupName"><el-input v-model="groupForm.groupName" maxlength="200" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责单位" prop="leadDeptId">
              <treeselect v-model="groupForm.leadDeptId" :options="deptTree" :normalizer="deptNormalizer" placeholder="请选择负责单位" @input="leadDeptChanged" />
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="单位负责人"><el-select v-model="leadManagerId" clearable filterable style="width:100%"><el-option v-for="user in usersByDept(groupForm.leadDeptId)" :key="user.userId" :label="user.nickName" :value="user.userId" /></el-select></el-form-item></el-col>
        </el-row>
        <el-form-item label="参与单位">
          <el-select v-model="participantDeptIds" multiple filterable style="width:100%" placeholder="请选择参与单位">
            <el-option v-for="dept in flatDepts" :key="dept.deptId" :label="dept.deptName" :value="dept.deptId" :disabled="dept.deptId === groupForm.leadDeptId" />
          </el-select>
        </el-form-item>
        <el-table v-if="participantDeptIds.length" :data="participantUnits" size="mini" class="unit-table">
          <el-table-column label="参与单位"><template slot-scope="scope">{{ deptName(scope.row.deptId) }}</template></el-table-column>
          <el-table-column label="单位负责人" min-width="220"><template slot-scope="scope"><el-select v-model="scope.row.managerUserId" clearable filterable size="small" style="width:100%"><el-option v-for="user in usersByDept(scope.row.deptId)" :key="user.userId" :label="user.nickName" :value="user.userId" /></el-select></template></el-table-column>
        </el-table>
        <el-row :gutter="20" class="mt16">
          <el-col :span="12"><el-form-item label="状态"><el-radio-group v-model="groupForm.status"><el-radio label="0">正常</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="groupForm.sort" :min="0" :max="9999" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="课题说明"><el-input v-model="groupForm.description" type="textarea" :rows="3" maxlength="1000" show-word-limit /></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="groupOpen = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveGroup">确定</el-button></div>
    </el-dialog>

    <el-drawer :title="memberGroupName + ' · 成员管理'" :visible.sync="memberOpen" size="720px" append-to-body>
      <div class="drawer-body">
        <el-button v-hasPermi="['research:group:edit']" type="primary" plain size="mini" icon="el-icon-plus" class="mb8" @click="openMemberForm()">添加成员</el-button>
        <el-table v-loading="memberLoading" :data="members">
          <el-table-column label="成员" min-width="140"><template slot-scope="scope">{{ userName(scope.row.userId) }}</template></el-table-column>
          <el-table-column label="所属单位" min-width="150"><template slot-scope="scope">{{ deptName(scope.row.deptId) }}</template></el-table-column>
          <el-table-column label="角色" width="100"><template slot-scope="scope">{{ roleName(scope.row.memberRole) }}</template></el-table-column>
          <el-table-column label="状态" width="80"><template slot-scope="scope">{{ scope.row.status === '0' ? '正常' : '停用' }}</template></el-table-column>
          <el-table-column label="操作" width="120"><template slot-scope="scope"><el-button type="text" @click="openMemberForm(scope.row)">编辑</el-button><el-button type="text" @click="removeMember(scope.row)">删除</el-button></template></el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <el-dialog :title="memberForm.id ? '编辑成员' : '添加成员'" :visible.sync="memberFormOpen" width="520px" append-to-body>
      <el-form ref="memberForm" :model="memberForm" :rules="memberRules" label-width="90px">
        <el-form-item label="成员" prop="userId"><el-select v-model="memberForm.userId" filterable style="width:100%" @change="memberUserChanged"><el-option v-for="user in selectableUsers" :key="user.userId" :label="user.nickName + '（' + deptName(user.deptId) + '）'" :value="user.userId" /></el-select></el-form-item>
        <el-form-item label="所属单位" prop="deptId"><el-input :value="deptName(memberForm.deptId)" disabled /></el-form-item>
        <el-form-item label="成员角色" prop="memberRole"><el-select v-model="memberForm.memberRole" style="width:100%"><el-option v-for="role in roles" :key="role.value" :label="role.label" :value="role.value" /></el-select></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="memberForm.status"><el-radio label="0">正常</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <div slot="footer"><el-button @click="memberFormOpen = false">取消</el-button><el-button type="primary" @click="saveMember">确定</el-button></div>
    </el-dialog>
  </div>
</template>

<script>
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import { listDept, treeselect } from '@/api/system/dept'
import { listUser } from '@/api/system/user'
import { listGroups, getGroup, addGroup, updateGroup, deleteGroup, listGroupMembers, addGroupMember, updateGroupMember, deleteGroupMember } from '@/api/research/group'

export default {
  name: 'ResearchGroup',
  components: { Treeselect },
  data() {
    return {
      loading: false, saving: false, showSearch: true, total: 0, groups: [],
      deptTree: [], flatDepts: [], users: [],
      query: { pageNum: 1, pageSize: 10, groupCode: undefined, groupName: undefined, status: undefined },
      groupOpen: false, groupForm: {}, leadManagerId: null, participantDeptIds: [], participantUnits: [],
      groupRules: { groupCode: [{ required: true, message: '请输入课题编码', trigger: 'blur' }], groupName: [{ required: true, message: '请输入课题名称', trigger: 'blur' }], leadDeptId: [{ required: true, message: '请选择负责单位', trigger: 'change' }] },
      memberOpen: false, memberLoading: false, memberFormOpen: false, memberGroupId: null, memberGroupName: '', members: [], memberForm: {},
      memberRules: { userId: [{ required: true, message: '请选择成员', trigger: 'change' }], deptId: [{ required: true, message: '成员缺少所属单位', trigger: 'change' }], memberRole: [{ required: true, message: '请选择成员角色', trigger: 'change' }] },
      roles: [{ value: 'LEADER', label: '课题负责人' }, { value: 'CORE', label: '核心成员' }, { value: 'MEMBER', label: '普通成员' }, { value: 'EXPERT', label: '专家' }]
    }
  },
  computed: {
    selectableUsers() {
      const group = this.groups.find(item => item.groupId === this.memberGroupId)
      const deptIds = group ? (group.units || []).map(item => item.deptId) : []
      return this.users.filter(item => deptIds.indexOf(item.deptId) >= 0)
    }
  },
  created() {
    Promise.all([treeselect(), listDept({ status: '0' }), listUser({ pageNum: 1, pageSize: 1000, status: '0' })]).then(([tree, depts, users]) => {
      this.deptTree = tree.data || []
      this.flatDepts = depts.data || []
      this.users = users.rows || []
      this.loadGroups()
    })
  },
  methods: {
    deptNormalizer(node) { return { id: node.id, label: node.label, children: node.children } },
    deptName(id) { const item = this.flatDepts.find(dept => dept.deptId === id); return item ? item.deptName : (id || '—') },
    userName(id) { const item = this.users.find(user => user.userId === id); return item ? item.nickName : (id || '—') },
    usersByDept(deptId) { return this.users.filter(user => user.deptId === deptId) },
    roleName(value) { const role = this.roles.find(item => item.value === value); return role ? role.label : value },
    participantNames(units) { return (units || []).filter(item => item.unitType === 'PARTICIPANT').map(item => this.deptName(item.deptId)).join('、') },
    loadGroups() { this.loading = true; listGroups(this.query).then(res => { this.groups = res.rows || []; this.total = res.total || 0 }).finally(() => { this.loading = false }) },
    search() { this.query.pageNum = 1; this.loadGroups() },
    resetSearch() { this.resetForm('queryForm'); this.search() },
    openGroup(row) {
      const open = data => {
        this.groupForm = Object.assign({ status: '0', sort: 0, description: '' }, data)
        const units = data.units || []
        const lead = units.find(item => item.unitType === 'LEAD')
        this.leadManagerId = lead ? lead.managerUserId : null
        this.participantDeptIds = units.filter(item => item.unitType === 'PARTICIPANT').map(item => item.deptId)
        this.participantUnits = units.filter(item => item.unitType === 'PARTICIPANT').map(item => ({ deptId: item.deptId, managerUserId: item.managerUserId }))
        this.groupOpen = true
      }
      row ? getGroup(row.groupId).then(res => open(res.data)) : open({})
    },
    leadDeptChanged() { this.leadManagerId = null; this.participantDeptIds = this.participantDeptIds.filter(id => id !== this.groupForm.leadDeptId) },
    saveGroup() {
      this.$refs.groupForm.validate(valid => {
        if (!valid) return
        const data = Object.assign({}, this.groupForm)
        data.units = [{ deptId: data.leadDeptId, unitType: 'LEAD', managerUserId: this.leadManagerId }].concat(this.participantUnits.map(item => ({ deptId: item.deptId, unitType: 'PARTICIPANT', managerUserId: item.managerUserId })))
        this.saving = true
        const save = data.groupId ? updateGroup : addGroup
        save(data).then(() => { this.$modal.msgSuccess('保存成功'); this.groupOpen = false; this.loadGroups() }).finally(() => { this.saving = false })
      })
    },
    removeGroup(row) { this.$modal.confirm('确认删除课题“' + row.groupName + '”吗？').then(() => deleteGroup(row.groupId)).then(() => { this.$modal.msgSuccess('删除成功'); this.loadGroups() }).catch(() => {}) },
    openMembers(row) { this.memberGroupId = row.groupId; this.memberGroupName = row.groupName; this.memberOpen = true; this.loadMembers() },
    loadMembers() { this.memberLoading = true; listGroupMembers(this.memberGroupId).then(res => { this.members = res.data || [] }).finally(() => { this.memberLoading = false }) },
    openMemberForm(row) { this.memberForm = row ? Object.assign({}, row) : { userId: null, deptId: null, memberRole: 'MEMBER', status: '0' }; this.memberFormOpen = true },
    memberUserChanged(userId) { const user = this.users.find(item => item.userId === userId); this.memberForm.deptId = user ? user.deptId : null },
    saveMember() { this.$refs.memberForm.validate(valid => { if (!valid) return; const save = this.memberForm.id ? updateGroupMember : addGroupMember; save(this.memberGroupId, this.memberForm).then(() => { this.$modal.msgSuccess('保存成功'); this.memberFormOpen = false; this.loadMembers() }) }) },
    removeMember(row) { this.$modal.confirm('确认移除成员“' + this.userName(row.userId) + '”吗？').then(() => deleteGroupMember(this.memberGroupId, row.userId)).then(() => { this.$modal.msgSuccess('移除成功'); this.loadMembers() }).catch(() => {}) }
  },
  watch: {
    participantDeptIds(ids) {
      const previous = this.participantUnits.reduce((result, item) => {
        result[item.deptId] = item.managerUserId
        return result
      }, {})
      this.participantUnits = ids.map(deptId => ({ deptId, managerUserId: previous[deptId] || null }))
    }
  }
}
</script>

<style scoped>
.research-group-page .unit-table { margin: -4px 0 16px 100px; width: calc(100% - 100px); }
.drawer-body { padding: 0 24px 24px; }
.mt16 { margin-top: 16px; }
</style>
