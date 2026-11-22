// Demo seed içeriğini üretir (#67): kapak görselleri (PNG), PDF ders notları, video dersler (MP4, H.264),
// DOCX dokümanlar ve backend/src/main/resources/seed/demo-seed.sql. Dış bağımlılık yok: görseller ve PDF'ler yerel
// Chrome'da (DevTools protokolü, Node'un kendi WebSocket'i), videolar ffmpeg ile, DOCX Python'un zipfile'ıyla üretilir.
//
// Kullanım (repo kökünden):  node scripts/seed-content/generate.mjs
// Gerekenler: Node 22+, Google Chrome (CHROME ile yolu verilebilir), ffmpeg ve ffprobe, python3.
// İçerik: icerik.mjs; kullanıcı verisi (kayıt, ilerleme, sınav, yorum, şikâyet, bildirim): kullanici-verisi.sql.
import { spawn, execFileSync } from "node:child_process";
import { mkdirSync, writeFileSync, readFileSync, rmSync, readdirSync, statSync, mkdtempSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { tmpdir } from "node:os";
import { COURSES, INSTRUCTORS, CATEGORY_STYLE } from "./icerik.mjs";

const HERE = dirname(fileURLToPath(import.meta.url));
const REPO = join(HERE, "..", "..");
const FILES = join(REPO, "backend/src/main/resources/seed/files");
const SQL_OUT = join(REPO, "backend/src/main/resources/seed/demo-seed.sql");
const TEMPLATE = join(HERE, "kullanici-verisi.sql");
const CHROME = process.env.CHROME ?? "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
const WORK = mkdtempSync(join(tmpdir(), "seed-content-"));
const MB = 1024 * 1024;

const STUDENTS = [
  ["ayse.yilmaz@miniudemy.dev", "Ayşe Yılmaz"],
  ["can.demir@miniudemy.dev", "Can Demir"],
  ["elif.sahin@miniudemy.dev", "Elif Şahin"],
  ["burak.celik@miniudemy.dev", "Burak Çelik"],
  ["deniz.arslan@miniudemy.dev", "Deniz Arslan"],
  ["selin.koc@miniudemy.dev", "Selin Koç"],
];

const MIME = {
  png: "image/png",
  pdf: "application/pdf",
  mp4: "video/mp4",
  docx: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
};

// ------------------------------------------------------------------------------------------------------------------
// Yardımcılar
// ------------------------------------------------------------------------------------------------------------------
const TR = { ç: "c", ğ: "g", ı: "i", ö: "o", ş: "s", ü: "u", İ: "i", Ç: "c", Ğ: "g", Ö: "o", Ş: "s", Ü: "u" };
const slug = (s) => s.replace(/[çğıöşüİÇĞÖŞÜ]/g, (c) => TR[c]).toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
const esc = (s) => String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
const sql = (v) => (v === null || v === undefined ? "NULL" : typeof v === "number" || typeof v === "boolean" ? String(v).toUpperCase() : `'${String(v).replace(/'/g, "''")}'`);
const sqlArray = (a) => `ARRAY[${a.map(sql).join(", ")}]`;
let keySeq = 0;
const nextKey = () => `5eed0000-0000-4000-8000-${String(++keySeq).padStart(12, "0")}`;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// ------------------------------------------------------------------------------------------------------------------
// Chrome (DevTools protokolü)
// ------------------------------------------------------------------------------------------------------------------
const port = 9400 + Math.floor(Math.random() * 400);
const chrome = spawn(CHROME, ["--headless=new", `--remote-debugging-port=${port}`, `--user-data-dir=${join(WORK, "chrome")}`,
  "--no-first-run", "--hide-scrollbars", "--force-device-scale-factor=1", "--font-render-hinting=none", "about:blank"], { stdio: "ignore" });
let version;
for (let i = 0; i < 60 && !version; i++) {
  try { version = await (await fetch(`http://127.0.0.1:${port}/json/version`)).json(); } catch { await sleep(200); }
}
if (!version) throw new Error("Chrome açılamadı: " + CHROME);
const page = (await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()).find((t) => t.type === "page");
const ws = new WebSocket(page.webSocketDebuggerUrl);
await new Promise((r) => ws.addEventListener("open", r, { once: true }));
let seq = 0;
const pending = new Map();
ws.addEventListener("message", (ev) => {
  const m = JSON.parse(ev.data);
  if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); }
});
const send = (method, params = {}) => new Promise((res, rej) => {
  const id = ++seq;
  pending.set(id, (m) => (m.error ? rej(new Error(`${method}: ${m.error.message}`)) : res(m.result)));
  ws.send(JSON.stringify({ id, method, params }));
});
await send("Page.enable");
const frameId = (await send("Page.getFrameTree")).frameTree.frame.id;

async function load(html, width, height) {
  await send("Emulation.setDeviceMetricsOverride", { width, height, deviceScaleFactor: 1, mobile: false });
  await send("Page.setDocumentContent", { frameId, html });
  await send("Runtime.evaluate", { expression: "document.fonts.ready.then(() => true)", awaitPromise: true });
  await sleep(60);
}
async function png(html, width = 1280, height = 720) {
  await load(html, width, height);
  const shot = await send("Page.captureScreenshot", { format: "png", clip: { x: 0, y: 0, width, height, scale: 1 } });
  return Buffer.from(shot.data, "base64");
}
async function pdf(html) {
  await load(html, 794, 1123);
  const out = await send("Page.printToPDF", { printBackground: true, preferCSSPageSize: true });
  return Buffer.from(out.data, "base64");
}

// ------------------------------------------------------------------------------------------------------------------
// Şablonlar
// ------------------------------------------------------------------------------------------------------------------
// Tek tırnak: değerler style="…" özniteliklerinin içine de yazılıyor.
const FONT = `-apple-system, BlinkMacSystemFont, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif`;
const MONO = `ui-monospace, Menlo, Consolas, monospace`;
const ICONS = {
  code: `<path d="M16 18l6-6-6-6"/><path d="M8 6l-6 6 6 6"/><path d="M14 4l-4 16"/>`,
  chart: `<path d="M3 3v18h18"/><path d="M7 16v-4"/><path d="M11 16V8"/><path d="M15 16v-6"/><path d="M19 16V5"/>`,
  pen: `<path d="M12 19l7-7 3 3-7 7-3-3z"/><path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5z"/><path d="M2 2l7.586 7.586"/><circle cx="11" cy="11" r="2"/>`,
  chat: `<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/><path d="M8 9h8"/><path d="M8 13h5"/>`,
  board: `<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M9 3v18"/><path d="M15 3v18"/><path d="M5.5 7h2"/><path d="M11.5 7h2"/><path d="M11.5 11h2"/><path d="M17.5 7h2"/>`,
};
const icon = (name, size, stroke = "#ffffff", width = 1.4) =>
  `<svg width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="${stroke}" stroke-width="${width}" stroke-linecap="round" stroke-linejoin="round">${ICONS[name]}</svg>`;
const BRAND = `<div style="display:flex;align-items:center;gap:12px"><div style="width:34px;height:34px;border-radius:10px;background:linear-gradient(135deg,#6d63f0,#3b33b8);position:relative;box-shadow:inset 0 0 0 1px rgba(255,255,255,.2)"><div style="position:absolute;left:13px;top:9px;border-left:11px solid #fff;border-top:8px solid transparent;border-bottom:8px solid transparent"></div></div><span style="font-weight:700;font-size:20px;letter-spacing:-.01em">Mini Udemy</span></div>`;

function coverHtml(course) {
  const st = CATEGORY_STYLE[course.category];
  return `<!doctype html><meta charset="utf-8"><body style="margin:0">
<div style="width:1280px;height:720px;position:relative;overflow:hidden;font-family:${FONT};color:#fff;
  background:linear-gradient(135deg, ${st.from} 0%, #3730a3 52%, ${st.to} 100%)">
  <div style="position:absolute;right:-140px;top:-160px;width:620px;height:620px;border-radius:50%;background:rgba(255,255,255,.08)"></div>
  <div style="position:absolute;right:120px;bottom:-220px;width:420px;height:420px;border-radius:50%;background:rgba(255,255,255,.06)"></div>
  <div style="position:absolute;inset:0;background-image:radial-gradient(rgba(255,255,255,.13) 1.2px, transparent 1.2px);background-size:28px 28px;opacity:.55"></div>
  <div style="position:absolute;right:110px;top:170px;width:330px;height:330px;border-radius:72px;background:rgba(255,255,255,.12);
    box-shadow:inset 0 0 0 2px rgba(255,255,255,.22);display:flex;align-items:center;justify-content:center">${icon(st.icon, 200)}</div>
  <div style="position:absolute;left:84px;top:84px;font-size:22px;font-weight:700;letter-spacing:.06em;
    background:rgba(255,255,255,.16);border:1px solid rgba(255,255,255,.28);border-radius:999px;padding:8px 20px">${esc(course.category.toLocaleUpperCase("tr"))}</div>
  <div style="position:absolute;left:84px;top:190px;width:700px;font-size:66px;line-height:1.08;font-weight:800;letter-spacing:-.025em;
    text-shadow:0 2px 24px rgba(0,0,0,.18)">${esc(course.title)}</div>
  <div style="position:absolute;left:84px;bottom:76px">${BRAND}</div>
</div></body>`;
}

function videoFrame(course, section, lesson, visible) {
  const st = CATEGORY_STYLE[course.category];
  const top = `<div style="position:absolute;left:72px;top:56px;right:72px;display:flex;justify-content:space-between;align-items:center;font-size:20px;color:rgba(255,255,255,.78)">
    ${BRAND}<span>${esc(course.title)} · ${esc(section.title)}</span></div>`;
  const bg = `background:radial-gradient(90% 120% at 100% 0%, ${st.from}55 0%, transparent 55%), linear-gradient(160deg,#1e1b4b 0%,#14122f 100%)`;
  const progress = (visible + 1) / (lesson.points.length + 1);
  const bar = `<div style="position:absolute;left:72px;right:72px;bottom:48px;height:6px;border-radius:3px;background:rgba(255,255,255,.14)">
    <div style="width:${(progress * 100).toFixed(1)}%;height:100%;border-radius:3px;background:${st.from}"></div></div>`;
  let body;
  if (visible === 0) {
    body = `<div style="position:absolute;left:72px;top:210px;width:1000px">
      <div style="font-size:22px;font-weight:700;letter-spacing:.12em;color:${st.from}">VİDEO DERS</div>
      <div style="font-size:72px;font-weight:800;line-height:1.08;letter-spacing:-.025em;margin-top:18px">${esc(lesson.title)}</div>
      <div style="font-size:30px;line-height:1.45;color:rgba(255,255,255,.82);margin-top:28px;width:960px">${esc(lesson.summary)}</div></div>`;
  } else {
    const items = lesson.points.map((p, i) => `<div style="display:flex;gap:22px;align-items:flex-start;opacity:${i < visible ? 1 : 0.22}">
      <div style="flex:none;width:44px;height:44px;border-radius:12px;background:${i < visible ? st.from : "rgba(255,255,255,.2)"};display:flex;align-items:center;justify-content:center;font-weight:800;font-size:22px">${i + 1}</div>
      <div style="font-size:36px;line-height:1.3;padding-top:2px">${esc(p)}</div></div>`).join("");
    body = `<div style="position:absolute;left:72px;top:150px;right:72px">
      <div style="font-size:46px;font-weight:800;letter-spacing:-.02em;margin-bottom:40px">${esc(lesson.title)}</div>
      <div style="display:flex;flex-direction:column;gap:26px">${items}</div></div>`;
  }
  return `<!doctype html><meta charset="utf-8"><body style="margin:0"><div style="width:1280px;height:720px;position:relative;overflow:hidden;font-family:${FONT};color:#fff;${bg}">${top}${body}${bar}</div></body>`;
}

function pdfHtml(course, section, lesson) {
  const st = CATEGORY_STYLE[course.category];
  const code = lesson.code ? `<h2>Örnek</h2><pre>${esc(lesson.code)}</pre>` : "";
  const ex = lesson.exercise ? `<div class="ex"><b>Kendini dene.</b> ${esc(lesson.exercise)}</div>` : "";
  return `<!doctype html><meta charset="utf-8"><style>
  @page { size: A4; margin: 0; }
  body { margin: 0; font-family: ${FONT}; color: #1f2133; font-size: 12.5pt; line-height: 1.6; }
  .band { background: linear-gradient(120deg, ${st.from}, #3730a3 60%, #1e1b4b); color: #fff; padding: 34px 56px 30px; }
  .band .k { font-size: 10pt; letter-spacing: .1em; font-weight: 700; opacity: .85; }
  .band h1 { margin: 8px 0 6px; font-size: 26pt; line-height: 1.15; letter-spacing: -.01em; }
  .band .m { font-size: 10.5pt; opacity: .9; }
  main { padding: 30px 56px 40px; }
  h2 { font-size: 13.5pt; color: #3730a3; margin: 22px 0 8px; }
  p { margin: 0 0 10px; }
  ul { margin: 0; padding-left: 22px; } li { margin: 4px 0; }
  pre { font-family: ${MONO}; font-size: 10.5pt; line-height: 1.5; background: #16172b; color: #e8e9f5; padding: 16px 18px; border-radius: 10px; white-space: pre-wrap; margin: 0; }
  .ex { margin-top: 22px; background: #eef0ff; border-left: 4px solid #4f46e5; padding: 12px 16px; border-radius: 6px; }
  footer { position: fixed; bottom: 18px; left: 56px; right: 56px; font-size: 9pt; color: #6b6f85; display: flex; justify-content: space-between; border-top: 1px solid #e2e4ee; padding-top: 8px; }
  </style><body>
  <div class="band"><div class="k">DERS NOTU · ${esc(course.category.toLocaleUpperCase("tr"))}</div><h1>${esc(lesson.title)}</h1>
  <div class="m">${esc(course.title)} · ${esc(section.title)} · yaklaşık ${lesson.minutes} dk okuma</div></div>
  <main><h2>Açıklama</h2><p>${esc(lesson.summary)}</p><h2>Önemli noktalar</h2><ul>${lesson.points.map((p) => `<li>${esc(p)}</li>`).join("")}</ul>${code}${ex}</main>
  <footer><span>Mini Udemy</span><span>${esc(course.title)}</span></footer></body>`;
}

// ------------------------------------------------------------------------------------------------------------------
// Üretim
// ------------------------------------------------------------------------------------------------------------------
rmSync(FILES, { recursive: true, force: true });
mkdirSync(FILES, { recursive: true });
const files = []; // { email, key, name, mime }
const lessonFile = new Map(); // ders → { key, duration }

for (const course of COURSES) {
  const cslug = slug(course.title);
  if (!course.noCover) {
    const key = nextKey();
    const buf = await png(coverHtml(course));
    if (buf.length > 2 * MB) throw new Error(`Kapak 2 MB'ı aşıyor: ${course.title}`);
    writeFileSync(join(FILES, key), buf);
    files.push({ email: course.instructor, key, name: `kapak-${cslug}.png`, mime: MIME.png });
    course.coverKey = key;
  }
  for (const section of course.sections) {
    for (const lesson of section.lessons) {
      const key = nextKey();
      const base = `${cslug}-${slug(lesson.title)}`;
      if (lesson.type === "PDF") {
        writeFileSync(join(FILES, key), await pdf(pdfHtml(course, section, lesson)));
        files.push({ email: course.instructor, key, name: `${base}.pdf`, mime: MIME.pdf });
        lessonFile.set(lesson, { key, duration: lesson.minutes * 60 });
      } else if (lesson.type === "DOCUMENT") {
        const out = join(WORK, `${key}.docx`);
        execFileSync("python3", ["-I", join(HERE, "docx.py"), out, JSON.stringify({
          course: course.title, title: lesson.title, summary: lesson.summary, items: lesson.points,
          footer: `Mini Udemy · ${course.title} · ${section.title}`,
        })]);
        writeFileSync(join(FILES, key), readFileSync(out));
        files.push({ email: course.instructor, key, name: `${base}.docx`, mime: MIME.docx });
        lessonFile.set(lesson, { key, duration: lesson.minutes * 60 });
      } else {
        const dir = join(WORK, key);
        mkdirSync(dir);
        const list = [];
        for (let v = 0; v <= lesson.points.length; v++) {
          writeFileSync(join(dir, `f${v}.png`), await png(videoFrame(course, section, lesson, v)));
          list.push(`file 'f${v}.png'`, `duration ${v === 0 ? 5 : 6}`);
        }
        list.push(`file 'f${lesson.points.length}.png'`); // concat: son karenin süresi için tekrar
        writeFileSync(join(dir, "list.txt"), list.join("\n") + "\n");
        const out = join(dir, "out.mp4");
        execFileSync("ffmpeg", ["-hide_banner", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", "list.txt",
          "-vf", "fps=25,format=yuv420p", "-c:v", "libx264", "-preset", "veryfast", "-tune", "stillimage", "-crf", "30",
          "-g", "50", "-movflags", "+faststart", out], { cwd: dir });
        const duration = Math.round(Number(execFileSync("ffprobe", ["-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", out]).toString()));
        const size = statSync(out).size;
        if (size > 2 * MB) throw new Error(`Video 2 MB'ı aşıyor: ${lesson.title} (${size})`);
        writeFileSync(join(FILES, key), readFileSync(out));
        files.push({ email: course.instructor, key, name: `${base}.mp4`, mime: MIME.mp4 });
        lessonFile.set(lesson, { key, duration });
      }
      process.stdout.write(".");
    }
  }
}
ws.close();
chrome.kill();

// ------------------------------------------------------------------------------------------------------------------
// SQL
// ------------------------------------------------------------------------------------------------------------------
const rows = (r) => r.map((x) => `    (${x.join(", ")})`).join(",\n");
const out = [];
out.push(`-- Demo ve manuel test verisi (#67). Yalnız dev profilinde, veritabanı boşken DemoSeedRunner çalıştırır.
-- BU DOSYA ÜRETİLİR: scripts/seed-content/generate.mjs (içerik: icerik.mjs, kullanıcı verisi: kullanici-verisi.sql).
-- Elle değiştirmeyin; kaynağı değiştirip betiği yeniden çalıştırın.
-- Bütün hesapların parolası README'de; {{PASSWORD_HASH}} çalışma anında BCrypt özetiyle değiştirilir.
-- Kimlikler sabit yazılmaz; satırlar e-posta, başlık ve depolama anahtarıyla bulunur (sıralar ilerler).
-- Dosyaların kendisi seed/files/<storage_key> altında; DemoSeedRunner depolama dizinine kopyalar ve boyutlarını yazar.

-- ---------------------------------------------------------------------------
-- Kullanıcılar: 1 yönetici, ${INSTRUCTORS.length} eğitmen, ${STUDENTS.length} kursiyer
-- ---------------------------------------------------------------------------
INSERT INTO users (email, password_hash, full_name, bio, role) VALUES
${rows([
  [sql("admin@miniudemy.dev"), sql("{{PASSWORD_HASH}}"), sql("Ali Yönetici"), "NULL", sql("ADMIN")],
  ...INSTRUCTORS.map((i) => [sql(i.email), sql("{{PASSWORD_HASH}}"), sql(i.name), sql(i.bio), sql("INSTRUCTOR")]),
  ...STUDENTS.map(([e, n]) => [sql(e), sql("{{PASSWORD_HASH}}"), sql(n), "NULL", sql("STUDENT")]),
])};

-- ---------------------------------------------------------------------------
-- Dosyalar: ${files.length} (size_bytes'ı DemoSeedRunner dosyayı kopyalarken gerçek boyutla günceller)
-- ---------------------------------------------------------------------------
INSERT INTO stored_files (uploader_id, storage_key, original_name, mime_type, size_bytes)
SELECT u.id, f.storage_key, f.original_name, f.mime_type, 0
FROM (VALUES
${rows(files.map((f) => [sql(f.email), sql(f.key), sql(f.name), sql(f.mime)]))}
) AS f (email, storage_key, original_name, mime_type)
JOIN users u ON u.email = f.email;

-- ---------------------------------------------------------------------------
-- Kurslar: ${COURSES.length} (her durumdan en az bir tane)
-- ---------------------------------------------------------------------------
INSERT INTO courses (instructor_id, category_id, cover_file_id, title, description, level, learning_outcomes, status,
                     rejection_reason, unpublish_reason)
SELECT u.id, c.id, sf.id, x.title, x.description, x.level, x.outcomes, x.status, x.rejection_reason, x.unpublish_reason
FROM (VALUES
${rows(COURSES.map((c) => [sql(c.instructor), sql(c.category), sql(c.coverKey ?? null), sql(c.title), sql(c.description),
  sql(c.level), sqlArray(c.outcomes), sql(c.status), sql(c.rejectionReason ?? null), sql(c.unpublishReason ?? null)]))}
) AS x (email, category, cover_key, title, description, level, outcomes, status, rejection_reason, unpublish_reason)
JOIN users u ON u.email = x.email
JOIN categories c ON c.name = x.category
LEFT JOIN stored_files sf ON sf.storage_key = x.cover_key;

-- ---------------------------------------------------------------------------
-- Bölümler ve dersler (videoların süresi gerçek video süresi; PDF ve dokümanda okuma süresi)
-- ---------------------------------------------------------------------------
INSERT INTO sections (course_id, title, position)
SELECT co.id, s.title, s.position
FROM (VALUES
${rows(COURSES.flatMap((c) => c.sections.map((s, i) => [sql(c.title), sql(s.title), i])))}
) AS s (course_title, title, position)
JOIN courses co ON co.title = s.course_title;

INSERT INTO lessons (section_id, content_file_id, title, content_type, mandatory, duration_seconds, position)
SELECT se.id, sf.id, l.title, l.content_type, l.mandatory, l.duration, l.position
FROM (VALUES
${rows(COURSES.flatMap((c) => c.sections.flatMap((s) => s.lessons.map((l, i) => {
  const f = lessonFile.get(l);
  return [sql(c.title), sql(s.title), sql(l.title), sql(l.type), String(l.mandatory).toUpperCase(), f.duration, i, sql(f.key)];
}))))}
) AS l (course_title, section_title, title, content_type, mandatory, duration, position, file_key)
JOIN courses co ON co.title = l.course_title
JOIN sections se ON se.course_id = co.id AND se.title = l.section_title
LEFT JOIN stored_files sf ON sf.storage_key = l.file_key;

-- ---------------------------------------------------------------------------
-- Soru bankası ve bölüm sınavları (her kursta en az bir zorunlu sınav)
-- ---------------------------------------------------------------------------
INSERT INTO questions (course_id, text, points)
SELECT co.id, q.text, 10
FROM (VALUES
${rows(COURSES.flatMap((c) => c.sections.flatMap((s) => (s.quiz ? s.quiz.questions : []).map((q) => [sql(c.title), sql(q.text)]))))}
) AS q (course_title, text)
JOIN courses co ON co.title = q.course_title;

INSERT INTO options (question_id, text, is_correct)
SELECT q.id, o.text, o.correct
FROM (VALUES
${rows(COURSES.flatMap((c) => c.sections.flatMap((s) => (s.quiz ? s.quiz.questions : []).flatMap((q) => q.options.map(([t, ok]) => [sql(q.text), sql(t), String(ok).toUpperCase()])))))}
) AS o (question, text, correct)
JOIN questions q ON q.text = o.question;

INSERT INTO quizzes (section_id, title, scoring_type, pass_threshold, required)
SELECT se.id, z.title, 'SINGLE_ANSWER', z.threshold, TRUE
FROM (VALUES
${rows(COURSES.flatMap((c) => c.sections.filter((s) => s.quiz).map((s) => [sql(c.title), sql(s.title), sql(s.quiz.title), s.quiz.threshold])))}
) AS z (course_title, section_title, title, threshold)
JOIN courses co ON co.title = z.course_title
JOIN sections se ON se.course_id = co.id AND se.title = z.section_title;

INSERT INTO quiz_questions (quiz_id, question_id, position)
SELECT qz.id, q.id, x.position
FROM (VALUES
${rows(COURSES.flatMap((c) => c.sections.filter((s) => s.quiz).flatMap((s) => s.quiz.questions.map((q, i) => [sql(s.quiz.title), sql(q.text), i]))))}
) AS x (quiz_title, question, position)
JOIN quizzes qz ON qz.title = x.quiz_title
JOIN questions q ON q.text = x.question;
`);
out.push(readFileSync(TEMPLATE, "utf8"));
writeFileSync(SQL_OUT, out.join("\n"));
rmSync(WORK, { recursive: true, force: true });

// Özet
const total = readdirSync(FILES).reduce((n, f) => n + statSync(join(FILES, f)).size, 0);
const byType = {};
for (const f of files) {
  const t = f.name.split(".").pop();
  byType[t] ??= { n: 0, bytes: 0 };
  byType[t].n++;
  byType[t].bytes += statSync(join(FILES, f.key)).size;
}
console.log(`\n${files.length} dosya, toplam ${(total / MB).toFixed(2)} MB`);
for (const [t, v] of Object.entries(byType)) console.log(`  ${t}: ${v.n} dosya, ${(v.bytes / MB).toFixed(2)} MB, en büyük ${(Math.max(...files.filter((f) => f.name.endsWith(t)).map((f) => statSync(join(FILES, f.key)).size)) / 1024).toFixed(0)} KB`);
console.log(`Videolar: ${[...lessonFile.entries()].filter(([l]) => l.type === "VIDEO").map(([, f]) => f.duration + " sn").join(", ")}`);
