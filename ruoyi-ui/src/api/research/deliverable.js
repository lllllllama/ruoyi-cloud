import request from '@/utils/request'

const base = '/ruoyi-research/deliverable'

export function listDeliverables(query) {
  return request({ url: base + '/list', method: 'get', params: query })
}

export function getDeliverable(id) {
  return request({ url: base + '/' + id, method: 'get' })
}

export function getDeliverableAssignees(id) {
  return request({ url: base + '/' + id + '/assignees', method: 'get' })
}

export function canSubmitDeliverable(id) {
  return request({ url: base + '/' + id + '/can-submit', method: 'get' })
}

export function assignDeliverable(id, userIds) {
  return request({ url: base + '/' + id + '/assignees', method: 'put', data: { userIds } })
}

export function addDeliverable(data) {
  return request({ url: base, method: 'post', data })
}

export function updateDeliverable(data) {
  return request({ url: base, method: 'put', data })
}

export function deleteDeliverable(id) {
  return request({ url: base + '/' + id, method: 'delete' })
}
