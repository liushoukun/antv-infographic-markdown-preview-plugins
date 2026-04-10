# AntV Infographic Markdown Preview

在 **VS Code / Cursor** 的 Markdown 中为 [AntV Infographic](https://github.com/antvis/infographic) 提供语法高亮与预览渲染，并支持独立的 `.infographic` 源文件与侧栏可视化编辑。

## 功能

1. **Markdown 内嵌**：识别 ` ```infographic ` 围栏代码块，在 Markdown 预览中调用 `@antv/infographic` 渲染 SVG。
2. **语法高亮**：为 Markdown 中的 ` ```infographic ` 围栏与 `.infographic` 文件提供一致着色（TextMate 注入）。
3. **独立 `.infographic` 文件**：资源管理器专用图标；在工作区中打开该类型文件时，侧栏可打开 **Infographic 编辑** Webview，便于可视化编辑、预览与导出图片等。

## 预览示意

![Demo](./media/demo/demo-1.png)

## 使用

在 Markdown 中加入如下代码块：

````markdown
```infographic
infographic list-row-simple-horizontal-arrow
data
  lists
    - label Step 1
      desc Start
    - label Step 2
      desc In Progress
```
````

保存后打开 **Markdown 预览** 即可查看渲染结果。

### 独立 `.infographic` 文件

将 DSL 保存为扩展名为 `.infographic` 的文件。在资源管理器中会有 AntV 图标；在编辑器中打开后，扩展会自动在侧栏打开 **Infographic 编辑** Webview。若只需文本编辑，可关闭侧栏面板。

## 许可证


