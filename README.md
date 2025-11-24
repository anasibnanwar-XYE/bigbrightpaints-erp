# BigBright ERP – Backend

## Prerequisites
- Java 21
- Maven 3.9+
- PostgreSQL (e.g., `bigbright_erp` DB)
- Docker (for Testcontainers/integration tests)

## Run locally
```bash
cd erp-domain
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bigbright_erp
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=Anas@2627
set SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```
Adjust the datasource/user/password to your environment. The `dev` profile now seeds critical fixtures (periods, GL accounts, dealer/supplier, FG stock) at startup.

## Flyway note
There was a gap at V42; a placeholder (`V42__placeholder.sql`) was added. If your database ran higher migrations already, run once with out-of-order to record it:
```bash
mvn -f erp-domain/pom.xml -DskipTests ^
  "-Dflyway.url=jdbc:postgresql://localhost:5432/bigbright_erp" ^
  "-Dflyway.user=postgres" "-Dflyway.password=Anas@2627" ^
  "-Dflyway.outOfOrder=true" flyway:migrate
```

## Build without tests
```bash
mvn -q -f erp-domain/pom.xml -DskipTests package
```

## Cloud/Testcontainers
Use `scripts/run-tests-cloud.sh` to run the full suite in a Docker-in-Docker harness (see `docs/CLOUD_CONTAINERS.md`).
