<template>
  <div class="fileManage" @contextmenu.prevent="">
    <el-dialog :title="'模板文件管理: ' + templateName"
               @close="forceSaveCurrentTemplateFileContent"
               :visible.sync="visible" :fullscreen="true">
      <el-header class="codePath">
        <template v-for="(node,index) in paths">
          <span :key="node.key" class="codePathCell">
            <i v-if="index>0"  class="el-icon-arrow-right"></i>
            <span>
              <svg-icon v-if="node.icon" className=""
                            :iconClass="node.icon"></svg-icon>
              {{node.name}}
            </span>
          </span>
        </template>
        <template v-if="currentFileStatus.visible">
          <span :class="currentFileStatus.class"
                class="file-status-cell">
            {{currentFileStatus.text}}
          </span>
          <el-button v-if="currentFileStatus.saveButton"
                     @click="forceSaveCurrentTemplateFileContent"
                     type="text" style="padding: 0 5px;"
                     size="mini">点击保存</el-button>
        </template>
      </el-header>
      <el-container ref="parent" class="codeContainer">
        <el-aside @contextmenu.native.prevent="showContextMenu" ref="aside" width="250px" class="codeAside">
          <el-scrollbar  style="height:100%">
            <el-tree ref="codeTree"
                     node-key="path"
                     :default-expanded-keys="fileExpandedDirs"
                     :props="treeProps"
                     :data="codeTree.tree"
                     :expand-on-click-node="false"
                     @node-expand="expandSingleNode"
                     @node-contextmenu="showContextMenu"
                     @node-click="nodeClick">
              <span slot-scope="{ node, data }"
                    class="codeTreeNode"
                    @dblclick="codeTreeNodeDblclick(data, node)">
                <svg-icon :iconClass="getCodeTreeNodeIcon(data).icon"
                          :className="getCodeTreeNodeIcon(data).className"></svg-icon>
                <span style="margin-left: 3px;">{{node.label}}</span>
              </span>
            </el-tree>
          </el-scrollbar>
        </el-aside>
        <div ref="splitLine" @mousedown="splitLineMousedown" class="splitLine"></div>
        <el-container ref="main">
          <el-main class="codeMain" v-loading="fileLoading">
            <vue-codemirror v-if="!currentFile.binary"
                            v-model="currentFile.content"
                            :options="cmOptions"
                            @input="fileContentChange"></vue-codemirror>
            <div v-else class="binary-display color-warning">
              <span class="binary-file-name color-primary" >{{currentFile.fileName}}</span>
              是二进制文件，不支持预览
            </div>
          </el-main>
        </el-container>
      </el-container>
    </el-dialog>
    <vue-simple-context-menu
      :options="contextMenuOptions"
      :showIcon="true"
      ref="contextMenu"
      menuClassName="codeContextMenu"
      @option-clicked="contextMenuOptionClicked"
    />
    <el-dialog :title="formTitleDisplay" :visible.sync="templateFileFormVisible" width="550px">
      <el-form ref="templateFileForm"
               :rules="templateFileRules"
               class="addTemplateForm"
               v-loading="templateFileFormLoading"
               :model="templateFileForm" size="small">
        <el-form-item v-if="templateFileFormMode !== 'upload'"
                      prop="fileName" label="文件名：" label-width="120px">
          <el-input style="width:300px;" v-model="templateFileForm.fileName"
                    placeholder="例如：xxxx.ftl"
                    tabindex="10"></el-input>
        </el-form-item>
        <el-form-item prop="fileDir" label="目录：" label-width="120px">
          <el-input style="width:300px;" v-model="templateFileForm.fileDir"
                    placeholder="例如：/aaa/bbb"
                    tabindex="20"></el-input>
        </el-form-item>
        <el-form-item v-if="!templateFileForm.binary"
                      label="是否抽象文件：" label-width="120px">
          <el-radio-group v-model="templateFileForm.abstracted">
            <el-radio border :label="true">是</el-radio>
            <el-radio border :label="false">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!templateFileForm.binary && !templateFileForm.abstracted"
                      label="上下文类型：" label-width="120px">
          <el-radio-group v-model="templateFileForm.contextType">
            <el-radio v-for="obj in contextType"
                      :key="obj.value"
                      :label="obj.value" border>{{obj.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <template v-if="templateFileFormMode !== 'upload'">
          <el-button @click="templateFileFormVisible = false">取 消</el-button>
          <el-button type="success" @click="handleSaveTemplateFile">{{formButtonDisplay}}</el-button>
        </template>
        <el-upload
          v-else
          :action="templateFileUploadUrl"
          :data="templateFileUploadParams"
          :before-upload="beforeUpload"
          :on-success="onUploadSuccess"
          :on-progress="onUploadProgress"
          :on-error="onUploadError"
          :show-file-list="false">
          <el-button size="small" type="primary">选择文件并上传</el-button>
        </el-upload>
      </div>
    </el-dialog>
  </div>
</template>

<script>
/**
 * 代码编辑器
 * https://codemirror.net/
 */
import { codemirror } from 'vue-codemirror'
import 'codemirror/lib/codemirror.css'
import 'codemirror/theme/base16-dark.css'
import 'codemirror/mode/xml/xml.js'
/**
 * 右键菜单组件
 * https://github.com/johndatserakis/vue-simple-context-menu
 */
import VueSimpleContextMenu from '@/components/VueSimpleContextMenu'
import { initTemplateFileFormBean, getTemplateFileRulesRules } from './model'
import options from '@/utils/options'
import { getExpandedNodes } from '@/utils/element-tree-util'
import FileTypeUtil from '@/utils/file-type-util'
import templateApi from '@/api/template'
import { apiPath } from '@/utils/request'

// 如果文件保存中，则等待多久以后再提交
const savingWaitTime = 2000
// 文件变更以后，等待多久以后再提交
const changWaitTime = 10000
const menuOptions1 = [
  {
    name: '新建模板文件',
    value: 'addTemplateFile',
    svgIcon: 'add-file',
    svgClassName: 'color-success'
  },
  {
    name: '上传二进制文件',
    value: 'addBinaryFile',
    svgIcon: 'upload',
    svgClassName: 'color-warning'
  }
]
const menuOptions2 = [
  {
    name: '新建模板文件',
    value: 'addTemplateFile',
    svgIcon: 'add-file',
    svgClassName: 'color-success'
  },
  {
    name: '上传二进制文件',
    value: 'addBinaryFile',
    svgIcon: 'upload',
    svgClassName: 'color-warning'
  },
  {
    name: '修改文件属性',
    value: 'editTemplateFile',
    svgIcon: 'edit-file',
    svgClassName: 'color-primary'
  },
  {
    name: '删除模板文件',
    value: 'deleteTemplateFile',
    svgIcon: 'delete-file',
    svgClassName: 'color-danger'
  }
]

/**
 * 打印日志函数
 */
function log (value) {
  // console.info(value)
}

/**
 * 初始化数据
 */
function initData () {
  return JSON.parse(JSON.stringify({
    templateId: null,
    templateName: '',
    treeProps: {
      children: 'children',
      label: 'name'
    },
    // 后端返回的代码目录数据
    codeTree: {
      templateId: null,
      templateVersion: null,
      tree: []
    },
    // 默认展开的节点
    fileExpandedDirs: [],
    cmOptions: {
      mode: FileTypeUtil.getCmMode('ftl'),
      theme: 'base16-dark',
      lineNumbers: true,
      line: true
    },
    currentNode: null,
    // 文件加载后跳过chang事件
    loadedSkipChange: true,
    // 当前正在进行文本编辑的文件
    currentFile: initTemplateFileFormBean(),
    // 当前文件内容是否变更过
    currentFileChange: false,
    // 文件是否需要保存
    currentFileNeedSave: false,
    // 定时保存文件的延迟任务id
    currentFileSavingTaskId: null,
    // 文件保存的promise
    currentFileSavingPromise: null,
    paths: [],
    fileLoading: false,
    // 当前模板文件编辑窗口是否可见
    visible: false,
    contextMenuOptions: menuOptions1,
    // 是否显示添加模板文件表单
    templateFileFormVisible: false,
    // 添加模板文件表单是否加载中
    templateFileFormLoading: false,
    // 模板文件表单模式
    templateFileFormMode: 'add',
    // 模板文件上传路径
    templateFileUploadUrl: `/${apiPath}/template_file/upload`,
    templateFileUploadParams: {
      fileDir: '',
      templateId: null
    },
    // 添加模板文件表单
    templateFileForm: initTemplateFileFormBean(),
    // 上下文类型
    contextType: options.contextType,
    // 模板文件校验规则
    templateFileRules: getTemplateFileRulesRules()
  }))
}

export default {
  name: 'template-files',
  components: {
    'vue-codemirror': codemirror,
    'vue-simple-context-menu': VueSimpleContextMenu
  },
  data () {
    return initData()
  },
  computed: {
    /**
     * 文件状态
     */
    currentFileStatus () {
      // 保存中
      if (this.currentFileSavingPromise) {
        return {
          visible: true,
          class: 'color-primary',
          text: '保存中',
          saveButton: false
        }
      }
      // 未保存
      if (this.currentFileNeedSave) {
        return {
          visible: true,
          class: 'color-warning',
          text: '未保存',
          saveButton: true
        }
      }
      // 已保存
      if (this.currentFileChange) {
        return {
          visible: true,
          class: 'color-success',
          text: '已保存',
          saveButton: false
        }
      }
      // 默认值
      return {
        visible: false,
        class: '',
        text: '',
        saveButton: false
      }
    },
    /**
     * 表单标题
     */
    formTitleDisplay () {
      if (this.templateFileFormMode === 'edit') {
        return '修改文件属性'
      } else if (this.templateFileFormMode === 'upload') {
        return '上传二进制文件'
      } else {
        return '新建模板文件'
      }
    },
    /**
     * 表单按钮文字
     */
    formButtonDisplay () {
      if (this.templateFileFormMode === 'edit') {
        return '确 认'
      } else if (this.templateFileFormMode === 'upload') {
        return '上 传'
      } else {
        return '新 建'
      }
    }
  },
  methods: {
    /**
     * item: 文件节点信息
     * option: 菜单项
     */
    contextMenuOptionClicked ({ item, option }) {
      if (option.value === 'addTemplateFile') {
        this.templateFileFormVisible = true
        this.templateFileFormLoading = false
        this.templateFileFormMode = 'add'
        this.templateFileForm = initTemplateFileFormBean()
        if (item) {
          // 如果在目录上右击新建，则自动填充该目录
          if (item.dir) {
            this.templateFileForm.fileDir = item.path
          } else {
            // 如果在文件上右击新建，则自动填充文件所在目录
            this.templateFileForm.fileDir = item.info.fileDir
          }
        }
      } else if (option.value === 'addBinaryFile') {
        this.templateFileFormVisible = true
        this.templateFileFormLoading = false
        this.templateFileFormMode = 'upload'
        this.templateFileForm = initTemplateFileFormBean()
        this.templateFileForm.binary = true
        if (item) {
          // 如果在目录上右击新建，则自动填充该目录
          if (item.dir) {
            this.templateFileForm.fileDir = item.path
          } else {
            // 如果在文件上右击新建，则自动填充文件所在目录
            this.templateFileForm.fileDir = item.info.fileDir
          }
        }
      } else if (option.value === 'editTemplateFile') {
        this.templateFileFormVisible = true
        this.templateFileFormLoading = true
        this.templateFileFormMode = 'edit'
        this.templateFileForm = initTemplateFileFormBean(true)
        // 加载远程数据
        this.queryFileInfo(item.info.fileId,
          file => {
            for (const key in initTemplateFileFormBean(true)) {
              this.templateFileForm[key] = file[key]
            }
          },
          () => {
            this.templateFileFormLoading = false
          })
      } else if (option.value === 'deleteTemplateFile') {
        // 删除模板文件
        this.handleDeleteTemplateFile(item.info.fileId)
      }
    },
    beforeUpload () {
      this.templateFileUploadParams = {
        templateId: this.templateId,
        fileDir: this.templateFileForm.fileDir
      }
      return true
    },
    onUploadSuccess (response, file, fileList) {
      this.templateFileFormVisible = false
      this.templateFileFormLoading = false
      this.$common.showMsg('success', '上传成功')
      this.queryCodeTree(this.templateId)
    },
    onUploadProgress (event, file, fileList) {
      this.templateFileFormLoading = true
    },
    onUploadError (error, file, fileList) {
      this.templateFileFormLoading = false
      this.$common.showNotifyError(JSON.parse(error.message))
    },
    /**
     * event: 右击事件
     * item: 文件节点信息
     */
    showContextMenu (event, item) {
      this.contextMenuOptions = item && !item.dir ? menuOptions2 : menuOptions1
      this.$refs.contextMenu.showMenu(event, item)
    },
    show (templateId, templateName) {
      Object.assign(this, initData())
      this.visible = true
      this.templateId = templateId
      this.templateName = templateName
      this.queryCodeTree(templateId)
    },
    /**
     * 菜单自由伸缩
     */
    splitLineMousedown (e) {
      const parent = this.$refs.parent.$el
      const aside = this.$refs.aside.$el
      const main = this.$refs.main.$el
      const splitLine = this.$refs.splitLine
      const disX = e.clientX
      splitLine.left = splitLine.offsetLeft
      document.onmousemove = function (e) {
        let iT = splitLine.left + (e.clientX - disX)
        const maxT = parent.clientWight - splitLine.offsetWidth
        splitLine.style.margin = 0
        iT < 0 && (iT = 0)
        iT > maxT && (iT = maxT)
        splitLine.style.left = aside.style.width = iT + 'px'
        main.style.width = parent.clientWidth - iT + 'px'
        return false
      }
      document.onmouseup = function () {
        document.onmousemove = null
        document.onmouseup = null
        splitLine.releaseCapture && splitLine.releaseCapture()
      }
      splitLine.setCapture && splitLine.setCapture()
      return false
    },
    queryCodeTree (templateId) {
      return templateApi.getDirTree(templateId)
        .then(data => this.setCodeTree(data))
        .catch(error => this.$common.showNotifyError(error))
    },
    /**
     * 设置目录树
     */
    setCodeTree (codeTree) {
      const expandedNodes = getExpandedNodes(this.$refs.codeTree)
      this.fileExpandedDirs = expandedNodes.map(node => node.path)
      this.codeTree = codeTree
    },
    /**
     * 目录树节点图标
     */
    getCodeTreeNodeIcon (data) {
      const svgIcon = {
        icon: '',
        className: ''
      }
      if (data.dir) {
        svgIcon.icon = FileTypeUtil.getIcon('folder')
      } else {
        svgIcon.icon = FileTypeUtil.getIcon(data.type)
        if (data.info.abstracted) {
          // 抽象文件-红色图标
          svgIcon.className += 'color-danger'
        } else {
          if (data.info.contextType === 2) {
            // 实体上下文-蓝色图标
            svgIcon.className += 'color-primary'
          } else if (data.info.contextType === 3) {
            // 枚举上下文-黄色图标
            svgIcon.className += 'color-warning'
          }
        }
      }
      return svgIcon
    },
    /**
     * 双击树节点
     */
    codeTreeNodeDblclick (data, node) {
      if (!node.isLeaf) {
        const expanded = node.expanded
        node.expanded = !expanded
        if (!expanded) {
          this.expandSingleNode(data, node)
        }
      }
    },
    parsePath (data) {
      this.paths = [{
        name: this.templateName,
        key: this.templateName,
        icon: FileTypeUtil.getIcon('folder')
      }]
      const paths = data.path.split('/').filter(p => p)
      for (let i = 0; i < paths.length; i++) {
        let p = paths[i]
        const icon = i < paths.length - 1 ? 'folder' : data.type
        const item = {
          name: p,
          key: i + '_' + p,
          icon: FileTypeUtil.getIcon(icon)
        }
        this.paths.push(item)
      }
    },
    queryFileInfo (fileId, callback, onComplete) {
      return templateApi.getTemplateFile(fileId)
        .then(file => callback(file))
        .catch(error => this.$common.showNotifyError(error))
        .finally(() => {
          if (onComplete) {
            onComplete()
          }
        })
    },
    nodeClick (data, node) {
      if (this.currentNode === data) {
        return
      }
      if (data.dir) {
        return
      }
      this.forceSaveCurrentTemplateFileContent()
        .then(() => this.showNodeFile(data))
    },
    showNodeFile (data) {
      this.currentNode = data
      this.parsePath(data)
      this.fileLoading = true
      this.queryFileInfo(data.info.fileId,
        file => {
          this.cmOptions.mode = FileTypeUtil.getCmMode(data.type)
          this.setCurrentFile(file)
        },
        () => {
          this.fileLoading = false
        })
    },
    /**
     * 设置当前文件
     */
    setCurrentFile (file) {
      this.loadedSkipChange = true
      this.currentFile = file
      this.currentFileChange = false
    },
    /**
     * 展开所有单节点下级
     */
    expandSingleNode (data, node) {
      function recursiveExpand (n) {
        if (n.childNodes.length === 1) {
          const childNode = n.childNodes[0]
          childNode.expanded = true
          recursiveExpand(childNode)
        }
      }
      recursiveExpand(node)
    },
    /**
     * 删除模板文件
     */
    handleDeleteTemplateFile (fileId) {
      this.$common.confirm('是否确认删除')
        .then(() => this.forceSaveCurrentTemplateFileContent())
        .then(() => templateApi.deleteTemplateFile(fileId))
        .then(() => {
          if (fileId === this.currentFile.fileId) {
            this.setCurrentFile(initTemplateFileFormBean())
          }
        })
        .then(() => this.queryCodeTree(this.templateId))
        .catch(error => this.$common.showNotifyError(error))
    },
    /**
     * 保存模板文件
     * 不包含文件内容
     */
    handleSaveTemplateFile () {
      this.$refs.templateFileForm.validate()
        .then(() => {
          const templateFileDTO = Object.assign({}, this.templateFileForm, { templateId: this.templateId })
          return templateApi.saveOrUpdateTemplateFile(templateFileDTO, this.templateFileFormMode === 'edit')
        })
        .then(() => {
          this.templateFileFormVisible = false
        })
        .then(() => this.queryCodeTree(this.templateId))
        .catch(error => this.$common.showNotifyError(error))
    },
    /**
     * 文件内容变更
     */
    fileContentChange () {
      if (this.loadedSkipChange) {
        log('跳过首次加载变更事件')
        this.loadedSkipChange = false
        return
      }
      log('内容变更')
      this.currentFileChange = true
      if (!this.currentFileNeedSave) {
        this.currentFileNeedSave = true
        this.currentFileSavingTaskId = setTimeout(() => {
          this.currentFileSavingTaskId = null
          this.saveCurrentTemplateFileContent()
        }, changWaitTime)
        log(changWaitTime / 1000 + '秒之后再尝试保存,taskId=' + this.currentFileSavingTaskId)
      }
    },
    /**
     * 保存当前模板文件内容
     */
    saveCurrentTemplateFileContent () {
      // 如果文件无改动，则不处理
      if (!this.currentFileNeedSave) {
        log('尝试保存，文件无变化')
        return
      }
      // 如果文件正在异步保存中，则等待几秒再试
      if (this.currentFileSavingPromise) {
        this.currentFileSavingTaskId = setTimeout(() => {
          this.currentFileSavingTaskId = null
          this.saveCurrentTemplateFileContent()
        }, savingWaitTime)
        log(savingWaitTime / 1000 + '秒之后再尝试保存,taskId=' + this.currentFileSavingTaskId)
        return
      }
      this.currentFileNeedSave = false
      this.currentFileSavingPromise = this.doSaveTemplateFileContent(this.currentFile.fileId,
        this.currentFile.version, this.currentFile.content)
    },
    /**
     * 执行保存动作
     * @param fileId 文件id
     * @param version 乐观锁版本号
     * @param content 文件内容
     */
    doSaveTemplateFileContent (fileId, version, content) {
      log('执行保存')
      return templateApi.updateTemplateFileContent(fileId, version, content)
        .then(version => {
          // 保存成功以后把最新乐观锁版本号写回当前文件
          this.currentFile.version = version
        })
        .catch(error => this.$common.showNotifyError(error))
        .finally(() => {
          // 保存完成以后，清除promise
          this.currentFileSavingPromise = null
          log('保存成功')
        })
    },
    /**
     * 强制保存当前文件内容
     */
    forceSaveCurrentTemplateFileContent () {
      log('force保存')
      // 如果文件无改动，则不处理
      if (!this.currentFileNeedSave) {
        return Promise.resolve()
      }
      // 定义一个立即保存文件内容的函数
      const saveFunction = () => {
        // 先结束延迟任务
        if (this.currentFileSavingTaskId) {
          log('清理task:' + this.currentFileSavingTaskId)
          clearTimeout(this.currentFileSavingTaskId)
        }
        this.currentFileNeedSave = false
        return this.doSaveTemplateFileContent(this.currentFile.fileId,
          this.currentFile.version, this.currentFile.content)
      }
      // 如果当前正在保存中，则在保存完成后执行立即保存
      if (this.currentFileSavingPromise) {
        return this.currentFileSavingPromise.then(() => saveFunction())
      }
      // 如果当前并没有正在执行保存，则立即保存
      return saveFunction()
    }
  }
}
</script>

<style lang="scss">
  @import '../../assets/common.scss';
  @import '../../assets/coding-panel.scss';

  .fileManage {
    @include coding-panel;

    .addTemplateForm {
      padding: 20px 20px;
    }

    .el-dialog {
    }

    .file-status-cell {
      font-size: 12px;
      margin-left: 10px;
    }

    .binary-display {
      font-size: 20px;
      text-align:center;
      .binary-file-name {
        font-style:italic;
      }
    }

  }
</style>
