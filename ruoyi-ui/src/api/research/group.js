import request from '@/utils/request'

const base = '/ruoyi-research'

export function listGroups(query) {
  return request({ url: base + '/group/list', method: 'get', params: query })
}

export function groupOptions() {
  return request({ url: base + '/group/options', method: 'get' })
}

export function accessibleGroups() {
  return request({ url: base + '/group/accessible', method: 'get' })
}

export function getGroup(id) {
  return request({ url: base + '/group/' + id, method: 'get' })
}

export function addGroup(data) {
  return request({ url: base + '/group', method: 'post', data })
}

export function updateGroup(data) {
  return request({ url: base + '/group', method: 'put', data })
}

export function deleteGroup(ids) {
  return request({ url: base + '/group/' + ids, method: 'delete' })
}

export function groupMembers(id) {
  return request({ url: base + '/group/' + id + '/member/options', method: 'get' })
}

export function listGroupMembers(groupId) {
  return request({ url: base + '/group/' + groupId + '/member/list', method: 'get' })
}

export function addGroupMember(groupId, data) {
  return request({ url: base + '/group/' + groupId + '/member', method: 'post', data })
}

export function updateGroupMember(groupId, data) {
  return request({ url: base + '/group/' + groupId + '/member', method: 'put', data })
}

export function deleteGroupMember(groupId, userId) {
  return request({ url: base + '/group/' + groupId + '/member/' + userId, method: 'delete' })
}
