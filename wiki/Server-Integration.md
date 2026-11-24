[中文](Server-Integration-zh-CN)

# Server Integration

Reading, search and scene previews run on the client. Guide content comes from the player's installed mods and resource packs; the server does not distribute page sources.

## Deployment

| Feature | Client | Server |
| --- | --- | --- |
| Reading, search, scene previews | GuideNH and guide resources | GuideNH installation unnecessary |
| World structure placement and `/guidenh` | GuideNH | GuideNH installed; placing player has command permission level 3 |

World placement is unavailable on servers without GuideNH. Check `sceneExportEnabled` when using scene export actions. Placement in singleplayer also depends on cheats/command permissions.

## Administrator commands

Run these commands as a player. Coordinates accept relative `~` notation:

| Command | Purpose |
| --- | --- |
| `/guidenhc importstructure <x> <y> <z> <file.snbt>` | Import on the client from `config/guidenh/structures`, then approve the upload |
| `/guidenh placeallstructures <x> <y> <z>` | Place that player's cached structures along the X axis from the target position |

Both commands and single-structure placement require command permission level 3. Import and batch placement use a per-player login-session cache, cleared on logout; it is not persistent server storage.

## Troubleshooting

1. World placement unavailable: check the server installation and scene export settings.
2. Permission denied: check the player's command permission level, including cheats in singleplayer.
3. No structures for batch placement: import or cache them again; reconnecting clears the cache.
4. Missing or outdated pages: check the client's resource packs and reload with `F3+T`.

## Related references

- [Installation](Installation)
- [GameScene](GameScene)
- [Structure Export](Structure-Export)
