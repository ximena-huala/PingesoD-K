import { useState, useRef, useEffect } from "react";
import {
  LayoutDashboard,
  BarChart2,
  Plug,
  Settings,
  Search,
  RefreshCw,
  Download,
  Upload,
  Link2,
  Activity,
  ChevronDown,
  Calendar,
  CheckCircle2,
  XCircle,
  Clock,
  X,
  ArrowUpDown,
  Loader2,
} from "lucide-react";
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from "recharts";
import {
  login, getDetalle, getResumen, getEstadoFalabella, descargarExcel,
  falabellaOrders, falabellaProducts, falabellaCategories, falabellaBrands, falabellaTestFirma,
  getCanales, getCostosCanal, crearCostoCanal, actualizarCostoCanal, eliminarCostoCanal,
  importarCostosMercadoLibre, importarVentasMercadoLibre, getCostosMercadoLibre, getCostoMercadoLibrePorSku, exportarCostosMercadoLibre,
  TIPOS_COSTO, type CanalVenta, type CostoCanal, type TipoCosto,
  type MercadoLibreCosto, type MercadoLibreImportResult,
} from "./api";
import { importarStockBsale, listarProductos, type BsaleImportResult, type Producto } from "@/lib/api";

// ─── Types ────────────────────────────────────────────────────────────────────

type SaleRow = {
  date: string;
  marketplace: string;
  sku: string;
  product: string;
  category: string;
  priceRaw: number;
  productCostRaw: number;
  shippingRaw: number;
  commissionRaw: number;
  netRaw: number;
  marginPct: number;
};

// ─── Data ─────────────────────────────────────────────────────────────────────

// Los datos de ventas ahora vienen del backend (GET /api/rentabilidad/detalle),
// mapeados al tipo SaleRow en WF2Reportes. Antes había un ALL_ROWS mock aquí.

const MARKETPLACES = ["Todos", "Falabella", "MercadoLibre", "Walmart", "Shopify", "Tienda física"];

function normalizarMarketplace(nombre: string): string {
  const clave = nombre.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().replace(/[\s_-]+/g, "");
  if (clave === "mercadolibre" || clave === "mercadolivre") return "MercadoLibre";
  if (clave === "falabella") return "Falabella";
  if (clave === "tiendafisica") return "Tienda física";
  return nombre;
}

const fmt = (n: number) =>
  "$" + n.toLocaleString("es-MX", { minimumFractionDigits: 0, maximumFractionDigits: 0 });

function calcKpis(rows: SaleRow[]) {
  const sales = rows.reduce((s, r) => s + r.priceRaw, 0);
  const net = rows.reduce((s, r) => s + r.netRaw, 0);
  const shipping = rows.reduce((s, r) => s + r.shippingRaw, 0);
  const commission = rows.reduce((s, r) => s + r.commissionRaw, 0);
  const producto = rows.reduce((s, r) => s + r.productCostRaw, 0);
  return { sales, net, shipping, commission, producto };
}

function buildChartData(rows: SaleRow[]) {
  if (rows.length === 0) return { data: [] as { date: string; ventas: number }[], porMes: false };
  // Si el rango abarca más de ~2 meses, agrupa por mes (YYYY-MM); si no, por día (MM-DD).
  // Así rangos largos quedan legibles y rangos cortos mantienen el detalle diario.
  const fechas = rows.map((r) => r.date);
  const min = fechas.reduce((a, b) => (a < b ? a : b));
  const max = fechas.reduce((a, b) => (a > b ? a : b));
  const spanDias = (Date.parse(max) - Date.parse(min)) / 86400000;
  const porMes = spanDias > 62;
  const acc: Record<string, { ventas: number; margen: number }> = {};
  rows.forEach((r) => {
    const key = porMes ? r.date.slice(0, 7) : r.date.slice(5); // YYYY-MM o MM-DD
    if (!acc[key]) acc[key] = { ventas: 0, margen: 0 };
    acc[key].ventas += r.priceRaw;
    acc[key].margen += r.netRaw;
  });
  const data = Object.entries(acc)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, v]) => ({ date, ventas: v.ventas, margen: v.margen }));
  return { data, porMes };
}

// ─── Shared primitives ────────────────────────────────────────────────────────

function WireframeNav({ activeScreen, onSelect }: { activeScreen: number; onSelect: (n: number) => void }) {
  const screens = ["Login / Registro", "Reportes", "Ingreso de Datos"];
  return (
    <nav className="fixed top-0 left-0 right-0 z-50 bg-white border-b border-border h-11 flex items-center px-6 justify-between"
      style={{ fontFamily: "Inter, sans-serif" }}>
      <div className="flex items-center gap-2">
        <div className="w-5 h-5 bg-foreground rounded-sm" />
        <span className="text-xs font-semibold tracking-widest uppercase text-foreground">D&K Integrador</span>
        <span className="ml-3 text-[10px] font-mono text-muted-foreground bg-muted px-2 py-0.5 rounded"></span>
      </div>
      <div className="flex items-center gap-1">
        {screens.map((label, i) => (
          <button
            key={i}
            onClick={() => onSelect(i)}
            className={`px-3 py-1 text-xs rounded font-medium transition-all ${
              activeScreen === i
                ? "bg-foreground text-white"
                : "text-muted-foreground hover:text-foreground hover:bg-accent"
            }`}
          >
            WF{i + 1} — {label}
          </button>
        ))}
      </div>
    </nav>
  );
}

function AnnotationLabel({ children }: { children: React.ReactNode }) {
  return (
    <span className="text-[9px] font-mono uppercase tracking-wider text-muted-foreground border border-dashed border-muted-foreground/40 px-1.5 py-0.5 rounded-sm bg-white/60">
      {children}
    </span>
  );
}

function PlaceholderBox({ label, className = "", height = "h-8" }: { label: string; className?: string; height?: string }) {
  return (
    <div className={`${height} ${className} bg-muted border border-dashed border-border rounded flex items-center justify-center`}>
      <span className="text-[10px] font-mono text-muted-foreground">{label}</span>
    </div>
  );
}

function InputField({ label, placeholder, type = "text", value, onChange }: {
  label: string; placeholder?: string; type?: string; value?: string; onChange?: (v: string) => void;
}) {
  return (
    <div className="flex flex-col gap-1">
      <label className="text-xs font-medium text-foreground">{label}</label>
      <input
        type={type}
        value={value ?? ""}
        onChange={(e) => onChange?.(e.target.value)}
        placeholder={placeholder || label}
        className="h-9 bg-input-background border border-border rounded px-3 text-xs text-foreground placeholder:text-muted-foreground outline-none focus:border-foreground/40 transition-colors"
      />
    </div>
  );
}

function PrimaryButton({ label, full = false, onClick, disabled }: {
  label: string; full?: boolean; onClick?: () => void; disabled?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`h-9 bg-foreground rounded flex items-center justify-center px-4 cursor-pointer transition-opacity ${full ? "w-full" : ""} ${disabled ? "opacity-40 cursor-not-allowed" : "hover:opacity-80"}`}
    >
      <span className="text-xs font-semibold text-white tracking-wide">{label}</span>
    </button>
  );
}

function SecondaryButton({ label, icon, onClick }: { label: string; icon?: React.ReactNode; onClick?: () => void }) {
  return (
    <button
      onClick={onClick}
      className="h-8 border border-border bg-white rounded flex items-center justify-center gap-1.5 px-3 cursor-pointer hover:bg-muted transition-colors"
    >
      {icon}
      <span className="text-xs font-medium text-foreground">{label}</span>
    </button>
  );
}

// ─── Dropdown component ───────────────────────────────────────────────────────

function Dropdown({ options, value, onChange, placeholder = "Seleccionar…" }: {
  options: string[]; value: string; onChange: (v: string) => void; placeholder?: string;
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handle(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", handle);
    return () => document.removeEventListener("mousedown", handle);
  }, []);

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen((o) => !o)}
        className="h-9 bg-input-background border border-border rounded flex items-center px-3 gap-2 text-xs w-full hover:border-foreground/30 transition-colors"
      >
        <span className={value ? "text-foreground" : "text-muted-foreground"}>{value || placeholder}</span>
        <ChevronDown size={11} className={`text-muted-foreground ml-auto transition-transform ${open ? "rotate-180" : ""}`} />
      </button>
      {open && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-border rounded-lg shadow-lg z-30 py-1 overflow-hidden">
          {options.map((opt) => (
            <button
              key={opt}
              onClick={() => { onChange(opt); setOpen(false); }}
              className={`w-full text-left px-3 py-2 text-xs hover:bg-muted transition-colors ${opt === value ? "bg-muted font-medium text-foreground" : "text-muted-foreground"}`}
            >
              {opt}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── Toast ────────────────────────────────────────────────────────────────────

function Toast({ message, onClose }: { message: string; onClose: () => void }) {
  useEffect(() => {
    const t = setTimeout(onClose, 3000);
    return () => clearTimeout(t);
  }, [onClose]);
  return (
    <div className="fixed bottom-6 right-6 z-50 bg-foreground text-white text-xs font-medium px-4 py-3 rounded-lg shadow-lg flex items-center gap-3 animate-in slide-in-from-bottom-2">
      <CheckCircle2 size={14} />
      {message}
      <button onClick={onClose} className="ml-1 opacity-60 hover:opacity-100"><X size={12} /></button>
    </div>
  );
}

// ─── WIREFRAME 1: Login / Register ───────────────────────────────────────────

function WF1Login({ onLogin }: { onLogin: () => void }) {
  const [tab, setTab] = useState<"login" | "register">("login");
  const [email, setEmail] = useState("kevin@dk.cl");
  const [password, setPassword] = useState("changeme");
  const [company, setCompany] = useState("");
  const [confirmPwd, setConfirmPwd] = useState("");
  const [toast, setToast] = useState("");
  const [checked, setChecked] = useState(false);
  const [authing, setAuthing] = useState(false);

  return (
    <div className="min-h-screen bg-background flex flex-col" style={{ fontFamily: "Inter, sans-serif" }}>
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
      <div className="h-10 bg-white border-b border-border flex items-center px-8">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 bg-foreground rounded-[2px]" />
          <span className="text-[11px] font-semibold text-muted-foreground tracking-widest uppercase">LOGIN</span>
        </div>
        <div className="ml-auto flex items-center gap-4">
          <PlaceholderBox label="nav link" height="h-5" className="w-16" />
          <PlaceholderBox label="nav link" height="h-5" className="w-16" />
        </div>
      </div>

      <div className="flex-1 flex items-center justify-center px-4 py-16">
        <div className="w-full max-w-[960px] grid grid-cols-2 gap-12 items-start">
          <div className="flex flex-col gap-6 pt-8">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-foreground rounded-md flex items-center justify-center">
                <span className="text-white text-xs font-bold font-mono">D&K</span>
              </div>
              <div>
                <p className="text-base font-semibold text-foreground">D&K Integrador</p>
                <p className="text-[10px] text-muted-foreground font-mono uppercase tracking-wider">Plataforma Integradora Marketplaces</p>
              </div>
            </div>
            <div className="flex flex-col gap-2">
              {["Centraliza tus datos de marketplaces", "Reportes automáticos y rentabilidad neta", "Integración con MercadoLibre, Walmart, Shopify, Falabella"].map((feat, i) => (
                <div key={i} className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-sm bg-muted-foreground/30 border border-border flex-shrink-0" />
                  <span className="text-xs text-muted-foreground">{feat}</span>
                </div>
              ))}
            </div>
            <div className="mt-auto"><AnnotationLabel>Screen: WF-01 · Auth</AnnotationLabel></div>
          </div>

          <div className="bg-white border border-border rounded-xl shadow-sm p-8 flex flex-col gap-6">
            <div className="flex border border-border rounded-lg overflow-hidden bg-muted p-1 gap-1">
              {(["login", "register"] as const).map((t) => (
                <button key={t} onClick={() => setTab(t)}
                  className={`flex-1 py-1.5 text-xs font-semibold rounded-md transition-all ${tab === t ? "bg-white text-foreground shadow-sm" : "text-muted-foreground"}`}>
                  {t === "login" ? "Iniciar Sesión" : "Registrarse"}
                </button>
              ))}
            </div>

            {tab === "login" ? (
              <div className="flex flex-col gap-4">
                <div>
                  <p className="text-base font-semibold text-foreground">Bienvenido de vuelta</p>
                  <p className="text-xs text-muted-foreground mt-0.5">Ingresa tus credenciales para continuar</p>
                </div>
                <InputField label="Correo electrónico" placeholder="empresa@correo.com" type="email" value={email} onChange={setEmail} />
                <InputField label="Contraseña" placeholder="••••••••" type="password" value={password} onChange={setPassword} />
                <div className="flex justify-end">
                  <span className="text-[11px] text-muted-foreground underline cursor-pointer">¿Olvidaste tu contraseña?</span>
                </div>
                <PrimaryButton label={authing ? "Ingresando…" : "Iniciar Sesión"} full disabled={authing} onClick={async () => {
                  if (!email || !password) {
                    setToast("Completa todos los campos");
                    return;
                  }
                  setAuthing(true);
                  try {
                    await login(email, password);
                    onLogin();
                  } catch (e: any) {
                    setToast(e?.message || "No se pudo iniciar sesión");
                  } finally {
                    setAuthing(false);
                  }
                }} />
                  <div className="flex items-center gap-2">
                  <div className="flex-1 h-px bg-border" />
                  <span className="text-[10px] text-muted-foreground font-mono">O</span>
                  <div className="flex-1 h-px bg-border" />
                </div>
                <PlaceholderBox label="[ SSO / OAuth Button ]" height="h-9" />
              </div>
            ) : (
              <div className="flex flex-col gap-4">
                <div>
                  <p className="text-base font-semibold text-foreground">Crear una cuenta</p>
                  <p className="text-xs text-muted-foreground mt-0.5">Completa los datos para registrarte</p>
                </div>
                <InputField label="Nombre de empresa" placeholder="Mi Empresa S.A." value={company} onChange={setCompany} />
                <InputField label="Correo electrónico" placeholder="empresa@correo.com" type="email" value={email} onChange={setEmail} />
                <div className="grid grid-cols-2 gap-3">
                  <InputField label="Contraseña" placeholder="••••••••" type="password" value={password} onChange={setPassword} />
                  <InputField label="Confirmar contraseña" placeholder="••••••••" type="password" value={confirmPwd} onChange={setConfirmPwd} />
                </div>
                <button className="flex items-start gap-2 mt-1 text-left" onClick={() => setChecked((c) => !c)}>
                  <div className={`w-3.5 h-3.5 border rounded-sm mt-0.5 flex-shrink-0 flex items-center justify-center transition-colors ${checked ? "bg-foreground border-foreground" : "border-border bg-muted"}`}>
                    {checked && <CheckCircle2 size={9} className="text-white" />}
                  </div>
                  <span className="text-[11px] text-muted-foreground leading-relaxed">
                    Acepto los <span className="underline">términos y condiciones</span> y la <span className="underline">política de privacidad</span>
                  </span>
                </button>
                <PrimaryButton label="Crear cuenta" full onClick={() => {
                  if (!company || !email || !password) { setToast("Completa todos los campos"); return; }
                  if (password !== confirmPwd) { setToast("Las contraseñas no coinciden"); return; }
                  if (!checked) { setToast("Debes aceptar los términos"); return; }
                  setToast("Cuenta creada exitosamente");
                }} />
              </div>
            )}
            <p className="text-[10px] text-muted-foreground text-center font-mono">
              {tab === "login" ? "¿No tienes cuenta? → Registrarse" : "¿Ya tienes cuenta? → Iniciar Sesión"}
            </p>
          </div>
        </div>
      </div>

      <div className="h-8 bg-white border-t border-border flex items-center px-8 justify-between">
        <span className="text-[10px] font-mono text-muted-foreground">© 2026 D&K </span>
        <div className="flex gap-4">
          {["Ayuda", "Privacidad", "Términos"].map((l) => (
            <span key={l} className="text-[10px] text-muted-foreground font-mono underline cursor-pointer">{l}</span>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── WIREFRAME 2: Reportes Dashboard ─────────────────────────────────────────

const sidebarItems = [
  { icon: <LayoutDashboard size={14} />, label: "Dashboard" },
  { icon: <BarChart2 size={14} />, label: "Reportes" },
  { icon: <Plug size={14} />, label: "Integraciones" },
  { icon: <Settings size={14} />, label: "Configuración" },
];

const tableCols = ["Fecha", "Marketplace", "SKU", "Producto", "Categoría", "Precio Venta", "Costo Producto", "Costo Envío", "Comisión", "Ganancia Neta", "Margen %"];

function WF2Reportes({ goTo }: { goTo: (n: number) => void }) {
  const [activeNav, setActiveNav] = useState("Reportes");
  const [mpFilter, setMpFilter] = useState("Todos");
  const [catFilter, setCatFilter] = useState("Todas");
  const [dateFrom, setDateFrom] = useState("2025-01-01");
  const [dateTo, setDateTo] = useState("2026-12-31");
  const [search, setSearch] = useState(""); // applied
  const [searchInput, setSearchInput] = useState(""); // live input
  const [mpDropOpen, setMpDropOpen] = useState(false);
  const [sortCol, setSortCol] = useState<keyof SaleRow | null>(null);
  const [sortAsc, setSortAsc] = useState(true);
  const [toast, setToast] = useState("");
  const mpRef = useRef<HTMLDivElement>(null);

  // Datos reales del backend (reemplazan el mock ALL_ROWS)
  const [rows, setRows] = useState<SaleRow[]>([]);
  const [resumen, setResumen] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [page, setPage] = useState(0);
  const pageSize = 20;
  const [vista, setVista] = useState<"venta" | "producto">("venta");
  const [prodSortCol, setProdSortCol] = useState<string>("ventas");
  const [prodSortAsc, setProdSortAsc] = useState(false);
  const [catSortCol, setCatSortCol] = useState<string>("margen");
  const [catSortAsc, setCatSortAsc] = useState(false);
  const [chartMetric, setChartMetric] = useState<"ventas" | "margen">("ventas");

  function cargarDatos() {
    setLoading(true);
    setLoadError("");
    Promise.all([getDetalle(), getResumen()])
      .then(([detalle, res]) => {
        setRows(
          detalle.map((d) => ({
            date: d.fecha,
            marketplace: normalizarMarketplace(d.canal),
            sku: d.sku,
            product: d.producto,
            category: d.categoria,
            priceRaw: Number(d.precioVenta),
            productCostRaw: Number(d.costoProducto),
            shippingRaw: Number(d.logistica),
            commissionRaw: Number(d.comision),
            netRaw: Number(d.margen),
            marginPct: Number(d.margenPorcentaje),
          }))
        );
        setResumen(res);
      })
      .catch((e: any) => setLoadError(e?.message || "No se pudieron cargar los datos"))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    cargarDatos();
    const refresh = () => cargarDatos();
    window.addEventListener("dk:reportes-refresh", refresh);
    return () => window.removeEventListener("dk:reportes-refresh", refresh);
  }, []);
  // Al cambiar filtros, volver a la primera página
  useEffect(() => { setPage(0); }, [search, mpFilter, catFilter, dateFrom, dateTo, vista]);

  async function handleExportExcel() {
    try {
      const blob = await descargarExcel(dateFrom, dateTo);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `DK-Reporte-${dateFrom}_a_${dateTo}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
      setToast("Reporte Excel descargado");
    } catch (e: any) {
      setToast(e?.message || "No se pudo generar el Excel");
    }
  }

  useEffect(() => {
    function h(e: MouseEvent) {
      if (mpRef.current && !mpRef.current.contains(e.target as Node)) setMpDropOpen(false);
    }
    document.addEventListener("mousedown", h);
    return () => document.removeEventListener("mousedown", h);
  }, []);

const filtered = rows.filter((r) => {
  const mpOk = mpFilter === "Todos" || r.marketplace === mpFilter;
  const catOk = catFilter === "Todas" || r.category === catFilter;

  const q = search.toLowerCase();
  const searchOk =
    !q ||
    r.sku.toLowerCase().includes(q) ||
    r.product.toLowerCase().includes(q);

  const dateOk =
    (!dateFrom || r.date >= dateFrom) &&
    (!dateTo || r.date <= dateTo);

  return mpOk && catOk && searchOk && dateOk;
});

const marketplacesDisponibles = [
  "Todos",
  ...MARKETPLACES.filter((marketplace) => marketplace !== "Todos"),
  ...Array.from(new Set(rows.map((r) => r.marketplace).filter(Boolean))).sort(),
].filter((marketplace, index, list) => list.indexOf(marketplace) === index);
const categorias = ["Todas", ...Array.from(new Set(rows.map((r) => r.category).filter(Boolean))).sort()];

  const sorted = sortCol
    ? [...filtered].sort((a, b) => {
        const av = a[sortCol];
        const bv = b[sortCol];
        const res = typeof av === "number" ? (av as number) - (bv as number) : String(av).localeCompare(String(bv));
        return sortAsc ? res : -res;
      })
    : filtered;

  const kpis = calcKpis(filtered);
  const chart = buildChartData(filtered);

  // Vista "Por Producto": agrega las ventas filtradas por SKU (una fila por producto)
  type ProductRow = { sku: string; product: string; category: string; unidades: number; ventas: number; costoProducto: number; envio: number; comision: number; ganancia: number; margenPct: number };
  const productRows: ProductRow[] = (() => {
    const map = new Map<string, ProductRow>();
    for (const r of filtered) {
      let p = map.get(r.sku);
      if (!p) {
        p = { sku: r.sku, product: r.product, category: r.category, unidades: 0, ventas: 0, costoProducto: 0, envio: 0, comision: 0, ganancia: 0, margenPct: 0 };
        map.set(r.sku, p);
      }
      p.unidades += 1;
      p.ventas += r.priceRaw;
      p.costoProducto += r.productCostRaw;
      p.envio += r.shippingRaw;
      p.comision += r.commissionRaw;
      p.ganancia += r.netRaw;
    }
    const arr = Array.from(map.values());
    arr.forEach((p) => { p.margenPct = p.ventas ? (p.ganancia / p.ventas) * 100 : 0; });
    arr.sort((a, b) => b.ventas - a.ventas);
    return arr;
  })();

  // Orden de la vista "Por Producto" (clic en encabezados)
  const productRowsSorted = [...productRows].sort((a, b) => {
    const av = (a as any)[prodSortCol]; const bv = (b as any)[prodSortCol];
    const res = typeof av === "number" ? av - bv : String(av ?? "").localeCompare(String(bv ?? ""));
    return prodSortAsc ? res : -res;
  });

  // Rentabilidad por categoría, ordenable por columna
  const catColToKey: Record<string, string> = {
    "Categoría": "etiqueta", "Unidades": "unidades", "Ingreso": "ingreso", "Margen": "margen", "Margen %": "margenPorcentaje",
  };
  const categoriasFiltradas = Array.from(
    productRows.reduce((map, row) => {
      const current = map.get(row.category) ?? { etiqueta: row.category, unidades: 0, ingreso: 0, margen: 0 };
      current.unidades += row.unidades;
      current.ingreso += row.ventas;
      current.margen += row.ganancia;
      map.set(row.category, current);
      return map;
    }, new Map<string, { etiqueta: string; unidades: number; ingreso: number; margen: number }>()).values()
  ).map((grupo) => ({
    ...grupo,
    margenPorcentaje: grupo.ingreso ? (grupo.margen / grupo.ingreso) * 100 : 0,
  }));

  const catsSorted: any[] = categoriasFiltradas.length > 0
    ? [...categoriasFiltradas].sort((a: any, b: any) => {
        const av = a[catSortCol]; const bv = b[catSortCol];
        const res = typeof av === "number" ? av - bv : String(av ?? "").localeCompare(String(bv ?? ""));
        return catSortAsc ? res : -res;
      })
    : [];

  // Paginación (según la vista activa: por venta o por producto)
  const listLen = vista === "venta" ? sorted.length : productRowsSorted.length;
  const totalPages = Math.max(1, Math.ceil(listLen / pageSize));
  const currentPage = Math.min(page, totalPages - 1);
  const paged = sorted.slice(currentPage * pageSize, currentPage * pageSize + pageSize);
  const pagedProductos = productRowsSorted.slice(currentPage * pageSize, currentPage * pageSize + pageSize);

  // Productos en pérdida (margen total negativo), para la alerta
  const enPerdida = productRows.filter((p) => p.ganancia < 0).sort((a, b) => a.ganancia - b.ganancia);
  const totalPerdida = enPerdida.reduce((s, p) => s + p.ganancia, 0);

  function handleSort(col: keyof SaleRow) {
    if (sortCol === col) setSortAsc((a) => !a);
    else { setSortCol(col); setSortAsc(true); }
  }

  function handleProdSort(key: string) {
    if (prodSortCol === key) setProdSortAsc((a) => !a);
    else { setProdSortCol(key); setProdSortAsc(true); }
  }

  function handleCatSort(key: string) {
    if (catSortCol === key) setCatSortAsc((a) => !a);
    else { setCatSortCol(key); setCatSortAsc(true); }
  }

  function handleBuscar() {
    setSearch(searchInput);
  }

  function handleExport() {
    const header = tableCols.join(",");
    const rows = sorted.map((r) =>
      [r.date, r.marketplace, r.sku, `"${r.product}"`, `"${r.category}"`, r.priceRaw, r.productCostRaw, r.shippingRaw, r.commissionRaw, r.netRaw, r.marginPct.toFixed(1)].join(",")
    );
    const csv = [header, ...rows].join("\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = "reporte-dk.csv"; a.click();
    URL.revokeObjectURL(url);
    setToast("Reporte exportado como CSV");
  }

  const colToKey: Record<string, keyof SaleRow> = {
    "Fecha": "date", "Marketplace": "marketplace", "SKU": "sku", "Producto": "product", "Categoría": "category",
    "Precio Venta": "priceRaw", "Costo Producto": "productCostRaw", "Costo Envío": "shippingRaw",
    "Comisión": "commissionRaw", "Ganancia Neta": "netRaw", "Margen %": "marginPct",
  };

  return (
    <div className="min-h-screen bg-background flex flex-col" style={{ fontFamily: "Inter, sans-serif" }}>
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
      <div className="h-12 bg-white border-b border-border flex items-center px-5 gap-4 flex-shrink-0">
        <div className="flex items-center gap-2 mr-4">
          <div className="w-5 h-5 bg-foreground rounded-[3px]" />
          <span className="text-[11px] font-bold tracking-widest uppercase text-foreground">D&K </span>
        </div>
        <div className="h-5 w-px bg-border" />
        <span className="text-xs text-muted-foreground">Reportes de Ventas</span>
        <div className="ml-auto flex items-center gap-3">
          <PlaceholderBox label="Notificaciones" height="h-6" className="w-24" />
          <div className="w-7 h-7 rounded-full bg-muted border border-border flex items-center justify-center">
            <span className="text-[9px] font-mono text-muted-foreground">US</span>
          </div>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        <aside className="w-52 bg-white border-r border-border flex flex-col flex-shrink-0">
          <div className="p-3 flex flex-col gap-0.5">
            {sidebarItems.map(({ icon, label }) => (
             <button
                key={label}
                onClick={() => {
                  setActiveNav(label);
              
                  if (label === "Reportes") goTo(1);
                  if (label === "Integraciones") goTo(2);
                }}
                className={`w-full flex items-center gap-2.5 px-3 py-2 rounded text-xs font-medium transition-all text-left ${
                  activeNav === label ? "bg-foreground text-white" : "text-muted-foreground hover:bg-accent hover:text-foreground"
                }`}>
                {icon}{label}
              </button>
            ))}
          </div>
          <div className="mt-auto p-3 border-t border-border">
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 rounded-full bg-muted border border-border" />
              <div>
                <p className="text-[10px] font-medium text-foreground">Admin Usuario</p>
                <p className="text-[9px] font-mono text-muted-foreground">admin@empresa.com</p>
              </div>
            </div>
          </div>
        </aside>

        <main className="flex-1 flex flex-col overflow-auto">
          {/* Filters */}
          <div className="bg-white border-b border-border px-6 py-3 flex flex-wrap items-center gap-3 flex-shrink-0">
            <AnnotationLabel>Filtros</AnnotationLabel>
              <div className="h-8 border border-border rounded flex items-center gap-2 px-3 bg-input-background">
                <Calendar size={11} className="text-muted-foreground" />
                <input
                  type="date"
                  value={dateFrom}
                  onChange={(e) => setDateFrom(e.target.value)}
                  className="bg-transparent text-xs text-foreground outline-none font-mono"
                />
                <span className="text-xs text-muted-foreground">—</span>
                <input
                  type="date"
                  value={dateTo}
                  onChange={(e) => setDateTo(e.target.value)}
                  className="bg-transparent text-xs text-foreground outline-none font-mono"
                />
              </div>

            {/* Atajos de rango de fecha */}
            <div className="flex items-center gap-1">
              {([["Todo", "2025-01-01", "2026-12-31"], ["2025", "2025-01-01", "2025-12-31"], ["2026", "2026-01-01", "2026-12-31"]] as const).map(([label, d, h]) => (
                <button key={label} onClick={() => { setDateFrom(d); setDateTo(h); }}
                  className="h-8 px-2 text-[10px] font-medium border border-border rounded bg-white text-muted-foreground hover:bg-muted transition-colors">
                  {label}
                </button>
              ))}
            </div>

            {/* Filtro por categoría */}
            <div className="w-[190px]">
              <Dropdown options={categorias} value={catFilter} onChange={setCatFilter} placeholder="Todas las categorías" />
            </div>

            {/* Marketplace dropdown */}
            <div ref={mpRef} className="relative">
              <button
                onClick={() => setMpDropOpen((o) => !o)}
                className="h-8 border border-border rounded flex items-center gap-1.5 px-3 bg-input-background min-w-[170px] hover:border-foreground/30 transition-colors"
              >
                <span className="text-xs text-foreground">{mpFilter === "Todos" ? "Todos los Marketplaces" : mpFilter}</span>
                <ChevronDown size={11} className={`text-muted-foreground ml-auto transition-transform ${mpDropOpen ? "rotate-180" : ""}`} />
              </button>
              {mpDropOpen && (
                <div className="absolute top-full left-0 mt-1 bg-white border border-border rounded-lg shadow-lg z-30 py-1 min-w-[170px]">
                  {marketplacesDisponibles.map((mp) => (
                    <button key={mp} onClick={() => { setMpFilter(mp); setMpDropOpen(false); }}
                      className={`w-full text-left px-3 py-2 text-xs hover:bg-muted transition-colors ${mp === mpFilter ? "bg-muted font-semibold text-foreground" : "text-muted-foreground"}`}>
                      {mp === "Todos" ? "Todos los Marketplaces" : mp}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className="h-8 border border-border rounded flex items-center gap-1.5 px-3 bg-input-background flex-1 max-w-[220px]">
              <Search size={11} className="text-muted-foreground" />
              <input
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleBuscar()}
                placeholder="Buscar SKU o producto…"
                className="flex-1 bg-transparent text-xs text-foreground placeholder:text-muted-foreground outline-none"
              />
              {searchInput && (
                <button onClick={() => { setSearchInput(""); setSearch(""); }}>
                  <X size={10} className="text-muted-foreground hover:text-foreground" />
                </button>
              )}
            </div>

            <PrimaryButton label="Buscar" onClick={handleBuscar} />

            <div className="ml-auto flex gap-2">
              <SecondaryButton label="Exportar CSV" icon={<Download size={11} />} onClick={handleExport} />
              <SecondaryButton label="Exportar Excel" icon={<Download size={11} />} onClick={handleExportExcel} />
              <SecondaryButton label="Actualizar" icon={<RefreshCw size={11} />} onClick={cargarDatos} />
            </div>
          </div>

          <div className="flex-1 p-6 flex flex-col gap-5">
            {/* KPI cards */}
            <div>
              <div className="flex items-center gap-2 mb-3">
                <AnnotationLabel>KPI Cards</AnnotationLabel>
                <span className="text-[10px] font-mono text-muted-foreground">
                  — {filtered.length} registros · {mpFilter === "Todos" ? "todos los marketplaces" : mpFilter}
                </span>
              </div>
              <div className="grid grid-cols-4 gap-4">
                {[
                  { label: "Ventas Totales", value: fmt(kpis.sales), sub: `${filtered.length} ventas` },
                  { label: "Ganancia Neta", value: fmt(kpis.net), sub: kpis.sales ? `margen ${((kpis.net / kpis.sales) * 100).toFixed(1)}%` : "—" },
                  { label: "Costos de Envío", value: fmt(kpis.shipping), sub: "logística real" },
                  { label: "Comisiones", value: fmt(kpis.commission), sub: "comisión real" },
                ].map(({ label, value, sub }) => (
                  <div key={label} className="bg-white border border-border rounded-lg p-4 flex flex-col gap-2">
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">{label}</p>
                    <p className="text-xl font-bold text-foreground" style={{ fontFamily: "JetBrains Mono, monospace" }}>{value}</p>
                    <span className="text-[9px] text-muted-foreground">{sub}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Estructura de costos + productos en pérdida */}
            <div className="grid grid-cols-3 gap-4">
              <div className="col-span-2 bg-white border border-border rounded-lg p-4 flex flex-col gap-3">
                <div className="flex items-center gap-2">
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Estructura de Costos</p>
                  <span className="text-[9px] font-mono text-muted-foreground">— a dónde va cada peso de venta</span>
                </div>
                {(() => {
                  const t = kpis.sales || 1;
                  const segs = [
                    { label: "Producto", val: kpis.producto, color: "#1a1a1a" },
                    { label: "Comisión", val: kpis.commission, color: "#6b7280" },
                    { label: "Logística", val: kpis.shipping, color: "#9ca3af" },
                    { label: "Margen", val: kpis.net, color: "#16a34a" },
                  ];
                  return (
                    <>
                      <div className="flex h-6 rounded overflow-hidden border border-border">
                        {segs.map((s) => (
                          <div key={s.label} style={{ width: `${Math.max(0, (s.val / t) * 100)}%`, background: s.color }} title={`${s.label}: ${fmt(s.val)}`} />
                        ))}
                      </div>
                      <div className="grid grid-cols-4 gap-2">
                        {segs.map((s) => (
                          <div key={s.label} className="flex flex-col gap-0.5">
                            <div className="flex items-center gap-1.5">
                              <span className="w-2.5 h-2.5 rounded-sm" style={{ background: s.color }} />
                              <span className="text-[10px] font-medium text-foreground">{s.label}</span>
                            </div>
                            <span className="text-[11px] font-mono text-foreground">{fmt(s.val)}</span>
                            <span className="text-[9px] font-mono text-muted-foreground">{((s.val / t) * 100).toFixed(1)}%</span>
                          </div>
                        ))}
                      </div>
                    </>
                  );
                })()}
              </div>

              <div className="bg-white border border-border rounded-lg p-4 flex flex-col gap-2">
                <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Productos en Pérdida</p>
                {enPerdida.length === 0 ? (
                  <div className="flex-1 flex items-center">
                    <span className="text-xs text-muted-foreground">Ningún producto en pérdida.</span>
                  </div>
                ) : (
                  <>
                    <p className="text-xl font-bold" style={{ fontFamily: "JetBrains Mono, monospace", color: "#dc2626" }}>{fmt(totalPerdida)}</p>
                    <span className="text-[9px] text-muted-foreground">{enPerdida.length} producto(s) con margen negativo</span>
                    <div className="flex flex-col gap-1 mt-1">
                      {enPerdida.slice(0, 3).map((p) => (
                        <div key={p.sku} className="flex items-center justify-between text-[10px] gap-2">
                          <span className="text-foreground truncate">{p.product}</span>
                          <span className="font-mono font-semibold flex-shrink-0" style={{ color: "#dc2626" }}>{fmt(p.ganancia)}</span>
                        </div>
                      ))}
                    </div>
                  </>
                )}
              </div>
            </div>

            {/* Chart */}
            <div>
              <div className="flex items-center gap-2 mb-3">
                <AnnotationLabel>{(chartMetric === "ventas" ? "Ventas" : "Margen") + (chart.porMes ? " por Mes" : " por Día")}</AnnotationLabel>
                <span className="text-[10px] font-mono text-muted-foreground">— {mpFilter === "Todos" ? "todos los marketplaces" : mpFilter}</span>
                <div className="ml-auto flex border border-border rounded overflow-hidden">
                  {(["ventas", "margen"] as const).map((m) => (
                    <button key={m} onClick={() => setChartMetric(m)}
                      className={`px-3 h-7 text-[11px] font-medium transition-colors ${chartMetric === m ? "bg-foreground text-white" : "bg-white text-muted-foreground hover:bg-muted"}`}>
                      {m === "ventas" ? "Ventas" : "Margen"}
                    </button>
                  ))}
                </div>
              </div>
              <div className="bg-white border border-border rounded-lg p-4" style={{ height: 160 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chart.data} margin={{ top: 4, right: 8, left: -10, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5e5e5" />
                    <XAxis dataKey="date" tick={{ fontSize: 9, fill: "#9ca3af", fontFamily: "JetBrains Mono" }} />
                    <YAxis tick={{ fontSize: 9, fill: "#9ca3af", fontFamily: "JetBrains Mono" }}
                      tickFormatter={(v) => "$" + (v / 1000).toFixed(0) + "k"} />
                    <Tooltip
                      formatter={(v: number) => [fmt(v), chartMetric === "ventas" ? "Ventas" : "Margen"]}
                      contentStyle={{ fontSize: 11, fontFamily: "JetBrains Mono", border: "1px solid #e5e5e5", borderRadius: 6 }}
                    />
                    <Bar dataKey={chartMetric} fill="#1a1a1a" radius={[3, 3, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Rentabilidad por categoría — pestaña "Por Categoría" del Excel, vía /resumen */}
            {categoriasFiltradas.length > 0 && (
              <div>
                <div className="flex items-center gap-2 mb-3">
                  <AnnotationLabel>Rentabilidad por Categoría</AnnotationLabel>
                  <span className="text-[10px] font-mono text-muted-foreground">— {categoriasFiltradas.length} categorías · margen ponderado</span>
                </div>
                <div className="bg-white border border-border rounded-lg overflow-hidden">
                  <div className="max-h-56 overflow-auto">
                    <table className="w-full text-xs">
                      <thead className="sticky top-0">
                        <tr className="border-b border-border bg-muted">
                          {["Categoría", "Unidades", "Ingreso", "Margen", "Margen %"].map((c) => {
                            const key = catColToKey[c];
                            const active = catSortCol === key;
                            return (
                              <th key={c} onClick={() => handleCatSort(key)}
                                className="px-4 py-2 text-left font-semibold text-muted-foreground text-[10px] uppercase tracking-wider whitespace-nowrap cursor-pointer hover:text-foreground select-none">
                                <span className="flex items-center gap-1">
                                  {c}
                                  <ArrowUpDown size={9} className={active ? "text-foreground" : "text-muted-foreground/40"} />
                                </span>
                              </th>
                            );
                          })}
                        </tr>
                      </thead>
                      <tbody>
                        {catsSorted.map((g: any, i: number) => (
                          <tr key={i} className={`border-b border-border last:border-0 ${i % 2 === 0 ? "bg-white" : "bg-background"}`}>
                            <td className="px-4 py-2 text-[11px] text-foreground max-w-[260px] truncate">{g.etiqueta}</td>
                            <td className="px-4 py-2 font-mono text-[11px] text-muted-foreground">{g.unidades}</td>
                            <td className="px-4 py-2 font-mono text-[11px] text-foreground">{fmt(Number(g.ingreso))}</td>
                            <td className="px-4 py-2 font-mono text-[11px] text-foreground">{fmt(Number(g.margen))}</td>
                            <td className="px-4 py-2 font-mono text-[11px] font-semibold" style={{ color: Number(g.margenPorcentaje) < 0 ? "#dc2626" : undefined }}>{Number(g.margenPorcentaje).toFixed(1)}%</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            )}

            {/* Table */}
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-3">
                <AnnotationLabel>{vista === "venta" ? "Detalle por Venta" : "Resumen por Producto"}</AnnotationLabel>
                <span className="text-[10px] font-mono text-muted-foreground">— {listLen} {vista === "venta" ? "ventas" : "productos"}</span>
                <div className="ml-auto flex border border-border rounded overflow-hidden">
                  {(["venta", "producto"] as const).map((v) => (
                    <button key={v} onClick={() => setVista(v)}
                      className={`px-3 h-7 text-[11px] font-medium transition-colors ${vista === v ? "bg-foreground text-white" : "bg-white text-muted-foreground hover:bg-muted"}`}>
                      {v === "venta" ? "Por Venta" : "Por Producto"}
                    </button>
                  ))}
                </div>
              </div>
              <div className="bg-white border border-border rounded-lg overflow-hidden">
                <div className="overflow-x-auto">
                {vista === "venta" ? (
                <table className="w-full min-w-[1080px] text-xs">
                  <thead>
                    <tr className="border-b border-border bg-muted">
                      {tableCols.map((col) => {
                        const key = colToKey[col];
                        const active = sortCol === key;
                        return (
                          <th key={col}
                            onClick={() => key && handleSort(key)}
                            className={`px-4 py-2.5 text-left font-semibold text-muted-foreground text-[10px] uppercase tracking-wider whitespace-nowrap ${key ? "cursor-pointer hover:text-foreground select-none" : ""}`}>
                            <span className="flex items-center gap-1">
                              {col}
                              {key && <ArrowUpDown size={9} className={active ? "text-foreground" : "text-muted-foreground/40"} />}
                            </span>
                          </th>
                        );
                      })}
                    </tr>
                  </thead>
                  <tbody>
                    {sorted.length === 0 ? (
                      <tr>
                        <td colSpan={11} className="px-4 py-8 text-center text-xs text-muted-foreground font-mono">
                          {loading
                            ? "Cargando datos desde el backend…"
                            : loadError
                              ? `⚠ ${loadError}`
                              : "No se encontraron resultados para la búsqueda aplicada."}
                        </td>
                      </tr>
                    ) : paged.map((row, i) => (
                      <tr key={i} className={`border-b border-border last:border-0 ${i % 2 === 0 ? "bg-white" : "bg-background"} hover:bg-muted/50 transition-colors`}>
                        <td className="px-4 py-2.5 font-mono text-[10px] text-muted-foreground whitespace-nowrap">{row.date}</td>
                        <td className="px-4 py-2.5">
                          <span className="bg-muted text-muted-foreground text-[10px] font-mono px-2 py-0.5 rounded-full">{row.marketplace}</span>
                        </td>
                        <td className="px-4 py-2.5 font-mono text-[10px] text-muted-foreground">{row.sku}</td>
                        <td className="px-4 py-2.5 text-[11px] text-foreground max-w-[180px] truncate">{row.product}</td>
                        <td className="px-4 py-2.5 text-[10px] text-muted-foreground max-w-[130px] truncate">{row.category}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-foreground">{fmt(row.priceRaw)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-muted-foreground">{fmt(row.productCostRaw)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-muted-foreground">{fmt(row.shippingRaw)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-muted-foreground">{fmt(row.commissionRaw)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] font-semibold text-foreground">{fmt(row.netRaw)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] font-semibold" style={{ color: row.marginPct < 0 ? "#dc2626" : undefined }}>{row.marginPct.toFixed(1)}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                ) : (
                <table className="w-full min-w-[980px] text-xs">
                  <thead>
                    <tr className="border-b border-border bg-muted">
                      {([["SKU", "sku"], ["Producto", "product"], ["Categoría", "category"], ["Unidades", "unidades"], ["Ventas", "ventas"], ["Costo Producto", "costoProducto"], ["Costo Envío", "envio"], ["Comisión", "comision"], ["Ganancia Neta", "ganancia"], ["Margen %", "margenPct"]] as const).map(([col, key]) => {
                        const active = prodSortCol === key;
                        return (
                          <th key={col} onClick={() => handleProdSort(key)}
                            className="px-4 py-2.5 text-left font-semibold text-muted-foreground text-[10px] uppercase tracking-wider whitespace-nowrap cursor-pointer hover:text-foreground select-none">
                            <span className="flex items-center gap-1">
                              {col}
                              <ArrowUpDown size={9} className={active ? "text-foreground" : "text-muted-foreground/40"} />
                            </span>
                          </th>
                        );
                      })}
                    </tr>
                  </thead>
                  <tbody>
                    {productRows.length === 0 ? (
                      <tr>
                        <td colSpan={10} className="px-4 py-8 text-center text-xs text-muted-foreground font-mono">
                          {loading ? "Cargando datos desde el backend…" : "No hay productos para el filtro aplicado."}
                        </td>
                      </tr>
                    ) : pagedProductos.map((p, i) => (
                      <tr key={p.sku} className={`border-b border-border last:border-0 ${i % 2 === 0 ? "bg-white" : "bg-background"} hover:bg-muted/50 transition-colors`}>
                        <td className="px-4 py-2.5 font-mono text-[10px] text-muted-foreground whitespace-nowrap">{p.sku}</td>
                        <td className="px-4 py-2.5 text-[11px] text-foreground max-w-[200px] truncate">{p.product}</td>
                        <td className="px-4 py-2.5 text-[10px] text-muted-foreground max-w-[130px] truncate">{p.category}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-foreground">{p.unidades}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-foreground">{fmt(p.ventas)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-muted-foreground">{fmt(p.costoProducto)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-muted-foreground">{fmt(p.envio)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-muted-foreground">{fmt(p.comision)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] font-semibold text-foreground">{fmt(p.ganancia)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] font-semibold" style={{ color: p.margenPct < 0 ? "#dc2626" : undefined }}>{p.margenPct.toFixed(1)}%</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                )}
                </div>
                <div className="px-4 py-2.5 border-t border-border flex items-center justify-between bg-muted/50">
                  <span className="text-[10px] font-mono text-muted-foreground">
                    {listLen} {vista === "venta" ? "ventas" : "productos"} · página {currentPage + 1} de {totalPages}
                  </span>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={currentPage === 0}
                      className="h-6 px-2 text-[10px] font-medium border border-border rounded bg-white hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                      ← Anterior
                    </button>
                    <button
                      onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                      disabled={currentPage >= totalPages - 1}
                      className="h-6 px-2 text-[10px] font-medium border border-border rounded bg-white hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                      Siguiente →
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}

// ─── WIREFRAME 3: Ingreso de Datos ───────────────────────────────────────────

type ConnStatus = "idle" | "connecting" | "connected" | "error";

const MARKETPLACE_LIST = ["MercadoLibre", "Walmart", "Shopify", "Tiendanube", "Falabella"];

function StatusPill({ status }: { status: string }) {
  const map: Record<string, { label: string; icon: React.ReactNode }> = {
    connected: { label: "Conectado", icon: <CheckCircle2 size={11} /> },
    disconnected: { label: "Desconectado", icon: <XCircle size={11} /> },
    pending: { label: "Pendiente", icon: <Clock size={11} /> },
    syncing: { label: "Sincronizando", icon: <Loader2 size={11} className="animate-spin" /> },
  };
  const { label, icon } = map[status] || map.pending;
  return (
    <div className="flex items-center gap-1 bg-muted border border-border rounded-full px-2.5 py-0.5">
      <span className="text-muted-foreground">{icon}</span>
      <span className="text-[10px] font-mono text-muted-foreground">{label}</span>
    </div>
  );
}

function SectionCard({ title, annotation, children }: { title: string; annotation: string; children: React.ReactNode }) {
  return (
    <div className="bg-white border border-border rounded-lg overflow-hidden">
      <div className="px-5 py-3 border-b border-border bg-muted/50 flex items-center gap-3">
        <p className="text-xs font-semibold text-foreground">{title}</p>
        <AnnotationLabel>{annotation}</AnnotationLabel>
      </div>
      <div className="p-5">{children}</div>
    </div>
  );
}

// Helpers de la carga de Stock Bsale (aporte del compañero).
function parseCsv(text: string) {
  return text
    .split(/\r?\n/)
    .filter((line) => line.trim().length > 0)
    .map((line) =>
      line
        .trim()
        .replace(/^"|"$/g, "")
        .split(/,(?=(?:[^"]*"[^"]*")*[^"]*$)/)
        .map((cell) => cell.trim().replace(/^"|"$/g, ""))
    );
}

function isCsvFile(file: File) {
  return /\.csv$/i.test(file.name);
}

function isXlsxFile(file: File) {
  return /\.xlsx$/i.test(file.name);
}

function isStockFile(file: File) {
  return isCsvFile(file) || isXlsxFile(file);
}

const fmtCl = (n: number) =>
  "$" + n.toLocaleString("es-CL", { minimumFractionDigits: 0, maximumFractionDigits: 0 });

function WF3IngresoData({ goTo }: { goTo: (n: number) => void }) {
  const [activeNav, setActiveNav] = useState("Integraciones");
  const [activeSubNav, setActiveSubNav] = useState("Carga CSV / XLSX");
  const [selectedMp, setSelectedMp] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [apiSecret, setApiSecret] = useState("");
  const [connStatus, setConnStatus] = useState<ConnStatus>("idle");
  const [toast, setToast] = useState("");
  const [fileName, setFileName] = useState("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [productosFile, setProductosFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState("");
  const [uploadedRows, setUploadedRows] = useState<string[][]>([]);
  const [loadingProductos, setLoadingProductos] = useState(false);
  const [importResult, setImportResult] = useState<BsaleImportResult | null>(null);
  const [productos, setProductos] = useState<Producto[]>([]);
  const [catalogoPage, setCatalogoPage] = useState(0);
  const catalogoPageSize = 15;
  const [uploading, setUploading] = useState(false);
  const [dragging, setDragging] = useState(false);
  // Falabella es real (viene del backend); el resto son placeholders que
  // conectará quien tome cada canal.
  const [syncStatuses, setSyncStatuses] = useState([
    { name: "Falabella", status: "pending", lastSync: "cargando…" },
    { name: "MercadoLibre", status: "disconnected", lastSync: "pendiente" },
    { name: "Walmart", status: "disconnected", lastSync: "pendiente" },
    { name: "Shopify", status: "disconnected", lastSync: "pendiente" },
    { name: "Tiendanube", status: "disconnected", lastSync: "pendiente" },
  ]);
  // ─── Comisiones por canal (real, conectado a /api/canales) ────────────────
  const [canales, setCanales] = useState<CanalVenta[]>([]);
  const [canalesError, setCanalesError] = useState("");
  const [selectedCanalId, setSelectedCanalId] = useState("");
  const [costos, setCostos] = useState<CostoCanal[]>([]);
  const [costosLoading, setCostosLoading] = useState(false);
  const [costosError, setCostosError] = useState("");
  const [editingCostoId, setEditingCostoId] = useState<string | null>(null);
  const [commTipo, setCommTipo] = useState<TipoCosto>("COMISION_PORCENTAJE");
  const [commCategoria, setCommCategoria] = useState("");
  const [commValor, setCommValor] = useState("");
  const [commEsPorcentaje, setCommEsPorcentaje] = useState(true);
  const [commFechaInicio, setCommFechaInicio] = useState("");
  const [commFechaFin, setCommFechaFin] = useState("");
  const [savingCosto, setSavingCosto] = useState(false);
  const [mlFile, setMlFile] = useState<File | null>(null);
  const [mlImporting, setMlImporting] = useState(false);
  const [mlImportResult, setMlImportResult] = useState<MercadoLibreImportResult | null>(null);
  const [mlError, setMlError] = useState("");
  const [mlCostos, setMlCostos] = useState<MercadoLibreCosto[]>([]);
  const [mlLoading, setMlLoading] = useState(false);
  const [mlSku, setMlSku] = useState("");
  const [mlSearch, setMlSearch] = useState("");
  const [mlPage, setMlPage] = useState(0);
  const [mlExporting, setMlExporting] = useState(false);
  const [mlVentasFile, setMlVentasFile] = useState<File | null>(null);
  const [mlVentasImporting, setMlVentasImporting] = useState(false);
  const [mlVentasResult, setMlVentasResult] = useState<MercadoLibreImportResult | null>(null);

  useEffect(() => {
    getCanales()
      .then((data) => {
        setCanales(data);
        if (data.length > 0) setSelectedCanalId((prev) => prev || data[0].id);
      })
      .catch((err) => setCanalesError(err instanceof Error ? err.message : "No se pudieron cargar los canales"));
  }, []);

  function cargarCostos(canalId: string) {
    if (!canalId) return;
    setCostosLoading(true);
    setCostosError("");
    getCostosCanal(canalId)
      .then(setCostos)
      .catch((err) => setCostosError(err instanceof Error ? err.message : "No se pudieron cargar los costos"))
      .finally(() => setCostosLoading(false));
  }

  useEffect(() => {
    if (selectedCanalId) cargarCostos(selectedCanalId);
  }, [selectedCanalId]);

  function limpiarFormularioComision() {
    setEditingCostoId(null);
    setCommTipo("COMISION_PORCENTAJE");
    setCommCategoria("");
    setCommValor("");
    setCommEsPorcentaje(true);
    setCommFechaInicio("");
    setCommFechaFin("");
  }

  function editarCosto(costo: CostoCanal) {
    setEditingCostoId(costo.id);
    setCommTipo(costo.tipoCosto);
    setCommCategoria(costo.categoria ?? "");
    setCommValor(String(costo.valor));
    setCommEsPorcentaje(costo.esPorcentaje);
    setCommFechaInicio(costo.fechaInicio ?? "");
    setCommFechaFin(costo.fechaFin ?? "");
  }

  async function handleSaveCommission() {
    if (!selectedCanalId) { setToast("Selecciona un canal"); return; }
    const valorNum = Number(commValor.replace(",", "."));
    if (!commValor || Number.isNaN(valorNum)) { setToast("Ingresa un valor numérico válido"); return; }

    const payload = {
      tipoCosto: commTipo,
      categoria: commCategoria.trim() || null,
      valor: valorNum,
      esPorcentaje: commEsPorcentaje,
      fechaInicio: commFechaInicio || undefined,
      fechaFin: commFechaFin || null,
    };

    setSavingCosto(true);
    try {
      if (editingCostoId) {
        await actualizarCostoCanal(selectedCanalId, editingCostoId, payload);
        setToast("Costo actualizado");
      } else {
        await crearCostoCanal(selectedCanalId, payload);
        setToast("Costo agregado");
      }
      limpiarFormularioComision();
      cargarCostos(selectedCanalId);
    } catch (err) {
      setToast(err instanceof Error ? err.message : "No se pudo guardar el costo");
    } finally {
      setSavingCosto(false);
    }
  }

  async function handleEliminarCosto(costoId: string) {
    if (!selectedCanalId) return;
    try {
      await eliminarCostoCanal(selectedCanalId, costoId);
      setToast("Costo eliminado");
      if (editingCostoId === costoId) limpiarFormularioComision();
      cargarCostos(selectedCanalId);
    } catch (err) {
      setToast(err instanceof Error ? err.message : "No se pudo eliminar el costo");
    }
  }

  async function cargarCostosMercadoLibre() {
    setMlLoading(true);
    setMlError("");
    try {
      setMlCostos(await getCostosMercadoLibre());
    } catch (err) {
      setMlError(err instanceof Error ? err.message : "No se pudieron cargar los costos de MercadoLibre");
    } finally {
      setMlLoading(false);
    }
  }

  async function handleImportarMercadoLibre() {
    if (!mlFile) { setMlError("Selecciona un archivo CSV primero"); return; }
    setMlImporting(true);
    setMlError("");
    try {
      setMlImportResult(await importarCostosMercadoLibre(mlFile));
      await cargarCostosMercadoLibre();
      setToast("Costos de MercadoLibre importados");
    } catch (err) {
      setMlError(err instanceof Error ? err.message : "No se pudo importar el CSV");
    } finally {
      setMlImporting(false);
    }
  }

  async function handleBuscarCostoMercadoLibre() {
    const sku = mlSku.trim();
    if (!sku) { cargarCostosMercadoLibre(); return; }
    setMlLoading(true);
    setMlError("");
    try {
      setMlCostos([await getCostoMercadoLibrePorSku(sku)]);
    } catch (err) {
      setMlCostos([]);
      setMlError(err instanceof Error ? err.message : "No se encontró el SKU");
    } finally {
      setMlLoading(false);
    }
  }

  async function handleExportarMercadoLibre() {
    setMlExporting(true);
    setMlError("");
    try {
      const blob = await exportarCostosMercadoLibre();
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "costos-mercadolibre-export.csv";
      link.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      setMlError(err instanceof Error ? err.message : "No se pudo exportar el CSV");
    } finally {
      setMlExporting(false);
    }
  }

  async function handleImportarVentasMercadoLibre() {
    if (!mlVentasFile) { setMlError("Selecciona un CSV de ventas primero"); return; }
    setMlVentasImporting(true);
    setMlError("");
    try {
      setMlVentasResult(await importarVentasMercadoLibre(mlVentasFile));
      setToast("Ventas de MercadoLibre importadas");
      window.dispatchEvent(new Event("dk:reportes-refresh"));
    } catch (err) {
      setMlError(err instanceof Error ? err.message : "No se pudo importar el CSV de ventas");
    } finally {
      setMlVentasImporting(false);
    }
  }

  useEffect(() => {
    cargarCostosMercadoLibre();
  }, []);

  useEffect(() => {
    setMlPage(0);
  }, [mlSearch]);
  const fileRef = useRef<HTMLInputElement>(null);
  const [apiResp, setApiResp] = useState<{ endpoint: string; time: string; ms: number; resumen: string; ok: boolean; body: string } | null>(null);
  const [apiLoading, setApiLoading] = useState("");

  function resumenApi(nombre: string, data: any): string {
    const n = (x: any) => (Array.isArray(x) ? x.length : x ? 1 : 0);
    if (nombre === "Órdenes") return `${n(data?.Orders?.Order)} órdenes reales, en vivo`;
    if (nombre === "Productos") return `${n(data?.Products?.Product)} productos — con campos que NO guardamos (Status, Stock, Price, SpecialPrice…)`;
    if (nombre === "Categorías") return `árbol COMPLETO de categorías de Falabella — nuestra base solo tiene ~110 (las que vendemos)`;
    if (nombre === "Marcas") return `${n(data?.Brands?.Brand)} marcas — nuestra base NO tiene tabla de marcas: esto SOLO puede venir de la API`;
    if (nombre === "Firma inválida") return `Falabella RECHAZÓ la firma adulterada (${data?.ErrorResponse?.Head?.ErrorCode || "E007"}) — así valida la autenticación real`;
    return "";
  }

  async function probarApi(nombre: string, fn: () => Promise<any>) {
    setApiLoading(nombre);
    const t0 = performance.now();
    try {
      const data = await fn();
      const ms = Math.round(performance.now() - t0);
      const full = JSON.stringify(data, null, 2);
      const body = full.length > 8000 ? full.slice(0, 8000) + "\n… (respuesta recortada para mostrar)" : full;
      setApiResp({ endpoint: nombre, time: new Date().toLocaleTimeString(), ms, resumen: resumenApi(nombre, data), ok: true, body });
    } catch (e: any) {
      setApiResp({ endpoint: nombre, time: new Date().toLocaleTimeString(), ms: Math.round(performance.now() - t0), resumen: "", ok: false, body: "Error: " + (e?.message || e) });
    } finally {
      setApiLoading("");
    }
  }

  // Estado real de la integración de Falabella (conexión en vivo + ventas cargadas).
  useEffect(() => {
    getEstadoFalabella()
      .then((e) =>
        setSyncStatuses((prev) =>
          prev.map((s) =>
            s.name === "Falabella"
              ? {
                  ...s,
                  status: e.conexionOk ? "connected" : "disconnected",
                  lastSync: e.ultimaVenta
                    ? `${e.ventasCargadas} ventas · última ${e.ultimaVenta}`
                    : "sin ventas cargadas",
                }
              : s
          )
        )
      )
      .catch(() =>
        setSyncStatuses((prev) =>
          prev.map((s) =>
            s.name === "Falabella" ? { ...s, status: "disconnected", lastSync: "sin conexión" } : s
          )
        )
      );
  }, []);

  function handleConnect() {
    if (!selectedMp) { setToast("Selecciona un marketplace"); return; }
    if (!apiKey) { setToast("Ingresa el API Key"); return; }
    setConnStatus("connecting");
    setTimeout(() => {
      const success = Math.random() > 0.3;
      setConnStatus(success ? "connected" : "error");
      if (success) {
        setToast(`${selectedMp} conectado correctamente`);
        setSyncStatuses((prev) =>
          prev.map((s) => s.name === selectedMp ? { ...s, status: "connected", lastSync: "justo ahora" } : s)
        );
      } else {
        setToast("Error de conexión — verifica tus credenciales");
      }
    }, 1800);
  }

  function handleSync(name: string) {
    setSyncStatuses((prev) => prev.map((s) => s.name === name ? { ...s, status: "syncing", lastSync: "Sincronizando…" } : s));
    setTimeout(() => {
      setSyncStatuses((prev) => prev.map((s) => s.name === name ? { ...s, status: "connected", lastSync: "justo ahora" } : s));
      setToast(`${name} sincronizado`);
    }, 2000);
  }

  async function cargarProductos() {
    setLoadingProductos(true);
    try {
      const data = await listarProductos(200);
      setProductos(data);
      setCatalogoPage(0);
    } catch (err) {
      setFileError(err instanceof Error ? err.message : "No se pudo cargar el catálogo");
    } finally {
      setLoadingProductos(false);
    }
  }

  useEffect(() => {
    cargarProductos();
  }, []);

  function handleFile(file: File) {
    setFileError("");
    setImportResult(null);
    setFileName(file.name);
    setSelectedFile(file);

    if (!isStockFile(file)) {
      setUploadedRows([]);
      setSelectedFile(null);
      setFileError("Formato no soportado. Usa .CSV o .XLSX exportado desde Bsale.");
      return;
    }

    if (isXlsxFile(file)) {
      setUploadedRows([]);
      setToast(`"${file.name}" listo para importar al servidor`);
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const text = reader.result as string;
      const rows = parseCsv(text);
      if (rows.length === 0) {
        setUploadedRows([]);
        setFileError("No se encontraron filas válidas en el archivo.");
        return;
      }
      setUploadedRows(rows);
      setToast(`Archivo "${file.name}" listo para importar`);
    };
    reader.onerror = () => {
      setUploadedRows([]);
      setFileError("Ocurrió un error leyendo el archivo.");
    };
    reader.readAsText(file, "UTF-8");
  }

  async function handleUpload() {
    if (!selectedFile) {
      setToast("Selecciona un archivo primero");
      return;
    }
    setUploading(true);
    setFileError("");
    try {
      const result = await importarStockBsale(selectedFile, productosFile ?? undefined);
      setImportResult(result);
      await cargarProductos();
      const msg = `Importación OK: ${result.productosCreados} creados, ${result.productosActualizados} actualizados`;
      setToast(result.errores > 0 ? `${msg}, ${result.errores} errores` : msg);
    } catch (err) {
      setFileError(err instanceof Error ? err.message : "Error al importar el archivo");
    } finally {
      setUploading(false);
    }
  }

  function handleFileDrop(e: React.DragEvent) {
    e.preventDefault(); setDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFile(file);
  }

  const subNavItems = ["Carga CSV / XLSX", "Integraciones API", "Ajuste Comisiones"];

  // Paginación de la tabla "Catálogo importado" (15 productos por página).
  const catalogoTotalPages = Math.max(1, Math.ceil(productos.length / catalogoPageSize));
  const catalogoCurrentPage = Math.min(catalogoPage, catalogoTotalPages - 1);
  const productosPagina = productos.slice(
    catalogoCurrentPage * catalogoPageSize,
    catalogoCurrentPage * catalogoPageSize + catalogoPageSize
  );
  const mlPageSize = 50;
  const mlCostosFiltrados = mlCostos.filter((c) => c.sku.toLowerCase().includes(mlSearch.toLowerCase()));
  const mlTotalPages = Math.max(1, Math.ceil(mlCostosFiltrados.length / mlPageSize));
  const mlCurrentPage = Math.min(mlPage, mlTotalPages - 1);
  const mlCostosPagina = mlCostosFiltrados.slice(mlCurrentPage * mlPageSize, mlCurrentPage * mlPageSize + mlPageSize);

  return (
    <div className="min-h-screen bg-background flex flex-col" style={{ fontFamily: "Inter, sans-serif" }}>
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
      <div className="h-12 bg-white border-b border-border flex items-center px-5 gap-4 flex-shrink-0">
        <div className="flex items-center gap-2 mr-4">
          <div className="w-5 h-5 bg-foreground rounded-[3px]" />
          <span className="text-[11px] font-bold tracking-widest uppercase text-foreground">D&K </span>
        </div>
        <div className="h-5 w-px bg-border" />
        <span className="text-xs text-muted-foreground">Ingreso de Datos & Configuración</span>
        <div className="ml-auto flex items-center gap-3">
          <PlaceholderBox label="Notificaciones" height="h-6" className="w-24" />
          <div className="w-7 h-7 rounded-full bg-muted border border-border flex items-center justify-center">
            <span className="text-[9px] font-mono text-muted-foreground">US</span>
          </div>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        <aside className="w-52 bg-white border-r border-border flex flex-col flex-shrink-0">
          <div className="p-3 flex flex-col gap-0.5">
            {sidebarItems.map(({ icon, label }) => (
              <button
                key={label}
                onClick={() => {
                  setActiveNav(label);
              
                  if (label === "Reportes") goTo(1);
                  if (label === "Integraciones") goTo(2);
                }}
                className={`w-full flex items-center gap-2.5 px-3 py-2 rounded text-xs font-medium transition-all text-left ${
                  activeNav === label ? "bg-foreground text-white" : "text-muted-foreground hover:bg-accent hover:text-foreground"
                }`}>
                {icon}{label}
              </button>
            ))}
          </div>
          <div className="px-3 mt-1">
            <p className="text-[9px] font-mono uppercase tracking-wider text-muted-foreground/60 px-3 mb-1">Ingreso de Datos</p>
            {subNavItems.map((sub) => (
              <button key={sub} onClick={() => setActiveSubNav(sub)}
                className={`w-full text-left px-3 py-1.5 text-[11px] rounded transition-colors ${
                  activeSubNav === sub ? "bg-accent text-foreground font-medium" : "text-muted-foreground hover:bg-accent hover:text-foreground"
                }`}>
                {sub}
              </button>
            ))}
          </div>
          <div className="mt-auto p-3 border-t border-border">
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 rounded-full bg-muted border border-border" />
              <div>
                <p className="text-[10px] font-medium text-foreground">Admin Usuario</p>
                <p className="text-[9px] font-mono text-muted-foreground">admin@empresa.com</p>
              </div>
            </div>
          </div>
        </aside>

        <main className="flex-1 overflow-auto p-6">
          <div className="max-w-[860px] mx-auto flex flex-col gap-5">

            {/* Section 1: CSV Upload (carga de Stock Bsale — aporte del compañero) */}
            <SectionCard title="Carga Stock actual Bsale (XLSX / CSV)" annotation="S1 · Importación Masiva">
              <div className="flex flex-col gap-4">
                <div
                  onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
                  onDragLeave={() => setDragging(false)}
                  onDrop={handleFileDrop}
                  className={`border-2 border-dashed rounded-lg p-8 flex flex-col items-center gap-3 transition-colors cursor-pointer ${dragging ? "border-foreground bg-muted" : "border-border bg-background"}`}
                  onClick={() => fileRef.current?.click()}
                >
                  <input ref={fileRef} type="file" accept=".csv,.xlsx" className="hidden"
                    onChange={(e) => { if (e.target.files?.[0]) handleFile(e.target.files[0]); }} />
                  <Upload size={28} className="text-muted-foreground/50" />
                  <div className="text-center">
                    <p className="text-sm font-medium text-foreground">Selecciona los archivos exportados desde Bsale</p>
                    <p className="text-[11px] text-muted-foreground mt-0.5">Usa un archivo de Productos y servicios y otro de Stock actual</p>
                  </div>
                  <span className="text-[10px] font-mono text-muted-foreground">o haz clic para seleccionar</span>
                </div>
                <div className="flex items-center gap-3">
                  <div className="flex-1 h-8 bg-input-background border border-border rounded flex items-center px-3">
                    <span className="text-xs text-muted-foreground font-mono truncate">{fileName || "Ningún archivo seleccionado"}</span>
                  </div>
                  <PrimaryButton
                    label={uploading ? "Importando…" : "Subir archivo"}
                    onClick={handleUpload}
                    disabled={uploading || !selectedFile}
                  />
                  <SecondaryButton label="Actualizar catálogo" onClick={cargarProductos} />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <label className="flex flex-col gap-1 text-xs font-medium text-foreground">
                    Productos y servicios
                    <input type="file" accept=".csv,.xlsx" onChange={(e) => setProductosFile(e.target.files?.[0] ?? null)} className="h-9 rounded border border-border bg-input-background px-2 py-1.5 text-xs file:mr-2 file:border-0 file:bg-transparent file:text-xs" />
                    <span className="text-[10px] font-mono font-normal text-muted-foreground">SKU, Producto, Marca, Categoría, Estado</span>
                  </label>
                  <label className="flex flex-col gap-1 text-xs font-medium text-foreground">
                    Stock actual
                    <input type="file" accept=".csv,.xlsx" onChange={(e) => { const file = e.target.files?.[0] ?? null; setSelectedFile(file); setFileName(file?.name ?? ""); }} className="h-9 rounded border border-border bg-input-background px-2 py-1.5 text-xs file:mr-2 file:border-0 file:bg-transparent file:text-xs" />
                    <span className="text-[10px] font-mono font-normal text-muted-foreground">SKU, Stock, Costo promedio</span>
                  </label>
                </div>
                {fileError ? (
                  <div className="mt-3 rounded border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {fileError}
                  </div>
                ) : null}
                {importResult ? (
                  <div className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-800">
                    Procesados: {importResult.totalProcesados} · Creados: {importResult.productosCreados} ·
                    Actualizados: {importResult.productosActualizados} · Omitidos: {importResult.productosOmitidos}
                    {importResult.errores > 0 ? ` · Errores: ${importResult.errores}` : ""}
                  </div>
                ) : null}
                {importResult && importResult.detalleErrores.length > 0 ? (
                  <div className="rounded border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-900 max-h-32 overflow-auto">
                    {importResult.detalleErrores.slice(0, 10).map((err, i) => (
                      <p key={i}>{err}</p>
                    ))}
                  </div>
                ) : null}
                {uploadedRows.length > 0 ? (
                  <div className="mt-4 border border-border rounded-lg bg-white overflow-auto">
                    <div className="px-4 py-3 border-b border-border bg-muted/50">
                      <p className="text-sm font-medium text-foreground">Previsualización CSV (cliente)</p>
                      <p className="text-[11px] text-muted-foreground">Mostrando hasta 5 filas del archivo cargado</p>
                    </div>
                    <table className="min-w-full text-xs">
                      <thead className="bg-muted">
                        <tr>
                          {uploadedRows[0].map((header, idx) => (
                            <th key={idx} className="px-3 py-2 text-left font-semibold uppercase tracking-wider text-muted-foreground">
                              {header}
                            </th>
                          ))}
                        </tr>
                      </thead>
                      <tbody>
                        {uploadedRows.slice(1, 6).map((row, rowIndex) => (
                          <tr key={rowIndex} className={rowIndex % 2 === 0 ? "bg-background" : "bg-muted/10"}>
                            {row.map((cell, cellIndex) => (
                              <td key={cellIndex} className="px-3 py-2 align-top text-xs text-foreground">
                                {cell}
                              </td>
                            ))}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                    <div className="p-3 text-[11px] text-muted-foreground">
                      {uploadedRows.length - 1} fila(s) en el archivo.
                    </div>
                  </div>
                ) : null}
                <div className="border border-border rounded-lg bg-white">
                  <div className="px-4 py-3 border-b border-border bg-muted/50 flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-foreground">Catálogo importado</p>
                      <p className="text-[11px] text-muted-foreground">
                        {loadingProductos ? "Cargando…" : `${productos.length} producto(s) en base de datos`}
                      </p>
                    </div>
                    {loadingProductos ? <Loader2 size={16} className="animate-spin text-muted-foreground" /> : null}
                  </div>
                  {productos.length > 0 ? (
                    <>
                      <div className="overflow-x-auto">
                        <table className="min-w-full text-xs">
                          <thead className="bg-muted">
                            <tr>
                              {["SKU", "Producto", "Marca", "Tipo", "Stock", "Costo neto"].map((h) => (
                                <th key={h} className="px-3 py-2 text-left font-semibold uppercase tracking-wider text-muted-foreground">
                                  {h}
                                </th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {productosPagina.map((p, i) => (
                              <tr key={p.id} className={i % 2 === 0 ? "bg-background" : "bg-muted/10"}>
                                <td className="px-3 py-2 font-mono text-muted-foreground">{p.sku}</td>
                                <td className="px-3 py-2 text-foreground">{p.nombre}</td>
                                <td className="px-3 py-2 text-foreground">{p.marca ?? "—"}</td>
                                <td className="px-3 py-2 text-foreground">{p.tipoProducto ?? "—"}</td>
                                <td className="px-3 py-2 font-mono">{Number(p.stock).toLocaleString("es-CL")}</td>
                                <td className="px-3 py-2 font-mono">{fmtCl(Number(p.costoBase))}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                      <div className="px-4 py-2.5 border-t border-border flex items-center justify-between bg-muted/50">
                        <span className="text-[10px] font-mono text-muted-foreground">
                          {productos.length} producto(s) · página {catalogoCurrentPage + 1} de {catalogoTotalPages}
                        </span>
                        <div className="flex items-center gap-1">
                          <button
                            onClick={() => setCatalogoPage((p) => Math.max(0, p - 1))}
                            disabled={catalogoCurrentPage === 0}
                            className="h-6 px-2 text-[10px] font-medium border border-border rounded bg-white hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                            ← Anterior
                          </button>
                          <button
                            onClick={() => setCatalogoPage((p) => Math.min(catalogoTotalPages - 1, p + 1))}
                            disabled={catalogoCurrentPage >= catalogoTotalPages - 1}
                            className="h-6 px-2 text-[10px] font-medium border border-border rounded bg-white hover:bg-muted disabled:opacity-40 disabled:cursor-not-allowed transition-colors">
                            Siguiente →
                          </button>
                        </div>
                      </div>
                    </>
                  ) : (
                    <p className="px-4 py-6 text-sm text-muted-foreground text-center">
                      Aún no hay productos. Sube el export «Stock actual» de Bsale para poblar el catálogo.
                    </p>
                  )}
                </div>
                <div className="bg-muted/50 border border-border rounded p-3">
                  <p className="text-[10px] font-mono text-muted-foreground">
                    Columnas esperadas (Bsale): Tipo de Producto · Producto · Variante · SKU · Stock ·
                    Costo Neto Prom. Unitario · Marca
                  </p>
                </div>
              </div>
            </SectionCard>

            <SectionCard title="Costos MercadoLibre" annotation="S2 · CSV Integración">
              <div className="flex flex-col gap-4">
                <div className="rounded border border-border bg-muted/30 p-3">
                  <p className="text-xs font-semibold text-foreground">Importar ventas para Reportes</p>
                  <p className="mt-1 text-[10px] font-mono text-muted-foreground">Requeridas: orderId, fecha, sku, cantidad, precioVenta. Opcionales: comision, envio.</p>
                  <div className="mt-3 flex items-end gap-3">
                    <input type="file" accept=".csv,text/csv" onChange={(event) => { setMlVentasFile(event.target.files?.[0] ?? null); setMlVentasResult(null); }} className="h-9 flex-1 rounded border border-border bg-input-background px-2 py-1.5 text-xs file:mr-3 file:border-0 file:bg-transparent file:text-xs file:font-medium" />
                    <PrimaryButton label={mlVentasImporting ? "Importando…" : "Importar ventas"} onClick={handleImportarVentasMercadoLibre} disabled={mlVentasImporting || !mlVentasFile} />
                  </div>
                  {mlVentasResult && <p className="mt-2 text-xs text-emerald-700">Creados: {mlVentasResult.creados} · Actualizados: {mlVentasResult.actualizados} · Omitidos: {mlVentasResult.omitidos} · Errores: {mlVentasResult.errores}</p>}
                </div>
                <div className="flex items-end gap-3">
                  <div className="flex-1 flex flex-col gap-1">
                    <label className="text-xs font-medium text-foreground">Archivo CSV de costos</label>
                    <input
                      type="file"
                      accept=".csv,text/csv"
                      onChange={(event) => { setMlFile(event.target.files?.[0] ?? null); setMlImportResult(null); setMlError(""); }}
                      className="h-9 w-full rounded border border-border bg-input-background px-2 py-1.5 text-xs text-foreground file:mr-3 file:border-0 file:bg-transparent file:text-xs file:font-medium"
                    />
                  </div>
                  <PrimaryButton label={mlImporting ? "Importando…" : "Importar CSV"} onClick={handleImportarMercadoLibre} disabled={mlImporting || !mlFile} />
                </div>
                <p className="text-[10px] font-mono text-muted-foreground">Encabezados: SKU,CostoProm,UltimoCosto,CostoMercadoLibre</p>

                {mlError && <div className="rounded border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">{mlError}</div>}
                {mlImportResult && (
                  <div className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs text-emerald-800">
                    Creados: {mlImportResult.creados} · Actualizados: {mlImportResult.actualizados} · Omitidos: {mlImportResult.omitidos} · Errores: {mlImportResult.errores} · Procesados: {mlImportResult.totalProcesados}
                    {mlImportResult.detalleErrores.length > 0 && (
                      <div className="mt-2 max-h-24 overflow-auto border-t border-emerald-200 pt-2">
                        {mlImportResult.detalleErrores.map((error, index) => <p key={index}>{error}</p>)}
                      </div>
                    )}
                  </div>
                )}

                <div className="flex items-end gap-3 border-t border-border pt-4">
                  <div className="flex-1 flex flex-col gap-1">
                    <label className="text-xs font-medium text-foreground">Buscar costo por SKU</label>
                    <input value={mlSku} onChange={(event) => setMlSku(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter") handleBuscarCostoMercadoLibre(); }} placeholder="Ej: SKU-001" className="h-9 rounded border border-border bg-input-background px-3 text-xs outline-none focus:border-foreground/40" />
                  </div>
                  <PrimaryButton label="Buscar" onClick={handleBuscarCostoMercadoLibre} disabled={mlLoading} />
                  <SecondaryButton label="Actualizar" icon={mlLoading ? <Loader2 size={13} className="animate-spin" /> : <RefreshCw size={13} />} onClick={cargarCostosMercadoLibre} />
                  <SecondaryButton label={mlExporting ? "Exportando…" : "Descargar CSV"} icon={<Download size={13} />} onClick={handleExportarMercadoLibre} />
                </div>

                <div className="border border-border rounded overflow-hidden">
                  <div className="bg-muted px-3 py-2 border-b border-border flex items-center justify-between">
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Costos importados ({mlCostos.length})</p>
                    <input value={mlSearch} onChange={(event) => setMlSearch(event.target.value)} placeholder="Filtrar tabla…" className="h-7 w-40 rounded border border-border bg-white px-2 text-[11px] outline-none" />
                  </div>
                  {mlLoading ? <div className="p-6 text-center text-xs text-muted-foreground"><Loader2 size={16} className="mx-auto animate-spin" /></div> : (
                    <div className="overflow-x-auto">
                      <table className="min-w-full text-[11px]">
                        <thead className="bg-muted/50"><tr>{["SKU", "Costo prom.", "Último costo", "Costo MercadoLibre", "Fuente", "Actualizado"].map((header) => <th key={header} className="px-3 py-2 text-left font-semibold uppercase tracking-wider text-muted-foreground">{header}</th>)}</tr></thead>
                        <tbody>
                          {mlCostosPagina.map((c, index) => (
                            <tr key={c.id} className={index % 2 === 0 ? "bg-background" : "bg-muted/10"}>
                              <td className="px-3 py-2 font-mono text-foreground">{c.sku}</td>
                              <td className="px-3 py-2 font-mono">{c.costoProm == null ? "—" : fmtCl(c.costoProm)}</td>
                              <td className="px-3 py-2 font-mono">{c.ultimoCosto == null ? "—" : fmtCl(c.ultimoCosto)}</td>
                              <td className="px-3 py-2 font-mono font-medium">{fmtCl(c.costoMercadoLibre)}</td>
                              <td className="px-3 py-2 text-muted-foreground">{c.fuenteArchivo}</td>
                              <td className="px-3 py-2 font-mono text-muted-foreground">{c.updatedAt}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                      {!mlLoading && mlCostosFiltrados.length === 0 && <p className="px-4 py-5 text-center text-xs text-muted-foreground">No hay costos para mostrar.</p>}
                      {mlCostosFiltrados.length > 0 && <div className="flex items-center justify-between border-t border-border bg-muted/50 px-3 py-2">
                        <span className="text-[10px] font-mono text-muted-foreground">{mlCostosFiltrados.length} registros · página {mlCurrentPage + 1} de {mlTotalPages}</span>
                        <div className="flex items-center gap-1">
                          <button onClick={() => setMlPage((page) => Math.max(0, page - 1))} disabled={mlCurrentPage === 0} className="h-6 px-2 text-[10px] border border-border rounded bg-white disabled:opacity-40">Anterior</button>
                          <button onClick={() => setMlPage((page) => Math.min(mlTotalPages - 1, page + 1))} disabled={mlCurrentPage >= mlTotalPages - 1} className="h-6 px-2 text-[10px] border border-border rounded bg-white disabled:opacity-40">Siguiente</button>
                        </div>
                      </div>}
                    </div>
                  )}
                </div>
              </div>
            </SectionCard>

            {/* Section 2: API Integration */}
            <SectionCard title="Integración con Marketplace (API)" annotation="S2 · Conexión Automática">
              <div className="grid grid-cols-2 gap-5">
                <div className="flex flex-col gap-3">
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-medium text-foreground">Marketplace</label>
                    <Dropdown options={MARKETPLACE_LIST} value={selectedMp} onChange={(v) => { setSelectedMp(v); setConnStatus("idle"); }} placeholder="Seleccionar marketplace…" />
                  </div>
                  <InputField label="API Key" placeholder="sk_live_xxxxxxxxxxxxxxxx" value={apiKey} onChange={setApiKey} />
                  <InputField label="API Secret / Token" placeholder="••••••••••••••••••••" type="password" value={apiSecret} onChange={setApiSecret} />
                  <div className="flex gap-2 mt-1">
                    <button
                      onClick={handleConnect}
                      disabled={connStatus === "connecting"}
                      className="h-9 bg-foreground rounded flex items-center justify-center px-4 gap-2 cursor-pointer hover:opacity-80 disabled:opacity-50 transition-opacity"
                    >
                      {connStatus === "connecting" && <Loader2 size={13} className="text-white animate-spin" />}
                      <span className="text-xs font-semibold text-white">
                        {connStatus === "connecting" ? "Conectando…" : connStatus === "connected" ? "Reconectar" : "Conectar"}
                      </span>
                    </button>
                    <SecondaryButton label="Verificar" onClick={async () => {
                      if (!selectedMp) { setToast("Selecciona un marketplace"); return; }
                      if (selectedMp === "Falabella") {
                        try {
                          const e = await getEstadoFalabella();
                          setToast(e.conexionOk
                            ? `Falabella OK — ${e.ventasCargadas} ventas cargadas`
                            : `Falabella: ${e.mensaje}`);
                        } catch {
                          setToast("No se pudo verificar Falabella");
                        }
                      } else {
                        setToast(`${selectedMp}: integración aún no implementada`);
                      }
                    }} />
                  </div>
                  {connStatus === "connected" && (
                    <div className="flex items-center gap-2 p-2.5 bg-muted rounded border border-border">
                      <CheckCircle2 size={12} className="text-foreground" />
                      <span className="text-[11px] font-mono text-foreground">{selectedMp} conectado correctamente</span>
                    </div>
                  )}
                  {connStatus === "error" && (
                    <div className="flex items-center gap-2 p-2.5 bg-muted rounded border border-border">
                      <XCircle size={12} className="text-muted-foreground" />
                      <span className="text-[11px] font-mono text-muted-foreground">Error — verifica las credenciales</span>
                    </div>
                  )}
                </div>
                <div className="flex flex-col gap-2">
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-1">Marketplaces disponibles</p>
                  {MARKETPLACE_LIST.map((mp) => {
                    const est = syncStatuses.find((s) => s.name === mp);
                    const isConnected = est?.status === "connected";
                    return (
                      <button key={mp} onClick={() => { setSelectedMp(mp); setConnStatus("idle"); }}
                        className={`flex items-center gap-2 px-3 py-2 border rounded text-left transition-colors ${selectedMp === mp ? "border-foreground bg-muted" : "border-border bg-background hover:bg-muted/50"}`}>
                        <div className="w-5 h-5 bg-muted rounded-sm border border-border flex-shrink-0" />
                        <span className="flex-1 min-w-0">
                          <span className="text-xs text-foreground block">{mp}</span>
                          {est?.lastSync && <span className="text-[9px] font-mono text-muted-foreground block truncate">{est.lastSync}</span>}
                        </span>
                        {isConnected && <CheckCircle2 size={11} className="text-muted-foreground flex-shrink-0" />}
                      </button>
                    );
                  })}
                </div>
              </div>
            </SectionCard>

            {/* Explorador API Falabella (en vivo) */}
            <SectionCard title="Explorador de API Falabella (en vivo)" annotation="Prueba los endpoints reales">
              <div className="flex flex-col gap-3">
                <div className="flex flex-wrap gap-2">
                  {([
                    ["Órdenes", () => falabellaOrders()],
                    ["Productos", () => falabellaProducts(5)],
                    ["Categorías", () => falabellaCategories()],
                    ["Marcas", () => falabellaBrands()],
                  ] as [string, () => Promise<any>][]).map(([label, fn]) => (
                    <button key={label} onClick={() => probarApi(label, fn)} disabled={!!apiLoading}
                      className="h-8 px-3 text-xs font-medium border border-border rounded bg-white hover:bg-muted disabled:opacity-50 transition-colors flex items-center gap-1.5">
                      {apiLoading === label && <Loader2 size={12} className="animate-spin" />}
                      {label}
                    </button>
                  ))}
                  <button onClick={() => probarApi("Firma inválida", () => falabellaTestFirma())} disabled={!!apiLoading}
                    className="h-8 px-3 text-xs font-medium border border-dashed border-border rounded bg-white text-muted-foreground hover:bg-muted disabled:opacity-50 transition-colors flex items-center gap-1.5">
                    {apiLoading === "Firma inválida" && <Loader2 size={12} className="animate-spin" />}
                    Firma inválida (debe fallar)
                  </button>
                </div>
                <p className="text-[10px] font-mono text-muted-foreground leading-relaxed">
                  Cada botón hace una llamada EN VIVO a la API de Falabella (Seller Center). Cómo se nota que NO es la base ni un archivo: el tiempo de respuesta son cientos de ms (viaje real a los servidores de Falabella, no una lectura local instantánea), y datos como <b>Marcas</b> (16.878) o el árbol completo de <b>Categorías</b> ni siquiera existen en nuestra base — solo Falabella los tiene.
                </p>
                {apiResp && (
                  <div className="border border-border rounded overflow-hidden">
                    <div className="bg-muted px-3 py-1.5 border-b border-border flex items-center gap-2">
                      <span className="text-[10px] font-mono text-foreground">{apiResp.endpoint}</span>
                      <span className="text-[9px] font-mono text-muted-foreground">— {apiResp.time} · <b>{apiResp.ms} ms</b> (ida y vuelta a Falabella)</span>
                      <CheckCircle2 size={11} className="text-muted-foreground ml-auto" />
                    </div>
                    {apiResp.resumen && (
                      <div className="px-3 py-2 bg-background border-b border-border">
                        <span className="text-[11px] font-medium text-foreground">→ {apiResp.resumen}</span>
                      </div>
                    )}
                    <pre className="text-[10px] font-mono text-foreground p-3 max-h-72 overflow-auto whitespace-pre-wrap bg-background">{apiResp.body}</pre>
                  </div>
                )}
              </div>
            </SectionCard>

            {/* Section 3: Comisiones y costos por canal (real, GET/POST/PUT/DELETE /api/canales) */}
            <SectionCard title="Comisiones y Costos por Canal" annotation="S3 · Conectado a /api/canales">
              {canalesError ? (
                <p className="text-[11px] font-mono text-destructive">{canalesError}</p>
              ) : (
                <>
                  <div className="flex items-end gap-4">
                    <div className="flex flex-col gap-1 w-64">
                      <label className="text-xs font-medium text-foreground">Canal</label>
                      <Dropdown
                        options={canales.map((c) => c.nombre)}
                        value={canales.find((c) => c.id === selectedCanalId)?.nombre ?? ""}
                        onChange={(nombre) => {
                          const canal = canales.find((c) => c.nombre === nombre);
                          if (canal) { setSelectedCanalId(canal.id); limpiarFormularioComision(); }
                        }}
                        placeholder="Seleccionar canal…"
                      />
                    </div>
                    {selectedCanalId && (
                      <span className="text-[10px] font-mono text-muted-foreground pb-2">
                        {canales.find((c) => c.id === selectedCanalId)?.tipo}
                      </span>
                    )}
                  </div>

                  <div className="mt-4 border border-border rounded overflow-hidden">
                    <div className="bg-muted px-3 py-2 border-b border-border flex items-center">
                      <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                        Costos vigentes {costosLoading ? "· cargando…" : `(${costos.length})`}
                      </p>
                    </div>
                    {costosError ? (
                      <p className="px-4 py-4 text-[11px] font-mono text-destructive text-center">{costosError}</p>
                    ) : !costosLoading && costos.length === 0 ? (
                      <p className="px-4 py-4 text-[11px] font-mono text-muted-foreground text-center">Este canal no tiene costos configurados</p>
                    ) : costos.map((c) => (
                      <div key={c.id} className="flex items-center px-3 py-2 border-b border-border last:border-0 text-[11px]">
                        <span className="text-foreground w-44">{TIPOS_COSTO.find((t) => t.value === c.tipoCosto)?.label ?? c.tipoCosto}</span>
                        <span className="font-mono text-muted-foreground w-28">{c.categoria || "Todo el canal"}</span>
                        <span className="font-mono text-foreground font-medium w-20">
                          {c.esPorcentaje ? `${c.valor}%` : fmt(c.valor)}
                        </span>
                        <span className="font-mono text-muted-foreground flex-1">
                          desde {c.fechaInicio} {c.fechaFin ? `hasta ${c.fechaFin}` : "(vigente)"}
                        </span>
                        <button onClick={() => editarCosto(c)}
                          className="ml-3 text-[10px] text-muted-foreground underline hover:text-foreground">Editar</button>
                        <button onClick={() => handleEliminarCosto(c.id)}
                          className="ml-3 text-[10px] text-muted-foreground underline hover:text-foreground">Eliminar</button>
                      </div>
                    ))}
                  </div>

                  <div className="mt-5">
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground mb-2">
                      {editingCostoId ? "Editar costo" : "Agregar costo"}
                    </p>
                    <div className="grid grid-cols-4 gap-4">
                      <div className="flex flex-col gap-1">
                        <label className="text-xs font-medium text-foreground">Tipo de costo</label>
                        <Dropdown
                          options={TIPOS_COSTO.map((t) => t.label)}
                          value={TIPOS_COSTO.find((t) => t.value === commTipo)?.label ?? ""}
                          onChange={(label) => {
                            const t = TIPOS_COSTO.find((x) => x.label === label);
                            if (t) { setCommTipo(t.value); setCommEsPorcentaje(t.esPorcentajeDefault); }
                          }}
                        />
                      </div>
                      <InputField label="Categoría (opcional)" placeholder="Ej: Electrohogar — vacío = todo el canal" value={commCategoria} onChange={setCommCategoria} />
                      <InputField label={commEsPorcentaje ? "Valor (%)" : "Valor ($)"} placeholder="12.00" value={commValor} onChange={setCommValor} />
                      <div className="flex flex-col gap-1">
                        <label className="text-xs font-medium text-foreground">Unidad</label>
                        <div className="flex gap-2">
                          <button
                            onClick={() => setCommEsPorcentaje(true)}
                            className={`flex-1 rounded border px-2 py-1.5 text-xs ${commEsPorcentaje ? "bg-accent border-accent text-foreground font-medium" : "border-border text-muted-foreground"}`}
                          >%</button>
                          <button
                            onClick={() => setCommEsPorcentaje(false)}
                            className={`flex-1 rounded border px-2 py-1.5 text-xs ${!commEsPorcentaje ? "bg-accent border-accent text-foreground font-medium" : "border-border text-muted-foreground"}`}
                          >$ fijo</button>
                        </div>
                      </div>
                    </div>
                    <div className="grid grid-cols-4 gap-4 mt-3">
                      <InputField label="Vigente desde" type="date" value={commFechaInicio} onChange={setCommFechaInicio} />
                      <InputField label="Vigente hasta (opcional)" type="date" value={commFechaFin} onChange={setCommFechaFin} />
                    </div>
                    <div className="mt-4 flex items-center gap-3">
                      <PrimaryButton
                        label={savingCosto ? "Guardando…" : editingCostoId ? "Guardar cambios" : "Agregar costo"}
                        onClick={handleSaveCommission}
                        disabled={savingCosto || !selectedCanalId}
                      />
                      <SecondaryButton label="Cancelar" onClick={limpiarFormularioComision} />
                      <span className="text-[10px] font-mono text-muted-foreground ml-2">
                        Afecta el cálculo de rentabilidad de todas las ventas del canal desde la fecha de vigencia
                      </span>
                    </div>
                  </div>
                </>
              )}
            </SectionCard>

          </div>
        </main>
      </div>
    </div>
  );
}

// ─── Root ─────────────────────────────────────────────────────────────────────

export default function App() {
  const [screen, setScreen] = useState(0);
  useEffect(() => {
    const handleLogout = () => setScreen(0);
    window.addEventListener("dk:logout", handleLogout);
    return () => window.removeEventListener("dk:logout", handleLogout);
  }, []);

  return (
    <div className="min-h-screen bg-background" style={{ fontFamily: "Inter, sans-serif" }}>
      {screen === 0 && <WF1Login onLogin={() => setScreen(1)} />}
      {screen === 1 && <WF2Reportes goTo={setScreen} />}
      {screen === 2 && <WF3IngresoData goTo={setScreen} />}
    </div>
  );
}
