function headingText(value) {
  return String(value || "").replace(/\[([^\]]+)\]\([^)]+\)/g, "$1").replace(/<[^>]*>|[`*_~]/g, "").trim().toLocaleLowerCase();
}

function sourceHeadings(source) {
  const headings = [];
  let fence = "";
  let frontmatter = false;
  const lines = String(source || "").replaceAll("\r\n", "\n").split("\n");
  for (let index = 0; index < lines.length; index += 1) {
    const line = lines[index];
    if (index === 0 && line === "---") { frontmatter = true; continue; }
    if (frontmatter) { if (line === "---") frontmatter = false; continue; }
    const marker = line.match(/^\s{0,3}(`{3,}|~{3,})/);
    if (marker) {
      if (!fence) fence = marker[1];
      else if (marker[1][0] === fence[0] && marker[1].length >= fence.length) fence = "";
      continue;
    }
    if (fence) continue;
    const heading = line.match(/^\s{0,3}(#{1,6})\s+(.+?)\s*#*\s*$/);
    if (heading) headings.push({ line: index, level: heading[1].length, text: headingText(heading[2]) });
  }
  return { headings, lineCount: lines.length };
}

function interpolate(value, points, sourceKey, targetKey, sourceMax, targetMax) {
  if (!sourceMax || !targetMax) return 0;
  if (points.length < 3) return value / sourceMax * targetMax;
  for (let index = 1; index < points.length; index += 1) {
    const end = points[index];
    if (value > end[sourceKey] && index < points.length - 1) continue;
    const start = points[index - 1];
    const span = end[sourceKey] - start[sourceKey];
    if (span <= 0) continue;
    return Math.max(0, Math.min(targetMax, start[targetKey] + (value - start[sourceKey]) / span * (end[targetKey] - start[targetKey])));
  }
  return value / sourceMax * targetMax;
}

export function createScrollSync(editor, preview) {
  const iframeOrigin = Symbol("iframe scroll");
  let enabled = false;
  let source = "";
  let markdown = false;
  let iframe = null;
  let iframeTop = 0;
  let iframeMax = 0;
  let points = [];
  let frame = 0;
  let blockedTarget = null;
  let blockedPosition = 0;
  const resizeObserver = globalThis.ResizeObserver ? new ResizeObserver(() => { measure(); if (enabled) follow(editor); }) : null;

  function measure() {
    const sourceMax = Math.max(0, editor.scrollHeight - editor.clientHeight);
    const previewMax = iframe ? iframeMax : Math.max(0, preview.scrollHeight - preview.clientHeight);
    points = [{ editor: 0, preview: 0 }];
    if (markdown && sourceMax && previewMax) {
      const { headings, lineCount } = sourceHeadings(source);
      const rendered = [...preview.querySelectorAll(".guide-page h1, .guide-page h2, .guide-page h3, .guide-page h4, .guide-page h5, .guide-page h6")]
        .filter((element) => element.getClientRects().length);
      const style = getComputedStyle(editor);
      const lineHeight = Number.parseFloat(style.lineHeight) || 22;
      const paddingTop = Number.parseFloat(style.paddingTop) || 0;
      const paddingBottom = Number.parseFloat(style.paddingBottom) || 0;
      const estimatedHeight = lineCount * lineHeight + paddingTop + paddingBottom;
      const wrapped = estimatedHeight > editor.clientHeight && editor.scrollHeight > estimatedHeight + lineHeight;
      let renderedIndex = 0;
      for (const heading of headings) {
        const match = rendered.findIndex((element, index) => index >= renderedIndex && Number(element.tagName.slice(1)) === heading.level && headingText(element.textContent) === heading.text);
        if (match < 0) continue;
        renderedIndex = match + 1;
        const element = rendered[match];
        const editorY = Math.min(sourceMax, wrapped ? heading.line / Math.max(1, lineCount - 1) * editor.scrollHeight : paddingTop + heading.line * lineHeight);
        const previewY = Math.min(previewMax, element.getBoundingClientRect().top - preview.getBoundingClientRect().top + preview.scrollTop);
        const previous = points.at(-1);
        if (editorY > previous.editor + 1 && previewY > previous.preview + 1 && editorY < sourceMax && previewY < previewMax) points.push({ editor: editorY, preview: previewY });
      }
    }
    points.push({ editor: sourceMax, preview: previewMax });
  }

  function follow(origin) {
    if (!enabled) return;
    if (iframe && origin === preview) return;
    const position = origin === iframeOrigin ? iframeTop : origin.scrollTop;
    if (origin === blockedTarget && Math.abs(position - blockedPosition) <= 1) { blockedTarget = null; return; }
    if (frame) cancelAnimationFrame(frame);
    frame = requestAnimationFrame(() => {
      frame = 0;
      const target = origin === editor ? iframe ? iframeOrigin : preview : editor;
      const sourceKey = origin === editor ? "editor" : "preview";
      const targetKey = origin === editor ? "preview" : "editor";
      const sourceMax = points.at(-1)[sourceKey];
      const targetMax = points.at(-1)[targetKey];
      const current = target === iframeOrigin ? iframeTop : target.scrollTop;
      const destination = interpolate(origin === iframeOrigin ? iframeTop : origin.scrollTop, points, sourceKey, targetKey, sourceMax, targetMax);
      if (Math.abs(current - destination) <= 1) return;
      blockedTarget = target;
      blockedPosition = destination;
      if (target === iframeOrigin) iframe?.contentWindow?.postMessage({ type: "guidenh-set-scroll", ratio: targetMax ? destination / targetMax : 0 }, "*");
      else target.scrollTop = destination;
    });
  }

  editor.addEventListener("scroll", () => follow(editor), { passive: true });
  preview.addEventListener("scroll", () => follow(preview), { passive: true });
  window.addEventListener("message", (event) => {
    if (!iframe || event.source !== iframe.contentWindow || event.data?.type !== "guidenh-scroll-position") return;
    const wasReady = iframeMax > 0;
    iframeMax = Math.max(0, Number(event.data.max) || 0);
    iframeTop = Math.max(0, Math.min(iframeMax, Number(event.data.top) || 0));
    measure();
    if (enabled) follow(wasReady ? iframeOrigin : editor);
  });
  window.addEventListener("resize", () => { measure(); if (enabled) follow(editor); }, { passive: true });

  return {
    setEnabled(value) {
      enabled = Boolean(value);
      if (!enabled && frame) { cancelAnimationFrame(frame); frame = 0; }
      blockedTarget = null;
      if (enabled) { measure(); follow(editor); }
    },
    refresh(text, isMarkdown, iframeElement = null) {
      source = text;
      markdown = isMarkdown;
      if (iframe !== iframeElement) { iframeTop = 0; iframeMax = 0; }
      iframe = iframeElement;
      resizeObserver?.disconnect();
      if (preview.firstElementChild) resizeObserver?.observe(preview.firstElementChild);
      measure();
      if (enabled) follow(editor);
    },
  };
}
