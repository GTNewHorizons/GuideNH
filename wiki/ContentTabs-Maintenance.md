# ContentTabs maintenance checklist

Use this checklist whenever the `ContentTabs` or `Tab` syntax changes. The `background` attribute is an example of a cross-renderer option: an attribute is incomplete until every consumer below agrees on its name, type, fallback, and validation.

## Syntax contract

- [ ] Keep the public spelling `background` consistent in GuideNH, Web Editor, VS Code, and IDEA.
- [ ] Accept `#RRGGBB`, `#AARRGGBB`, and `transparent` through the normal color parser.
- [ ] Keep the omitted-value fallback equal to the existing gray callout background.
- [ ] Keep `color` separate: it controls the accent line, title, and selected-tab indicator.
- [ ] Reject malformed values with an author-facing diagnostic; never emit unsanitized CSS.
- [ ] Keep `ContentTabs` limited to direct `Tab` children and preserve existing default-tab behavior.

## Runtime implementations

- [ ] Update `ContentTabsSpec` and `ContentTabsTagCompiler`.
- [ ] Update `LytContentTabsBlock` rendering and expose only the data needed by export serializers.
- [ ] Update the direct Site Export MDX renderer.
- [ ] Update the compiled layout/site serializer path.
- [ ] Update Site Export CSS fallback variables.
- [ ] Update Web Editor Markdown rendering and CSS fallback variables.
- [ ] Verify that old pages render unchanged when `background` is omitted.

## Editor support

- [ ] Add the attribute to the in-game `BuiltinSyntaxContributor`.
- [ ] Update the VS Code schema generator and generated `tags.json`.
- [ ] Update the IDEA schema enhancer and generated `tags.json`.
- [ ] Check attribute completion, color value completion, and diagnostics in both plugins.
- [ ] Rebuild and install both plugins when their generated schemas change.

## Documentation and examples

- [ ] Update `wiki/Tags-Reference.md` and `wiki/Tags-Reference-zh-CN.md`.
- [ ] Update every `web-editor/public/reference/<language>/<region>/Tags-Reference.md` file.
- [ ] Update both language versions of the resource-pack `markdown.md` example.
- [ ] Include one readable dark-background example and explain the default fallback.
- [ ] Keep the syntax description, attribute table, and example snippet in sync.
- [ ] If the public contract changes, update the relevant wiki examples and localized fallback text.

## Verification

- [ ] Read changed files as UTF-8 and check for accidental UTF-16 or BOM conversion.
- [ ] Run `git diff --check`.
- [ ] Run `./gradlew compileJava --offline`.
- [ ] Run `node --check` for changed Web Editor JavaScript.
- [ ] Run VS Code schema generation/build and IDEA schema generation/build when those repositories are changed.
- [ ] Manually inspect one default-background page and one custom-background page in the game, Web Editor, and Site Export.
- [ ] Check light and dark themes, opaque and transparent colors, narrow layouts, and nested rich content.

## 发布前核对

1. 语法名称、颜色格式和默认回退必须在所有实现中一致。
2. 游戏内、Site Export、Web Editor 不能只改其中一个渲染路径。
3. 游戏内智能补全、VS Code、IDEA 的 schema 和生成脚本必须同时更新。
4. Wiki、Web Editor 多语言文档和资源包双语示例必须同时更新。
5. 默认不填写属性时必须保持旧页面外观。
6. 完成 UTF-8、编译、脚本检查以及三端人工验收后再提交。
