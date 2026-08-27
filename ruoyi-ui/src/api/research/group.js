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

export function groupMembers(id) {
  return request({ url: base + '/group/' + id + '/member/options', method: 'get' })
}
