<template>
  <div class="fund-file-upload">
    <el-upload
      ref="upload"
      :action="uploadUrl"
      :headers="headers"
      :before-upload="beforeUpload"
      :on-error="uploadError"
      :on-success="uploadSuccess"
      :show-file-list="false"
    >
      <el-button size="mini" type="primary">选取文件</el-button>
      <div slot="tip" class="el-upload__tip">
        最多{{ limit }}个，单个不超过{{ fileSize }}MB，支持{{ fileType.join('/') }}
      </div>
    </el-upload>
    <ul class="el-upload-list el-upload-list--text">
      <li v-for="(file,index) in fileList" :key="file.token" class="el-upload-list__item is-success">
        <span class="el-icon-document"> {{ file.name }}</span>
        <el-link class="remove-link" type="danger" :underline="false" @click="remove(index)">删除</el-link>
      </li>
    </ul>
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'

export default {
  name: 'FundFileUpload',
  props: {
    value: { type: String, default: '' },
    limit: { type: Number, default: 5 },
    fileSize: { type: Number, default: 5 },
    fileType: { type: Array, default: () => ['pdf','doc','docx','xls','xlsx','jpg','jpeg','png'] }
  },
  data() {
    return {
      uploadUrl: process.env.VUE_APP_BASE_API + '/ruoyi-fund/fund/attachment/upload',
      headers: { Authorization: 'Bearer ' + getToken() },
      fileList: []
    }
  },
  watch: {
    value: {
      immediate: true,
      handler(value) {
        const tokens = value ? value.split(',').filter(Boolean) : []
        if (tokens.join(',') === this.fileList.map(file => file.token).join(',')) return
        this.fileList = tokens.map(token => ({ token, name: '已上传附件' }))
      }
    }
  },
  methods: {
    beforeUpload(file) {
      if (this.fileList.length >= this.limit) {
        this.$message.error('上传文件数量不能超过 ' + this.limit + ' 个')
        return false
      }
      const dot = file.name.lastIndexOf('.')
      const extension = dot >= 0 ? file.name.slice(dot + 1).toLowerCase() : ''
      if (this.fileType.indexOf(extension) < 0) {
        this.$message.error('文件格式不正确，请上传' + this.fileType.join('/') + '格式文件')
        return false
      }
      if (file.size / 1024 / 1024 > this.fileSize) {
        this.$message.error('上传文件大小不能超过 ' + this.fileSize + ' MB')
        return false
      }
      return true
    },
    uploadError() {
      this.$refs.upload.clearFiles()
      this.$message.error('上传失败，请重试')
    },
    uploadSuccess(response) {
      this.$refs.upload.clearFiles()
      if (!response || response.code !== 200 || !response.data || !response.data.token) {
        this.$message.error((response && response.msg) || '上传失败，请重试')
        return
      }
      this.fileList.push({ token: response.data.token, name: response.data.originalName || '附件' })
      this.emitValue()
      this.$message.success('上传成功')
    },
    remove(index) {
      this.fileList.splice(index, 1)
      this.emitValue()
    },
    emitValue() {
      this.$emit('input', this.fileList.map(file => file.token).join(','))
    }
  }
}
</script>

<style scoped>
.el-upload-list__item { display: flex; justify-content: space-between; padding-right: 8px; }
.remove-link { margin-left: 12px; }
</style>
