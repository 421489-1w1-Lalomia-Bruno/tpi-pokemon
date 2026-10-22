# Pokémon TCG — Frontend (Angular 21)

## Stack

| Tecnología | Versión |
|---|---|
| Angular | 21 |
| TypeScript | 5.9 |
| RxJS | ~7.8 |
| Angular CDK | ~21 |
| Playwright | latest |

## Arquitectura

- **100% Standalone Components** (sin NgModules)
- **Signals** para estado reactivo (`signal()`, `computed()`)
- **Control Flow nativo** (`@if`, `@for`, `@switch`)
- **ChangeDetection OnPush** en todos los componentes
- **Lazy Loading** por feature module en rutas

## Features

| Feature | Ruta | Descripción |
|---|---|---|
| Auth | `/login`, `/register` | Login/registro con JWT |
| Pokedex | `/pokedex` | Catálogo de cartas |
| Deck Builder | `/deck-builder` | Constructor de mazos |
| Lobby | `/lobby` | Partidas disponibles |
| Game | `/game/:matchId` | Tablero de juego |
| Profile | `/profile` | Perfil de usuario |

## Comunicación

- **REST** para CRUD (cartas, mazos, auth, lobby)
- **WebSocket STOMP** para juego en tiempo real
- Interceptor HTTP para JWT
- AuthGuard para rutas protegidas

Para documentación detallada, ver `docs/sdd/06-frontend.md`.
