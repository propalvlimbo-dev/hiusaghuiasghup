#!/usr/bin/env node
/**
 * stress-bots.js — нагрузочные боты для СВОЕГО Minecraft-сервера.
 *
 * Закрывает то, чего нет ни в SoulFire, ни в обычных стресс-тестерах:
 *   ✔ быстрый вход (0,1–0,5 с на бота вместо 3–15 с),
 *   ✔ авто-регистрация / авто-логин,
 *   ✔ обход капчи: текстовая (regex) и карта-картинка (PNG + OCR или ручной ответ),
 *   ✔ НАСТОЯЩЕЕ появление и игра: боты ходят, прыгают, крутят головой, машут рукой,
 *     грузят чанки и физику — то, что реально нагружает сервер,
 *   ✔ SOCKS5-прокси на каждого бота,
 *   ✔ живая статистика: кто подключился, кто заспавнился, кто получил кик.
 *
 * Установка (Node.js 18+):
 *   npm init -y
 *   npm i mineflayer              # обязательно
 *   npm i socks                   # только если нужны прокси
 *   npm i tesseract.js            # только если нужен авто-OCR капчи
 *
 * Запуск:
 *   # 50 ботов на своём сервере
 *   node stress-bots.js --host play.example.com --count 50 --password "pass12345"
 *
 *   # с прокси (одна строка на прокси: socks5://user:pass@ip:port или ip:port)
 *   node stress-bots.js --host play.example.com --count 50 --password pass --proxy-file proxies.txt
 *
 *   # капча-картинка: авто через OCR; если не распознает — PNG лежит в captcha-images/
 *   node stress-bots.js --host play.example.com --count 20 --captcha ocr
 *
 *   # капча-картинка: ручной ввод (удобно на 1–5 ботах — проверить свой сценарий)
 *   node stress-bots.js --host play.example.com --count 3 --captcha manual
 *
 *   # полный «стресс»: движение + редкий чат + нагрузка
 *   node stress-bots.js --host play.example.com --count 100 --move --chat-every 90
 *
 * Флаги: --port --version --prefix --join-delay --radius --stats-every --log
 *        --answer-cmd "/captcha " --send-as command --register-cmd --login-cmd
 *        --reconnect --no-move --quiet
 *
 * ⚠️ Запускать только против СВОЕГО сервера или с письменного разрешения владельца.
 */

'use strict';

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const readline = require('readline');

// ---------------------------------------------------------------------------
// Аргументы
// ---------------------------------------------------------------------------
function arg(name, def) {
  const i = process.argv.indexOf('--' + name);
  if (i === -1) return def;
  const v = process.argv[i + 1];
  return v === undefined || v.startsWith('--') ? true : v;
}
const has = (name) => process.argv.includes('--' + name);

const CFG = {
  host: arg('host', 'localhost'),
  port: Number(arg('port', 25565)),
  version: arg('version', false),
  count: Number(arg('count', 10)),
  prefix: arg('prefix', 'StressBot_'),
  password: arg('password', 'pass12345'),
  joinDelay: Number(arg('join-delay', 400)),
  proxyFile: arg('proxy-file', ''),
  captcha: arg('captcha', 'ocr'),            // ocr | manual | service | off
  solver: arg('solver', 'rucaptcha'),        // rucaptcha | 2captcha | (свой URL)
  solverKey: arg('solver-key', ''),          // API-ключ сервиса распознавания
  solverUrl: arg('solver-url', ''),          // переопределить адрес API (тесты/свои сервисы)
  answerCmd: arg('answer-cmd', ''),          // напр. "/captcha " → ответ уйдёт командой
  sendAs: arg('send-as', 'chat'),            // chat | command
  registerCmd: arg('register-cmd', '/register {pass} {pass}'),
  loginCmd: arg('login-cmd', '/login {pass}'),
  move: !has('no-move') && arg('move', true) !== 'false',
  radius: Number(arg('radius', 64)),
  chatEvery: Number(arg('chat-every', 0)),   // 0 = не писать в чат
  chatText: arg('chat-text', 'привет'),
  reconnect: has('reconnect'),
  statsEvery: Number(arg('stats-every', 15)),
  outDir: arg('out-dir', 'captcha-images'),
  logFile: arg('log', 'stress-bots.log'),
  quiet: has('quiet'),
};

fs.mkdirSync(CFG.outDir, { recursive: true });
const logStream = fs.createWriteStream(CFG.logFile, { flags: 'a' });
const log = (tag, msg) => {
  const line = `${new Date().toISOString().slice(11, 19)} [${tag}] ${msg}`;
  logStream.write(line + '\n');
  if (!CFG.quiet) console.log(line);
};

// ---------------------------------------------------------------------------
// Зависимости
// ---------------------------------------------------------------------------
let mineflayer;
try { mineflayer = require('mineflayer'); } catch (e) {
  console.error('Нет пакета mineflayer. Установите:  npm i mineflayer'); process.exit(1);
}
let mc = null, SocksClient = null;
if (CFG.proxyFile) {
  try { mc = require('minecraft-protocol'); } catch (e) { /* ниже понятная ошибка */ }
  try { ({ SocksClient } = require('socks')); } catch (e) { /* ниже понятная ошибка */ }
  if (!mc || !SocksClient) {
    console.error('Для прокси нужны пакеты:  npm i socks minecraft-protocol'); process.exit(1);
  }
}

// ---------------------------------------------------------------------------
// Прокси
// ---------------------------------------------------------------------------
function parseProxy(line) {
  line = line.trim();
  if (!line) return null;
  const m = line.match(/^(?:socks5h?:\/\/)?(?:(?<user>[^:@/]+):(?<pass>[^@/]+)@)?(?<host>[^:]+):(?<port>\d+)$/);
  if (!m) return null;
  return { host: m.groups.host, port: Number(m.groups.port), user: m.groups.user, pass: m.groups.pass };
}
const PROXIES = CFG.proxyFile && fs.existsSync(CFG.proxyFile)
  ? fs.readFileSync(CFG.proxyFile, 'utf8').split('\n').map(parseProxy).filter(Boolean)
  : [];
if (CFG.proxyFile && PROXIES.length === 0) log('main', 'ВНИМАНИЕ: список прокси пуст или не разобран');

/** Создать клиент, при необходимости — через SOCKS5-прокси */
function makeClient(index, proxy) {
  const base = {
    host: CFG.host, port: CFG.port, username: `${CFG.prefix}${index}`,
    auth: 'offline', version: CFG.version, hideErrors: true,
  };
  if (!proxy) return mineflayer.createBot(base);
  const client = mc.createClient({
    ...base,
    connect: (c) => {
      SocksClient.createConnection({
        proxy: { host: proxy.host, port: proxy.port, type: 5, userId: proxy.user, password: proxy.pass },
        command: 'connect',
        destination: { host: CFG.host, port: CFG.port },
      }).then(({ socket }) => c.setSocket(socket))
        .catch((err) => c.emit('error', new Error('прокси ' + proxy.host + ':' + proxy.port + ' — ' + err.message)));
    },
  });
  return mineflayer.createBot({ ...base, client, hideErrors: true });
}

// ---------------------------------------------------------------------------
// Карта Minecraft → PNG (без внешних зависимостей)
// ---------------------------------------------------------------------------
const MAP_COLORS = require('./map-colors-1.16.json');
const CRC_TABLE = (() => { const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; t[n] = c; } return t; })();
const crc32 = (b) => { let c = 0xffffffff; for (let i = 0; i < b.length; i++) c = CRC_TABLE[(c ^ b[i]) & 0xff] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
const chunk = (type, data) => {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length, 0);
  const t = Buffer.from(type, 'ascii'); const c = Buffer.alloc(4);
  c.writeUInt32BE(crc32(Buffer.concat([t, data])), 0);
  return Buffer.concat([len, t, data, c]);
};
function encodePng(rgba, w, h) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) rgba.copy(raw, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4);
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}
function mapDataToPng(data) {
  const rgba = Buffer.alloc(128 * 128 * 4);
  for (let i = 0; i < 128 * 128; i++) {
    const hex = MAP_COLORS[data[i]] || '#00000000';
    rgba[i * 4] = parseInt(hex.slice(1, 3), 16);
    rgba[i * 4 + 1] = parseInt(hex.slice(3, 5), 16);
    rgba[i * 4 + 2] = parseInt(hex.slice(5, 7), 16);
    rgba[i * 4 + 3] = parseInt(hex.slice(7, 9), 16);
  }
  return encodePng(rgba, 128, 128);
}

// ---------------------------------------------------------------------------
// Капча
// ---------------------------------------------------------------------------
const CODE_PATTERNS = [
  /(?:captcha|капч|код|code)[^A-Za-z0-9]{0,24}([A-Za-z0-9]{3,10})/i,
  /\[[^\]]*captcha[^\]]*\]\s*([A-Za-z0-9]{3,10})/i,
];
let ocrWorker = null;
async function ocr(pngFile) {
  if (!ocrWorker) {
    let Tesseract;
    try { Tesseract = require('tesseract.js'); } catch (e) {
      log('ocr', 'нет tesseract.js (npm i tesseract.js) — сохраняю только PNG'); return null;
    }
    ocrWorker = await Tesseract.createWorker('eng');
  }
  const { data } = await ocrWorker.recognize(pngFile);
  const text = (data.text || '').replace(/[^A-Za-z0-9]/g, '');
  return text || null;
}

// --- распознавание капчи через платный API (RuCaptcha / 2Captcha) -------------
// Логика: POST base64 PNG -> получаем id задачи, затем опрашиваем результат.
// Это ровно то, что используют коммерческие бот-платформы внутри себя.
const SOLVER_URLS = {
  rucaptcha: 'https://rucaptcha.com',
  '2captcha': 'https://2captcha.com',
};

async function solveViaService(pngFile) {
  const base = CFG.solverUrl || SOLVER_URLS[CFG.solver] || SOLVER_URLS.rucaptcha;
  if (!CFG.solverKey) { log('solver', 'нет --solver-key, не могу отправить капчу'); return null; }
  const b64 = fs.readFileSync(pngFile).toString('base64');

  const form = new URLSearchParams();
  form.set('key', CFG.solverKey);
  form.set('method', 'base64');
  form.set('body', b64);
  form.set('json', '1');
  form.set('numeric', '0');
  form.set('min_len', '4');
  form.set('max_len', '10');

  const send = await fetch(base + '/in.php', { method: 'POST', body: form });
  const sent = await send.json().catch(() => null);
  if (!sent || sent.status !== 1) {
    log('solver', 'отказ сервиса: ' + JSON.stringify(sent).slice(0, 160));
    return null;
  }
  const id = sent.request;
  log('solver', 'капча отправлена, id=' + id);

  for (let i = 0; i < 24; i++) {                    // до ~2 минут
    await new Promise((r) => setTimeout(r, 5000));
    const res = await fetch(`${base}/res.php?key=${encodeURIComponent(CFG.solverKey)}&action=get&id=${id}&json=1`);
    const data = await res.json().catch(() => null);
    if (!data) continue;
    if (data.status === 1) {
      const code = String(data.request || '').replace(/[^A-Za-z0-9]/g, '');
      log('solver', 'ответ сервиса: ' + code);
      return code || null;
    }
    if (data.request && data.request !== 'CAPCHA_NOT_READY') {
      log('solver', 'ошибка: ' + String(data.request).slice(0, 80));
      return null;
    }
  }
  log('solver', 'таймаут ожидания ответа');
  return null;
}

const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
const ask = (q) => new Promise((res) => rl.question(q, (a) => res(a.trim())));

function sendAnswer(bot, code) {
  const payload = CFG.answerCmd ? CFG.answerCmd + code : code;
  bot.chat(CFG.sendAs === 'command' && !CFG.answerCmd ? '/captcha ' + code : payload);
  STATS.captchaAnswered++;
  log(bot.__name, 'ответ капчи: ' + payload);
}

// ---------------------------------------------------------------------------
// Статистика
// ---------------------------------------------------------------------------
const STATS = { started: 0, connected: 0, spawned: 0, kicked: 0, failed: 0, captchaSeen: 0, captchaAnswered: 0, joinTimes: [] };
const BOTS = new Map();

function printStats() {
  const online = [...BOTS.values()].filter((b) => b.__spawned).length;
  const avg = STATS.joinTimes.length
    ? (STATS.joinTimes.reduce((a, b) => a + b, 0) / STATS.joinTimes.length / 1000).toFixed(2) + ' с'
    : '—';
  log('stats', `онлайн ${online}/${STATS.started} | спавн ${STATS.spawned} | капч ${STATS.captchaSeen} ` +
    `(отвечено ${STATS.captchaAnswered}) | киков ${STATS.kicked} | ошибок ${STATS.failed} | ср. вход ${avg}`);
}

// ---------------------------------------------------------------------------
// Нагрузка: движение и действия
// ---------------------------------------------------------------------------
function moveBurst(bot, ms = 2000) {
  if (!bot.entity) return;
  try {
    const yaw = Math.random() * Math.PI * 2;
    bot.look(yaw, 0, true);
    bot.setControlState('forward', true);
    if (Math.random() < 0.6) bot.setControlState('sprint', true);
    if (Math.random() < 0.5) bot.setControlState('jump', true);
    setTimeout(() => {
      try {
        bot.setControlState('forward', false);
        bot.setControlState('sprint', false);
        bot.setControlState('jump', false);
      } catch (e) { /* бот отключился */ }
    }, ms);
  } catch (e) { /* ignore */ }
}

function startLoad(bot) {
  const pos0 = bot.entity && bot.entity.position ? bot.entity.position.clone() : null;
  bot.__loadTimer = setInterval(() => {
    if (!bot.entity) return;
    // если ушли слишком далеко — идём обратно
    if (pos0) {
      const d = bot.entity.position.distanceTo(pos0);
      if (d > CFG.radius) {
        const yaw = Math.atan2(pos0.x - bot.entity.position.x, pos0.z - bot.entity.position.z);
        bot.look(yaw, 0, true);
        bot.setControlState('forward', true);
        setTimeout(() => { try { bot.setControlState('forward', false); } catch (e) {} }, 1500);
        return;
      }
    }
    moveBurst(bot, 1200 + Math.random() * 2500);
    if (Math.random() < 0.3) { try { bot.swingArm(); } catch (e) {} }
  }, 3000 + Math.random() * 3000);

  if (CFG.chatEvery > 0) {
    bot.__chatTimer = setInterval(() => {
      try { bot.chat(CFG.chatText); } catch (e) {}
    }, CFG.chatEvery * 1000 + Math.random() * 5000);
  }
}

// ---------------------------------------------------------------------------
// Один бот
// ---------------------------------------------------------------------------
function spawnBot(index) {
  const name = `${CFG.prefix}${index}`;
  const proxy = PROXIES.length ? PROXIES[index % PROXIES.length] : null;
  STATS.started++;
  const t0 = Date.now();

  let bot;
  try {
    bot = makeClient(index, proxy);
  } catch (e) {
    STATS.failed++; log(name, 'не удалось создать: ' + e.message); return;
  }
  bot.__name = name;
  bot.__spawned = false;
  bot.__didRegister = false;
  bot.__didLogin = false;
  bot.__lastMapAt = 0;
  BOTS.set(name, bot);

  bot.once('spawn', () => {
    bot.__spawned = true;
    STATS.spawned++;
    STATS.joinTimes.push(Date.now() - t0);
    log(name, `появился на сервере за ${((Date.now() - t0) / 1000).toFixed(2)} с` +
      (proxy ? ` (прокси ${proxy.host})` : ''));
    if (CFG.move) startLoad(bot);
  });
  bot.on('connect', () => { STATS.connected++; });

  bot.on('messagestr', (msg) => {
    if (!msg) return;
    log(name, 'чат: ' + msg);

    // сервер требует подвигаться перед вводом капчи
    if (/(move|двиг|walk)/i.test(msg) && CFG.move) moveBurst(bot, 1200);

    // регистрация / логин
    if (/regist|регистр|зарегистр/i.test(msg) && !bot.__didRegister && CFG.registerCmd) {
      bot.__didRegister = true;
      const cmd = CFG.registerCmd.replace(/\{pass\}/g, CFG.password);
      setTimeout(() => { try { bot.chat(cmd); } catch (e) {} }, 500 + Math.random() * 500);
      log(name, 'регистрация: ' + cmd.replace(CFG.password, '***'));
      return;
    }
    if (/(\/log(in)?|войти|авториз)/i.test(msg) && !bot.__didLogin && CFG.loginCmd) {
      bot.__didLogin = true;
      const cmd = CFG.loginCmd.replace(/\{pass\}/g, CFG.password);
      setTimeout(() => { try { bot.chat(cmd); } catch (e) {} }, 500 + Math.random() * 500);
      log(name, 'логин: ' + cmd.replace(CFG.password, '***'));
      return;
    }

    // текстовая капча
    for (const re of CODE_PATTERNS) {
      const m = msg.match(re);
      if (m) {
        STATS.captchaSeen++;
        setTimeout(() => sendAnswer(bot, m[1]), 300 + Math.random() * 300);
        return;
      }
    }
  });

  // капча-картинка
  bot._client.on('map', async (packet) => {
    try {
      if (!packet || !packet.data || packet.columns !== 128) return;
      if (Date.now() - bot.__lastMapAt < 2500) return;   // защита от дублей
      bot.__lastMapAt = Date.now();
      const file = path.join(CFG.outDir, `${name}_map_${String(packet.itemDamage).padStart(6, '0')}.png`);
      fs.writeFileSync(file, mapDataToPng(packet.data));
      STATS.captchaSeen++;
      log(name, '[КАПЧА] картинка: ' + file);
      if (CFG.captcha === 'off') return;

      let code = null;
      if (CFG.captcha === 'service') {
        code = await solveViaService(file);
      } else if (CFG.captcha === 'ocr') {
        code = await ocr(file);
        log(name, code ? 'OCR прочитал: ' + code : 'OCR не распознал — картинка сохранена');
      } else {
        code = (await ask(`Введи код для ${name} (Enter — пропустить): `)).replace(/[^A-Za-z0-9]/g, '');
      }
      if (code) sendAnswer(bot, code);
    } catch (e) {
      log(name, 'ошибка капчи: ' + e.message);
    }
  });

  bot.on('kicked', (r) => { STATS.kicked++; log(name, 'КИК: ' + JSON.stringify(r).slice(0, 160)); });
  bot.on('error', (e) => { STATS.failed++; log(name, 'ошибка: ' + (e.message || e)); });
  bot.on('end', (reason) => {
    bot.__spawned = false;
    if (bot.__loadTimer) clearInterval(bot.__loadTimer);
    if (bot.__chatTimer) clearInterval(bot.__chatTimer);
    log(name, 'отключился (' + (reason || '?') + ')');
    if (CFG.reconnect) setTimeout(() => { BOTS.delete(name); spawnBot(index); }, 5000);
  });
  return bot;
}

// ---------------------------------------------------------------------------
// Запуск
// ---------------------------------------------------------------------------
(async () => {
  log('main', `Цель: ${CFG.host}:${CFG.port} | ботов: ${CFG.count} | капча: ${CFG.captcha} | ` +
    `прокси: ${PROXIES.length || 'нет'} | движение: ${CFG.move ? 'да' : 'нет'}`);
  log('main', 'Только свой сервер или письменное разрешение владельца.');

  for (let i = 1; i <= CFG.count; i++) {
    spawnBot(i);
    await new Promise((r) => setTimeout(r, CFG.joinDelay));
  }

  const statsTimer = setInterval(printStats, CFG.statsEvery * 1000);
  printStats();

  const bye = () => {
    clearInterval(statsTimer);
    printStats();
    log('main', 'остановлено пользователем');
    process.exit(0);
  };
  process.on('SIGINT', bye);
  process.on('SIGTERM', bye);
})();
