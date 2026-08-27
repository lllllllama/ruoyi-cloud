import request from '@/utils/request'

const base = '/ruoyi-research/task'

export function listTasks(query) {
  return request({ url: base + '/list', method: 'get', params: query })
}

export function listMyTasks() {
  return request({ url: base + '/my', method: 'get' })
}

export function getTask(id) {
  return request({ url: base + '/' + id, method: 'get' })
}

export function validateTaskFramework(frameworkId) {
  return request({ url: base + '/framework/' + frameworkId + '/validate', method: 'get' })
}

export function addTask(data) {
  return request({ url: base, method: 'post', data })
}

export function updateTask(data) {
  return request({ url: base, method: 'put', data })
}

export function deleteTask(ids) {
  return request({ url: base + '/' + ids, method: 'delete' })
}
