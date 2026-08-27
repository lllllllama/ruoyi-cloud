import request from '@/utils/request'
const base='/ruoyi-fund'
export function listFundDepts(){return request({url:base+'/org/depts',method:'get'})}
export function listFundUsers(deptId){return request({url:base+'/org/dept/'+deptId+'/users',method:'get'})}
