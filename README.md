# AntV Infographic Markdown Preview

VS Code 上的 AntV Infographic 扩展：支持在 Markdown 中内嵌预览，也支持独立的 `.infographic` 源文件。

## Features

1. **Markdown 内嵌支持**：识别 ` ```infographic ` 围栏代码块，在 Markdown 预览中调用 `@antv/infographic` 即时渲染 SVG。
2. **语法高亮**：为 Markdown 中 ` ```infographic ` 围栏与 `.infographic` 文件提供一致着色（TextMate 注入；v0.8.0 起修复编辑器内围栏块不着色问题）。
3. **独立 `.infographic` 文件（v0.7.0+）**：资源管理器专用图标；打开工作区内的文件时，侧栏自动打开 **Infographic 编辑** Webview，便于可视化编辑、预览与导出图片等（与从 Markdown 打开的临时编辑缓冲区相区分）。

## Demo

![Demo](./media/demo/demo-1.png)

## Usage

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

然后打开 Markdown 预览即可查看渲染结果。  
可直接使用示例文件：`examples/sample.md`。

### 独立 `.infographic` 文件

将 DSL 保存为扩展名为 `.infographic` 的文件（例如 `examples/sample.infographic`）。在资源管理器中会有 AntV 图标；在编辑器中打开该文件后，扩展会自动在侧栏打开 **Infographic 编辑** Webview，便于可视化编辑、预览与导出图片。若仅需文本编辑，可照常关闭侧栏面板。

## License

[MIT](LICENSE)
