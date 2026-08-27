import request from '@/utils/request'

export function listFundOperationLogs(params) {
  return request({
    url: '/ruoyi-fund/fund/operation-log/list',
    method: 'get',
    params
  })
}
