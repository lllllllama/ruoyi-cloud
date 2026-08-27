<template><div class="app-container">
  <el-row :gutter="16" class="mb8"><el-col :span="24"><el-button type="primary" plain icon="el-icon-plus" size="mini" @click="openTopicDialog()" v-hasPermi="['fund:topic:add']">新增课题</el-button></el-col></el-row>
  <el-table v-loading="loading" :data="topics">
    <el-table-column label="课题名称" prop="topicName" min-width="220"/><el-table-column label="负责单位" prop="leadDeptName"/><el-table-column label="课题负责人" prop="leaderUserName" width="120"/>
    <el-table-column label="项目总资金(元)" min-width="140"><template slot-scope="s">{{ money(budgetOf(s.row.topicId).totalAmount) }}</template></el-table-column>
    <el-table-column label="操作" width="300"><template slot-scope="s">
      <el-button type="text" @click="showOverview(s.row)">查看统计</el-button><el-button type="text" @click="openTopicDialog(s.row)" v-hasPermi="['fund:topic:edit']">编辑课题</el-button>
      <el-button type="text" @click="openBudgetDialog(s.row)" v-hasPermi="['fund:budget:add','fund:budget:edit']">{{budgetOf(s.row.topicId).budgetId?'编辑总资金':'设置总资金'}}</el-button>
    </template></el-table-column>
  </el-table>
  <pagination v-show="total>0" :total="total" :page.sync="query.pageNum" :limit.sync="query.pageSize" @pagination="load"/>

  <el-dialog :title="topicForm.topicId?'编辑课题':'新增课题'" :visible.sync="topicOpen" width="620px" append-to-body>
    <el-form ref="topicForm" :model="topicForm" :rules="topicRules" label-width="100px">
      <el-form-item label="课题名称" prop="topicName"><el-input v-model="topicForm.topicName"/></el-form-item>
      <el-form-item label="负责单位" prop="leadDeptId"><el-select v-model="topicForm.leadDeptId" filterable style="width:100%" @change="loadLeaderUsers"><el-option v-for="d in depts" :key="d.deptId" :label="d.deptName" :value="d.deptId"/></el-select></el-form-item>
      <el-form-item label="课题负责人" prop="leaderUserId"><el-select v-model="topicForm.leaderUserId" filterable style="width:100%"><el-option v-for="u in leaderUsers" :key="u.userId" :label="u.nickName||u.userName" :value="u.userId"/></el-select></el-form-item>
      <el-form-item label="参与单位"><el-select v-model="topicForm.participantDeptIds" multiple filterable style="width:100%"><el-option v-for="d in depts" :key="d.deptId" :label="d.deptName" :value="d.deptId"/></el-select></el-form-item>
      <el-form-item label="备注"><el-input type="textarea" v-model="topicForm.remark"/></el-form-item>
    </el-form><div slot="footer"><el-button @click="topicOpen=false">取消</el-button><el-button type="primary" @click="saveTopic">确定</el-button></div>
  </el-dialog>

  <el-dialog title="课题总资金" :visible.sync="budgetOpen" width="560px" append-to-body>
    <el-form ref="budgetForm" :model="budgetForm" :rules="budgetRules" label-width="120px"><el-form-item label="课题"><b>{{budgetTopicName}}</b></el-form-item>
      <el-form-item label="课题总资金" prop="totalAmount"><el-input-number v-model="budgetForm.totalAmount" :min="0.01" :precision="2" :step="10000" style="width:100%"/></el-form-item>
      <el-form-item label="计划拨付完成"><el-date-picker v-model="budgetForm.planEndTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%"/></el-form-item>
      <el-form-item label="资金说明"><el-input type="textarea" v-model="budgetForm.fundDesc"/></el-form-item>
    </el-form><div slot="footer"><el-button @click="budgetOpen=false">取消</el-button><el-button type="primary" @click="saveBudget">确定</el-button></div>
  </el-dialog>

  <el-dialog title="资金统计" :visible.sync="overviewOpen" width="720px"><el-row :gutter="12" v-if="overview"><el-col :span="8" v-for="x in cards" :key="x.k"><el-card shadow="never" class="money-card"><div class="label">{{x.label}}</div><div class="amount">{{money(overview[x.k])}}</div></el-card></el-col></el-row></el-dialog>
</div></template>
<script>
import {listTopic,getTopic,addTopic,updateTopic} from '@/api/fund/topic'; import {listBudget,addBudget,updateBudget,getOverview} from '@/api/fund/budget'; import {listFundDepts,listFundUsers} from '@/api/fund/org';
export default {name:'FundBudget',data(){return{loading:false,total:0,query:{pageNum:1,pageSize:10},topics:[],budgets:[],depts:[],leaderUsers:[],topicOpen:false,budgetOpen:false,overviewOpen:false,overview:null,budgetTopicName:'',topicForm:{participantDeptIds:[]},budgetForm:{},topicRules:{topicName:[{required:true,message:'请输入课题名称'}],leadDeptId:[{required:true,message:'请选择负责单位'}],leaderUserId:[{required:true,message:'请选择负责人'}]},budgetRules:{totalAmount:[{required:true,message:'请输入总资金'}]},cards:[{k:'totalAmount',label:'课题总资金(元)'},{k:'arrivedAmount',label:'已到位资金(元)'},{k:'usedAmount',label:'已使用资金(元)'},{k:'pendingAllocationAmount',label:'待拨资金(元)'},{k:'plannedAllocationAmount',label:'计划拨付总额(元)'},{k:'remainingUseAmount',label:'资金余额(元)'}]};},created(){this.loadDepts();this.load();},methods:{money(v){return Number(v||0).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})},budgetOf(id){return this.budgets.find(x=>x.topicId===id)||{}},load(){this.loading=true;Promise.all([listTopic(this.query),listBudget({pageNum:1,pageSize:1000})]).then(([t,b])=>{this.topics=t.rows;this.total=t.total;this.budgets=b.rows;this.loading=false;}).catch(()=>this.loading=false)},loadDepts(){listFundDepts().then(r=>this.depts=r.data||[])},loadLeaderUsers(){this.topicForm.leaderUserId=undefined;if(this.topicForm.leadDeptId)listFundUsers(this.topicForm.leadDeptId).then(r=>this.leaderUsers=r.data||[])},openTopicDialog(row){this.topicForm={topicId:undefined,topicName:'',leadDeptId:undefined,leaderUserId:undefined,participantDeptIds:[],remark:''};this.leaderUsers=[];if(row)getTopic(row.topicId).then(r=>{this.topicForm=r.data;this.loadUsersKeep(r.data.leadDeptId,r.data.leaderUserId)});this.topicOpen=true;},loadUsersKeep(deptId,userId){listFundUsers(deptId).then(r=>{this.leaderUsers=r.data||[];this.topicForm.leaderUserId=userId})},saveTopic(){this.$refs.topicForm.validate(ok=>{if(!ok)return;const fn=this.topicForm.topicId?updateTopic:addTopic;fn(this.topicForm).then(()=>{this.$modal.msgSuccess('保存成功');this.topicOpen=false;this.load();})})},openBudgetDialog(topic){const old=this.budgetOf(topic.topicId);this.budgetTopicName=topic.topicName;this.budgetForm=old.budgetId?Object.assign({},old):{topicId:topic.topicId,totalAmount:0,planEndTime:null,fundDesc:''};this.budgetOpen=true;},saveBudget(){this.$refs.budgetForm.validate(ok=>{if(!ok)return;const fn=this.budgetForm.budgetId?updateBudget:addBudget;fn(this.budgetForm).then(()=>{this.$modal.msgSuccess('保存成功');this.budgetOpen=false;this.load();})})},showOverview(t){getOverview(t.topicId).then(r=>{this.overview=r.data;this.overviewOpen=true})}}}
</script><style scoped>.money-card{margin-bottom:12px;text-align:center}.money-card .label{color:#909399}.money-card .amount{font-size:22px;font-weight:600;margin-top:10px}</style>
