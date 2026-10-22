<p align="center"> <img src="./BE/docs/assets/images/pokemon-tcg.png" alt="Pokemon TCG"/> </p>

# Pokémon Trading Card Game

Trabajo Práctico Integrador de Programación III Grupo 12: Pokémon Trading Card Game para 2 jugadores en tiempo real, basada en las reglas oficiales del set XY1.

---

## Stack Tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Backend | Java + Spring Boot | Java 21, Spring Boot 3.5 |
| Frontend | Angular + TypeScript | Angular 21, TypeScript 5.9 |
| Base de Datos | H2 (En desarrollo) - PostgreSQL (Final) | 16 |
| Comunicación en Tiempo Real | WebSocket STOMP | — |
| API Externa | pokemontcg.io | v2 |
| Cache | Caffeine | — |
| Testing Backend | JUnit 5, Mockito, JaCoCo, Embedded PostgreSQL | — |
| Testing Frontend | Jasmine, Karma, Playwright | — |
| Build | Maven + npm | — |
| Control de Versiones | Git + GitHub (GitFlow) | — |

---

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                     CLIENTE (Angular 21)                         │
│  ┌──────────┐  ┌──────────────┐  ┌──────────┐  ┌────────────┐  │
│  │ Pokedex  │  │ Deck Builder │  │  Lobby   │  │ Game Board │  │
│  └────┬─────┘  └──────┬───────┘  └────┬─────┘  └─────┬──────┘  │
│       └───────────────┴───────────────┴─────────────┘           │
│              Signals · Computed · Control Flow (@if / @for)     │
└─────────────────────┬───────────────────────────────┬───────────┘
                      │          HTTP REST            │
                      │                      WebSocket STOMP
┌─────────────────────┼───────────────────────────────┼───────────┐
│                SERVIDOR (Spring Boot 3.5)                        │
│  ┌───────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────────┐  │
│  │CardCtrl   │ │DeckCtrl  │ │MatchCtrl │ │   Game Engine    │  │
│  └─────┬─────┘ └────┬─────┘ └────┬─────┘ │ State Machine    │  │
│        │             │           │        │ Combat Pipeline  │  │
│  ┌─────┴─────────────┴───────────┴────────┤ Efectos Trainer  │  │
│  │         Capa de Servicios (Cache-Aside) │ Persistencia     │  │
│  └──────────────────────┬─────────────────┴──────────────────┘  │
│                         │                                       │
│  ┌──────────────────────┴───────────────────────────────────┐  │
│  │           Cache Caffeine (TTL 14 días, max 500)          │  │
│  └──────────────────────┬───────────────────────────────────┘  │
│                         │ (miss)                                │
│  ┌──────────────────────┴───────────────────────────────────┐  │
│  │       Repositorios JPA · Migraciones Flyway V1–V14       │  │
│  └──────────────────────┬───────────────────────────────────┘  │
└─────────────────────────┼──────────────────────────────────────┘
                          │
                ┌─────────┴─────────┐
                │   PostgreSQL 16   │
                │ (dev · test · prod)│
                └───────────────────┘
```

---

## Instalación

### Requisitos

| Herramienta | Versión | Verificación |
|---|---|---|
| Java JDK | 21 (Temurin o Eclipse Adoptium) | `java -version` |
| Maven | 3.9+ | `mvn --version` |
| Node.js | 18+ | `node --version` |
| npm | 10+ | `npm --version` |
| PostgreSQL | 16 | `psql --version` |

### Clonar

```bash
git clone <url-del-repositorio>
cd tpi-pokemon-2w1-12
git checkout develop
```

### Variables de Entorno

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `POKEMON_TCG_API_KEY` | Clave incluida en `application.properties` | API key de pokemontcg.io |
| `JWT_SECRET` | Clave por defecto en `application.properties` | Firma de tokens JWT |

**Windows (PowerShell):**

```powershell
$env:POKEMON_TCG_API_KEY="tu-api-key"
$env:JWT_SECRET="tu-secret-key"
```

**Linux / macOS:**

```bash
export POKEMON_TCG_API_KEY="tu-api-key"
export JWT_SECRET="tu-secret-key"
```

### Base de Datos

Instalar PostgreSQL 16 y crear la base de datos:

```sql
CREATE DATABASE pokemon_tcg;
```

Credenciales por defecto: `localhost:5432`, usuario `postgres`, contraseña `postgres`.

Flyway ejecuta automáticamente las migraciones V1 a V14 al iniciar el backend.

### Instalar dependencias

```bash
cd BE
mvn clean compile       # Descarga dependencias Java

cd ../FE
npm install             # Descarga dependencias Node
```

---

## Ejecución Local

Se necesitan **dos terminales** simultáneas:

**Terminal 1 — Backend (puerto 8080)**

```bash
cd BE
mvn spring-boot:run
```

**Terminal 2 — Frontend (puerto 4200)**

```bash
cd FE
npm start
```

La primera vez que se inicia con la base de datos vacía, `DataInitializer` descarga automáticamente las 146 cartas del set XY1 desde pokemontcg.io (requiere conexión a internet). `CacheWarmupRunner` las carga en memoria inmediatamente después.

Abrir `http://localhost:4200` en el navegador. La aplicación redirige automáticamente a la pantalla de login/registro en el primer acceso.

Swagger UI disponible en `http://localhost:8080/swagger-ui.html`.

---

## Build de Producción

```bash
cd FE && npm run build    # Frontend → dist/FE/
cd BE && mvn package      # Backend → target/tpi-pokemon.jar
```

---

## Testing

```bash
# Backend: tests unitarios + integración + cobertura JaCoCo
cd BE && mvn clean verify

# Frontend: tests unitarios (Jasmine + Karma)
cd FE && npm test -- --watch=false --browsers=ChromeHeadless

# Frontend: tests E2E (Playwright) — requiere backend corriendo
cd FE && npm run test:e2e
```

---

## Endpoints Principales

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/auth/register` | Registrar nuevo usuario |
| `POST` | `/api/auth/login` | Iniciar sesión (devuelve JWT) |
| `GET` | `/api/users/me` | Perfil del usuario autenticado |
| `GET` | `/api/cards` | Catálogo de cartas (paginado) |
| `POST` | `/api/cards/sync` | Sincronizar con API externa |
| `GET/POST/PUT/DELETE` | `/api/decks` | CRUD de mazos |
| `GET/POST` | `/api/matches` | Lobby de partidas |
| WebSocket | `/ws` | Juego en tiempo real (STOMP) |

---

## Documentación

- [SDD](docs/sdd/) — Software Design Document completo (18 capítulos)
- [Backend](docs/backend.md) — Stack y descripción del backend
- [Frontend](docs/frontend.md) — Stack y descripción del frontend
- [Integrantes](docs/INTEGRANTES.md) — Grupo 12
- [Changelog](docs/CHANGELOG.md) — Registro de cambios
- [Swagger UI](http://localhost:8080/swagger-ui.html) — API REST documentada
- [JavaDoc](BE/docs/java_doc/) — Documentación del código backend
- [Diagramas UML](BE/docs/app_doc/diagrams/) — Clases, componentes y secuencias

<p align="center"> <img src="./BE/docs/assets/images/UTN-FRC_logo.png" alt="UTN - FRC"/> </p>

<p align="center"> <img src="./BE/docs/assets/images/Tup_completo_negro_transparente.png" alt="TUP"/> </p>
