export function advanceCodeFence(line, fence) {
  const marker = line.match(fence ? /^\s*(`{3,}|~{3,})(.*)$/ : /^\s{0,8}(`{3,}|~{3,})(.*)$/);
  if (!marker) return fence;
  if (!fence) return { character: marker[1][0], length: marker[1].length };
  return marker[1][0] === fence.character && marker[1].length >= fence.length && !marker[2].trim() ? null : fence;
}

export function stripMdxComments(source) {
  const output = [];
  let fence = null;
  let comment = false;
  let codeSpan = 0;
  let rawTag = "";
  for (const line of String(source ?? "").split(/\r\n?|\n/)) {
    if (fence || (!comment && !codeSpan && !rawTag && advanceCodeFence(line, null))) {
      fence = advanceCodeFence(line, fence);
      output.push(line);
      continue;
    }
    const parts = [];
    let cursor = 0;
    let start = 0;
    while (cursor < line.length) {
      if (comment) {
        const end = line.indexOf("*/}", cursor);
        if (end < 0) { start = line.length; break; }
        comment = false;
        cursor = end + 3;
        start = cursor;
      } else if (rawTag) {
        const closing = new RegExp(`</${rawTag}\\s*>`, "i").exec(line.slice(cursor));
        if (!closing) { cursor = line.length; break; }
        cursor += closing.index + closing[0].length;
        rawTag = "";
      } else if (line[cursor] === "`") {
        let end = cursor + 1;
        while (line[end] === "`") end += 1;
        const length = end - cursor;
        if (!codeSpan) codeSpan = length;
        else if (length === codeSpan) codeSpan = 0;
        cursor = end;
      } else if (!codeSpan && line.startsWith("{/*", cursor)) {
        parts.push(line.slice(start, cursor));
        comment = true;
        cursor += 3;
      } else if (!codeSpan && line[cursor] === "<") {
        const opening = /^<(pre|code)\b[^>]*>/i.exec(line.slice(cursor));
        if (opening) { rawTag = opening[1]; cursor += opening[0].length; }
        else cursor += 1;
      } else cursor += 1;
    }
    parts.push(line.slice(start));
    const cleaned = parts.join("");
    if (!comment && !codeSpan && !rawTag) fence = advanceCodeFence(cleaned, null);
    output.push(cleaned);
  }
  return output.join("\n");
}
