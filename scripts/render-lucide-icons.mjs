// Иконки UI из открытого набора Lucide (лицензия ISC, https://lucide.dev).
// SVG → белые PNG с прозрачностью под каждый масштаб интерфейса:
//   <name>_x1..x4.png    — 16 единиц (16/32/48/64 px)
//   <name>_10_x1..x4.png — 10 единиц (10/20/30/40 px), сайдбар/поиск
// Толщина линии подбирается под размер в пикселях: на мелких — не меньше ~1.1 px,
// на крупных — тонкая, как в SF Symbols.
//
// Запуск (зависимости — во временной папке, в репозиторий не попадают):
//   mkdir -p /tmp/tools && cd /tmp/tools && npm i @resvg/resvg-js lucide-static
//   NODE_PATH=/tmp/tools/node_modules node scripts/render-lucide-icons.mjs
import { createRequire } from "node:module";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const tools = process.env.NODE_PATH || "/tmp/tools/node_modules";
const { Resvg } = require(path.join(tools, "@resvg/resvg-js"));
const iconsDir = path.join(tools, "lucide-static/icons");

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const out = path.join(root, "src/main/resources/assets/elytrixclient/textures/gui/icons");

// наше имя → имя в Lucide
const MAP = {
  home: "house", bots: "bot", proxy: "waypoints", globe: "globe", wifi: "wifi",
  console: "square-terminal", settings: "settings", close: "x", chevron: "chevron-down",
  check: "check", search: "search", plus: "plus", minus: "minus", dots: "ellipsis",
  bolt: "zap", play: "play", stop: "square", refresh: "refresh-cw", trash: "trash-2",
  copy: "copy", key: "key-round", clock: "clock", user: "user", list: "list",
  folder: "folder", chart: "chart-no-axes-column", shield: "shield", server: "server",
  palette: "palette", sound: "volume-2", users: "users", cloud: "cloud", power: "power",
  image: "image", music: "music",
};

function strokeFor(px) {
  // в единицах viewBox 24: px-толщина = sw * px / 24
  const targetPx = Math.max(1.1, px * 0.075);     // 10px → 1.1, 30px → 2.25, 64px → 4.8
  return Math.min(2.4, Math.max(1.25, (targetPx * 24) / px));
}

let n = 0;
for (const [name, lucide] of Object.entries(MAP)) {
  const src = fs.readFileSync(path.join(iconsDir, lucide + ".svg"), "utf8");
  for (const [base, suffix] of [[16, ""], [10, "_10"]]) {
    for (let k = 1; k <= 4; k++) {
      const px = base * k;
      const sw = strokeFor(px).toFixed(2);
      const svg = src
        .replace(/stroke="currentColor"/g, 'stroke="#ffffff"')
        .replace(/stroke-width="[^"]*"/g, `stroke-width="${sw}"`)
        .replace(/class="[^"]*"/g, "");
      const png = new Resvg(svg, { fitTo: { mode: "width", value: px }, background: "rgba(0,0,0,0)" })
        .render().asPng();
      fs.writeFileSync(path.join(out, `${name}${suffix}_x${k}.png`), png);
      n++;
    }
  }
}
fs.copyFileSync(path.join(tools, "lucide-static/LICENSE"), path.join(out, "LICENSE-lucide.txt"));
console.log(`Lucide: ${Object.keys(MAP).length} иконок, ${n} файлов -> ${path.relative(root, out)}`);
