Levantar el backend y repoblar los datos

Instructivo para dejar el backend andando con datos desde cero, en Windows. Son pasos manuales: se corren uno por uno y se entiende qué hace cada uno. Probado en junio de 2026.

Al final el backend queda en http://localhost:8080 con sus ~420 ventas, productos y costos cargados.

1. Instalar Java 21 y PostgreSQL 17

En una terminal PowerShell (puede pedir permiso de administrador, acéptalo):

    winget install --id Microsoft.OpenJDK.21 --silent --accept-package-agreements --accept-source-agreements
    winget install --id PostgreSQL.PostgreSQL.17 --silent --accept-package-agreements --accept-source-agreements --override "--mode unattended --superpassword postgres --serverport 5432"

El JDK queda en `C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot` (ojo: el instalador no lo agrega al PATH). PostgreSQL queda como servicio que arranca solo con Windows, usuario `postgres`, contraseña `postgres`.

2. Crear la base D&K

    $env:PGPASSWORD = "postgres"
    & "C:\Program Files\PostgreSQL\17\bin\createdb.exe" -U postgres "D&K"

(Las tablas no se crean acá: las crea Flyway solo cuando arranca el backend.)

3. Configurar el .env

Desde la carpeta `DK-Backend`, copia la plantilla y edítala:

    copy .env.example .env

En el `.env`, descomenta y completa:

- `DB_PASSWORD=postgres`
- `JWT_SECRET=` con un texto de al menos 32 caracteres (si queda vacío, el login falla).
- `FALABELLA_USER_ID`, `FALABELLA_API_KEY`, `FALABELLA_SELLER_ID` con las credenciales del Seller Center del equipo.

4. Arrancar el backend

    cd DK-Backend
    $env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
    .\gradlew.bat bootRun

Espera a ver `Started RentabilidadApplication` y `Tomcat started on port 8080`. Deja esa terminal abierta (el servidor corre ahí). Para detenerlo, `Ctrl + C`.

En este punto Flyway ya creó las tablas, los 8 canales y 3 usuarios seed (login `kevin@dk.cl` / `changeme`). Falta cargar productos, ventas y costos.

5. Repoblar los datos

Abre otra terminal (la del backend queda corriendo) y párate en la raíz del repo. Prepara las variables una vez:

    $env:PGPASSWORD = "postgres"
    $env:PGCLIENTENCODING = "UTF8"
    $psql = "C:\Program Files\PostgreSQL\17\bin\psql.exe"

Corre los cuatro pasos en este orden (importa: las ventas necesitan los productos, y los costos necesitan las ventas):

a) Productos (catálogo Falabella + costo Bsale, ~769 filas):

    & $psql -U postgres -d "D&K" -f DK-Backend\datos\seed-productos.sql

b) Ventas (en vivo desde la API de Falabella; tarda varios minutos por las pausas anti rate-limit):

    Invoke-RestMethod -Uri "http://localhost:8080/api/dev/falabella/sync?desde=2025-11-01" -Method Post

c) Costos reales (comisión y logística por unidad, desde el estado de cuenta):

    & $psql -U postgres -d "D&K" -f DK-Backend\datos\seed-costos.sql

d) Tarifas de comisión estimadas por categoría (respaldo para las ventas que aún no aparecen en el estado de cuenta; sin esto quedan con costo operacional en $0 y margen inflado):

    & $psql -U postgres -d "D&K" -f DK-Backend\datos\seed-costo-canal.sql

e) Recalcular la rentabilidad:

    Invoke-RestMethod -Uri "http://localhost:8080/api/dev/rentabilidad/recalcular" -Method Post

6. Obtener el reporte Excel

    $login = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body '{"email":"kevin@dk.cl","password":"changeme"}'
    Invoke-WebRequest -Uri "http://localhost:8080/api/reporte/excel?desde=2025-11-01&hasta=2026-06-30" -Headers @{ Authorization = "Bearer $($login.token)" } -OutFile "$env:USERPROFILE\Desktop\DK-Reporte.xlsx"

Queda en el Escritorio. (Swagger en `/swagger-ui.html` sale en blanco por la política de seguridad CSP del backend; por eso el reporte se baja con estos comandos.)

Notas

-Las próximas veces no hay que instalar ni repoblar nada**: con la base ya cargada, basta el paso 4 para levantar el backend.
- Los archivos de datos viven en `DK-Backend/datos/`. Si llega un estado de cuenta nuevo, se reemplaza el CSV correspondiente y se vuelve a generar `seed-costos.sql` (el cruce es por "Id Artículo" = `referencia_externa`, por unidad, nunca por orden+SKU). Luego se repiten los pasos 5c y 5d.
- Los datos de `datos/` son información comercial de D&K. El repo es privado del equipo; aun así, no compartir esos archivos fuera del equipo.

## Solución de problemas

### El backend no arranca: "Migration checksum mismatch for migration version 5"

En el log aparece algo como:

    Migration checksum mismatch for migration version 5
    Applied to database : -1091040492
    Resolved locally    : 1919015585

Pasa cuando tu base ya tenía aplicada una versión anterior de la migración V5 y después el archivo cambió (el commit que "corrige V5 para bases legacy"). Flyway ve que el checksum no calza y se niega a arrancar. A quien clona el repo de cero **no le pasa** (la V5 se aplica nueva); solo afecta a bases creadas antes de ese cambio.

Dos formas de resolverlo:

**Opción A — conservar los datos (repair).** Alinea el checksum en el historial de Flyway, sin tocar el esquema ni los datos. Copia del error el número que dice "Resolved locally" y reemplázalo abajo por `<RESOLVED>`:

    $env:PGPASSWORD = "postgres"
    & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d "D&K" -c "UPDATE flyway_schema_history SET checksum = <RESOLVED> WHERE version = '5';"

Luego arranca el backend (paso 4); aplicará las migraciones pendientes y levantará normal.

**Opción B — empezar de cero (más simple si no tienes datos que conservar).** Borra y recrea la base; las migraciones se aplican limpias:

    $env:PGPASSWORD = "postgres"
    & "C:\Program Files\PostgreSQL\17\bin\dropdb.exe" -U postgres "D&K"
    & "C:\Program Files\PostgreSQL\17\bin\createdb.exe" -U postgres "D&K"

Después repite desde el paso 4 (arrancar) y el paso 5 (repoblar).

La causa de fondo es que se editó una migración ya publicada, en vez de agregar una nueva. Para el futuro conviene no tocar migraciones ya aplicadas: si hace falta un ajuste, se agrega una migración nueva (V7, V8…).
