import axios from 'axios'
import { Message } from 'element-ui'
import { saveAs } from 'file-saver'
import { getToken } from '@/utils/auth'
import { blobValidate } from '@/utils/ruoyi'
import errorCode from '@/utils/errorCode'

export function downloadFundAttachment(id, fileName) {
  return axios({
    method: 'get',
    url: process.env.VUE_APP_BASE_API + '/ruoyi-fund/fund/attachment/' + id + '/download',
    responseType: 'blob',
    headers: { Authorization: 'Bearer ' + getToken() }
  }).then(async response => {
    if (await blobValidate(response.data)) {
      saveAs(response.data, fileName || 'attachment')
      return
    }
    const text = await response.data.text()
    const result = JSON.parse(text)
    Message.error(errorCode[result.code] || result.msg || errorCode.default)
  }).catch(error => {
    Message.error((error && error.message) || '附件下载失败')
    return Promise.reject(error)
  })
}
