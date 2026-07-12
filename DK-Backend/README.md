# DK-Backend — API de rentabilidad D&K

API REST en **Spring Boot 3** + **PostgreSQL** para el sistema de análisis de rentabilidad por producto y canal de venta.

## Requisitos

| Herramienta | Versión |
|-------------|---------|
| Java | 21 |
| PostgreSQL | 14+ |
| Gradle | incluido (`./gradlew`) |

## Inicio rápido

### 1. Base de datos

Crear la base de datos en PostgreSQL (nombre por defecto: `D&K`):

```sql
CREATE DATABASE "D&K";
```

Flyway aplica automáticamente las migraciones al arrancar la aplicación.

En desarrollo, copia `application-example.yml` → `application-local.yml` con tu usuario y contraseña de PostgreSQL (el archivo está en `.gitignore`).

### 2. Configuración

**Desarrollo** (perfil `dev`, activo por defecto):

```bash
export SPRING_PROFILES_ACTIVE=dev
./gradlew bootRun
```

**Producción** — usar variables de entorno (ver `application-example.yml`):

```bash
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://host:5432/D%26K
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=<secreto-fuerte>
export JWT_SECRET=<minimo-32-caracteres-aleatorios>
export CORS_ALLOWED_ORIGINS=https://app.tuempresa.cl
```

| Variable | Obligatoria en prod | Descripción |
|----------|---------------------|-------------|
| `JWT_SECRET` | Sí | Clave HMAC para firmar tokens (≥ 32 chars) |
| `DATABASE_*` | Sí | Conexión PostgreSQL |
| `CORS_ALLOWED_ORIGINS` | Sí | Orígenes del frontend (separados por coma) |
| `LOGIN_MAX_ATTEMPTS` | No | Intentos antes de bloqueo (default: 5) |
| `LOGIN_LOCKOUT_MINUTES` | No | Minutos de bloqueo (default: 15) |

Usuarios seed (contraseña temporal `changeme` — **cambiar antes de producción**):

| Email | Nombre |
|-------|--------|
| kevin@dk.cl | Kevin Jensen |
| daniel@dk.cl | Daniel Cuevas |
| arnely@dk.cl | Arnely Colmenarez |

### 3. Ejecutar

```bash
./gradlew bootRun
```

La API queda en `http://localhost:8080`.

## Documentación de la API

| Recurso | URL |
|---------|-----|
| Swagger UI (interactivo) | http://localhost:8080/swagger-ui.html |
| Especificación OpenAPI JSON | http://localhost:8080/v3/api-docs |

En Swagger UI: primero autenticarse con `POST /api/auth/login`, copiar el token y usar el botón **Authorize** con `Bearer {token}`.

## Autenticación

La API es **stateless** y usa **JWT**.

```http
POST /api/auth/login
Content-Type: application/json

{"email": "kevin@dk.cl", "password": "changeme"}
```

Respuesta:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "kevin@dk.cl",
  "nombre": "Kevin Jensen"
}
```

Peticiones protegidas:

```http
Authorization: Bearer {token}
```

## Arquitectura

```
controller/   → Endpoints REST (capa HTTP)
service/      → Lógica de negocio y transacciones
repository/   → Acceso a datos (Spring Data JPA)
entity/       → Modelo persistente (tablas PostgreSQL)
dto/          → Objetos de entrada/salida específicos
config/       → Seguridad JWT, OpenAPI
exception/    → Excepciones de dominio y manejo global de errores
```

Flujo principal al registrar una venta:

1. Se persiste la venta en `venta`.
2. `RentabilidadService` calcula margen usando `producto.costo_base` y costos vigentes del canal.
3. Se guarda el resultado en `rentabilidad`.

## Endpoints principales

### Autenticación

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/auth/login` | Obtener JWT |
| PUT | `/api/auth/password` | Cambiar contraseña (requiere JWT) |

**Cambio de contraseña** — cuerpo de la petición:

```json
{
  "currentPassword": "changeme",
  "newPassword": "NuevaClave2026",
  "confirmNewPassword": "NuevaClave2026"
}
```

Requisitos de la nueva contraseña: 8–128 caracteres, al menos una mayúscula, una minúscula y un número.

### Productos

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/productos` | Listar activos |
| GET | `/api/productos/todos` | Listar todos (incluye inactivos) |
| GET | `/api/productos/{id}` | Obtener por UUID |
| GET | `/api/productos/sku/{sku}` | Obtener por SKU |
| POST | `/api/productos` | Crear producto |
| PUT | `/api/productos/{id}` | Actualizar producto |
| DELETE | `/api/productos/{id}` | Desactivar (borrado lógico) |

### Canales de venta

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/canales` | Listar todos |
| GET | `/api/canales/activos` | Listar activos |
| GET | `/api/canales/{id}` | Obtener por UUID |
| POST | `/api/canales` | Crear canal |
| PUT | `/api/canales/{id}` | Actualizar canal |
| DELETE | `/api/canales/{id}` | Desactivar canal |

### Costos por canal

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/canales/{canalId}/costos` | Listar costos del canal |
| GET | `/api/canales/{canalId}/costos/{costoId}` | Obtener un costo |
| POST | `/api/canales/{canalId}/costos` | Agregar costo |
| PUT | `/api/canales/{canalId}/costos/{costoId}` | Actualizar costo |
| DELETE | `/api/canales/{canalId}/costos/{costoId}` | Eliminar costo |

Tipos de costo (`tipoCosto`): `COMISION_PORCENTAJE`, `COSTO_ENVIO_FIJO`, `COSTO_ENVIO_PORCENTAJE`, `COSTO_LOGISTICO`, `PUBLICIDAD`, `OTRO`.

### Ventas

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/ventas` | Registrar venta (+ cálculo de rentabilidad) |
| GET | `/api/ventas/{id}` | Obtener venta |
| PUT | `/api/ventas/{id}` | Actualizar venta (recalcula rentabilidad) |
| DELETE | `/api/ventas/{id}` | Eliminar venta y su rentabilidad |
| GET | `/api/ventas?desde=&hasta=` | Filtrar por rango de fechas |
| GET | `/api/ventas/canal/{canalId}` | Ventas de un canal |

### Reportes

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/reporte/excel?desde=&hasta=` | Descargar reporte Excel (.xlsx) |

## Modelo de datos (resumen)

| Tabla | Propósito |
|-------|-----------|
| `producto` | Catálogo: SKU único, nombre, `costo_base` |
| `canal_venta` | Marketplaces y tiendas (MercadoLibre, Walmart, etc.) |
| `costo_canal` | Comisiones, envíos y costos operacionales por canal |
| `venta` | Una fila = una unidad vendida |
| `rentabilidad` | Margen calculado por venta |

El **SKU es único a nivel empresa** y se reutiliza en todos los canales.

## Seguridad

Medidas implementadas para entorno empresarial:

| Medida | Detalle |
|--------|---------|
| **JWT stateless** | Tokens firmados HMAC-SHA256, expiración configurable (8 h por defecto) |
| **BCrypt cost 12** | Hash de contraseñas con factor de trabajo elevado |
| **Anti fuerza bruta** | Bloqueo temporal tras 5 intentos fallidos de login |
| **CORS restrictivo** | Solo orígenes explícitos en `CORS_ALLOWED_ORIGINS` |
| **Headers HTTP** | HSTS, X-Content-Type-Options, X-Frame-Options, CSP, Referrer-Policy |
| **Sin fugas de errores** | Errores internos retornan mensaje genérico; detalle solo en logs |
| **Auditoría de login** | Accesos exitosos en tabla `log_acceso` con IP |
| **Secretos externos** | JWT y BD vía variables de entorno en producción |
| **Swagger solo en dev** | Documentación interactiva deshabilitada en perfil `prod` |
| **open-in-view off** | Previene consultas lazy fuera de transacción |

### Checklist antes de producción

- [ ] Cambiar contraseñas de usuarios seed (`changeme`)
- [ ] Generar `JWT_SECRET` aleatorio (≥ 32 caracteres)
- [ ] Configurar `CORS_ALLOWED_ORIGINS` con el dominio real del frontend
- [ ] Usar HTTPS detrás de un reverse proxy (Nginx / ALB)
- [ ] Crear usuario de BD dedicado con permisos mínimos (no `postgres`)
- [ ] Activar `SPRING_PROFILES_ACTIVE=prod`

## Códigos de error HTTP

| Código | Cuándo |
|--------|--------|
| 400 | Datos inválidos o validación fallida |
| 401 | Token ausente, inválido o credenciales incorrectas |
| 403 | Acceso denegado |
| 404 | Recurso no encontrado |
| 409 | Conflicto (SKU o nombre de canal duplicado) |
| 500 | Error interno (sin detalle expuesto) |

Formato de respuesta de error:

```json
{"error": "Mensaje descriptivo"}
```

## Comandos útiles

```bash
# Compilar
./gradlew compileJava

# Tests
./gradlew test

# JAR ejecutable
./gradlew bootJar
```

## Integraciones externas

### Arquitectura del equipo

```
                    ┌─────────────────────────────────────┐
                    │         D&K Integrador API          │
                    │         (PostgreSQL)                │
                    └─────────────────────────────────────┘
                           ▲           ▲           ▲
              SKU + costo  │           │           │  ventas + costos canal
                           │           │           │
                    ┌──────┴───┐ ┌─────┴────┐ ┌────┴──────┐
                    │  BSALE   │ │ Mercado  │ │ Falabella │
                    │ (Ximena) │ │Libre(Juan)│ │(Vladimir) │
                    └──────────┘ └──────────┘ └───────────┘
```

| Fuente | Responsable | Qué aporta | Tabla destino |
|--------|-------------|------------|---------------|
| **Bsale** | Ximena | SKU, estado, marca, tipo, stock, costo promedio | `producto` |
| **MercadoLibre** | Juan | Ventas, precio, comisiones, envíos | `venta`, `costo_canal` |
| **Falabella** | Vladimir | Ventas, precio, comisiones, envíos | `venta`, `costo_canal` |

**Bsale es la fuente maestra del catálogo.** Los marketplaces comparten el mismo SKU definido en Bsale.

### Integración Bsale — carga manual (recomendada)

Sin token de API, exporta desde el panel Bsale y sube los archivos:

| Archivo en Bsale | Módulo | Campos que importa |
|------------------|--------|-------------------|
| Productos y servicios | Productos → exportar | SKU, nombre, estado, marca, tipo de producto |
| Stock actual | Stock → exportar | nombre/SKU, stock total, costo unitario promedio |

```http
POST /api/integraciones/bsale/import
Content-Type: multipart/form-data
Authorization: Bearer {token}

productos: [archivo.xlsx]   (opcional)
stock:     [archivo.xlsx]   (opcional)
```

Consultar última importación: `GET /api/integraciones/bsale/import/ultima`

**Nota:** las listas de precio por marketplace (Falabella, MercadoLibre, etc.) se gestionan en Bsale pero **no se importan aún** en esta versión.

### Integración Bsale — API (cuando haya token)

1. Obtener token en Bsale → ayuda@bsale.app o panel de la empresa.
2. Configurar variables:

```bash
export BSALE_ENABLED=true
export BSALE_ACCESS_TOKEN=tu_token_bsale
```

3. Ejecutar sincronización:

```http
POST /api/integraciones/bsale/sync
Authorization: Bearer {token}
```

4. Consultar última ejecución: `GET /api/integraciones/bsale/sync/ultima`

**Qué hace la sync API:**
- Lee variantes activas (`GET /v1/variants.json?state=0`)
- Por cada variante: SKU → `producto.sku`, costo promedio → `producto.costo_base`
- Upsert por `bsale_variant_id` o SKU
- Registra resultado en `integracion_sync_log`

### Próximas fases

| Fase | Integración | Estado |
|------|-------------|--------|
| 2 | Bsale listas de precio por canal | Pendiente (manual en Bsale por ahora) |
| 3 | MercadoLibre (Juan) | Pendiente |
| 4 | Falabella ventas (Vladimir) | Cliente API listo; sync ventas pendiente |

## Próximos pasos planificados

- Importación masiva CSV/XLSX genérica (`ImportService`)
- Listas de precio Bsale por marketplace (fase futura, sin integrar aún)
- Integración APIs de marketplaces (ventas)
- DTOs de respuesta para evitar exponer entidades JPA directamente
