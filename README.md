# Pingeso D&K — Integrador de rentabilidad

Monorepo del proyecto **D&K Integrador**:

```
├── DK-Backend/    # API Spring Boot (Java 21 + PostgreSQL)
└── DK-Frontend/   # UI React (Vite)
```

## Documentación

| Módulo | Descripción |
|--------|-------------|
| [DK-Backend/README.md](DK-Backend/README.md) | Instalación, arquitectura, endpoints y Swagger |
| [DK-Backend/docs/configurar-env.md](DK-Backend/docs/configurar-env.md) | Credenciales Falabella (`.env`) |
| Swagger UI (con backend corriendo) | http://localhost:8080/swagger-ui.html |

## Requisitos

- Java (JDK) 21
- PostgreSQL 14+
- Node.js 20+ (solo frontend)

## Inicio rápido — Backend

### 1. Base de datos

```sql
CREATE DATABASE "D&K";
```

Flyway aplica las migraciones al arrancar.

### 2. Configuración local

Copia `DK-Backend/.env.example` → `DK-Backend/.env` y ajusta credenciales, o usa `application-local.yml` (ver `DK-Backend/README.md`).

Variables de BD (acepta `DATABASE_*` o `DB_*`):

| Variable | Default |
|----------|---------|
| `DATABASE_URL` / `DB_URL` | `jdbc:postgresql://localhost:5432/D%26K` |
| `DATABASE_USERNAME` / `DB_USERNAME` | `postgres` |
| `DATABASE_PASSWORD` / `DB_PASSWORD` | (vacío) |

### 3. Ejecutar

```bash
cd DK-Backend
./gradlew bootRun
```

API en `http://localhost:8080`.

## Inicio rápido — Frontend

```bash
cd DK-Frontend
npm install
npm run dev
```

UI en `http://localhost:5173`.

## Integraciones

| Canal | Responsable | Estado |
|-------|-------------|--------|
| Bsale (catálogo maestro) | Ximena | Sync de productos + costo base |
| Falabella (ventas) | Vladimir | Cliente API + endpoints dev |
| MercadoLibre | Juan | Pendiente |
