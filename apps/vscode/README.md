# AntV Infographic Markdown Preview

## Introduction

**AntV Infographic Markdown Preview** brings syntax highlighting and live Markdown preview for [AntV Infographic](https://github.com/antvis/infographic) to **VS Code** and **Cursor**. It supports fenced `infographic` code blocks, standalone `.infographic` files, and a sidebar visual editor with export.

## Features

1. **Markdown**: Recognizes fenced code blocks with language `infographic`.
2. **Live preview**: Renders SVG in the Markdown preview via `@antv/infographic`.
3. **Syntax highlighting**: Infographic DSL in fences and `.infographic` files.
4. **Standalone `.infographic` files**: Custom explorer icon; opening a file can open the **Infographic Editor** Webview in the sidebar for visual editing, preview, and image export.
5. **Local workflow**: No external service—write and preview inside the editor.

---

## 简介

**AntV Infographic Markdown Preview** 在 **VS Code** / **Cursor** 中为 [AntV Infographic](https://github.com/antvis/infographic) 提供语法高亮与 Markdown 实时预览，支持 `infographic` 围栏代码块、独立的 `.infographic` 源文件，以及侧栏可视化编辑与导出。

## 功能

1. **Markdown 内嵌**：识别语言为 `infographic` 的围栏代码块。
2. **实时预览**：在 Markdown 预览中通过 `@antv/infographic` 渲染 SVG。
3. **语法高亮**：围栏内与 `.infographic` 文件中的 Infographic DSL 高亮。
4. **独立 `.infographic` 文件**：资源管理器专用图标；打开文件可在侧栏打开 **Infographic 编辑** Webview，用于可视化编辑、预览与导出图片。
5. **本地工作流**：无需外部服务，在编辑器内编写与预览。

## Preview

### Markdown

![Markdown](https://github.com/liushoukun/antv-infographic-markdown-preview-plugins/raw/HEAD/apps/vscode/media/demo/v-1.png)

### Standalone `.infographic` files

![Standalone .infographic](https://github.com/liushoukun/antv-infographic-markdown-preview-plugins/raw/HEAD/apps/vscode/media/demo/v-2.png)

## Usage

Add a fenced block like this in Markdown:

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

Save and open **Markdown Preview** to see the result.

### Standalone `.infographic` files

Save your DSL as a file with the `.infographic` extension. It shows an AntV icon in the explorer; when opened in the editor, the extension opens the **Infographic Editor** Webview in the sidebar automatically. Close the sidebar panel if you only need plain text editing.

## License

[MIT](./LICENSE)
