# layout-engine schema：guidenh_layout.fbs 生成链路与变更策略

> 本文档由架构审计行动表 A5 补齐，是"动 schema"类任务的前置阅读材料。

## 1. Schema 位置与命名空间

- Schema 文件：`layout-engine/schema/guidenh_layout.fbs`（555 行，38 个 table）
- namespace：`com.hfstudio.guidenh.guide.layout.flatbuffers`
- 数据合约方向：Java 侧序列化 → Rust 侧（layout-engine）消费
- Java 生成类检入位置：`src/main/java/com/hfstudio/guidenh/guide/layout/flatbuffers/`（38 个 .java，与 38 个 table 一一对应）

## 2. 生成链路现状

### Rust 侧（自动）
- `layout-engine/build.rs` 在 cargo build 时调用 `flatc` crate 提供的二进制（`flatc::flatc()`），执行：
  `flatc --rust -o layout-engine/src layout-engine/schema/guidenh_layout.fbs`
- 输出 `layout-engine/src/guidenh_layout_generated.rs`，随后 build.rs 自动打补丁：
  1. 删除所有 `extern crate flatbuffers;`（Rust 2021 自动导入 extern crate）
  2. 把 `use self::flatbuffers::` 改为 `use ::flatbuffers::`（`self::flatbuffers` 在 `mod flatbuffers { }` 内指模块而非 crate）
- 因此 Rust 侧无需手工再生成。

### Java 侧（手工/脚本）
- 生成方式：`tools/regen_java_flatc.bat` —— 定位 flatc → 校验版本 → `flatc --java` 生成到临时目录 → 与检入类比对 → 不一致时覆盖（`--dry-run` 只预览，`--check-only` 供 CI 校验）。
- **切勿手改生成类**。历史教训：commit `2b547572` / `b52e2ab1` 曾手工改生成类，造成检入类与 schema 漂移。
  现状核查（2026-08-02）：`TextData.java` 仍残留手改痕迹 —— R4-17 文档注释与 `addAlignment`/`addSeparator` 调用顺序与 flatc 输出不一致；运行脚本 apply 模式会将其归一。本次审计不代改。
- flatc 唯一来源：cargo `flatc` crate 构建产物
  `E:/build_out/guide_nh_rust/{debug,release}/build/flatc-*/out/bin/flatc.exe`（实测版本 23.5.26）。
  Gradle 侧无 flatc 配置（全仓 grep 无结果），Gradle 构建不会自动再生成 Java 类。

## 3. 变更流程（改 schema 的标准步骤）

1. 修改 `layout-engine/schema/guidenh_layout.fbs`（必须遵守 wire-compat，见 §5）
2. 重新生成 Java 类：
   - 预览差异：`tools\regen_java_flatc.bat --dry-run`
   - 应用覆盖：`tools\regen_java_flatc.bat`（无参数 = 覆盖式再生成）
   - CI/手动同步校验：`tools\regen_java_flatc.bat --check-only`（不一致 exit 1）
3. Rust 侧无需操作：build.rs 在下次 cargo build 时自动再生成 + 打补丁
4. 跑 gate：`./gradlew compileJava compileTestJava test runLayoutDump`
5. 提交（schema + Java 生成类 + Rust 生成 rs 同一 commit）

## 4. 版本策略

- flatc 必须为 **23.5.26**，与运行时 `flatbuffers-java` 23.5.26 完全一致。
- 生成的每个 Java 类含版本守卫：`ValidateVersion() { Constants.FLATBUFFERS_23_5_26(); }`，与运行时版本不符会在反序列化入口处失败。
- 升级 flatbuffers-java 运行时**必须同步**：升级 flatc → 重新生成全部 38 个类 → 跑 gate。
- `tools/regen_java_flatc.bat` 内置版本校验：`flatc --version` 不含 `23.5.26` 直接报错退出。

## 5. Wire-compat 策略

- schema 变更必须 **append-only**：只加字段/表，不删字段、不改类型、不改字段语义。
- 字段弃用约定：**keep field, write zero** —— 字段保留在 schema（vtable 槽位不变），写入侧传 0/默认值。
- DEPRECATED 先例清单（4 个字段名 / 5 处，行号为 2026-08-02 快照，随变更漂移）：

  | 表 | 字段 | 位置 |
  |---|---|---|
  | `TextData` | `bands` | guidenh_layout.fbs:106 |
  | `TextData` | `float_clips` | guidenh_layout.fbs:108 |
  | `PieChartData` | `chrome_height` | guidenh_layout.fbs:221 |
  | `ChartData` | `chrome_height` | guidenh_layout.fbs:241 |
  | `MediaWikiSpecialGeneratedData` | `max_content_height` | guidenh_layout.fbs:290 |

- 上述字段当前语义：写入侧传 0/默认，Rust 侧已改为内部计算（parley 迁移、chrome、maxColumnHeight）。

## 6. 第二生成集（不在本脚本范围）

- `src/main/java/guideme/flatbuffers/scene/`（16 个 `Exp*` 类：`ExpScene`、`ExpMesh`、`ExpMaterial` 等）来自上游 GuideME schema。
- 本仓库**无对应 .fbs 源**，`tools/regen_java_flatc.bat` 不覆盖该类；改动需在上游工程完成。
