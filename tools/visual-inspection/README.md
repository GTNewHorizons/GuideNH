# 视觉初筛工具 (Visual Inspection Screener)

廉价初筛员，产出供上游（K3）裁决合并。

## 环境要求

- Python 3（Windows 用 `py -3` 运行）
- Pillow (`pip install Pillow`)
- 其余仅用标准库，**禁止引入额外第三方依赖**

## 配置

复制 `.env.example` 为 `.env` 并填入阿里云百炼 API Key：

```
cp .env.example .env
```

`.env` 文件格式（手写解析，不支持 `python-dotenv`）：

```
DASHSCOPE_API_KEY=sk-你的密钥
VLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
VLM_MODEL=qwen2.5-vl-32b-instruct
VLM_CONCURRENCY=4
VLM_TIMEOUT=120
```

## 子命令

### 1. geometric — 第 0 层机械检测

```bash
py -3 screen.py geometric --shots <截图目录> --page-width 1800 --out <输出.json>
```

检测规则：
- **overflow_width**: 块右边界超出页宽（`x + w > page-width`，容差 2px）
- **zero_size**: 块宽度或高度 ≤ 0。LytThematicBreak、LytItemImage 两类降为 info 级（已知良性类，待校准确认）
- **off_page**: 块坐标 `x < 0` 或 `y < 0`
- **sibling_intersection**: 同一父节点的直接子节点间 IoU > 0.05。同一对块只报告一条（按 id 排序去重）；跳过 LytDocumentFloat 与文本类块（LytParagraph/LytHeading/LytListBlock）的合法环绕重叠——浮动环绕是设计行为，非排版缺陷

### 2. vlm — 第 1 层多模态初筛

```bash
py -3 screen.py vlm --shots <截图目录> [--pages name1,name2] [--model 覆盖.env] [--tile-h 1400] [--overlap 200] [--dry-run] --out <输出.json>
```

- 将 PNG 切成宽全幅、高 `tile-h`、重叠 `overlap` 的瓦片（overlap 必须小于 tile-h，否则抛出 ValueError 避免死循环）
- 每瓦片调用 OpenAI 兼容 VLM API
- 响应解析后坐标换算回整页坐标，跨瓦片去重（同 class 且 IoU > 0.5 合并）
- `--dry-run` 仅打印信息，不发起 HTTP
- 并发控制：`ThreadPoolExecutor(VLM_CONCURRENCY)`
- API 失败时指数退避重试 3 次（1s/4s/16s）

### 3. report — 合并报告

```bash
py -3 screen.py report --inputs a.json,b.json --out triage.json
```

- 合并多来源 findings，按 page 分组，按 severity（error → warn → info）排序
- stdout 打印每页计数表

## 输出格式

### geometric 输出

```json
{
  "tool": "visual-inspection/screen.py",
  "subcommand": "geometric",
  "page_width": 1800,
  "total_pages": 50,
  "total_findings": 123,
  "pages": [
    {
      "page": "page-name",
      "source": "geometric",
      "findings": [
        {
          "page": "page-name",
          "rule": "overflow_width",
          "bbox": [x, y, w, h],
          "severity": "error|warn",
          "evidence": "描述"
        }
      ]
    }
  ]
}
```

### vlm 输出

```json
{
  "tool": "visual-inspection/screen.py",
  "subcommand": "vlm",
  "model": "qwen2.5-vl-32b-instruct",
  "total_pages": 5,
  "total_findings": 10,
  "findings": [
    {
      "page": "page-name",
      "source": "vlm",
      "rule": "文字重叠",
      "bbox": [x, y, w, h],
      "severity": "error|warn|info",
      "confidence": 0.95,
      "evidence": "一句话描述"
    }
  ],
  "errors": []
}
```

### report 输出

```json
{
  "tool": "visual-inspection/screen.py",
  "subcommand": "report",
  "sources": ["a.json", "b.json"],
  "total_pages": 50,
  "total_findings": 200,
  "summary": {
    "page-name": {"error": 2, "warn": 5, "info": 0}
  },
  "pages": { ... }
}
```

## bounds JSON schema

经过对 `run/client_new/screenshots/` 目录下多个实际 JSON 文件的读取确认，bounds JSON 结构如下：

```json
[
  {
    "i": 0,
    "cls": "LytParagraph",
    "x": 5,
    "y": 5,
    "w": 393,
    "h": 40,
    "depth": 1
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `i` | int | 块在页面中的序号索引 |
| `cls` | str | 块类型（如 `LytParagraph`, `LytHeading`, `LytTable`, `LytTableCell`, `LytFloatAwareBlock`, `LytCodeBlock`, `ScenePlaceholder`, `CategoryPlaceholder` 等） |
| `x` | int | 左上角 x 坐标（px） |
| `y` | int | 左上角 y 坐标（px） |
| `w` | int | 宽度（px） |
| `h` | int | 高度（px） |
| `depth` | int | 树嵌套深度（1-based）。块按深度优先排列；depth=1 为根节点，depth=D 的父节点为前一个 depth=D-1 的块 |

### 父子关系推导

由于 JSON 是扁平数组，父子关系通过 `depth` 字段和顺序推导：

- 维护每个深度的最近祖先栈
- 块 B 的父节点 = 在 B 之前出现的、depth = B.depth - 1 的最近块
- 兄弟节点 = 共享同一父节点的直接子节点

示例（来自 `scene-blocks.md`）：

```
depth 1: LytFloatAwareBlock   ← 父: 无（根）
depth 2: LytTable             ← 父: LytFloatAwareBlock
depth 3: LytTableRow          ← 父: LytTable
depth 4: LytTableCell         ← 父: LytTableRow
depth 5: LytParagraph         ← 父: LytTableCell
depth 4: LytTableCell         ← 父: LytTableRow（与前一个 LytTableCell 是兄弟）
```


## 4. check_freshness — 截图新鲜度校验（A2/P4）

防"陈旧截图清单误报"（F7c 教训）：校验页清单中每页是否都有最新批次截图、截图是否新鲜。

```bash
py -3 tools/visual-inspection/check_freshness.py --shots <截图目录> --list <页清单> [--stale-min 10]
```

- 页清单格式：一行一个 `guidenh:visualtest/<path>.md`，`#` 开头为注释
- 截图命名 `<page_stem>_<YYYY-MM-DD_HHMMSS>.png`，按 stem 取最新批次
- 三态：OK / MISSING（无截图）/ STALE（最新截图 mtime 早于清单 mtime + stale-min 分钟）
- 有 MISSING 或 STALE 时退出码 1；TAP 风格输出对齐 assert_bounds.py
- `--self-test`：内置 OK/MISSING/STALE 三态冒烟用例
- 纯标准库，禁第三方依赖

## 5. scan_logs — 日志扫描（A2/P5）

落实 WORKFLOW §3.1.5 日志卫生强制项：渲染轮次客户端日志命中 `glyph atlas full` / `OutOfMemory` 告警即判定该轮失败。

```bash
py -3 tools/visual-inspection/scan_logs.py [--logs run/client_new/logs] [--pattern "glyph atlas full"]
```

- 扫描 `*.log`、`*.log.gz`（gzip 流式解压）与 `latest.log`
- 每条命中输出：文件、行号、行内容；有命中或读取错误退出码 1
- `--self-test`：内置样本日志（两种模式 + 噪声）验证命中与退出码
- 纯标准库，禁第三方依赖
