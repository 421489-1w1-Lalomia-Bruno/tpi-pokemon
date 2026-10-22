# Changelog

## [1.0.0] - 2026-06-27

### Added
- Implementación completa del motor de juego con State Machine (WAITING → SETUP → ACTIVE → FINISHED)
- Pipeline de combate en 7 pasos (Chain of Responsibility)
- 5 condiciones especiales con Strategy Pattern (Poisoned, Burned, Asleep, Paralyzed, Confused)
- Sistema de usuarios con registro/login JWT
- Deck Builder con validación de reglas (60 cartas, 4 copias, ACE SPEC, 1+ Basic)
- Lobby con creación y unión a partidas en tiempo real
- Tablero de juego interactivo con drag & drop (Angular CDK)
- Sincronización con API pokemontcg.io (set XY1, 146 cartas)
- Cache-Aside con Caffeine (TTL 14 días)
- Persistencia atómica en PostgreSQL 16 con JSONB
- Migraciones Flyway V1 a V14
- WebSocket STOMP con autenticación JWT
- 722 tests unitarios y de integración (JaCoCo ≥ 87%)
- Tests E2E con Playwright

### Technical
- Java 21 + Spring Boot 3.5
- Angular 21 + TypeScript 5.9 (Standalone Components, Signals)
- PostgreSQL 16 + Flyway + JSONB nativo
- Embedded PostgreSQL para tests (zonky, sin Docker)
