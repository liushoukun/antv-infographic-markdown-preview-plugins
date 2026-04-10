# AntV Infographic 语法要求（IDE 高亮与校验对齐用）

本文依据官方文档 [信息图语法 - AntV Infographic](https://infographic.antv.vision/learn/infographic-syntax#) 整理，供 JetBrains 插件词法/高亮与仓库内其它工具保持一致。引擎以 AntV Infographic 解析为准；IDE 侧采用宽松词法，避免把合法 DSL 标成错误。

## 1. 总体结构

- 入口：`infographic <template-name>`（模板名通常为包含连字符的标识 slug）。
- 其后通过块描述 **`template` / `design` / `data` / `theme`**（`template` 亦可在入口书写；块级关键字与入口一致）。
- 信息图可理解为：**信息结构（数据） + 图形表意（设计/主题）**。

## 2. 书写规范

| 规则 | 说明 |
|------|------|
| 缩进 | 使用**两个空格**一级（与官方示例一致） |
| 键值 | 键与值以**空格**分隔；一行可表示 `键 …值`（值可取剩余行直至换行） |
| 省略 `type` | `structure [name]`、`item [name]`、`title [name]` 等可省略 type 关键字 |
| 列表 | **对象数组**：每项以行首 `-` 开始换行书写（如 `lists` 下的项） |
| 简单数组 | 可用行内写法（如 `palette` 多个色值） |
| 注释 | 行注释：`#` 至行尾 |
| 字符串 | 支持双引号、单引号字符串（含常见转义） |

## 3. 点号路径（dot path）

- 深层且字段较少时可用 `a.b.c` 减少缩进，例如：`theme.base.text.fill #fff`。
- 点号路径用于**对象字段**；若路径中间层级是**数组**（如 `data.items` 的点号形式），官方说明为语法错误（引擎侧）。
- 同一路径重复赋值：**后写覆盖前写**。
- IDE 词法：将行首的 `[\w.-]+`（以字母或下划线开头、含 `.` 的路径）视为**一整段键**，其后的行尾为值（如 `#fff`）。

## 4. 块与常用键（高亮关键字 / 属性名）

### 4.1 块级与入口

- `infographic`
- `data`
- `theme`
- `template`
- `design`

### 4.2 `data` 常见容器与字段

- 容器 / 列表名：`title`、`desc`、`lists`、`sequences`、`values`、`nodes`、`relations`、`compares`、`root`、`children`、`items`、`order`
- 数据项通用字段：`label`、`value`、`desc`、`icon`、`id`、`category`、`group`
- 关系边（YAML 风格）：`from`、`to`，以及 `direction`、`showArrow`、`arrowType` 等
- Mermaid 风格关系可出现在 `relations` 中（`->`、`<-`、`--`、`-->` 等，及节点 `[label]` 等），**整行可作为值域**

### 4.3 `design`

- `structure`、`item`、`title`（模块名）、以及各模块下的数值/布尔配置（如 `gap`、`showIcon`、`align` 等）

### 4.4 `theme`

- 预设：`theme <theme-name>`（与 `theme` 块首行同类）
- 自定义示例字段：`colorBg`、`colorPrimary`、`palette`、`stylize`、`roughness` 等；可与点号路径混用

## 5. 词法与高亮策略（本插件约定）

1. **行模型**（不含围栏外 Markdown）：可选前导空白 → 可选列表符 `-` 与空格 → **键**（`infographic` 单独规则 → 其余为一段 `[\w.-]+`）→ 可选 **值（取至行尾）**。
2. **`infographic` 行**：`infographic` 为关键字；**同一行剩余 slug**（如 `list-row-horizontal-icon-arrow`）为模板引用高亮，不拆成非法字符。
3. **`theme <name>`**：`theme` 为关键字；**同一行主题名**（可含 `-`）整体作为值高亮，避免子词被误标错误。
4. **值域可包含**：中文、数字、小数、`#` 颜色、`/`（如图标 `mdi/xxx`）、关系运算符、`|`、`[]` 等；**值域内不设 BAD_CHARACTER**，以免误判。
5. **纯关系行**（如 `A -> B`）：行首标识符视为键类 token，**整行剩余**为值（与数据块中 `relations` 的 Mermaid 风格一致）。

## 6. 与 VS Code `syntaxes/infographic.tmLanguage.json` 的关系

- TextMate 规则仅匹配少量键名，**不匹配值**；JetBrains 定制 Lexer 在此基础上扩展键集合与**行尾值**，并与本文档一致，减少「假红线」。

## 7. 参考链接

- [信息图语法 – AntV Infographic](https://infographic.antv.vision/learn/infographic-syntax#)
- 仓库：`syntaxes/infographic.tmLanguage.json`
