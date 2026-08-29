import request from '@/utils/request'

const base = '/ruoyi-research/framework'

export function listFrameworks(query) {
  return request({ url: base + '/list', method: 'get', params: query })
}

export function frameworkOptions() {
  return request({ url: base + '/options', method: 'get' })
}

export function frameworkGroupOptions() {
  return request({ url: base + '/group-options', method: 'get' })
}

export function getFramework(id) {
  return request({ url: base + '/' + id, method: 'get' })
}

export function addFramework(data) {
  return request({ url: base, method: 'post', data })
}

export function updateFramework(data) {
  return request({ url: base, method: 'put', data })
}

export function deleteFramework(ids) {
  return request({ url: base + '/' + ids, method: 'delete' })
}
