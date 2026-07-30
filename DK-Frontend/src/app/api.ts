// Cliente HTTP del backend DK. Guarda el JWT en localStorage y lo adjunta a cada
// llamada. La URL base se resuelve así:
//   1. Si VITE_API_URL está definida, se usa tal cual.
//   2. En build de producción (PROD), queda vacía → llamadas relativas al mismo
//      origen (/api/...). Así funciona detrás de nginx tanto en HTTP como HTTPS,
//      sin CORS y sin hardcodear la IP/dominio.
//   3. En dev (npm run dev), apunta a localhost:8080 (el backend local).
const BASE =
  (import.meta as any).env?.VITE_API_URL ??
  ((import.meta as any).env?.PROD ? "" : "http://localhost:8080");

let token: string | null = localStorage.getItem("dk_token");

export function isAuthed(): boolean {
  return !!token;
}

export function logout(): void {
  token = null;
  localStorage.removeItem("dk_token");
}

function authHeaders(): Record<string, string> {
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`, { headers: authHeaders() });
  if (res.status === 401) {
    logout();
    throw new Error("Sesión expirada, vuelve a iniciar sesión");
  }
  if (!res.ok) throw new Error(`Error ${res.status} en ${path}`);
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
