<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Delete,
  Document,
  Download,
  Folder,
  FolderOpened,
  Plus,
  Refresh,
  UploadFilled,
  View,
} from '@element-plus/icons-vue'
import {
  createBucket,
  deleteBucket,
  deleteFile,
  getBuckets,
  getDownloadUrl,
  getFileConfig,
  listFiles,
  uploadFile,
} from '@/api/system/file'
import type { MinioFileConfig, MinioObject } from '@/types/api'

const loading = ref(false)
const uploading = ref(false)
const config = ref<MinioFileConfig | null>(null)
const buckets = ref<string[]>([])
const bucket = ref('')
const prefix = ref('')
const objects = ref<MinioObject[]>([])
const fileInput = ref<HTMLInputElement>()
const bucketDialogVisible = ref(false)
const bucketSubmitting = ref(false)
const bucketForm = reactive({ bucketName: '' })

type PreviewKind = 'image' | 'video' | 'audio' | 'pdf' | 'text' | 'other'

const previewVisible = ref(false)
const previewLoading = ref(false)
const previewUrl = ref('')
const previewName = ref('')
const previewKind = ref<PreviewKind>('other')
const previewText = ref('')
const previewItem = ref<MinioObject | null>(null)

const imageExts = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'avif'])
const videoExts = new Set(['mp4', 'webm', 'ogg', 'mov', 'm4v', 'avi', 'mkv'])
const audioExts = new Set(['mp3', 'wav', 'ogg', 'flac', 'm4a', 'aac'])
const textExts = new Set([
  'txt', 'md', 'json', 'xml', 'csv', 'log', 'yml', 'yaml', 'js', 'ts', 'jsx', 'tsx',
  'java', 'sql', 'html', 'css', 'sh', 'bat', 'ps1', 'properties', 'conf', 'ini',
])

const breadcrumbs = computed(() =>
  prefix.value
    .split('/')
    .filter(Boolean)
    .map((name, index, all) => ({
      name,
      prefix: `${all.slice(0, index + 1).join('/')}/`,
    })),
)

async function loadBuckets() {
  config.value = await getFileConfig()
  buckets.value = await getBuckets()
  if (!bucket.value || !buckets.value.includes(bucket.value)) {
    bucket.value = buckets.value.includes(config.value.defaultBucket)
      ? config.value.defaultBucket
      : (buckets.value[0] ?? '')
  }
  prefix.value = ''
  await loadFiles()
}

async function loadFiles() {
  if (!bucket.value) {
    objects.value = []
    return
  }
  loading.value = true
  try {
    objects.value = await listFiles(bucket.value, prefix.value)
  } finally {
    loading.value = false
  }
}

function handleBucketChange() {
  prefix.value = ''
  loadFiles()
}

function openBucketDialog() {
  bucketForm.bucketName = ''
  bucketDialogVisible.value = true
}

async function submitBucket() {
  const name = bucketForm.bucketName.trim()
  if (!/^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$/.test(name)) {
    ElMessage.warning('Bucket 只能包含小写字母、数字、点和短横线，长度 3-63')
    return
  }
  bucketSubmitting.value = true
  try {
    await createBucket(name)
    ElMessage.success('Bucket 创建成功')
    bucketDialogVisible.value = false
    await loadBuckets()
    bucket.value = name
    prefix.value = ''
    await loadFiles()
  } finally {
    bucketSubmitting.value = false
  }
}

async function removeBucket() {
  if (!bucket.value) {
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除 Bucket「${bucket.value}」吗？Bucket 必须为空才能删除。`,
      '删除 Bucket',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deleteBucket(bucket.value)
    ElMessage.success('Bucket 已删除')
    bucket.value = ''
    await loadBuckets()
  } catch {
    // 用户取消或后端提示
  }
}

function enterDirectory(item: MinioObject) {
  prefix.value = item.objectName
  loadFiles()
}

function goRoot() {
  prefix.value = ''
  loadFiles()
}

function goBreadcrumb(target: string) {
  prefix.value = target
  loadFiles()
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFiles(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  if (!files.length || !bucket.value) {
    return
  }
  uploading.value = true
  let uploaded = 0
  try {
    for (const file of files) {
      if (file.size > 100 * 1024 * 1024) {
        ElMessage.warning(`${file.name} 超过 100MB，已跳过`)
        continue
      }
      await uploadFile(bucket.value, prefix.value, file)
      uploaded += 1
    }
    if (uploaded > 0) {
      ElMessage.success(`已上传 ${uploaded} 个文件`)
      await loadFiles()
    }
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function downloadObject(item: MinioObject) {
  const url = await getDownloadUrl(bucket.value, item.objectName)
  window.open(url, '_blank', 'noopener')
}

function extensionOf(name: string): string {
  const index = name.lastIndexOf('.')
  return index >= 0 ? name.slice(index + 1).toLowerCase() : ''
}

function previewKindFor(name: string): PreviewKind {
  const ext = extensionOf(name)
  if (imageExts.has(ext)) return 'image'
  if (videoExts.has(ext)) return 'video'
  if (audioExts.has(ext)) return 'audio'
  if (ext === 'pdf') return 'pdf'
  if (textExts.has(ext)) return 'text'
  return 'other'
}

function previewKindLabel(kind: PreviewKind): string {
  return {
    image: '图片预览',
    video: '视频预览',
    audio: '音频预览',
    pdf: 'PDF 预览',
    text: '文本预览',
    other: '文件预览',
  }[kind]
}

async function openPreview(item: MinioObject) {
  previewItem.value = item
  previewName.value = item.name
  previewKind.value = previewKindFor(item.name)
  previewText.value = ''
  previewUrl.value = ''
  previewVisible.value = true
  previewLoading.value = true
  try {
    const url = await getDownloadUrl(bucket.value, item.objectName)
    previewUrl.value = url
    if (previewKind.value === 'text') {
      try {
        const response = await fetch(url)
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }
        const text = await response.text()
        previewText.value = text.length > 300_000
          ? `${text.slice(0, 300_000)}\n\n……内容过长，仅展示前 300000 个字符`
          : text
      } catch {
        previewKind.value = 'other'
      }
    }
  } catch {
    ElMessage.error('获取文件预览地址失败')
    previewVisible.value = false
  } finally {
    previewLoading.value = false
  }
}

function handleRowDblClick(row: MinioObject) {
  if (row.dir) {
    enterDirectory(row)
  } else {
    openPreview(row)
  }
}

function downloadPreview() {
  if (previewItem.value) {
    downloadObject(previewItem.value)
  }
}

function openPreviewInNewWindow() {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank', 'noopener')
  }
}

async function removeObject(item: MinioObject) {
  try {
    await ElMessageBox.confirm(`确定删除文件「${item.name}」吗？`, '删除文件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteFile(bucket.value, item.objectName)
    ElMessage.success('文件已删除')
    await loadFiles()
  } catch {
    // 用户取消或后端提示
  }
}

function formatSize(size: number): string {
  if (!size) {
    return '—'
  }
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  if (size < 1024 * 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(1)} MB`
  }
  return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`
}

onMounted(loadBuckets)
</script>

<template>
  <div class="page">
    <div class="page-heading">
      <div>
        <h1 class="page-title">文件管理</h1>
        <p class="page-subtitle">基于 MinIO 的对象存储管理，支持 Bucket、目录和文件上传下载。</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadBuckets">刷新</el-button>
        <el-button
          v-permission="'system:file:upload'"
          type="primary"
          :icon="UploadFilled"
          :loading="uploading"
          :disabled="!bucket"
          @click="triggerUpload"
        >
          上传文件
        </el-button>
        <input ref="fileInput" type="file" multiple hidden @change="handleFiles" />
      </div>
    </div>

    <div class="card">
      <div class="toolbar file-toolbar">
        <div class="toolbar-left bucket-control">
          <span class="toolbar-label">Bucket</span>
          <el-select v-model="bucket" placeholder="选择 Bucket" style="width: 210px" @change="handleBucketChange">
            <el-option v-for="item in buckets" :key="item" :label="item" :value="item" />
          </el-select>
          <el-button v-permission="'system:file:bucket'" :icon="Plus" @click="openBucketDialog">新建</el-button>
          <el-button
            v-permission="'system:file:bucket'"
            type="danger"
            plain
            :icon="Delete"
            :disabled="!bucket"
            @click="removeBucket"
          >
            删除 Bucket
          </el-button>
        </div>
        <div class="toolbar-right">
          <span class="text-muted">对象存储文件管理</span>
        </div>
      </div>

      <div class="path-bar">
        <button type="button" class="path-link" @click="goRoot">
          <el-icon><FolderOpened /></el-icon>
          根目录
        </button>
        <template v-for="crumb in breadcrumbs" :key="crumb.prefix">
          <span class="path-separator">/</span>
          <button type="button" class="path-link" @click="goBreadcrumb(crumb.prefix)">
            {{ crumb.name }}
          </button>
        </template>
        <span class="path-meta">共 {{ objects.length }} 项</span>
      </div>

      <div class="table-wrap file-table-wrap">
        <el-table v-loading="loading" :data="objects" row-key="objectName" @row-dblclick="handleRowDblClick">
          <el-table-column label="名称" min-width="360">
            <template #default="{ row }">
              <button v-if="row.dir" type="button" class="file-name folder" @click="enterDirectory(row as MinioObject)">
                <el-icon><Folder /></el-icon>
                <span>{{ row.name }}</span>
              </button>
              <div v-else class="file-name">
                <el-icon><Document /></el-icon>
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="120" align="right">
            <template #default="{ row }">
              {{ row.dir ? '—' : formatSize(row.size) }}
            </template>
          </el-table-column>
          <el-table-column label="修改时间" width="180">
            <template #default="{ row }">
              <span class="text-muted">{{ row.lastModified || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <template v-if="!row.dir">
                <el-tooltip content="预览" placement="top">
                  <el-button
                    link
                    type="primary"
                    :icon="View"
                    aria-label="预览"
                    @click="openPreview(row as MinioObject)"
                  />
                </el-tooltip>
                <el-tooltip content="下载" placement="top">
                  <el-button
                    link
                    type="primary"
                    :icon="Download"
                    aria-label="下载"
                    @click="downloadObject(row as MinioObject)"
                  />
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button
                    v-permission="'system:file:delete'"
                    link
                    type="danger"
                    :icon="Delete"
                    aria-label="删除"
                    @click="removeObject(row as MinioObject)"
                  />
                </el-tooltip>
              </template>
              <span v-else class="text-muted">目录</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="当前目录暂无文件" :image-size="80" />
          </template>
        </el-table>
      </div>
    </div>

    <el-dialog
      v-model="previewVisible"
      :title="previewName || '文件预览'"
      width="min(960px, 94vw)"
      destroy-on-close
      class="preview-dialog"
      @closed="previewUrl = ''; previewText = ''; previewItem = null"
    >
      <div class="preview-toolbar">
        <el-tag size="small" effect="plain">{{ previewKindLabel(previewKind) }}</el-tag>
        <span class="text-muted preview-path">{{ previewItem?.objectName }}</span>
        <div class="preview-actions">
          <el-button :icon="Download" @click="downloadPreview">下载</el-button>
          <el-button :icon="View" @click="openPreviewInNewWindow">新窗口打开</el-button>
        </div>
      </div>
      <div v-loading="previewLoading" class="preview-body">
        <template v-if="previewUrl">
          <img
            v-if="previewKind === 'image'"
            :src="previewUrl"
            :alt="previewName"
            class="preview-image"
          />
          <video
            v-else-if="previewKind === 'video'"
            :src="previewUrl"
            class="preview-media"
            controls
          />
          <audio
            v-else-if="previewKind === 'audio'"
            :src="previewUrl"
            class="preview-audio"
            controls
          />
          <pre v-else-if="previewKind === 'text'" class="preview-text">{{ previewText }}</pre>
          <iframe
            v-else
            :src="previewUrl"
            :title="previewName"
            class="preview-frame"
          />
        </template>
        <el-empty v-else-if="!previewLoading" description="暂不支持预览该文件，请下载后查看" :image-size="70" />
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bucketDialogVisible" title="新建 Bucket" width="460px" destroy-on-close>
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="Bucket 名称">
          <el-input
            v-model="bucketForm.bucketName"
            placeholder="例如 living-frame-files"
            maxlength="63"
            @keyup.enter="submitBucket"
          />
          <span class="dialog-hint">只能包含小写字母、数字、点和短横线，长度 3-63。</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bucketDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="bucketSubmitting" @click="submitBucket">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.heading-actions {
  display: flex;
  gap: 8px;
}

.file-toolbar {
  align-items: center;
}

.bucket-control {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-label {
  color: #8d96a8;
  font-size: 12px;
}

.path-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
  margin: 16px 18px 12px;
  padding: 10px 12px;
  border: 1px solid #eef1f7;
  border-radius: 10px;
  background: #fbfcff;
}

.path-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  color: #4f6bfb;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}

.path-link:hover {
  text-decoration: underline;
}

.path-separator {
  color: #c4cad6;
}

.path-meta {
  margin-left: auto;
  color: #9aa3b5;
  font-size: 11px;
}

.file-name {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #263042;
  font-size: 13px;
}

.file-name.folder {
  padding: 0;
  border: none;
  color: #4f6bfb;
  background: transparent;
  cursor: pointer;
}

.file-name.folder:hover span {
  text-decoration: underline;
}

.file-name .el-icon {
  color: #8d96a8;
  font-size: 16px;
}

.file-name.folder .el-icon {
  color: #f0a020;
}

.dialog-hint {
  display: block;
  margin-top: 4px;
  color: #9aa3b5;
  font-size: 10px;
}

.file-table-wrap {
  padding-bottom: 16px;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.preview-path {
  flex: 1;
  overflow: hidden;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-actions {
  display: flex;
  gap: 6px;
}

.preview-body {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  height: 62vh;
  overflow: auto;
  border: 1px solid #eef1f7;
  border-radius: 10px;
  background: #f7f8fc;
}

.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.preview-media {
  width: 100%;
  max-height: 100%;
  background: #000000;
}

.preview-audio {
  width: min(520px, 90%);
}

.preview-text {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 18px;
  overflow: auto;
  color: #263042;
  background: #ffffff;
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: #ffffff;
}

@media (max-width: 760px) {
  .file-toolbar {
    align-items: flex-start;
  }

  .bucket-control {
    flex-wrap: wrap;
  }

  .path-bar {
    margin-left: 14px;
    margin-right: 14px;
  }
}
</style>
