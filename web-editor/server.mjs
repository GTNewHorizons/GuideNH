import express from "express";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.dirname(fileURLToPath(import.meta.url));
const publicDir = path.join(root, "public");
const app = express();
const port = Number.parseInt(process.env.PORT || "8787", 10);
const host = process.env.HOST || "0.0.0.0";

app.disable("x-powered-by");
app.get("/api/health", (_request, response) => {
  response.json({ status: "ok", service: "guidenh-web-editor", version: "1.0.0" });
});
app.use(express.static(publicDir, { extensions: ["html"] }));
app.get("*", (_request, response) => response.sendFile(path.join(publicDir, "index.html")));

app.listen(port, host, () => {
  console.log(`GuideNH Web Editor listening on http://${host === "0.0.0.0" ? "localhost" : host}:${port}`);
});
