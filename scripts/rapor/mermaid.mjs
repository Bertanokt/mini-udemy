// Mermaid kod bloğunu PNG'ye çevirir (rapor birleştirici yardımcısı, #69).
// Kullanım: node mermaid.mjs <girdi.mmd> <çıktı.png>
// Chrome'u DevTools protokolüyle sürer (Node'un kendi WebSocket'i; npm bağımlılığı yok). Mermaid, sabit sürümüyle
// jsDelivr'dan yüklenir; bu yüzden internet bağlantısı gerekir. Chrome başka bir yoldaysa: CHROME=/yol/chrome
import { spawn } from "node:child_process";
import { readFileSync, writeFileSync, mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

const MERMAID = "https://cdn.jsdelivr.net/npm/mermaid@11.4.1/dist/mermaid.min.js";
const [, , input, output] = process.argv;
if (!input || !output) {
  console.error("kullanım: node mermaid.mjs <girdi.mmd> <çıktı.png>");
  process.exit(2);
}
// Rapor basılı olduğu için "bugün" çizgisi kapatılır.
const code = readFileSync(input, "utf8").replace(/^(\s*gantt\s*)$/m, "$1\n    todayMarker off");
const chromePath = process.env.CHROME ?? "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const profile = mkdtempSync(join(tmpdir(), "rapor-mermaid-"));
const port = 9400 + Math.floor(Math.random() * 400);
const chrome = spawn(chromePath, ["--headless=new", `--remote-debugging-port=${port}`, `--user-data-dir=${profile}`,
  "--no-first-run", "--hide-scrollbars", "about:blank"], { stdio: "ignore" });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

try {
  let ok = false;
  for (let i = 0; i < 50 && !ok; i++) { try { await fetch(`http://127.0.0.1:${port}/json/version`); ok = true; } catch { await sleep(200); } }
  if (!ok) throw new Error("Chrome açılmadı (CHROME değişkenini kontrol edin)");
  const page = (await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()).find((t) => t.type === "page");
  const ws = new WebSocket(page.webSocketDebuggerUrl);
  await new Promise((r) => ws.addEventListener("open", r, { once: true }));
  let seq = 0; const pending = new Map();
  ws.addEventListener("message", (ev) => { const m = JSON.parse(ev.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } });
  const send = (method, params = {}) => new Promise((res, rej) => { const id = ++seq;
    pending.set(id, (m) => (m.error ? rej(new Error(method + ": " + m.error.message)) : res(m.result))); ws.send(JSON.stringify({ id, method, params })); });
  const evaluate = async (expression) => (await send("Runtime.evaluate", { expression, awaitPromise: true, returnByValue: true })).result.value;

  await send("Page.enable");
  await send("Emulation.setDeviceMetricsOverride", { width: 1400, height: 900, deviceScaleFactor: 2, mobile: false });
  const frameId = (await send("Page.getFrameTree")).frameTree.frame.id;
  const escaped = code.replace(/&/g, "&amp;").replace(/</g, "&lt;");
  await send("Page.setDocumentContent", { frameId, html: `<!doctype html><meta charset="utf-8">
<style>body{margin:0;background:#fff;font-family:Arial,Helvetica,sans-serif}#d{width:1300px;padding:16px}</style>
<div id="d"><pre class="mermaid">${escaped}</pre></div>
<script src="${MERMAID}"></script>
<script>mermaid.initialize({startOnLoad:false,theme:"neutral",gantt:{useWidth:1300,fontSize:13,sectionFontSize:13}});
window.done=mermaid.run({querySelector:".mermaid"}).then(()=>"ok",e=>"hata: "+e.message);</script>` });
  let state;
  for (let i = 0; i < 100; i++) { state = await evaluate("window.done ? window.done : (typeof mermaid === 'undefined' ? 'yükleniyor' : 'bekliyor')"); if (state === "ok" || String(state).startsWith("hata")) break; await sleep(150); }
  if (state !== "ok") throw new Error("Mermaid çizilemedi: " + state);
  const box = await evaluate("(() => { const r = document.getElementById('d').getBoundingClientRect(); return { x: r.x, y: r.y, width: Math.ceil(r.width), height: Math.ceil(r.height) }; })()");
  const shot = await send("Page.captureScreenshot", { format: "png", captureBeyondViewport: true, clip: { ...box, scale: 1 } });
  writeFileSync(output, Buffer.from(shot.data, "base64"));
  ws.close();
} finally {
  chrome.kill();
  await sleep(300);
  rmSync(profile, { recursive: true, force: true });
}
