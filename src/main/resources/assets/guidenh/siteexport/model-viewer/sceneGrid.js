export function expandSceneGrid(grid) {
  if (Array.isArray(grid)) return grid;
  if (grid?.version !== 1 || grid.type !== "grid"
    || ![grid.minX, grid.minZ, grid.maxX, grid.maxZ].every(Number.isSafeInteger)
    || ![grid.y, grid.halfWidth, grid.thickness].every(Number.isFinite)
    || grid.minX > grid.maxX || grid.minZ > grid.maxZ || grid.halfWidth < 0 || grid.thickness < 0) {
    throw new Error("Invalid scene floor grid descriptor");
  }
  const annotations = [];
  const half = Math.fround(grid.halfWidth);
  const append = (minX, minZ, maxX, maxZ) => annotations.push({
    type: "box",
    minCorner: [Math.fround(minX), grid.y, Math.fround(minZ)],
    maxCorner: [Math.fround(maxX), grid.y, Math.fround(maxZ)],
    color: grid.color,
    thickness: grid.thickness,
    alwaysOnTop: grid.alwaysOnTop,
  });
  // Match the float coordinates used by the game's exported grid lines.
  for (let x = grid.minX; x <= grid.maxX; x++) append(x - half, grid.minZ, x + half, grid.maxZ);
  for (let z = grid.minZ; z <= grid.maxZ; z++) append(grid.minX, z - half, grid.maxX, z + half);
  return annotations;
}
