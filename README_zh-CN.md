<p align="center">
    <img width="690" src="./src/main/resources/assets/logo.png" alt="GuideNH" style="image-rendering: pixelated;">
</p>
<hr>
<p align="center">
    <img src="https://img.shields.io/badge/Available%20for-MC%201.7.10-c70039" alt="支持的 Minecraft 版本">
    <img src="https://img.shields.io/badge/Forge-10.13.4.1614-f6a21a" alt="支持的 Forge 版本">
    <img src="https://img.shields.io/badge/license-LGPL--3.0-green" alt="许可证">
</p>

<p align="center">
    <a href="README.md">English</a> |
    <a href="README_zh-CN.md">Chinese</a>
</p>

## 简介

GuideNH 是面向 Minecraft 1.7.10 / Forge 10.13.4.1614 的游戏内指南框架。它移植并扩展了 GuideME 风格的
Markdown 文档系统，让模组与整合包可以以资源包形式发布内容丰富的指南书：导航与搜索、物品索引、
Mermaid 与 LaTeX 渲染、可交互的 3D 场景，以及在游戏内直接编辑指南页面。

可以使用 [GuideNH 在线编辑器](https://www.gtnewhorizons.com/GuideNH) 在浏览器中导入、编辑和预览指南文件夹或
ZIP 压缩包。投稿说明见 [GTNH 贡献指南](https://github.com/GTNewHorizons/.github/blob/main/CONTRIBUTING.md)。

## 环境要求

- 必需依赖：[GTNHLib (>= 0.11.16)](https://github.com/GTNewHorizons/GTNHLib)。

- 受支持的运行时矩阵使用 JDK 8、17 或 21。Gradle 构建本身运行在 JDK 25 工具链上，由
  `gradle/gradle-daemon-jvm.properties` 中的设置在 wrapper 内自动准备。
- 使用 [rustup](https://rustup.rs) 安装的 Rust 工具链，仅在构建布局引擎原生库时需要。
- 无需单独安装 Gradle，直接使用仓库自带的 wrapper（`gradlew`、`gradlew.bat`）。
- Python 3，在 Windows 上以 `py -3` 调用，用于视觉测试工具。

## 构建

```powershell
./gradlew build
```

指南引擎通过原生库完成排版渲染，运行游戏前请先构建一次：

```powershell
./gradlew buildRustNative
```

也可以直接在该 crate 目录下构建：

```powershell
cd src/rust/layout-engine
cargo build --release
```

`buildRustNative` 会编译 `src/rust/layout-engine`，随后由 Gradle 构建把生成的
`guide_layout_engine.dll` 复制到 `src/main/resources/natives/`。该目录是构建产物，不入库，
因此这一步复制才是把原生库放到游戏加载位置的环节。若 cargo 目标目录被重定向，复制会跟随解析后的
目录；请交给 Gradle 任务完成复制，不要手工拷贝文件。

## 运行

```powershell
./gradlew runClient
./gradlew runServer
```

进入游戏后：

- 按 `G` 打开指南主页。
- 鼠标悬停在已建立索引的物品上并按住 `G`，可以跳转到对应指南页面。
- 按 `F3+T` 可以重新加载已编辑的指南资源。

## 测试

```powershell
./gradlew compileJava compileTestJava test
```

```powershell
cd src/rust/layout-engine
cargo test
```

## 视觉验证

以 headless 方式渲染视觉 fixture 资源包。每批不超过 40 页，并且必须由渲染看门狗包裹执行：

```powershell
py -3 test/visual/tools/render_watchdog.py --timeout 2400 --log <log> -- cmd /c "gradlew.bat runClient25 -Dguidenh.guide.sources=<repo>/test/visual/resourcepack -Dguidenh.headlessRender=true -Dguidenh.renderpage.guide=guidenh:guidenh -Dguidenh.renderpage.list=<list-file> -Dguidenh.renderpage.out=<shots> -Dguidenh.renderpage.width=900 -Dguidenh.renderpage.scale=2 -Dguidenh.renderpage.bounds=true"
```

用断言棘轮核对渲染出的 bounds：

```powershell
py -3 test/visual/tools/assert_bounds.py --shots run/client_new/<shots> --assertions test/visual/ratchet/assertions.json
```

渲染输出与客户端日志写在 `run/client_new/` 下。fixture 语料、棘轮与可用工具的说明见
[test/visual/README.md](test/visual/README.md)。

## Wiki

- [英文 Wiki](wiki/Home-en-US.md)
- [中文 Wiki](wiki/Home-zh-CN.md)
- [快速上手](wiki/Getting-Started-zh-CN.md)
- [指南页面格式](wiki/Guide-Page-Format-zh-CN.md)
- [结构导出](wiki/Structure-Export-zh-CN.md)
- [示例资源包](wiki/resourcepack)

## 许可证

- 代码：[LGPL-3.0](LICENSE.txt)
- 内置第三方库遵循各自的许可证。

## 致谢

感谢 persephone 提供图标纹理。GuideNH 借鉴了 [GuideME](https://github.com/AppliedEnergistics/GuideME)
的设计思路，GuideME 以 LGPL-3.0 分发；本项目还使用了 SnakeYAML、Apache Lucene、Apache Commons Lang、
FlatBuffers Java、JLaTeXMath 等开源库。

<a href="https://github.com/GTNewHorizons/GuideNH/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=GTNewHorizons/GuideNH&max=1000" alt="contributors" />
</a>

## 面向编码代理

仓库布局、完整命令参考与注释和文档规范见 [AGENTS.md](AGENTS.md)（英文）。
