# 调试模式

调试覆盖层帮助作者检查页面布局、交互区域和场景元素。

## 开启

在任意 GuideNH 界面按 `Ctrl + Shift + Alt + C` 切换，或在模组配置中启用 `GUI Debug Overlay`（`guiDebugMode`）。

## 检查页面

- 左下信息面板显示鼠标位置、FPS、内存、悬停元素及可选的父元素信息。
- 开启元素轮廓后，可核对位置、尺寸和嵌套范围。
- 底部 `Debug Options` 菜单可切换显示内容、打印文档树、重新编译当前页或导出调试数据。
- 文档树与导出数据写入 Minecraft 日志；报告问题时附上页面源文件、复现步骤及相关日志。

## 配置

设置位于 `config/guidenh/guidenh.cfg` 的调试分类，或可通过模组配置界面调整。

| 设置 | 默认值 | 范围 |
| --- | --- | --- |
| `debugTextScale` | `0.8` | 0.5–2.0 |
| `debugOutlineThickness` | `2.0` | 0.5–5.0 px |
| `debugDashAnimationCycleMs` | `2000` | 500–10000 ms |
| `debugDashWidth` | `2.0` | 0.5–5.0 px |
| `debugDashOnLength` | `4.0` | 1–20 px |
| `debugDashOffLength` | `4.0` | 1–20 px |

可分别控制悬停/父元素信息、轮廓、FPS、内存和鼠标位置。关闭调试模式后不显示覆盖层，设置会保留。

模组开发者可参阅 [开发者集成](Developer-Integration-zh-CN) 注册自定义元素信息。
