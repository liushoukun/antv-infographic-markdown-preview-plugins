# 变更日志

## 0.8.0

- **修复** Markdown 中 ` ```infographic ` 围栏内无语法高亮：改为向 `markup.fenced_code.block.markdown` 注入（与 VS Code 内置 markdown-math 围栏机制一致），在未知语言围栏内用 `\G` 匹配语言标识后再套 `source.infographic`
- **改进** `syntaxes/infographic.tmLanguage.json`：对齐仓库语法规范——`infographic` 整行入口、`theme`/`template` 同行名称、点号路径键、列表项 `-` 与行尾值域、`meta.value` 避免值域误标
- **调整** `language-configuration.json` 缩进规则：去掉不适配 DSL 的 YAML 风格 `:` 规则，在块关键字行与列表 `- ` 后增加缩进

## 0.7.1

- 修复侧边栏「导出为图片」时 PNG/SVG 与画布预览**字体不一致**：`data:` 栅格化脱离文档后无法继承 VS Code 与主题 Web 字体
- 导出前内联 `text`/`tspan` 与 `foreignObject span` 的计算字体与颜色；等待 `document.fonts.ready` 后再序列化
- 序列化前调用与 `@antv/infographic` 一致的 `embedFonts`，将主题 woff2 内联为 SVG 内 `@font-face`；并补充隐藏探测节点以覆盖仅写在 `<text>` 上的字族，避免库原收集逻辑遗漏

## 0.7.0

- 为 `.infographic` 文件在资源管理器中注册语言图标（`logo.svg`，与 Mermaid Chart 对 `.mmd` 的处理方式一致）
- 打开工作区内的 `.infographic` 文件时自动在侧栏打开 Infographic 可视化编辑 Webview；从 Markdown 打开的临时 `untitled` 缓冲不受影响
- 新增扩展激活事件 `onLanguage:infographic`，确保仅打开独立信息图文件时扩展也会加载

## 0.4.0

- 修复编辑器侧边栏单独渲染时 `theme hand-drawn` 字体不一致问题
- 优化侧边栏 Webview 渲染初始化与 CSP 字体/样式加载策略，提升主题字体加载稳定性

## 0.1.0

- 首次发布：Markdown 内 `infographic` 围栏代码块语法高亮（TextMate 注入 + `source.infographic`）
- 内置 Markdown 预览中渲染 AntV Infographic（`markdown.markdownItPlugins` + `markdown.previewScripts`）
- 可选独立语言 id：`infographic`，扩展名 `.infographic`
- 示例见 `examples/sample.md`
