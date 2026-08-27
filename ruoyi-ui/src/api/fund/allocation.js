import request from '@/utils/request'
const base='/ruoyi-fund/allocation'
export function listAllocationPlans(q){return request({url:base+'/plan/list',method:'get',params:q})}
export function getAllocationPlan(id){return request({url:base+'/plan/'+id,method:'get'})}
export function addAllocationPlan(data){return request({url:base+'/plan',method:'post',data})}
export function updateAllocationPlan(data){return request({url:base+'/plan',method:'put',data})}
export function delAllocationPlan(id){return request({url:base+'/plan/'+id,method:'delete'})}
export function assignAllocation(id,userId){return request({url:base+'/plan/'+id+'/assign',method:'put',data:{responsibleUserId:userId}})}
export function listAllocationRecords(id){return request({url:base+'/plan/'+id+'/records',method:'get'})}
export function addAllocationRecord(data){return request({url:base+'/record',method:'post',data})}
export function updateAllocationRecord(data){return request({url:base+'/record',method:'put',data})}
export function delAllocationRecord(id){return request({url:base+'/record/'+id,method:'delete'})}
export function allocationFinishCheck(id){return request({url:base+'/plan/'+id+'/finish-check',method:'get'})}
export function finishAllocation(id,confirmDifference){return request({url:base+'/plan/'+id+'/finish',method:'put',data:{confirmDifference}})}
