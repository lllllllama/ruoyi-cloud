import request from '@/utils/request'
const base='/ruoyi-fund'
export function listTopic(query){return request({url:base+'/topic/list',method:'get',params:query})}
export function accessibleTopics(){return request({url:base+'/topic/accessible',method:'get'})}
export function getTopic(id){return request({url:base+'/topic/'+id,method:'get'})}
export function addTopic(data){return request({url:base+'/topic',method:'post',data})}
export function updateTopic(data){return request({url:base+'/topic',method:'put',data})}
export function delTopic(id){return request({url:base+'/topic/'+id,method:'delete'})}
