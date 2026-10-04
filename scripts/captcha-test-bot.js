#!/usr/bin/env node
/**
 * captcha-test-bot.js — быстрый стенд для проверки СВОЕЙ капчи на Minecraft-сервере.
 *
 * Зачем: SoulFire заходит 3–15 с и держит настоящий клиент. Для цикла
 * «бот увидел капчу → я посмотрел → я усилил защиту → повторил» нужен инструмент
 * с входом за доли секунды и понятным выводом. Это он.
 *
 * Что умеет:
 *   • заходит ботом за ~0,3 с (mineflayer, протокол без рендера);
 *   • автоматически отвечает на текстовую капчу/регистрацию/логин;
 *   • ЛОВИТ КАПЧУ-КАРТИНКУ: декодирует пакет карты, сохраняет PNG в папку
 *     и (в ручном режиме) спрашивает у вас код — вы его вводите, бот отвечает;
 *   • ведёт лог в файл, чтобы было видно, что именно сломалось.
 *
 * Установка:
 *   npm init -y
 *   npm i mineflayer
 *
 * Запуск:
 *   node captcha-test-bot.js --host play.cequar.ru --port 25565 --name TestBot_1
 *   node captcha-test-bot.js --host play.cequar.ru --count 5 --password "pass123"
 *   node captcha-test-bot.js --host play.cequar.ru --ocr        # нужен tesseract.js
 *
 * Ручной режим (по умолчанию): как только пришла карта-капча, скрипт пишет
 *   [КАПЧА] картинка: captcha-images/TestBot_1_map_000123.png
 *   Введи код для TestBot_1 (или Enter чтобы пропустить):
 * Вы смотрите PNG и вводите код — бот отправляет его в чат. Идеально, чтобы
 * быстро понять, какой именно вызов ваша капча НЕ ломает ботов.
 *
 * Использовать только на СВОЁМ сервере или с письменного разрешения владельца.
 */

'use strict';

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const readline = require('readline');

let mineflayer;
try {
  mineflayer = require('mineflayer');
} catch (e) {
  console.error('Нет пакета mineflayer. Установите:  npm i mineflayer');
  process.exit(1);
}

// ---------------------------------------------------------------------------
// Аргументы
// ---------------------------------------------------------------------------
function arg(name, def) {
  const i = process.argv.indexOf('--' + name);
  if (i === -1) return def;
  const v = process.argv[i + 1];
  return v === undefined || v.startsWith('--') ? true : v;
}

const CFG = {
  host: arg('host', 'localhost'),
  port: Number(arg('port', 25565)),
  version: arg('version', false),          // false = автоопределение
  count: Number(arg('count', 1)),
  prefix: arg('name-prefix', 'TestBot_'),
  password: arg('password', 'pass12345'),
  outDir: arg('out-dir', 'captcha-images'),
  logFile: arg('log', 'captcha-test.log'),
  registerCmd: arg('register-cmd', '/register {pass} {pass}'),
  loginCmd: arg('login-cmd', '/login {pass}'),
  sendAs: arg('send-as', 'chat'),           // chat | command
  captchaCommand: arg('captcha-command', ''), // напр. "/captcha " → ответ уйдёт командой
  answerMode: arg('answer-mode', 'manual'), // manual | ocr | none
  ocrLangs: arg('ocr-langs', 'eng'),
  quiet: !!arg('quiet', false),
  autoReconnect: !!arg('auto-reconnect', false),
  joinDelayMs: Number(arg('join-delay', 700)),
};

fs.mkdirSync(CFG.outDir, { recursive: true });
const logStream = fs.createWriteStream(CFG.logFile, { flags: 'a' });

function log(tag, msg) {
  const line = `${new Date().toISOString().slice(11, 19)} [${tag}] ${msg}`;
  logStream.write(line + '\n');
  if (!CFG.quiet) console.log(line);
}

// ---------------------------------------------------------------------------
// Карта Minecraft → PNG (без внешних зависимостей)
// ---------------------------------------------------------------------------
const MAP_COLORS = require('./map-colors-1.16.json'); // #RRGGBBAA, 248 записей

const PNG_CRC = (() => {
  const table = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c;
  }
  return table;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = PNG_CRC[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const typeBuf = Buffer.from(type, 'ascii');
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
  return Buffer.concat([len, typeBuf, data, crc]);
}

/** RGBA-пиксели (Buffer width*height*4) → PNG-буфер */
function encodePng(rgba, width, height) {
  const raw = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y++) {
    raw[y * (width * 4 + 1)] = 0; // фильтр None
    rgba.copy(raw, y * (width * 4 + 1) + 1, y * width * 4, (y + 1) * width * 4);
  }
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;   // bit depth
  ihdr[9] = 6;   // RGBA
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}

/** Данные пакета карты (16384 байта) → PNG-буфер 128×128 */
function mapDataToPng(data) {
  const W = 128, H = 128;
  const rgba = Buffer.alloc(W * H * 4);
  for (let i = 0; i < W * H; i++) {
    const hex = MAP_COLORS[data[i]] || '#00000000';
    rgba[i * 4] = parseInt(hex.slice(1, 3), 16);     // R
    rgba[i * 4 + 1] = parseInt(hex.slice(3, 5), 16); // G
    rgba[i * 4 + 2] = parseInt(hex.slice(5, 7), 16); // B
    rgba[i * 4 + 3] = parseInt(hex.slice(7, 9), 16); // A
  }
  return encodePng(rgba, W, H);
}

// ---------------------------------------------------------------------------
// Распознавание кода
// ---------------------------------------------------------------------------
const patterns = [
  /(?:captcha|капч|код|code)[^A-Za-z0-9]{0,24}([A-Za-z0-9]{3,10})/i,
  /\[[^\]]*captcha[^\]]*\]\s*([A-Za-z0-9]{3,10})/i,
  /^\s*([A-Za-z0-9]{4,8})\s*$/,
];

function extractCode(text) {
  for (const re of patterns) {
    const m = text.match(re);
    if (m) return m[1];
  }
  return null;
}

// ---------------------------------------------------------------------------
// Ответы на текст
// ---------------------------------------------------------------------------
const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
const pendingQuestion = { active: false };

function ask(question) {
  return new Promise((resolve) => {
    pendingQuestion.active = true;
    rl.question(question, (answer) => {
      pendingQuestion.active = false;
      resolve(answer.trim());
    });
  });
}

/** Отправить ответ капчи: как чат-сообщение или как команду */
function sendCaptcha(bot, code) {
  const payload = CFG.captchaCommand ? CFG.captchaCommand + code : code;
  if (CFG.sendAs === 'command' && !CFG.captchaCommand) {
    bot.chat('/captcha ' + code);
  } else {
    bot.chat(payload);
  }
  log(bot.__name, `ответ капчи отправлен: ${payload.replace(/\n/g, ' ')}`);
}

// ---------------------------------------------------------------------------
// Один бот
// ---------------------------------------------------------------------------
let ocrWorker = null;
async function getOcr() {
  if (ocrWorker) return ocrWorker;
  let Tesseract;
  try {
    Tesseract = require('tesseract.js');
  } catch (e) {
    console.error('Нет пакета tesseract.js. Установите:  npm i tesseract.js');
    process.exit(1);
  }
  ocrWorker = await Tesseract.createWorker(CFG.ocrLangs);
  return ocrWorker;
}

function spawnBot(index) {
  const name = `${CFG.prefix}${index}`;
  const bot = mineflayer.createBot({
    host: CFG.host,
    port: CFG.port,
    username: name,
    version: CFG.version,
    auth: 'offline',
    hideErrors: CFG.quiet,
  });
  bot.__name = name;
  bot.__lastCaptcha = { code: null, at: 0 };

  bot.once('spawn', () => {
    log(name, `вошёл: ${bot.game.version} (${bot.game.difficulty ?? '?'}), мир ${bot.game.dimension ?? '?'}`);
  });

  bot.on('messagestr', (msg) => {
    if (!msg || /^\s*$/.test(msg)) return;
    log(name, 'чат: ' + msg);

    // регистрация / логин
    if (/\/(reg(ister)?)?\b/i.test(msg) && !/logged|success|успешно/i.test(msg)) {
      if (/regist|регистр|зарегистр/i.test(msg) && !bot.__didRegister) {
        bot.__didRegister = true;
        const cmd = CFG.registerCmd.replace(/\{pass\}/g, CFG.password);
        setTimeout(() => bot.chat(cmd), 600 + Math.random() * 400);
        log(name, 'отправляю: ' + cmd.replace(CFG.password, '***'));
        return;
      }
      if (/log(in)?\b|войти|авториз/i.test(msg) && !bot.__didLogin) {
        bot.__didLogin = true;
        const cmd = CFG.loginCmd.replace(/\{pass\}/g, CFG.password);
        setTimeout(() => bot.chat(cmd), 600 + Math.random() * 400);
        log(name, 'отправляю: ' + cmd.replace(CFG.password, '***'));
        return;
      }
    }

    // текстовая капча
    const code = extractCode(msg);
    if (code && Date.now() - bot.__lastCaptcha.at > 3000) {
      bot.__lastCaptcha = { code, at: Date.now() };
      log(name, `распознал код из чата: ${code}`);
      setTimeout(() => sendCaptcha(bot, code), 400 + Math.random() * 300);
    }
  });

  // ---- карта-капча ----
  bot._client.on('map', async (packet) => {
    try {
      if (!packet || !packet.data || packet.columns !== 128) return;
      const png = mapDataToPng(packet.data);
      const file = path.join(CFG.outDir, `${name}_map_${String(packet.itemDamage).padStart(6, '0')}.png`);
      fs.writeFileSync(file, png);
      log(name, `[КАПЧА] картинка сохранена: ${file} (map id ${packet.itemDamage})`);

      if (CFG.answerMode === 'none') return;

      let code = null;
      if (CFG.answerMode === 'ocr') {
        const worker = await getOcr();
        const { data } = await worker.recognize(file);
        code = (data.text || '').replace(/[^A-Za-z0-9]/g, '');
        log(name, `OCR прочитал: «${code}»`);
      } else {
        const answer = await ask(`Введи код для ${name} (Enter — пропустить): `);
        code = answer.replace(/[^A-Za-z0-9]/g, '');
      }
      if (code) sendCaptcha(bot, code);
    } catch (e) {
      log(name, 'ошибка разбора карты: ' + e.message);
    }
  });

  bot.on('kicked', (reason) => log(name, 'кик: ' + JSON.stringify(reason).slice(0, 200)));
  bot.on('error', (e) => log(name, 'ошибка: ' + (e.message || e)));
  bot.on('end', (reason) => {
    log(name, `отключился (${reason || '?'})`);
    if (CFG.autoReconnect) {
      setTimeout(() => spawnBot(index), 3000);
    }
  });
  return bot;
}

// ---------------------------------------------------------------------------
(async () => {
  log('main', `Стенд капчи: ${CFG.host}:${CFG.port}, ботов ${CFG.count}, режим ответа: ${CFG.answerMode}`);
  log('main', 'Напоминание: только свой сервер или письменное разрешение владельца.');
  for (let i = 1; i <= CFG.count; i++) {
    spawnBot(i);
    await new Promise((r) => setTimeout(r, CFG.joinDelayMs));
  }
  process.on('SIGINT', () => {
    log('main', 'выход');
    process.exit(0);
  });
})();
