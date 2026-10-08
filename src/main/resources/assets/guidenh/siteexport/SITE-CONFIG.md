# ExportSite header link

Edit `site-config.json` in the exported site root before running `npm run build`:

```json
{
  "headerLink": "https://github.com/GTNewHorizons/guidenh",
  "headerLinkLabel": "GuideNH on GitHub",
  "fluidUnit": "mB"
}
```

An empty `headerLink` hides the icon. Only HTTP and HTTPS links are accepted. The icon opens the link in a new tab. `headerLinkLabel` is optional; the target domain is used when it is omitted. Re-exporting into the same directory preserves the existing `site-config.json`.


The exported site records the GTNHLib fluid preference as `fluidUnit`. Fluid Amount spans can be switched at runtime with `window.GuideNHAmounts.setFluidUnit('mB')` or `window.GuideNHAmounts.setFluidUnit('L')`.
