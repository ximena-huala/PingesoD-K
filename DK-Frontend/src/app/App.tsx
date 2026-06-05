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

// ─── Types ────────────────────────────────────────────────────────────────────

type SaleRow = {
  date: string;
  marketplace: string;
  sku: string;
  product: string;
  priceRaw: number;
  shippingRaw: number;
  commissionRaw: number;
  netRaw: number;
};

// ─── Data ─────────────────────────────────────────────────────────────────────

const ALL_ROWS: SaleRow[] = [
  { date: "2025-05-20", marketplace: "MercadoLibre", sku: "MLU-4482", product: "Auriculares Bluetooth Pro", priceRaw: 8490, shippingRaw: 480, commissionRaw: 1019, netRaw: 6991 },
  { date: "2025-05-20", marketplace: "Walmart", sku: "WMT-1129", product: "Cargador USB-C 65W GaN", priceRaw: 3200, shippingRaw: 200, commissionRaw: 448, netRaw: 2552 },
  { date: "2025-05-19", marketplace: "Shopify", sku: "SHP-0087", product: "Funda Silicona iPhone 15", priceRaw: 1850, shippingRaw: 150, commissionRaw: 185, netRaw: 1515 },
  { date: "2025-05-19", marketplace: "MercadoLibre", sku: "MLU-3301", product: "Teclado Mecánico TKL", priceRaw: 12900, shippingRaw: 890, commissionRaw: 1548, netRaw: 10462 },
  { date: "2025-05-18", marketplace: "Tienda física", sku: "TN-0052", product: "Mouse Inalámbrico Ergonómico", priceRaw: 4750, shippingRaw: 300, commissionRaw: 380, netRaw: 4070 },
  { date: "2025-05-18", marketplace: "Walmart", sku: "WMT-0934", product: "Hub USB 7 Puertos", priceRaw: 5490, shippingRaw: 420, commissionRaw: 769, netRaw: 4301 },
  { date: "2025-05-17", marketplace: "MercadoLibre", sku: "MLU-8821", product: 'Monitor 24" FHD 144Hz', priceRaw: 68000, shippingRaw: 2100, commissionRaw: 8160, netRaw: 57740 },
  { date: "2025-05-17", marketplace: "Falabella", sku: "FAL-2210", product: "Silla Gamer Pro RGB", priceRaw: 32000, shippingRaw: 1500, commissionRaw: 3200, netRaw: 27300 },
  { date: "2025-05-16", marketplace: "Shopify", sku: "SHP-0120", product: "Lámpara LED Escritorio", priceRaw: 2800, shippingRaw: 180, commissionRaw: 280, netRaw: 2340 },
  { date: "2025-05-16", marketplace: "Tienda física", sku: "TN-0091", product: "Soporte Celular Auto", priceRaw: 1200, shippingRaw: 90, commissionRaw: 96, netRaw: 1014 },
  { date: "2025-05-15", marketplace: "Walmart", sku: "WMT-1540", product: "Webcam Full HD 1080p", priceRaw: 9800, shippingRaw: 600, commissionRaw: 1372, netRaw: 7828 },
  { date: "2025-05-15", marketplace: "MercadoLibre", sku: "MLU-6610", product: "Disco SSD 1TB NVMe", priceRaw: 22500, shippingRaw: 0, commissionRaw: 2700, netRaw: 19800 },
  { date: "2025-05-14", marketplace: "Falabella", sku: "FAL-0882", product: "Auriculares Over-Ear ANC", priceRaw: 15900, shippingRaw: 800, commissionRaw: 1590, netRaw: 13510 },
  { date: "2025-05-14", marketplace: "Shopify", sku: "SHP-0055", product: "Cable HDMI 2.1 2m", priceRaw: 980, shippingRaw: 60, commissionRaw: 98, netRaw: 822 },
];

const MARKETPLACES = ["Todos", "MercadoLibre", "Walmart", "Shopify", "Tienda física", "Falabella"];

const fmt = (n: number) =>
  "$" + n.toLocaleString("es-MX", { minimumFractionDigits: 0, maximumFractionDigits: 0 });

function calcKpis(rows: SaleRow[]) {
  const sales = rows.reduce((s, r) => s + r.priceRaw, 0);
  const net = rows.reduce((s, r) => s + r.netRaw, 0);
  const shipping = rows.reduce((s, r) => s + r.shippingRaw, 0);
  const commission = rows.reduce((s, r) => s + r.commissionRaw, 0);
  return { sales, net, shipping, commission };
}

function buildChartData(rows: SaleRow[]) {
  const byDate: Record<string, number> = {};
  rows.forEach((r) => {
    const label = r.date.slice(5); // MM-DD
    byDate[label] = (byDate[label] || 0) + r.priceRaw;
  });
  return Object.entries(byDate)
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([date, ventas]) => ({ date, ventas }));
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
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [company, setCompany] = useState("");
  const [confirmPwd, setConfirmPwd] = useState("");
  const [toast, setToast] = useState("");
  const [checked, setChecked] = useState(false);

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
                <PrimaryButton label="Iniciar Sesión" full onClick={() => {
                  if (!email || !password) { 
                    setToast("Completa todos los campos"); 
                    return; 
                  }
                  setToast("Acceso simulado correctamente");
                  setTimeout(() => {
                    onLogin();
                  }, 500);
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

const tableCols = ["Fecha", "Marketplace", "SKU", "Producto", "Precio Venta", "Costo Envío", "Comisión", "Ganancia Neta"];

function WF2Reportes({ goTo }: { goTo: (n: number) => void }) {
  const [activeNav, setActiveNav] = useState("Reportes");
  const [mpFilter, setMpFilter] = useState("Todos");
  const [dateFrom, setDateFrom] = useState("2025-05-01");
  const [dateTo, setDateTo] = useState("2025-05-20");
  const [search, setSearch] = useState(""); // applied
  const [searchInput, setSearchInput] = useState(""); // live input
  const [mpDropOpen, setMpDropOpen] = useState(false);
  const [sortCol, setSortCol] = useState<keyof SaleRow | null>(null);
  const [sortAsc, setSortAsc] = useState(true);
  const [toast, setToast] = useState("");
  const mpRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function h(e: MouseEvent) {
      if (mpRef.current && !mpRef.current.contains(e.target as Node)) setMpDropOpen(false);
    }
    document.addEventListener("mousedown", h);
    return () => document.removeEventListener("mousedown", h);
  }, []);

const filtered = ALL_ROWS.filter((r) => {
  const mpOk = mpFilter === "Todos" || r.marketplace === mpFilter;

  const q = search.toLowerCase();
  const searchOk =
    !q ||
    r.sku.toLowerCase().includes(q) ||
    r.product.toLowerCase().includes(q);

  const dateOk =
    (!dateFrom || r.date >= dateFrom) &&
    (!dateTo || r.date <= dateTo);

  return mpOk && searchOk && dateOk;
});

  const sorted = sortCol
    ? [...filtered].sort((a, b) => {
        const av = a[sortCol];
        const bv = b[sortCol];
        const res = typeof av === "number" ? (av as number) - (bv as number) : String(av).localeCompare(String(bv));
        return sortAsc ? res : -res;
      })
    : filtered;

  const kpis = calcKpis(filtered);
  const chartData = buildChartData(filtered);

  function handleSort(col: keyof SaleRow) {
    if (sortCol === col) setSortAsc((a) => !a);
    else { setSortCol(col); setSortAsc(true); }
  }

  function handleBuscar() {
    setSearch(searchInput);
  }

  function handleExport() {
    const header = tableCols.join(",");
    const rows = sorted.map((r) =>
      [r.date, r.marketplace, r.sku, r.product, r.priceRaw, r.shippingRaw, r.commissionRaw, r.netRaw].join(",")
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
    "Fecha": "date", "Marketplace": "marketplace", "SKU": "sku", "Producto": "product",
    "Precio Venta": "priceRaw", "Costo Envío": "shippingRaw", "Comisión": "commissionRaw", "Ganancia Neta": "netRaw",
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
          <div className="bg-white border-b border-border px-6 py-3 flex items-center gap-3 flex-shrink-0">
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
                  {MARKETPLACES.map((mp) => (
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
              <SecondaryButton label="Actualizar" icon={<RefreshCw size={11} />} onClick={() => setToast("Datos actualizados")} />
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
                  { label: "Ventas Totales", value: fmt(kpis.sales), delta: "+12.4%" },
                  { label: "Ganancia Neta", value: fmt(kpis.net), delta: "+7.8%" },
                  { label: "Costos de Envío", value: fmt(kpis.shipping), delta: "+3.1%" },
                  { label: "Comisiones", value: fmt(kpis.commission), delta: "-1.2%" },
                ].map(({ label, value, delta }) => (
                  <div key={label} className="bg-white border border-border rounded-lg p-4 flex flex-col gap-2">
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">{label}</p>
                    <p className="text-xl font-bold text-foreground" style={{ fontFamily: "JetBrains Mono, monospace" }}>{value}</p>
                    <div className="flex items-center gap-1.5">
                      <div className="h-4 px-1.5 rounded-sm flex items-center bg-muted">
                        <span className="text-[9px] font-mono text-muted-foreground">{delta}</span>
                      </div>
                      <span className="text-[9px] text-muted-foreground">vs mes anterior</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Chart */}
            <div>
              <div className="flex items-center gap-2 mb-3">
                <AnnotationLabel>Ventas por Día</AnnotationLabel>
                <span className="text-[10px] font-mono text-muted-foreground">— {mpFilter === "Todos" ? "todos los marketplaces" : mpFilter}</span>
              </div>
              <div className="bg-white border border-border rounded-lg p-4" style={{ height: 160 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={chartData} margin={{ top: 4, right: 8, left: -10, bottom: 0 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5e5e5" />
                    <XAxis dataKey="date" tick={{ fontSize: 9, fill: "#9ca3af", fontFamily: "JetBrains Mono" }} />
                    <YAxis tick={{ fontSize: 9, fill: "#9ca3af", fontFamily: "JetBrains Mono" }}
                      tickFormatter={(v) => "$" + (v / 1000).toFixed(0) + "k"} />
                    <Tooltip
                      formatter={(v: number) => [fmt(v), "Ventas"]}
                      contentStyle={{ fontSize: 11, fontFamily: "JetBrains Mono", border: "1px solid #e5e5e5", borderRadius: 6 }}
                    />
                    <Bar dataKey="ventas" fill="#1a1a1a" radius={[3, 3, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* Table */}
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-3">
                <AnnotationLabel>Tabla de Reportes</AnnotationLabel>
                <span className="text-[10px] font-mono text-muted-foreground">— {sorted.length} resultados</span>
              </div>
              <div className="bg-white border border-border rounded-lg overflow-hidden">
                <table className="w-full text-xs">
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
                        <td colSpan={8} className="px-4 py-8 text-center text-xs text-muted-foreground font-mono">
                          No se encontraron resultados para la búsqueda aplicada.
                        </td>
                      </tr>
                    ) : sorted.map((row, i) => (
                      <tr key={i} className={`border-b border-border last:border-0 ${i % 2 === 0 ? "bg-white" : "bg-background"} hover:bg-muted/50 transition-colors`}>
                        <td className="px-4 py-2.5 font-mono text-[10px] text-muted-foreground whitespace-nowrap">{row.date}</td>
                        <td className="px-4 py-2.5">
                          <span className="bg-muted text-muted-foreground text-[10px] font-mono px-2 py-0.5 rounded-full">{row.marketplace}</span>
                        </td>
                        <td className="px-4 py-2.5 font-mono text-[10px] text-muted-foreground">{row.sku}</td>
                        <td className="px-4 py-2.5 text-[11px] text-foreground max-w-[180px] truncate">{row.product}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-foreground">{fmt(row.priceRaw)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-muted-foreground">{fmt(row.shippingRaw)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] text-muted-foreground">{fmt(row.commissionRaw)}</td>
                        <td className="px-4 py-2.5 font-mono text-[11px] font-semibold text-foreground">{fmt(row.netRaw)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <div className="px-4 py-2.5 border-t border-border flex items-center justify-between bg-muted/50">
                  <span className="text-[10px] font-mono text-muted-foreground">Mostrando {sorted.length} de {ALL_ROWS.length} registros totales</span>
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

function WF3IngresoData({ goTo }: { goTo: (n: number) => void }) {
  const [activeNav, setActiveNav] = useState("Integraciones");
  const [activeSubNav, setActiveSubNav] = useState("Integraciones API");
  const [selectedMp, setSelectedMp] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [apiSecret, setApiSecret] = useState("");
  const [connStatus, setConnStatus] = useState<ConnStatus>("idle");
  const [toast, setToast] = useState("");
  const [fileName, setFileName] = useState("");
  const [dragging, setDragging] = useState(false);
  const [syncStatuses, setSyncStatuses] = useState([
    { name: "MercadoLibre", status: "connected", lastSync: "hace 8 min" },
    { name: "Walmart", status: "connected", lastSync: "hace 22 min" },
    { name: "Shopify", status: "disconnected", lastSync: "hace 3 días" },
    { name: "Tiendanube", status: "pending", lastSync: "En espera" },
  ]);
  const [commSku, setCommSku] = useState("");
  const [commMp, setCommMp] = useState("");
  const [commPct, setCommPct] = useState("");
  const [overrides, setOverrides] = useState([
    { sku: "MLU-4482", mp: "MercadoLibre", pct: "11.5%" },
    { sku: "WMT-1129", mp: "Walmart", pct: "14.0%" },
  ]);
  const fileRef = useRef<HTMLInputElement>(null);

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

  function handleSaveCommission() {
    if (!commSku || !commMp || !commPct) { setToast("Completa todos los campos"); return; }
    setOverrides((prev) => [...prev.filter((o) => !(o.sku === commSku && o.mp === commMp)), { sku: commSku, mp: commMp, pct: commPct + "%" }]);
    setCommSku(""); setCommMp(""); setCommPct("");
    setToast("Ajuste de comisión guardado");
  }

  function handleFileDrop(e: React.DragEvent) {
    e.preventDefault(); setDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) setFileName(file.name);
  }

  const subNavItems = ["Carga CSV / XLSX", "Integraciones API", "Ajuste Comisiones"];

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

            {/* Section 1: CSV Upload */}
            <SectionCard title="Carga de Archivo CSV / XLSX" annotation="S1 · Importación Masiva">
              <div className="flex flex-col gap-4">
                <div
                  onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
                  onDragLeave={() => setDragging(false)}
                  onDrop={handleFileDrop}
                  className={`border-2 border-dashed rounded-lg p-8 flex flex-col items-center gap-3 transition-colors cursor-pointer ${dragging ? "border-foreground bg-muted" : "border-border bg-background"}`}
                  onClick={() => fileRef.current?.click()}
                >
                  <input ref={fileRef} type="file" accept=".csv,.xlsx" className="hidden"
                    onChange={(e) => { if (e.target.files?.[0]) setFileName(e.target.files[0].name); }} />
                  <Upload size={28} className="text-muted-foreground/50" />
                  <div className="text-center">
                    <p className="text-sm font-medium text-foreground">Arrastra y suelta tu archivo aquí</p>
                    <p className="text-[11px] text-muted-foreground mt-0.5">Soporta: .CSV, .XLSX — máx. 25 MB</p>
                  </div>
                  <span className="text-[10px] font-mono text-muted-foreground">o haz clic para seleccionar</span>
                </div>
                <div className="flex items-center gap-3">
                  <div className="flex-1 h-8 bg-input-background border border-border rounded flex items-center px-3">
                    <span className="text-xs text-muted-foreground font-mono truncate">{fileName || "Ningún archivo seleccionado"}</span>
                  </div>
                  <PrimaryButton label="Subir archivo" onClick={() => {
                    if (!fileName) { setToast("Selecciona un archivo primero"); return; }
                    setToast(`Archivo "${fileName}" cargado correctamente`);
                  }} />
                  <SecondaryButton label="Ver plantilla" onClick={() => setToast("Descargando plantilla…")} />
                </div>
                <div className="bg-muted/50 border border-border rounded p-3">
                  <p className="text-[10px] font-mono text-muted-foreground">Columnas esperadas: Fecha · Marketplace · SKU · Producto · Precio Venta · Costo Envío · Comisión</p>
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
                    <SecondaryButton label="Verificar" onClick={() => {
                      if (!selectedMp) { setToast("Selecciona un marketplace"); return; }
                      setToast(`Conexión con ${selectedMp} verificada`);
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
                    const isConnected = syncStatuses.find((s) => s.name === mp)?.status === "connected";
                    return (
                      <button key={mp} onClick={() => { setSelectedMp(mp); setConnStatus("idle"); }}
                        className={`flex items-center gap-2 px-3 py-2 border rounded text-left transition-colors ${selectedMp === mp ? "border-foreground bg-muted" : "border-border bg-background hover:bg-muted/50"}`}>
                        <div className="w-5 h-5 bg-muted rounded-sm border border-border flex-shrink-0" />
                        <span className="text-xs text-foreground flex-1">{mp}</span>
                        {isConnected && <CheckCircle2 size={11} className="text-muted-foreground" />}
                      </button>
                    );
                  })}
                </div>
              </div>
            </SectionCard>

            {/* Section 3: Commission override */}
            <SectionCard title="Ajuste Manual de Comisiones" annotation="S3 · Override por SKU">
              <div className="grid grid-cols-3 gap-4">
                <InputField label="SKU / Producto" placeholder="Ej: MLU-4482" value={commSku} onChange={setCommSku} />
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-medium text-foreground">Marketplace</label>
                  <Dropdown options={MARKETPLACE_LIST} value={commMp} onChange={setCommMp} placeholder="Seleccionar…" />
                </div>
                <InputField label="% Comisión" placeholder="12.00" value={commPct} onChange={setCommPct} />
              </div>
              <div className="mt-4 flex items-center gap-3">
                <PrimaryButton label="Guardar ajuste" onClick={handleSaveCommission} />
                <SecondaryButton label="Limpiar campos" onClick={() => { setCommSku(""); setCommMp(""); setCommPct(""); }} />
                <span className="text-[10px] font-mono text-muted-foreground ml-2">⚠ Afecta únicamente al SKU especificado</span>
              </div>
              <div className="mt-4 border border-border rounded overflow-hidden">
                <div className="bg-muted px-3 py-2 border-b border-border">
                  <p className="text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">Ajustes activos ({overrides.length})</p>
                </div>
                {overrides.length === 0 ? (
                  <p className="px-4 py-4 text-[11px] font-mono text-muted-foreground text-center">Sin ajustes activos</p>
                ) : overrides.map((row) => (
                  <div key={row.sku + row.mp} className="flex items-center px-3 py-2 border-b border-border last:border-0 text-[11px]">
                    <span className="font-mono text-muted-foreground w-28">{row.sku}</span>
                    <span className="text-foreground flex-1">{row.mp}</span>
                    <span className="font-mono text-foreground font-medium">{row.pct}</span>
                    <button onClick={() => setOverrides((p) => p.filter((o) => !(o.sku === row.sku && o.mp === row.mp)))}
                      className="ml-4 text-[10px] text-muted-foreground underline hover:text-foreground">Eliminar</button>
                  </div>
                ))}
              </div>
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
  return (
    <div className="min-h-screen bg-background" style={{ fontFamily: "Inter, sans-serif" }}>
      <WireframeNav activeScreen={screen} onSelect={setScreen} />
      <div className="pt-11">
        {screen === 0 && <WF1Login onLogin={() => setScreen(1)} />}
        {screen === 1 && <WF2Reportes goTo={setScreen} />}
        {screen === 2 && <WF3IngresoData goTo={setScreen} />}
      </div>
    </div>
  );
}
