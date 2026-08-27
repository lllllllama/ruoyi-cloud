import request from '@/utils/request'
const base='/ruoyi-fund'
export function listBudget(query){return request({url:base+'/budget/list',method:'get',params:query})}
export function getBudgetByTopic(topicId){return request({url:base+'/budget/topic/'+topicId,method:'get'})}
export function getAllocationOverview(groupId){return request({url:base+'/allocation/overview/'+groupId,method:'get'})}
export function getUseOverview(groupId){return request({url:base+'/use/overview/'+groupId,method:'get'})}
export function addBudget(data){return request({url:base+'/budget',method:'post',data})}
export function updateBudget(data){return request({url:base+'/budget',method:'put',data})}
export function delBudget(id){return request({url:base+'/budget/'+id,method:'delete'})}
