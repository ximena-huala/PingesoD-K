// Prueba rápida de la firma contra la API real de Falabella, desde la terminal.
// Hace lo mismo que el script de Postman y que HmacSignatureService.java, para
// poder verificar credenciales/firma sin levantar el backend. Solo lectura.
//
// Uso:  node validar-firma.mjs <Action> [Key=Value ...]
// Las credenciales salen de ../.env y nunca se imprimen.
import { createHmac } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const envFile = join(dirname(fileURLToPath(import.meta.url)), '..', '.env');
const env = Object.fromEntries(
  readFileSync(envFile, 'utf8').split('\n')
    .filter(l => l.includes('=') && !l.trim().startsWith('#'))
    .map(l => [l.slice(0, l.indexOf('=')).trim(), l.slice(l.indexOf('=') + 1).trim()])
);

const [action, ...extra] = process.argv.slice(2);
if (!action) { console.error('Falta el Action'); process.exit(1); }

// Timestamp ISO 8601 con hora LOCAL y offset local
const now = new Date();
const offsetMin = now.getTimezoneOffset();
const sign = offsetMin <= 0 ? '+' : '-';
const pad = n => String(n).padStart(2, '0');
const tz = `${sign}${pad(Math.floor(Math.abs(offsetMin) / 60))}:${pad(Math.abs(offsetMin) % 60)}`;
const timestamp = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}` +
  `T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}` + tz;

const params = {
  Action: action,
  Format: 'JSON',
  Timestamp: timestamp,
  UserID: env.FALABELLA_USER_ID,
  Version: '1.0',
};
for (const kv of extra) {
  const i = kv.indexOf('=');
  params[kv.slice(0, i)] = kv.slice(i + 1);
}

const stringToSign = Object.keys(params).sort()
  .map(k => encodeURIComponent(k) + '=' + encodeURIComponent(params[k]))
  .join('&');
const signature = createHmac('sha256', env.FALABELLA_API_KEY)
  .update(stringToSign).digest('hex');

const query = stringToSign + '&Signature=' + signature;
const url = 'https://sellercenter-api.falabella.com/?' + query;

console.log('stringToSign:', stringToSign);

// curl.exe nativo de Windows: valida TLS con el almacén del sistema (schannel).
// (fetch de Node falla aquí: el AV local intercepta SSL y Node no ve esa CA.)
const { execFileSync } = await import('node:child_process');
const raw = execFileSync('curl.exe', [
  '-s', '--ssl-no-revoke', '-w', '\n__HTTP_STATUS__%{http_code}',
  '-A', `${env.FALABELLA_SELLER_ID}/Java/21`,
  url,
], { encoding: 'utf8', maxBuffer: 50 * 1024 * 1024 });
const statusMark = raw.lastIndexOf('\n__HTTP_STATUS__');
const body = raw.slice(0, statusMark);
console.log('HTTP', raw.slice(statusMark + 16).trim());
try {
  const json = JSON.parse(body);
  if (json.ErrorResponse) {
    const h = json.ErrorResponse.Head;
    console.log(`❌ ${h.ErrorCode}: ${h.ErrorMessage}`);
    process.exit(2);
  }
  const bodyStr = JSON.stringify(json.SuccessResponse?.Body ?? json, null, 2);
  console.log(`✅ SuccessResponse (${bodyStr.length} chars de Body)`);
  console.log(bodyStr.length > 3000 ? bodyStr.slice(0, 3000) + '\n…(truncado)' : bodyStr);
} catch {
  console.log('Respuesta no-JSON:', body.slice(0, 500));
  process.exit(3);
}
