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

更多示例见 GitHub 仓库中的 [`examples`](https://github.com/liushoukun/antv-infographic-markdown-preview-plugins/tree/main/examples) 目录（如 `sample.md`、`sample.infographic`）。

## 设置

可在设置中搜索 **AntV Infographic**，调整侧栏编辑区画布宽度、高度等（对应 `antvInfographic.editorWidth`、`antvInfographic.editorHeight`）。

## 命令

- **编辑 Infographic 代码块**：在 Markdown 中聚焦 `infographic` 围栏时，可通过命令面板执行（见扩展贡献的命令列表）。

## 许可证

[MIT](LICENSE)

## 源码与协作

本扩展发布包来自 monorepo 中的 `apps/vscode`；克隆 [antv-infographic-markdown-preview-plugins](https://github.com/liushoukun/antv-infographic-markdown-preview-plugins) 后，在仓库根目录执行 `pnpm install` 与 `pnpm run build` 即可本地调试（详见仓库根目录 README）。
