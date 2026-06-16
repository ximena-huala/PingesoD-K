// Baja el catálogo completo de Falabella (GetProducts, paginando) y lo deja en
// skus-falabella.csv. La idea es cruzar esos SKUs contra Bsale y MercadoLibre
// para ver cuánto calza. Solo lectura.
//
// Uso:  node extraer-skus.mjs
// Las credenciales salen de ../.env y nunca se imprimen.
import { createHmac } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const env = Object.fromEntries(
  readFileSync(join(here, '..', '.env'), 'utf8').split('\n')
    .filter(l => l.includes('=') && !l.trim().startsWith('#'))
    .map(l => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()])
);

function llamar(action, extra = {}) {
  const now = new Date();
  const offsetMin = now.getTimezoneOffset();
  const sign = offsetMin <= 0 ? '+' : '-';
  const pad = n => String(n).padStart(2, '0');
  const tz = `${sign}${pad(Math.floor(Math.abs(offsetMin) / 60))}:${pad(Math.abs(offsetMin) % 60)}`;
  const timestamp = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}` +
    `T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}` + tz;

  const params = {
    Action: action, Format: 'JSON', Timestamp: timestamp,
    UserID: env.FALABELLA_USER_ID, Version: '1.0', ...extra,
  };
  const stringToSign = Object.keys(params).sort()
    .map(k => encodeURIComponent(k) + '=' + encodeURIComponent(params[k]))
    .join('&');
  const signature = createHmac('sha256', env.FALABELLA_API_KEY).update(stringToSign).digest('hex');
  const url = 'https://sellercenter-api.falabella.com/?' + stringToSign + '&Signature=' + signature;

  const raw = execFileSync('curl.exe',
    ['-s', '--ssl-no-revoke', '-A', `${env.FALABELLA_SELLER_ID}/Java/21`, url],
    { encoding: 'utf8', maxBuffer: 100 * 1024 * 1024 });
  const json = JSON.parse(raw);
  if (json.ErrorResponse) {
    const h = json.ErrorResponse.Head;
    throw new Error(`${h.ErrorCode}: ${h.ErrorMessage}`);
  }
  return json.SuccessResponse?.Body ?? {};
}

const csvEscape = v => {
  const s = String(v ?? '');
  return /[",\n;]/.test(s) ? '"' + s.replaceAll('"', '""') + '"' : s;
};

const LIMIT = 100;
const filas = [];
let offset = 0;

for (let pagina = 1; pagina <= 100; pagina++) {
  const body = llamar('GetProducts', { Limit: String(LIMIT), Offset: String(offset) });
  let productos = body?.Products?.Product ?? [];
  if (!Array.isArray(productos)) productos = [productos]; // un solo producto llega como objeto
  if (productos.length === 0) break;

  if (pagina === 1) {
    console.error('Campos disponibles en el primer producto:', Object.keys(productos[0]).join(', '));
  }
  for (const p of productos) {
    // Precio y estado vienen anidados por unidad de negocio (Falabella global)
    let bu = p.BusinessUnits?.BusinessUnit ?? {};
    if (Array.isArray(bu)) bu = bu[0] ?? {};
    filas.push([
      p.SellerSku, p.ParentSku, p.ShopSku, p.Name, p.Brand,
      p.PrimaryCategory ?? p.PrimaryCategoryId,
      bu.Price ?? '', bu.SpecialPrice ?? '', bu.Status ?? '', bu.Stock ?? '',
    ].map(csvEscape).join(','));
  }
  console.error(`Página ${pagina}: ${productos.length} productos (acumulado ${filas.length})`);
  if (productos.length < LIMIT) break;
  offset += LIMIT;
  execFileSync(process.execPath, ['-e', 'setTimeout(()=>{}, 800)']); // pausa ~rate limit
}

const csv = 'SellerSku,ParentSku,ShopSku,Name,Brand,PrimaryCategory,Price,SpecialPrice,Status,Stock\n' + filas.join('\n') + '\n';
const salida = join(here, 'skus-falabella.csv');
writeFileSync(salida, '﻿' + csv, 'utf8'); // BOM para que Excel abra bien acentos
console.log(`OK: ${filas.length} productos exportados a ${salida}`);
