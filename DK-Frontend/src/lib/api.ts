const API_BASE = (import.meta.env.VITE_API_URL ?? import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

export type Producto = {
  id: string;
  sku: string;
  nombre: string;
  categoria: string | null;
  marca: string | null;
  tipoProducto: string | null;
  stock: number;
  costoBase: number;
  activo: boolean;
};

export type BsaleImportResult = {
  productosCreados: number;
  productosActualizados: number;
  productosOmitidos: number;
  errores: number;
  totalProcesados: number;
  sincronizadoEn: string;
  detalleErrores: string[];
};

function apiUrl(path: string): string {
  return `${API_BASE}${path}`;
}

// El JWT lo guarda el login (en app/api.ts) bajo "dk_token"; lo reusamos aquí.
function authHeaders(): Record<string, string> {
  const token = localStorage.getItem("dk_token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function fetchError(err: unknown): Error {
  if (err instanceof TypeError && /fetch/i.test(err.message)) {
    return new Error("No se pudo conectar con el backend. Verifica que esté corriendo.");
  }
  if (err instanceof Error) return err;
  return new Error("Error de red desconocido");
}

async function parseError(res: Response): Promise<string> {
  const text = await res.text();
  if (!text) return `Error ${res.status}`;
  try {
    const json = JSON.parse(text) as { message?: string; error?: string };
    return json.message ?? json.error ?? text;
  } catch {
    return text;
  }
}

export async function importarStockBsale(file: File, productosFile?: File): Promise<BsaleImportResult> {
  const form = new FormData();
  form.append("stock", file);
  if (productosFile) form.append("productos", productosFile);
  try {
    const res = await fetch(apiUrl("/api/integraciones/bsale/import"), {
      method: "POST",
      headers: authHeaders(), // sin Content-Type: el navegador pone el boundary del multipart
      body: form,
    });
    if (!res.ok) throw new Error(await parseError(res));
    return res.json();
  } catch (err) {
    throw fetchError(err);
  }
}

export async function listarProductos(limit = 200): Promise<Producto[]> {
  try {
    const res = await fetch(apiUrl("/api/productos"), {
      headers: authHeaders(),
    });
    if (!res.ok) throw new Error(await parseError(res));
    const productos = await res.json() as Producto[];
    return productos.slice(0, limit);
  } catch (err) {
    throw fetchError(err);
  }
}
