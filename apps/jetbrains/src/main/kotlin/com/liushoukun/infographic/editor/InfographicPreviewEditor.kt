package com.liushoukun.infographic.editor

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.Alarm
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Base64
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 独立 .infographic 预览：与 VS Code 相同的前端（editorWebview.js），含缩放、主题/色板、导出等。
 */
class InfographicPreviewEditor(
  private val project: Project,
  private val file: VirtualFile,
) : UserDataHolderBase(), FileEditor {

  private val panel = JPanel(BorderLayout())
  private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
  private var previewDir: Path? = null
  private val browser: JBCefBrowser? = if (JBCefApp.isSupported()) JBCefBrowser() else null
  private val jsQuery: JBCefJSQuery? = browser?.let { JBCefJSQuery.create(it) }
  private val gson = Gson()
  @Volatile
  private var webReady = false
  private val log = logger<InfographicPreviewEditor>()

  init {
    val b = browser
    val q = jsQuery
    if (b != null && q != null) {
      Disposer.register(this, q)
      wireJsBridge(q)
      panel.add(b.component, BorderLayout.CENTER)
      Disposer.register(this, b)
      ensureShellLoaded()
      FileDocumentManager.getInstance().getDocument(file)?.addDocumentListener(
        object : DocumentListener {
          override fun documentChanged(event: DocumentEvent) {
            schedulePushUpdate()
          }
        },
        this
      )
    } else {
      panel.add(
        JBLabel("当前运行环境不支持 JCEF，无法显示 Infographic 预览。"),
        BorderLayout.CENTER
      )
    }
  }

  private fun wireJsBridge(query: JBCefJSQuery) {
    query.addHandler { request ->
      try {
        val obj = gson.fromJson(request, JsonObject::class.java)
        val type = obj.get("type")?.asString
        when (type) {
          "ready" -> {
            webReady = true
            ApplicationManager.getApplication().invokeLater { pushUpdateToWebview() }
          }
          "visualEdit" -> {
            val content = obj.get("content")?.asString ?: ""
            ApplicationManager.getApplication().invokeLater { applyFromWebview(content) }
          }
          "exportPng" -> {
            val b64 = obj.get("pngBase64")?.asString ?: ""
            ApplicationManager.getApplication().invokeLater { savePng(b64) }
          }
          "exportSvg" -> {
            val svg = obj.get("svgText")?.asString ?: ""
            ApplicationManager.getApplication().invokeLater { saveSvg(svg) }
          }
          "error" -> {
            val msg = obj.get("message")?.asString ?: ""
            ApplicationManager.getApplication().invokeLater {
              Messages.showErrorDialog(project, msg, "Infographic")
            }
          }
          "showWarning" -> {
            val msg = obj.get("message")?.asString ?: ""
            ApplicationManager.getApplication().invokeLater {
              Messages.showWarningDialog(project, msg, "Infographic")
            }
          }
        }
      } catch (e: Exception) {
        log.warn("Infographic preview bridge", e)
      }
      JBCefJSQuery.Response("")
    }
  }

  private fun ensureShellLoaded() {
    val b = browser ?: return
    val q = jsQuery ?: return
    try {
      if (previewDir == null) {
        val dir = Files.createTempDirectory("antv-infographic-editor-preview-")
        copyResource("/web/editorWebview.js", dir.resolve("editorWebview.js"))
        previewDir = dir
      }
      webReady = false
      writeIndexHtml(q)
      b.loadURL(previewDir!!.resolve("index.html").toUri().toString())
    } catch (e: Exception) {
      log.warn("Infographic preview load failed", e)
    }
  }

  private fun copyResource(resourcePath: String, target: Path) {
    javaClass.getResourceAsStream(resourcePath)?.use { input ->
      Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
    } ?: log.warn("Missing resource $resourcePath")
  }

  private fun writeIndexHtml(query: JBCefJSQuery) {
    val dir = previewDir ?: return
    val template =
      javaClass.getResourceAsStream("/web/infographic-editor-host.html")?.use {
        it.reader(StandardCharsets.UTF_8).readText()
      } ?: error("missing /web/infographic-editor-host.html")
    val bodyClass = if (UIUtil.isUnderDarcula()) "jb-infographic-dark" else "jb-infographic-light"
    val bridge =
      """
      window.__antvInfographicEditorHost__ = {
        postMessage: function(msg) {
          ${query.inject("JSON.stringify(msg)")}
        }
      };
      """.trimIndent()
    val html =
      template
        .replace("__BODY_CLASS__", bodyClass)
        .replace("__BRIDGE_SCRIPT__", bridge)
    Files.writeString(dir.resolve("index.html"), html, StandardCharsets.UTF_8)
  }

  private fun schedulePushUpdate() {
    alarm.cancelAllRequests()
    alarm.addRequest({ pushUpdateToWebview() }, 280)
  }

  private fun pushUpdateToWebview() {
    val b = browser ?: return
    if (!webReady) {
      return
    }
    val doc = FileDocumentManager.getInstance().getDocument(file) ?: return
    val content = doc.text
    val payload =
      JsonObject().apply {
        addProperty("type", "update")
        addProperty("content", content)
        addProperty("width", "100%")
        addProperty("height", 480)
      }
    val json = gson.toJson(payload)
    val b64 = Base64.getEncoder().encodeToString(json.toByteArray(StandardCharsets.UTF_8))
    val js =
      """
      (function(){
        var b64 = '$b64';
        var bin = atob(b64);
        var bytes = new Uint8Array(bin.length);
        for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
        var text = new TextDecoder('utf-8').decode(bytes);
        window.postMessage(JSON.parse(text), '*');
      })();
      """.trimIndent()
    try {
      b.cefBrowser.executeJavaScript(js, b.cefBrowser.url, 0)
    } catch (e: Exception) {
      log.warn("pushUpdateToWebview failed", e)
    }
  }

  private fun applyFromWebview(text: String) {
    WriteCommandAction.runWriteCommandAction(project) {
      val doc = FileDocumentManager.getInstance().getDocument(file) ?: return@runWriteCommandAction
      doc.setText(text)
    }
  }

  private fun exportStem(): String {
    val stem = FileUtilRt.getNameWithoutExtension(file.name)
    return stem.ifEmpty { "infographic" }
  }

  private fun savePng(pngBase64: String) {
    val stem = exportStem()
    val descriptor = FileSaverDescriptor("导出 PNG", "选择保存位置", "png")
    val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
    val base = file.parent
    val target = dialog.save(base, "$stem.png") ?: return
    try {
      val path = target.file.toPath()
      Files.write(path, Base64.getDecoder().decode(pngBase64))
      Messages.showInfoMessage(project, "PNG 已保存。", "Infographic")
    } catch (e: Exception) {
      Messages.showErrorDialog(project, e.message ?: "写入失败", "保存 PNG 失败")
    }
  }

  private fun saveSvg(svgText: String) {
    val stem = exportStem()
    val descriptor = FileSaverDescriptor("导出 SVG", "选择保存位置", "svg")
    val dialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
    val base = file.parent
    val target = dialog.save(base, "$stem.svg") ?: return
    try {
      Files.writeString(target.file.toPath(), svgText, StandardCharsets.UTF_8)
      Messages.showInfoMessage(project, "SVG 已保存。", "Infographic")
    } catch (e: Exception) {
      Messages.showErrorDialog(project, e.message ?: "写入失败", "保存 SVG 失败")
    }
  }

  override fun dispose() {
    alarm.dispose()
    webReady = false
    previewDir?.let { dir ->
      try {
        FileUtil.delete(dir.toFile())
      } catch (_: Exception) {
      }
    }
    previewDir = null
  }

  override fun getComponent(): JComponent = panel

  override fun getPreferredFocusedComponent(): JComponent? = browser?.component

  override fun getName(): String = "Infographic Preview"

  override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE

  override fun setState(state: FileEditorState) {}

  override fun isModified(): Boolean = false

  override fun isValid(): Boolean = file.isValid

  override fun selectNotify() {
    schedulePushUpdate()
  }

  override fun deselectNotify() {}

  override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

  override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

  override fun getCurrentLocation(): FileEditorLocation? = null

  companion object {
    const val EDITOR_TAB_NAME = "Preview"
  }
}
