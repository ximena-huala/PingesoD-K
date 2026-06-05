# Pingeso D&K — Integrador de rentabilidad

Monorepo del proyecto **D&K Integrador**.

## Estructura

```
├── backend/    # API Spring Boot (Java 21 + PostgreSQL)
└── frontend/   # React (próximamente)
```

## Backend

```bash
cd backend
./gradlew bootRun
```

API en `http://localhost:8080`. Requiere PostgreSQL según `backend/src/main/resources/application.yml`.
