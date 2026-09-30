# ExportSite header link

Edit `site-config.json` in the exported site root before running `npm run build`:

```json
{
  "headerLink": "https://github.com/GTNewHorizons/guidenh",
  "headerLinkLabel": "GuideNH on GitHub"
}
```

An empty `headerLink` hides the icon. Only HTTP and HTTPS links are accepted. The icon opens the link in a new tab. `headerLinkLabel` is optional; the target domain is used when it is omitted. Re-exporting into the same directory preserves the existing `site-config.json`.
