# Pingeso D&K — Integrador de rentabilidad

Monorepo del proyecto **D&K Integrador**.

## Estructura

```
└── DK-Backend/   # API Spring Boot (Java 21 + PostgreSQL)
```

## DK-Backend

```bash
cd DK-Backend
./gradlew bootRun
```

API en `http://localhost:8080`. Requiere PostgreSQL según `DK-Backend/src/main/resources/application.yml`.
