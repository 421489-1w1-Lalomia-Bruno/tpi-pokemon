# 00 - Overview

## Visión del Proyecto

Desarrollo de una versión digital del Pokémon Trading Card Game (TCG) para 2 jugadores en tiempo real, siguiendo las reglas oficiales del set XY1. El proyecto implementa tanto el front-end como el back-end de la aplicación, integrando lógica de negocio con una interfaz clara y funcional.

## Stack Tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| **Backend** | Java + Spring Boot | Java 21, Spring Boot 3.x |
| **Frontend** | Angular + TypeScript | Angular 21, TS 5.9 |
| **Base de Datos** | PostgreSQL | 16 (nativo + embedded para tests) |
| **Comunicación Real-time** | WebSocket | STOMP |
| **API Externa** | pokemontcg.io | v2 |
| **Build** | Maven + npm | - |
| **Testing** | JUnit 5, Mockito, JaCoCo, Jasmine, Embedded PostgreSQL | - |
| **Control de Versiones** | Git + GitHub | GitFlow |

## Objetivos

### Obligatorio (100 puntos)

| Categoría | Puntos | Descripción |
|---|---|---|
| Reglas del Juego (RF-01 a RF-07) | 40 | Implementación completa de las reglas del Pokémon TCG |
| Arquitectura & Código | 25 | SOLID, patrones de diseño, código limpio |
| Base de Datos | 10 | Diseño relacional, migraciones versionadas |
| Frontend | 15 | UI funcional, interactiva, responsive |
| Testing | 10 | JaCoCo ≥ 80% global, ≥ 90% crítico, 1+ E2E |
| **Total** | **100** | Mínimo 60 para aprobar |

### Mínimo para Aprobar

- **60%** de los puntos obligatorios
- Los puntos opcionales solo cuentan si se cumplen todos los obligatorios

## Alcance

### Incluye

- Sistema de cartas con datos reales (pokemontcg.io API)
- Deck Builder con validación de reglas (Nota: Regla de límite de cartas ACE SPEC omitida por requerimiento)
- Lobby para crear y unirse a partidas
- Juego en tiempo real con WebSocket
- Motor de juego con todas las reglas oficiales (incluyendo resolución de empates y restricciones de primer turno)
- Persistencia del estado de partida
- Pokedex para consultar cartas

### No Incluye (Plan Inicial)

- Animaciones avanzadas
- Sistema de ranking
- Chat en partida
- Mazos temáticos precargados

Estas funcionalidades se documentan en [14-post-proyecto.md](14-post-proyecto.md).

## Estado Actual del Proyecto

### Backend

- **167 archivos Java** completamente implementados
- **Motor de juego** con state machine (Waiting, Setup, Active, Finished), pipeline de combate en 7 pasos, 5 efectos de estado, efectos continuos (Stadium/Tool) y efectos Trainer
- **Sistema de usuarios** con registro, login JWT, perfil, stats (wins/losses)
- **API REST** completa: Cards, Decks, Matches, Auth, Users
- **WebSocket STOMP** con autenticación JWT, sincronización en tiempo real de partidas
- **Persistencia atómica** en PostgreSQL 16 con JSONB nativo, migraciones Flyway V1-V14
- **Cache-Aside** con Caffeine (TTL 14 días) y sincronización programada con pokemontcg.io
- **Embedded PostgreSQL** (zonky) para tests sin Docker
- **Cobertura JaCoCo** ≥ 87% global, **Checkstyle 0 violaciones, PMD 0 violaciones**
- **722 tests** unitarios y de integración, 0 fallos

### Frontend

- **~80+ archivos TypeScript** completamente implementados
- **5 features**: Auth (login, registro, perfil), Deck Builder, Game (tablero completo con drag & drop CDK), Lobby, Pokedex
- **Arquitectura standalone** con Signals (`computed()`, `input()`, `output()`), Control Flow nativo (`@if`, `@for`)
- **Shared components**: Card, CardStack, CardZoom, DamageCounter, EnergyBadge, GameLog, NavBar, StatusIndicator
- **E2E tests** con Playwright (auth, deck-builder, game-flow)
- **Build producción**: `npm run build` exitoso (338 KB inicial, lazy loading por feature)

## Referencias

- [Reglas oficiales XY1](../../materiales/xy1-rulebook-es.pdf)
- [Especificación del proyecto](../../materiales/TUP_3C_PIII_TPI_POKEMON_TCG.pdf)
- [API pokemontcg.io](https://pokemontcg.io/)
- [Portal de desarrolladores pokemontcg.io](https://dev.pokemontcg.io/)
