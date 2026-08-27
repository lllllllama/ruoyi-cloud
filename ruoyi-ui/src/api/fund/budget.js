import request from '@/utils/request'
const base='/ruoyi-fund'
export function listBudget(query){return request({url:base+'/budget/list',method:'get',params:query})}
export function getBudgetByTopic(topicId){return request({url:base+'/budget/topic/'+topicId,method:'get'})}
export function getOverview(topicId){return request({url:base+'/budget/overview/'+topicId,method:'get'})}
export function addBudget(data){return request({url:base+'/budget',method:'post',data})}
export function updateBudget(data){return request({url:base+'/budget',method:'put',data})}
export function delBudget(id){return request({url:base+'/budget/'+id,method:'delete'})}
