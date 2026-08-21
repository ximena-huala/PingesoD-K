// Cliente HTTP del backend DK. Guarda el JWT en localStorage y lo adjunta a cada
// llamada. La URL base se resuelve así:
//   1. Si VITE_API_URL está definida, se usa tal cual.
//   2. En build de producción (PROD), queda vacía → llamadas relativas al mismo
//      origen (/api/...). Así funciona detrás de nginx tanto en HTTP como HTTPS,
//      sin CORS y sin hardcodear la IP/dominio.
//   3. En dev (npm run dev), apunta a localhost:8080 (el backend local).
const BASE =
  (import.meta as any).env?.VITE_API_URL ??
  (import.meta as any).env?.VITE_API_BASE_URL ??
  (import.meta as any).env?.FRONTEND_API_BASE_URL ??
  ((import.meta as any).env?.PROD ? "" : "http://localhost:8080");

let token: string | null = localStorage.getItem("dk_token");

export function isAuthed(): boolean {
  return !!token;
}

export function logout(): void {
  token = null;
  localStorage.removeItem("dk_token");
  window.dispatchEvent(new Event("dk:logout"));
}

function authHeaders(): Record<string, string> {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function parseErrorBody(res: Response, path: string): Promise<string> {
  try {
    const text = await res.text();
    if (!text) return `Error ${res.status} en ${path}`;
    const json = JSON.parse(text) as { message?: string; error?: string };
    return json.message ?? json.error ?? text;
  } catch {
    return `Error ${res.status} en ${path}`;
  }
}

async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`, { headers: authHeaders() });
  if (res.status === 401) {
    logout();
    throw new Error("Sesión expirada, vuelve a iniciar sesión");
  }
  if (!res.ok) throw new Error(await parseErrorBody(res, path));
  return res.json() as Promise<T>;
}

async function sendJson<T>(path: string, method: "POST" | "PUT" | "DELETE", body?: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401) {
    logout();
    throw new Error("Sesión expirada, vuelve a iniciar sesión");
  }
  if (!res.ok) throw new Error(await parseErrorBody(res, path));
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export type LoginResponse = { token: string; nombre: string; email: string };

export async function login(email: string, password: string): Promise<LoginResponse> {
  const res = await fetch(`${BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) throw new Error("Credenciales inválidas");
  const data: LoginResponse = await res.json();
  token = data.token;
  localStorage.setItem("dk_token", token);
  return data;
}

// ─── Rentabilidad ─────────────────────────────────────────────────────────────

export type RentabilidadDetalle = {
  ventaId: string;
  fecha: string;
  canal: string;
  sku: string;
  producto: string;
  categoria: string;
  precioVenta: number;
  descuento: number;
  ingresoNeto: number;
  costoProducto: number;
  comision: number;
  logistica: number;
  costoOperacional: number;
  costoTotal: number;
  margen: number;
  margenPorcentaje: number;
};

function query(params?: Record<string, string | undefined>): string {
  if (!params) return "";
  const qs = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) if (v) qs.set(k, v);
  const s = qs.toString();
  return s ? `?${s}` : "";
}

export function getDetalle(params?: Record<string, string | undefined>): Promise<RentabilidadDetalle[]> {
  return getJson<RentabilidadDetalle[]>(`/api/rentabilidad/detalle${query(params)}`);
}

export function getResumen(params?: Record<string, string | undefined>): Promise<any> {
  return getJson<any>(`/api/rentabilidad/resumen${query(params)}`);
}

// ─── Integraciones ──────────────────────────────────────────────────────────────

export type FalabellaEstado = {
  canal: string;
  sellerId: string;
  credencialesConfiguradas: boolean;
  conexionOk: boolean;
  mensaje: string;
  ventasCargadas: number;
  ultimaVenta: string | null;
};

export function getEstadoFalabella(): Promise<FalabellaEstado> {
  return getJson<FalabellaEstado>("/api/integraciones/falabella/estado");
}

// Explorador: consultas en vivo a la API de Falabella (devuelven el JSON crudo).
export function falabellaOrders(): Promise<any> {
  return getJson<any>("/api/integraciones/falabella/orders");
}
export function falabellaProducts(limit = 5): Promise<any> {
  return getJson<any>(`/api/integraciones/falabella/products?limit=${limit}`);
}
export function falabellaCategories(): Promise<any> {
  return getJson<any>("/api/integraciones/falabella/categories");
}
export function falabellaBrands(): Promise<any> {
  return getJson<any>("/api/integraciones/falabella/brands");
}
export function falabellaTestFirma(): Promise<any> {
  return getJson<any>("/api/integraciones/falabella/test-firma");
}

// ─── Reporte Excel ──────────────────────────────────────────────────────────────

/** Descarga el reporte de rentabilidad en Excel (bytes). desde/hasta son obligatorios. */
export async function descargarExcel(desde: string, hasta: string): Promise<Blob> {
  const res = await fetch(`${BASE}/api/reporte/excel?desde=${desde}&hasta=${hasta}`, {
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(`Error ${res.status} al generar el Excel`);
  return res.blob();
}

// ─── Import catálogo Bsale ────────────────────────────────────────────────────

export type BsaleImportResult = Record<string, any>;

/**
 * Sube un archivo exportado de Bsale (Excel/CSV) al importador manual del backend.
 * `tipo` indica si el archivo es de "productos" o de "stock".
 */
export async function importarBsale(archivo: File, tipo: "productos" | "stock"): Promise<BsaleImportResult> {
  const form = new FormData();
  form.append(tipo, archivo);
  const res = await fetch(`${BASE}/api/integraciones/bsale/import`, {
    method: "POST",
    headers: authHeaders(), // sin Content-Type: el navegador pone el boundary del multipart
    body: form,
  });
  if (!res.ok) throw new Error(`Error ${res.status} en la importación`);
  return res.json();
}

// ─── Canales de venta y comisiones ───────────────────────────────────────────
// Corresponde a CanalController/CanalService del backend (/api/canales).
// Un CanalVenta es un marketplace/tienda (ej: MercadoLibre, Falabella).
// Cada canal tiene una lista de CostoCanal (comisión %, envío, logística, etc.)
// con vigencia por fecha; fechaFin = null significa "vigente".

export type TipoCosto =
  | "COMISION_PORCENTAJE"
  | "COSTO_ENVIO_FIJO"
  | "COSTO_ENVIO_PORCENTAJE"
  | "COSTO_LOGISTICO"
  | "PUBLICIDAD"
  | "OTRO";

export const TIPOS_COSTO: { value: TipoCosto; label: string; esPorcentajeDefault: boolean }[] = [
  { value: "COMISION_PORCENTAJE", label: "Comisión (%)", esPorcentajeDefault: true },
  { value: "COSTO_ENVIO_FIJO", label: "Envío (monto fijo)", esPorcentajeDefault: false },
  { value: "COSTO_ENVIO_PORCENTAJE", label: "Envío (%)", esPorcentajeDefault: true },
  { value: "COSTO_LOGISTICO", label: "Logística / Fulfillment", esPorcentajeDefault: false },
  { value: "PUBLICIDAD", label: "Publicidad", esPorcentajeDefault: false },
  { value: "OTRO", label: "Otro", esPorcentajeDefault: false },
];

export type CanalVenta = {
  id: string;
  nombre: string;
  tipo: "MARKETPLACE" | "TIENDA_WEB_PROPIA" | "TIENDA_FISICA";
  activo: boolean;
};

export type CostoCanal = {
  id: string;
  tipoCosto: TipoCosto;
  categoria: string | null;
  descripcion: string | null;
  valor: number;
  esPorcentaje: boolean;
  fechaInicio: string; // yyyy-MM-dd
  fechaFin: string | null;
  createdAt?: string;
};

export type CostoCanalInput = {
  tipoCosto: TipoCosto;
  categoria?: string | null;
  descripcion?: string | null;
  valor: number;
  esPorcentaje: boolean;
  fechaInicio?: string;
  fechaFin?: string | null;
};

export function getCanales(): Promise<CanalVenta[]> {
  return getJson<CanalVenta[]>("/api/canales");
}

export function getCostosCanal(canalId: string): Promise<CostoCanal[]> {
  return getJson<CostoCanal[]>(`/api/canales/${canalId}/costos`);
}

export function crearCostoCanal(canalId: string, costo: CostoCanalInput): Promise<CostoCanal> {
  return sendJson<CostoCanal>(`/api/canales/${canalId}/costos`, "POST", costo);
}

export function actualizarCostoCanal(canalId: string, costoId: string, costo: CostoCanalInput): Promise<CostoCanal> {
  return sendJson<CostoCanal>(`/api/canales/${canalId}/costos/${costoId}`, "PUT", costo);
}

export function eliminarCostoCanal(canalId: string, costoId: string): Promise<void> {
  return sendJson<void>(`/api/canales/${canalId}/costos/${costoId}`, "DELETE");
}

// ─── Integración MercadoLibre ───────────────────────────────────────────────

export type MercadoLibreImportResult = {
  creados: number;
  actualizados: number;
  omitidos: number;
  errores: number;
  totalProcesados: number;
  importadoEn: string;
  detalleErrores: string[];
};

export type MercadoLibreCosto = {
  id: string;
  sku: string;
  costoProm: number | null;
  ultimoCosto: number | null;
  costoMercadoLibre: number;
  fuenteArchivo: string;
  updatedAt: string;
};

export async function importarCostosMercadoLibre(file: File): Promise<MercadoLibreImportResult> {
  const form = new FormData();
  form.append("file", file);
  const res = await fetch(`${BASE}/api/integraciones/mercadolibre/import`, {
    method: "POST",
    headers: authHeaders(),
    body: form,
  });
  if (res.status === 401) {
    logout();
    throw new Error("Sesión expirada, vuelve a iniciar sesión");
  }
  if (!res.ok) throw new Error(await parseErrorBody(res, "/api/integraciones/mercadolibre/import"));
  return res.json() as Promise<MercadoLibreImportResult>;
}

export function getCostosMercadoLibre(): Promise<MercadoLibreCosto[]> {
  return getJson<MercadoLibreCosto[]>("/api/integraciones/mercadolibre");
}

export function getCostoMercadoLibrePorSku(sku: string): Promise<MercadoLibreCosto> {
  return getJson<MercadoLibreCosto>(`/api/integraciones/mercadolibre/${encodeURIComponent(sku)}`);
}

export async function exportarCostosMercadoLibre(): Promise<Blob> {
  const res = await fetch(`${BASE}/api/integraciones/mercadolibre/export`, { headers: authHeaders() });
  if (res.status === 401) {
    logout();
    throw new Error("Sesión expirada, vuelve a iniciar sesión");
  }
  if (!res.ok) throw new Error(await parseErrorBody(res, "/api/integraciones/mercadolibre/export"));
  return res.blob();
}
