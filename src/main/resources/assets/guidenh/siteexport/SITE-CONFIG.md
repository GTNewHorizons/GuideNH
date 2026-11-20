# Exported site configuration

Edit `site-config.json` in the exported site root before running `npm run build`:

```json
{
  "headerLink": "https://github.com/GTNewHorizons/GuideNH",
  "headerLinkLabel": "GuideNH on GitHub",
  "fluidUnit": "mB"
}
```

| Setting | Behavior |
| --- | --- |
| `headerLink` | HTTP/HTTPS link opened by the header icon in a new tab; an empty value hides the icon |
| `headerLinkLabel` | Accessible link label; defaults to the target domain |
| `fluidUnit` | Fluid amount display unit: `mB` or `L`; initially taken from the game's GTNHLib preference |

Re-exporting into the same directory preserves this file. Item and fluid quantity digits use the bundled Pixeloid font.

## Custom site scripts

To change the fluid display unit after the site loads, call `window.GuideNHAmounts.setFluidUnit('mB')` or `window.GuideNHAmounts.setFluidUnit('L')`.
