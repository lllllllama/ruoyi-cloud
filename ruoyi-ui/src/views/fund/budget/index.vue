<template>
  <div class="app-container">
    <el-alert
      title="课题基础信息由课题管理模块统一维护，本页面仅配置项目总资金。"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />

    <el-table v-loading="loading" :data="groups">
      <el-table-column label="课题编码" prop="groupCode" min-width="140" />
      <el-table-column label="课题名称" prop="groupName" min-width="220" />
      <el-table-column label="负责单位ID" prop="leadDeptId" width="120" />
      <el-table-column label="项目总资金（元）" min-width="150">
        <template slot-scope="scope">{{ money(budgetOf(scope.row.groupId).totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template slot-scope="scope">
          <el-button type="text" @click="showOverview(scope.row)">查看统计</el-button>
          <el-button
            type="text"
            @click="openBudgetDialog(scope.row)"
            v-hasPermi="['fund:budget:add','fund:budget:edit']"
          >{{ budgetOf(scope.row.groupId).budgetId ? '编辑总资金' : '设置总资金' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="课题总资金" :visible.sync="budgetOpen" width="560px" append-to-body>
      <el-form ref="budgetForm" :model="budgetForm" :rules="budgetRules" label-width="120px">
        <el-form-item label="课题"><b>{{ budgetGroupName }}</b></el-form-item>
        <el-form-item label="课题总资金" prop="totalAmount">
          <el-input-number v-model="budgetForm.totalAmount" :min="0.01" :precision="2" :step="10000" style="width:100%" />
        </el-form-item>
        <el-form-item label="计划拨付完成">
          <el-date-picker v-model="budgetForm.planEndTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="资金说明"><el-input v-model="budgetForm.fundDesc" type="textarea" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="budgetOpen=false">取消</el-button>
        <el-button type="primary" @click="saveBudget">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="资金统计" :visible.sync="overviewOpen" width="720px">
      <el-row v-if="overview" :gutter="12">
        <el-col v-for="item in cards" :key="item.key" :span="8">
          <el-card shadow="never" class="money-card">
            <div class="label">{{ item.label }}</div>
            <div class="amount">{{ money(overview[item.key]) }}</div>
          </el-card>
        </el-col>
      </el-row>
    </el-dialog>
  </div>
</template>

<script>
import { groupOptions } from '@/api/research/group'
import { listBudget, addBudget, updateBudget, getAllocationOverview, getUseOverview } from '@/api/fund/budget'

export default {
  name: 'FundBudget',
  data() {
    return {
      loading: false,
      groups: [],
      budgets: [],
      budgetOpen: false,
      overviewOpen: false,
      overview: null,
      budgetGroupName: '',
      budgetForm: {},
      budgetRules: { totalAmount: [{ required: true, message: '请输入总资金' }] },
      cards: [
        { key: 'totalAmount', label: '课题总资金（元）' },
        { key: 'plannedAllocation', label: '计划拨付（元）' },
        { key: 'actualAllocation', label: '实际拨付（元）' },
        { key: 'plannedUse', label: '计划使用（元）' },
        { key: 'actualUse', label: '实际使用（元）' },
        { key: 'overspend', label: '使用超计划（元）' }
      ]
    }
  },
  created() {
    this.load()
  },
  methods: {
    money(value) {
      return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    budgetOf(groupId) {
      return this.budgets.find(item => item.topicId === groupId) || {}
    },
    load() {
      this.loading = true
      Promise.all([groupOptions(), listBudget({ pageNum: 1, pageSize: 1000 })])
        .then(([groupResult, budgetResult]) => {
          this.groups = groupResult.data || []
          this.budgets = budgetResult.rows || []
          this.loading = false
        })
        .catch(() => { this.loading = false })
    },
    openBudgetDialog(group) {
      const stored = this.budgetOf(group.groupId)
      this.budgetGroupName = group.groupName
      this.budgetForm = stored.budgetId
        ? Object.assign({}, stored)
        : { topicId: group.groupId, totalAmount: 0, planEndTime: null, fundDesc: '' }
      this.budgetOpen = true
    },
    saveBudget() {
      this.$refs.budgetForm.validate(valid => {
        if (!valid) return
        const save = this.budgetForm.budgetId ? updateBudget : addBudget
        save(this.budgetForm).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.budgetOpen = false
          this.load()
        })
      })
    },
    showOverview(group) {
      Promise.all([getAllocationOverview(group.groupId), getUseOverview(group.groupId)]).then(([allocation, use]) => {
        this.overview = Object.assign({}, allocation.data, use.data)
        this.overviewOpen = true
      })
    }
  }
}
</script>

<style scoped>
.money-card { margin-bottom: 12px; text-align: center; }
.money-card .label { color: #909399; }
.money-card .amount { margin-top: 10px; font-size: 22px; font-weight: 600; }
</style>
