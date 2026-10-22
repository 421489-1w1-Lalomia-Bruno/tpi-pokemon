# Pokémon TCG - Web Multiplayer Game

Plataforma de juego de cartas coleccionables multijugador en tiempo real, basada en el set clásico XY1. Desarrollada con arquitectura reactiva sobre Spring Boot (Java) y Angular 20.

## Stack Tecnológico

### Backend (Core Engine & API)
* **Java 21** + **Spring Boot 3.x**
* **Spring WebSockets (STOMP)** para mensajería bidireccional de baja latencia (<50ms).
* **Spring Security + JWT** con validación en interceptores de canal.
* **PostgreSQL** con columnas `JSONB` para persistencia atómica de estados.
* **Flyway** para migraciones versionadas de base de datos.
* **JaCoCo** y **Testcontainers** para Quality Gates.

### Frontend (SPA & UI)
* **Angular 20** (Arquitectura 100% Standalone y Control Flow nativo).
* **Signals** para gestión de estado global derivado (`computed`).
* **Angular CDK (Drag & Drop)** para interactividad física en el tablero.
* **RxJS** para la gestión reactiva del canal WebSocket.

---

## Requisitos Previos

Para levantar el entorno de desarrollo local, asegúrese de tener instalado:
* **JDK 21** (Temurin / Eclipse Adoptium recomendado).
* **Node.js 18** o superior.
* **Maven 3.9+**.
* **Docker Desktop** (Opcional: solo necesario para ejecutar Testcontainers en la suite de integración del backend).

Para una lista completa y verificada, consulte [17-setup-ejecucion.md](17-setup-ejecucion.md).

---

## Configuración del Entorno (Variables)

El sistema utiliza variables de entorno inyectadas para proteger los secretos. Antes de iniciar, configure las siguientes variables en su sistema operativo o en el IDE:

```env
# Seguridad y Autenticación
JWT_SECRET=tu_clave_secreta_alfanumerica_muy_larga_de_256_bits
```

# API Externa (Requerida solo para la sincronización inicial del administrador)
POKEMON_TCG_API_KEY=tu_api_key_de_pokemontcg_io

# Base de Datos (Perfil de Producción)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=pokemon_tcg
DB_USERNAME=postgres
DB_PASSWORD=tu_password

## Instrucciones de Ejecución Local

La guía completa y actualizada se encuentra en [17-setup-ejecucion.md](17-setup-ejecucion.md). A continuación, un resumen rápido:

### Inicio rápido

```bash
# Terminal 1 — Backend
cd BE
mvn clean install
mvn spring-boot:run

# Terminal 2 — Frontend (esperar a que el backend esté listo)
cd FE
npm install
npm start
```

- **Backend:** `http://localhost:8080/api` — Swagger en `http://localhost:8080/swagger-ui.html`
- **Frontend:** `http://localhost:4200`
- **Base de datos:** H2 en archivo (`BE/data/pokemon_tcg.mv.db`), persiste entre reinicios
- **Sincronización de cartas:** Automática al iniciar con la DB vacía (vía `DataInitializer`)

Para más detalles, troubleshooting y configuración de tests, consulte [17-setup-ejecucion.md](17-setup-ejecucion.md).

## Estrategia de Testing y Calidad

El proyecto exige un estándar de calidad estricto (RNF-03). Los Pull Requests fallarán automáticamente en GitHub Actions si no se cumplen los umbrales de cobertura.

### Ejecutar Suite del Backend

```
cd BE
# Ejecuta unitarios, integración y valida cobertura JaCoCo
mvn clean verify jacoco:check
```

### Ejecutar Suite del Frontend

```
cd FE
# Tests unitarios (headless, para CI)
npm test -- --watch=false --browsers=ChromeHeadless
```

Para más detalles sobre testing, consulte [17-setup-ejecucion.md](17-setup-ejecucion.md).


## Reglas de Contribución

1. No hacer push directo a `main` ni a `develop`.
2. Todas las ramas deben nacer de `develop` con la nomenclatura `feature/nombre-de-la-tarea` o `fix/descripcion-del-bug`.
3. Los mensajes de commit deben respetar estrictamente el formato Conventional Commits (ej. `feat(engine): implementar validacion de energia especial`).
4. Todo Pull Request debe venir acompañado de sus respectivas pruebas automatizadas pasando exitosamente.



