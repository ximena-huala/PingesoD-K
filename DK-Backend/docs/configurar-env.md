# Configurar el `.env` (para nuevos integrantes)

El backend lee sus credenciales de un archivo `.env` en `DK-Backend/`. Ese archivo
no está en el repo (está en `.gitignore`), así que la primera vez cada uno se crea el
suyo. Acá va cómo.

## 1. Crear el archivo

Desde la carpeta `DK-Backend/`, copia la plantilla con `cp .env.example .env`. Eso te
deja un archivo con todo comentado; solo hay que rellenar las 3 variables de Falabella.

## 2. Sacar las credenciales de Falabella

Entra a Seller Center con la cuenta de administrador que el cliente creó para el
equipo y copia:

- `FALABELLA_USER_ID` y `FALABELLA_API_KEY` → en https://sellercenter.falabella.com/api-explorer
- `FALABELLA_SELLER_ID` → en https://sellercenter.falabella.com/user/profile/account, campo **"ID del Vendedor"**

Pega cada uno en tu `.env`. La API Key no viaja en las llamadas (solo firma
localmente), pero igual es un secreto: no la pegues en el repo, en un issue ni en el
chat del grupo.

## 3. (Opcional) PostgreSQL y JWT

Si tu Postgres local usa un usuario o clave distintos a `postgres`/`postgres`,
descomenta y ajusta `DB_USERNAME` / `DB_PASSWORD` en el `.env`. Si no, déjalo como
está. El `JWT_SECRET` igual: el default alcanza para desarrollo.

## 4. Verificar que conecta

Sin necesidad de levantar la base de datos, corre desde `DK-Backend/` el comando
`./gradlew test --tests "*FalabellaConnectivitySmokeTest*"`. Si termina en
`BUILD SUCCESSFUL`, tu backend ya habla con Falabella. Si sale **skipped**, es que el
`.env` está vacío o con algún valor mal escrito (el test se salta solo cuando no
encuentra credenciales).

## Recordatorios

- El `.env` es tuyo y local: no se sube al repo nunca (ya está ignorado).
- Si la API Key se filtra por error, se puede regenerar en Seller Center — avisa al
  equipo si pasa.
- La integración es solo lectura (el cliente Java solo tiene métodos `Get*`), así que
  no toca datos reales aunque la cuenta sea admin.
