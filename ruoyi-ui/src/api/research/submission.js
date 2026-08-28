import request from '@/utils/request'

const base = '/ruoyi-research/submission'

export function listSubmissions(query) {
  return request({ url: base + '/list', method: 'get', params: query })
}

export function getSubmission(id) {
  return request({ url: base + '/' + id, method: 'get' })
}

export function listMySubmissions(deliverableId) {
  return request({ url: base + '/mine', method: 'get', params: { deliverableId } })
}

export function addSubmission(data) {
  return request({ url: base, method: 'post', data })
}

export function updateSubmission(data) {
  return request({ url: base, method: 'put', data })
}

export function deleteSubmission(id) {
  return request({ url: base + '/' + id, method: 'delete' })
}

export function submitSubmission(id) {
  return request({ url: base + '/' + id + '/submit', method: 'put' })
}

export function withdrawSubmission(id, reason) {
  return request({ url: base + '/' + id + '/withdraw', method: 'put', data: { opinion: reason } })
}

export function resubmitSubmission(id, opinion) {
  return request({ url: base + '/' + id + '/resubmit', method: 'put', data: { opinion } })
}

export function approveSubmission(id, opinion) {
  return request({ url: base + '/' + id + '/approve', method: 'put', data: { opinion } })
}

export function rejectSubmission(id, opinion) {
  return request({ url: base + '/' + id + '/reject', method: 'put', data: { opinion } })
}

export function cancelSubmissionApproval(id, opinion) {
  return request({ url: base + '/' + id + '/cancel-approve', method: 'put', data: { opinion } })
}

export function listSubmissionAttachments(id) {
  return request({ url: base + '/' + id + '/attachments', method: 'get' })
}

export function addSubmissionAttachment(id, data) {
  return request({ url: base + '/' + id + '/attachments', method: 'post', data })
}

export function deleteSubmissionAttachment(attachmentId) {
  return request({ url: base + '/attachment/' + attachmentId, method: 'delete' })
}

export function downloadSubmissionAttachment(attachmentId) {
  return request({
    url: base + '/attachment/' + attachmentId + '/download',
    method: 'get',
    responseType: 'blob'
  })
}

export function listSubmissionAudits(id) {
  return request({ url: base + '/' + id + '/audits', method: 'get' })
}
