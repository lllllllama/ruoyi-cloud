import request from '@/utils/request'
const base='/ruoyi-fund'
export function listFundDepts(){return request({url:base+'/org/depts',method:'get'})}
