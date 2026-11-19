# FAQ

## Where should I edit the built-in runtime guide?

Edit UTF-8 pages under `wiki/resourcepack/assets/`. Load the example resource pack separately or use [Live Preview](Live-Preview); building the mod does not bundle these files.

## Where should I put a client-local guide that is not bundled into the jar?

Under `config/guidenh/DefaultGuide/<modid>/guidenh/`. This runtime directory is created automatically on the client
and uses the native namespace-root layout without an outer `assets/` folder.

## Why does this repository have both wiki pages and runtime markdown pages?

The files under `wiki/*.md` are human-facing GitHub Wiki documentation. The files under `wiki/resourcepack/...` are the in-game guide pages consumed by GuideNH itself.

## Can I use the same markdown in both places?

Not directly. Runtime guide markdown may contain GuideNH-specific MDX tags that GitHub Wiki does not understand. Keep runtime syntax inside the runtime guide tree and show it in the wiki only inside fenced code blocks.

## How do I preview guide edits quickly?

Add the JVM arguments documented in [Live Preview](Live-Preview) to your game launch configuration, then edit the configured source folder. Use `guideme.showOnStartup` if you want the guide to open automatically.
