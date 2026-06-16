# Pingeso D&K — Integrador de rentabilidad

Monorepo del proyecto **D&K Integrador**. Tiene dos partes:

- `DK-Backend/` — API en Spring Boot (Java 21 + PostgreSQL)
- `DK-Frontend/` — SPA en React + Vite (UI generada desde Figma)

## Qué necesitas tener instalado

- Java (JDK) 21
- PostgreSQL 14 o superior
- Node.js 20 o superior

## Cómo levantarlo

### 1. Base de datos (solo la primera vez)

El backend espera una base de datos llamada `D&K`. Créala con un usuario superusuario
(la migración inicial usa la extensión `pgcrypto`) corriendo `createdb -U postgres "D&K"`.

Las tablas y los datos iniciales los crea Flyway solo al arrancar el backend (están en
`DK-Backend/src/main/resources/db/migration/V1__initial_schema.sql`).

### 2. Backend

Desde `DK-Backend/`, corre `./gradlew bootRun` (en Windows, `.\gradlew.bat bootRun`).
Queda escuchando en `http://localhost:8080`.

Las credenciales (base de datos, JWT) salen de variables de entorno con defaults
pensados para desarrollo local, así que normalmente no tienes que tocar nada. Si tu
PostgreSQL usa otra contraseña, defínela antes de arrancar: en PowerShell con
`$env:DB_PASSWORD = "tu-contraseña"` y luego `.\gradlew.bat bootRun`; en Linux o macOS
con `DB_PASSWORD="tu-contraseña" ./gradlew bootRun`.

Las variables que puedes sobrescribir (todas con su default en `application.yml`):

- `DB_URL` — URL JDBC. Por defecto `jdbc:postgresql://localhost:5432/D%26K`; ese `%26`
  es un `&` escapado, por el nombre "D&K".
- `DB_USERNAME` / `DB_PASSWORD` — usuario y clave de Postgres (`postgres` / `postgres`).
- `JWT_SECRET` — clave para firmar los JWT, mínimo 32 caracteres.
- `SERVER_PORT` — puerto de la API (`8080`).

Para las credenciales de la integración con Falabella, mira
[`DK-Backend/docs/configurar-env.md`](DK-Backend/docs/configurar-env.md).

### 3. Frontend

Desde `DK-Frontend/`, corre `npm install` y luego `npm run dev`. Queda en
`http://localhost:5173` (Vite). El diseño original está en
[Figma](https://www.figma.com/design/1ODzwzwUp3txSNWBXfoU63/D-K).
