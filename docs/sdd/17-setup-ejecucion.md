# 17 - Guía de Configuración y Ejecución

> **Nota:** Esta es la guía definitiva para configurar y ejecutar el proyecto Pokémon TCG en entorno local. Aplica para todas las semanas del ciclo de desarrollo (semanas 1-6).

---

## 1. Requisitos Previos

Asegúrese de tener instalado en su sistema:

| Herramienta | Versión mínima | Verificación |
|---|---|---|
| **Java JDK** | 21 (Temurin / Eclipse Adoptium) | `java -version` |
| **Maven** | 3.9+ | `mvn --version` |
| **Node.js** | 18+ | `node --version` |
| **npm** | 10+ (incluido con Node.js) | `npm --version` |
| **Git** | 2.40+ | `git --version` |
| **PostgreSQL** | 16 | `psql --version` |

> **Nota:** Docker **no** es obligatorio. Los tests usan Embedded PostgreSQL (zonky) que descarga y ejecuta binarios nativos de PostgreSQL automáticamente sin necesidad de contenedores.

---

## 2. Clonación y Rama de Trabajo

```bash
git clone <url-del-repositorio>
cd tpi-pokemon-2w1-12
git checkout develop
```

> **Regla:** Todo el trabajo de desarrollo se realiza sobre ramas que nacen de `develop`. Nunca trabaje directamente sobre `main` ni `develop`.

---

## 3. Variables de Entorno

El sistema utiliza variables de entorno inyectadas en `application.properties` con valores por defecto para desarrollo.

| Variable | Valor por defecto | Obligatoria | Descripción |
|---|---|---|---|
| `POKEMON_TCG_API_KEY` | `307c341f-3f91-4f04-b234-e5f803c66597` | No (dev) | API key de pokemontcg.io para sincronización de cartas |
| `JWT_SECRET` | `default-secret-key-change-in-production...` | No (dev) | Clave de firma JWT para autenticación |

### Cómo configurar

**Opción A — Variables del sistema operativo:**

```bash
# Windows (PowerShell)
$env:POKEMON_TCG_API_KEY="tu-api-key"
$env:JWT_SECRET="tu-secret-key"

# Linux / macOS
export POKEMON_TCG_API_KEY="tu-api-key"
export JWT_SECRET="tu-secret-key"
```

**Opción B — Configuración en el IDE:**

- **IntelliJ IDEA:** Run Configuration → Environment variables
- **VS Code:** `launch.json` → `env`
- **Eclipse:** Run Configuration → Environment

> **Importante:** Los valores por defecto son funcionales para desarrollo local. No los use en producción.

---

## 4. Estrategia de Base de Datos

El proyecto utiliza **PostgreSQL 16** como base de datos única en todos los entornos.

### 4.1. Instalación de PostgreSQL 16

**Windows:**
1. Descargar el instalador desde https://www.postgresql.org/download/windows/
2. Ejecutar el instalador. Configurar:
   - Puerto: `5432` (por defecto)
   - Usuario: `postgres`
   - Contraseña: `postgres`
3. Al finalizar, abrir **pgAdmin** o **psql** y crear la base de datos:
   ```sql
   CREATE DATABASE pokemon_tcg;
   ```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install postgresql-16
sudo -u postgres psql -c "CREATE DATABASE pokemon_tcg;"
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'postgres';"
```

### 4.2. Perfiles de Base de Datos

| Perfil | Motor | `ddl-auto` | Flyway | Uso |
|---|---|---|---|---|
| `dev` (por defecto) | PostgreSQL 16 (localhost) | `validate` | Habilitado | Desarrollo local |
| `test` | Embedded PostgreSQL (zonky) | `validate` | Habilitado | Tests automatizados |

> **Embedded PostgreSQL (tests):** La dependencia `io.zonky.test:embedded-database-spring-test` descarga binarios nativos de PostgreSQL 16 y los ejecuta como subproceso JVM durante los tests. **No requiere instalación manual ni Docker.**

### 4.3. Flyway + Hibernate

- **Flyway** ejecuta las migraciones versionadas (`V1__` a `V14__`)
- **Hibernate** con `ddl-auto=validate` verifica que el schema de la DB coincida exactamente con las entidades JPA
- **NO usar `defer-datasource-initialization=true`** — Genera dependencia circular entre `flyway` y `entityManagerFactory`

### 4.4. Cuándo reiniciar la base de datos

Elimine y recree la base de datos PostgreSQL cuando:

1. Se agrega una nueva migración Flyway que modifica tablas existentes
2. El schema cambia de forma incompatible con los datos actuales
3. Se necesita un estado limpio para pruebas

```bash
# Windows / Linux / macOS — psql
psql -U postgres -c "DROP DATABASE pokemon_tcg;"
psql -U postgres -c "CREATE DATABASE pokemon_tcg;"
```

> **Nota:** Al recrear la DB, el `DataInitializer` volverá a sincronizar las cartas del set XY1 automáticamente al iniciar el backend.

### 4.5. Cache Local (Caffeine)

El catálogo de cartas utiliza un sistema de cache en memoria con patrón **Cache-Aside**:

- **Motor:** Caffeine (integrado con Spring Cache)
- **TTL:** 14 días (las entradas expiran automáticamente)
- **Tamaño máximo:** 500 entradas (eviction LRU)
- **Eviction manual:** `POST /api/cards/sync` limpia todo el cache

**Sincronización programada:**
- Cada **domingo a las 00:00**, un scheduler verifica si pasaron ≥ 14 días desde la última sincronización
- Si es así, refresca los datos desde `pokemontcg.io` y limpia el cache
- Si la API externa está caída, el cache sigue sirviendo los datos existentes (resiliencia)

**Verificación del cache:**
- Las primeras consultas después de un restart pueden ser más lentas (cache miss)
- Las consultas subsecuentes son instantáneas (cache hit)
- Para forzar un refresh manual: `curl -X POST http://localhost:8080/api/cards/sync`

---

## 5. Ejecución del Backend

### Paso 1: Compilar

```bash
cd BE
mvn clean test
```

Esto descarga dependencias, compila el código y ejecuta los tests unitarios.

### Paso 2: Ejecutar

```bash
mvn spring-boot:run
```

> **Nota:** Este comando inicia un servidor que se mantiene corriendo. La terminal quedará ocupada hasta que se detenga manualmente (Ctrl+C). No lo ejecute en segundo plano si necesita ver los logs.

### Paso 3: Verificar

- **Consola:** Busque el mensaje `Started PokemonApplication in X.XXX seconds`
- **API REST:** Abra `http://localhost:8080/api/cards` en el navegador
- **Swagger UI:** Abra `http://localhost:8080/swagger-ui.html`
- **Base de datos:** Conéctese con psql o pgAdmin a `localhost:5432`, database `pokemon_tcg`, usuario `postgres`

### Sincronización de cartas

- **Automática (startup):** Al iniciar el backend con la DB vacía, el `DataInitializer` ejecuta `POST /api/cards/sync` automáticamente y descarga las 146 cartas del set XY1. El `CacheWarmupRunner` carga el catálogo en cache (Caffeine) inmediatamente después.
- **Programada (scheduler):** Cada domingo a las 00:00, el `CardSyncScheduler` verifica si pasaron ≥ 14 días desde la última sincronización. Si es así, refresca los datos y limpia el cache.
- **Manual (opcional):** Si necesita forzar una resincronización inmediata, ejecute:
  ```bash
  curl -X POST http://localhost:8080/api/cards/sync
  ```
  Esto sincroniza desde la API, actualiza la DB y evicta todo el cache.

> **Requiere conexión a internet** para comunicarse con `api.pokemontcg.io`. Si la API está caída, el cache sigue sirviendo los datos existentes.

---

## 6. Ejecución del Frontend

> **Importante:** El backend **debe estar corriendo** antes de iniciar el frontend. El frontend se conecta al backend en `http://localhost:8080` mediante el proxy configurado.

### Paso 1: Instalar dependencias

```bash
cd FE
npm install
```

### Paso 2: Iniciar el servidor de desarrollo

```bash
npm start
```

Esto ejecuta `ng serve` y levanta el servidor de desarrollo de Angular.

> **Nota:** Este comando mantiene el servidor corriendo con hot-reload. La terminal quedará ocupada hasta que se detenga manualmente (Ctrl+C).

### Paso 3: Verificar

- Abra `http://localhost:4200` en el navegador
- Debería ver la Pokedex con las cartas del set XY1 cargadas

---

## 6.1. Registro y Autenticación (Semana 5+)

A partir de la Semana 5, el frontend requiere autenticación para acceder a cualquier página excepto `/login` y `/register`.

**Primera vez:**
1. Abrir `http://localhost:4200` → redirige automáticamente a `/login`
2. Hacer clic en "Crear cuenta" → ir a `/register`
3. Completar formulario: username (3-30 caracteres), contraseña (mín. 8), género (opcional)
4. Submit → queda autenticado automáticamente → redirige a `/pokedex`

**Sesiones siguientes:**
1. Abrir `http://localhost:4200` → redirige a `/login`
2. Ingresar username + contraseña → JWT se guarda en localStorage
3. Redirige a `/pokedex` (o a la ruta que intentaba acceder)

**Cerrar sesión:**
1. Hacer clic en "Cerrar sesión" en el menú/header
2. Se limpia el token de localStorage
3. Redirige a `/login`

---

## 7. Verificación End-to-End

### Checklist de verificación

| # | Verificación | Resultado esperado |
|---|---|---|
| 1 | Backend compilado | `BUILD SUCCESS` en consola |
| 2 | Backend iniciado | `Started PokemonApplication` en logs |
| 3 | API de cartas | JSON con cartas en `http://localhost:8080/api/cards` |
| 4 | Swagger UI | Interfaz accesible en `/swagger-ui.html` |
| 5 | Frontend iniciado | `Compiled successfully` en consola |
| 6 | Registro de usuario | POST `/api/auth/register` devuelve JWT |
| 7 | Login de usuario | POST `/api/auth/login` devuelve JWT |
| 8 | Redirección sin auth | `http://localhost:4200` redirige a `/login` |
| 9 | Pokedex funcional (autenticado) | Cartas visibles en `http://localhost:4200/pokedex` |
| 7 | CORS funcionando | No hay errores de CORS en la consola del navegador |

### Endpoints clave para verificar

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/auth/register` | Registrar nuevo usuario |
| `POST` | `/api/auth/login` | Iniciar sesión (devuelve JWT) |
| `GET` | `/api/users/me` | Perfil del usuario autenticado |
| `GET` | `/api/cards` | Lista paginada de cartas |
| `GET` | `/api/cards/{id}` | Detalle de una carta |
| `GET` | `/api/cards/search?name=pikachu` | Búsqueda por nombre |
| `GET` | `/api/cards/sets` | Lista de sets disponibles |
| `GET` | `/api/cards/types` | Lista de tipos de carta |
| `POST` | `/api/cards/sync` | Sincronización con API externa |

---

## 8. Troubleshooting

### 8.1. Puerto 8080 ya en uso

**Síntoma:** `Port 8080 was already in use`

**Solución:**

```bash
# Windows (PowerShell) — identificar proceso
Get-NetTCPConnection -LocalPort 8080 | Select-Object OwningProcess

# Windows — matar proceso
Stop-Process -Id <PID> -Force

# Linux / macOS — identificar proceso
lsof -i :8080

# Linux / macOS — matar proceso
kill -9 <PID>
```

### 8.2. Base de datos no accesible

**Síntoma:** `Connection refused` o `FATAL: database "pokemon_tcg" does not exist`

**Solución:**
1. Verifique que PostgreSQL 16 esté corriendo:
   ```bash
   # Windows (PowerShell)
   Get-Service postgresql*

   # Linux
   sudo systemctl status postgresql
   ```
2. Si no está corriendo, inícielo:
   ```bash
   # Windows
   Start-Service postgresql*

   # Linux
   sudo systemctl start postgresql
   ```
3. Cree la base de datos:
   ```bash
   psql -U postgres -c "CREATE DATABASE pokemon_tcg;"
   ```

### 8.3. Sincronización de cartas falla

**Síntoma:** `Auto-sync failed: ...` en los logs del backend

**Causas posibles:**

1. **Sin conexión a internet:** Verifique la conectividad
2. **API key inválida:** Verifique `POKEMON_TCG_API_KEY` en `application.properties`
3. **Rate limit de la API:** Espere unos minutos y reintente

**Solución:**

```bash
# Forzar resincronización manual
curl -X POST http://localhost:8080/api/cards/sync
```

### 8.4. Errores de CORS en el frontend

**Síntoma:** `Access to XMLHttpRequest at 'http://localhost:8080/api/cards' from origin 'http://localhost:4200' has been blocked by CORS policy`

**Solución:**

1. Verifique que el backend esté corriendo en `http://localhost:8080`
2. La configuración CORS ya permite `http://localhost:4200` en `WebConfig.java`
3. Si cambió el puerto del frontend, actualice la configuración CORS

### 8.5. Frontend no encuentra el backend

**Síntoma:** `Http failure response for http://localhost:8080/api/cards: 0 Unknown Error`

**Solución:**

1. Verifique que el backend esté corriendo: `curl http://localhost:8080/api/cards`
2. Verifique que no haya firewall bloqueando el puerto 8080
3. Reinicie el backend si es necesario

### 8.6. Tests del backend fallan

**Síntoma:** `BUILD FAILURE` al ejecutar `mvn clean install`

**Solución:**

```bash
# Ejecutar tests con más detalle
mvn clean test -X

# Ejecutar tests sin mostrar logs de Spring
mvn clean test -Dlogging.level.root=WARN
```

### 8.7. Flyway + JPA en Spring Boot 3.5.x

**Síntoma:** `Circular depends-on relationship between 'flyway' and 'entityManagerFactory'`

**Causa:** La propiedad `spring.jpa.defer-datasource-initialization=true` genera una dependencia circular en Spring Boot 3.5.x. Flyway se ejecuta antes que Hibernate por defecto, por lo que esta propiedad no es necesaria.

**Solución:** Verifique que la propiedad **no exista** en ningún archivo de configuración de perfil:

- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-prod.properties`

Elimine la línea `spring.jpa.defer-datasource-initialization=true` dondequiera que aparezca, luego reinicie el backend.

### 8.8. Spring Security genera contraseña por defecto

**Síntoma:** En los logs aparece `Using generated security password: <uuid>` y las APIs devuelven `401 Unauthorized` o `403 Forbidden`.

**Causa:** Spring Boot auto-configura Spring Security si la dependencia está en el classpath y no existe una configuración personalizada.

**Solución:**

1. Si el proyecto tiene una configuración de seguridad permisiva para desarrollo (`SecurityConfig` con `permitAll`), el password generado se puede ignorar — las APIs funcionarán normalmente
2. Si las APIs devuelven 401/403, verifique que `SecurityConfig` esté configurada correctamente para permitir las rutas necesarias en desarrollo
3. Para deshabilitar Security temporalmente en desarrollo, puede excluir la dependencia o agregar una configuración que permita todas las solicitudes

---

## 9. Tests

### Backend

```bash
cd BE

# Tests unitarios + integración
mvn clean test

# Tests unitarios + integración + validación de cobertura JaCoCo
mvn clean verify jacoco:check

# Tests unitarios solamente (más rápido)
mvn clean test -Dtest=*Test

# Tests de integración solamente
mvn clean test -Dtest=*IntegrationTest
```

### Frontend

```bash
cd FE

# Tests unitarios (modo interactivo)
npm test

# Tests unitarios (headless, para CI)
npm test -- --watch=false --browsers=ChromeHeadless
```

---

## 10. Flujo de Trabajo Recomendado

### Inicio diario de desarrollo

> **Nota:** Tanto `mvn spring-boot:run` como `npm start` son procesos de larga duración que mantienen el servidor activo. Deben ejecutarse en terminales separadas y **no** se cierran automáticamente. No ejecute ambos comandos en la misma terminal.

```bash
# Terminal 1 — Backend (se mantiene corriendo)
cd BE
mvn spring-boot:run

# Terminal 2 — Frontend (se mantiene corriendo)
cd FE
npm start
```

### Antes de hacer commit

```bash
# Backend: compilar y tests
cd BE && mvn clean test

# Frontend: build y tests
cd FE && npm run build && npm test -- --watch=false --browsers=ChromeHeadless
```

### Cuando se agregan migraciones Flyway

1. Crear el script `Vn__descripcion.sql` en `BE/src/main/resources/db/migration/`
2. En PostgreSQL, eliminar y recrear la base de datos:
   ```bash
   psql -U postgres -c "DROP DATABASE pokemon_tcg;"
   psql -U postgres -c "CREATE DATABASE pokemon_tcg;"
   ```
3. Reiniciar el backend — Flyway ejecutará la nueva migración automáticamente
4. Verificar que la aplicación funcione correctamente

### Cuando se agregan nuevas entidades JPA

1. Crear la migración Flyway correspondiente con la definición SQL de la tabla
2. Anotar la entidad con `@Entity` y `@Table(name = "...")`
3. Con `ddl-auto=validate`, Hibernate verificará que la tabla en PostgreSQL coincida con la entidad
