import { loadSharedText } from "../sharedAssets.js";

export async function loadSceneHoverTargetsJson(source) {
  const url = new URL(source, document.baseURI);
  const text = await loadSharedText(url);
  const bindings = JSON.parse(text);
  if (Array.isArray(bindings)) return text;
  if (![1, 2].includes(bindings?.version) || typeof bindings.geometrySrc !== "string" || !Array.isArray(bindings.templates)) {
    throw new Error(`Unsupported scene hover bindings: ${url}`);
  }
  const geometryUrl = new URL(bindings.geometrySrc, url);
  const geometry = JSON.parse(await loadSharedText(geometryUrl));
  if (geometry?.version !== bindings.version || !Array.isArray(geometry.styles) || !Array.isArray(geometry.targets)) {
    throw new Error(`Unsupported scene hover geometry: ${geometryUrl}`);
  }
  const legacy = bindings.version === 1;
  if (!legacy && (!Array.isArray(bindings.templateSlots) || bindings.templateSlots.length !== geometry.targets.length)) {
    throw new Error(`Invalid scene hover template slots: ${url}`);
  }
  const targets = geometry.targets.map((row, index) => {
    if (!Array.isArray(row) || !(legacy ? row.length === 5 : row.length === 2 || row.length === 4)
      || !Number.isInteger(row[0]) || !geometry.styles[row[0]]) {
      throw new Error(`Invalid scene hover target: ${geometryUrl}`);
    }
    const style = row[0];
    const blockPos = legacy ? row[3] : row[1];
    const template = legacy ? row[4] : bindings.templateSlots[index];
    if (!Number.isInteger(template) || template < -1 || template >= bindings.templates.length) {
      throw new Error(`Invalid scene hover template reference: ${url}`);
    }
    const cube = !legacy && row.length === 2;
    if (cube && (!Array.isArray(blockPos) || blockPos.length !== 3 || !blockPos.every(Number.isFinite))) {
      throw new Error(`Invalid scene hover cube position: ${geometryUrl}`);
    }
    const minCorner = legacy ? row[1] : cube ? [...blockPos] : row[2];
    const maxCorner = legacy ? row[2] : cube ? blockPos.map(value => value + 1) : row[3];
    const target = { ...geometry.styles[style], minCorner, maxCorner };
    if (blockPos !== null) target.blockPos = blockPos;
    if (template >= 0) target.contentTemplateId = bindings.templates[template];
    return target;
  });
  return JSON.stringify(targets);
}
