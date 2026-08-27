import request from '@/utils/request'
const base='/ruoyi-fund/use'
export function listUsePlans(q){return request({url:base+'/plan/list',method:'get',params:q})}
export function getUsePlan(id){return request({url:base+'/plan/'+id,method:'get'})}
export function addUsePlan(data){return request({url:base+'/plan',method:'post',data})}
export function updateUsePlan(data){return request({url:base+'/plan',method:'put',data})}
export function delUsePlan(id){return request({url:base+'/plan/'+id,method:'delete'})}
export function listUseRecords(id){return request({url:base+'/plan/'+id+'/records',method:'get'})}
export function addUseRecord(data){return request({url:base+'/record',method:'post',data})}
export function updateUseRecord(data){return request({url:base+'/record',method:'put',data})}
export function delUseRecord(id){return request({url:base+'/record/'+id,method:'delete'})}
export function useFinishCheck(id){return request({url:base+'/plan/'+id+'/finish-check',method:'get'})}
export function finishUse(id,confirmDifference,reason){return request({url:base+'/plan/'+id+'/finish',method:'put',data:{confirmDifference,reason}})}
export function confirmForceFinish(id){return request({url:base+'/plan/'+id+'/force-finish/confirm',method:'put'})}
