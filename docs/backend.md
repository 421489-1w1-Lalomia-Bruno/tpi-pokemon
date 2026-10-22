# Pokémon TCG — Backend (Spring Boot 3.5)

## Stack

| Tecnología | Versión |
|---|---|
| Java | 21 (Temurin) |
| Spring Boot | 3.5 |
| Maven | 3.9+ |
| PostgreSQL | 16 |
| Flyway | latest |
| Caffeine | latest |
| JaCoCo | latest |

## Game Engine (Core)

El motor de juego está completamente aislado del transporte (REST/WebSocket):

- `engine/state/` — State Pattern (Waiting, Setup, Active, Finished)
- `engine/combat/steps/` — Chain of Responsibility (7 pasos de ataque)
- `engine/effects/` — Strategy Pattern (5 condiciones especiales)
- `engine/rules/` — RuleValidator, TurnManager, VictoryConditionChecker
- `engine/events/` — Observer Pattern (ApplicationEventPublisher)

## Documentación disponible

| Recurso | Ubicación |
|---|---|
| SDD completo | `docs/sdd/` |
| JavaDoc | `BE/docs/java_doc/` (generar con `mvn javadoc:javadoc`) |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Diagramas UML | `BE/docs/app_doc/diagrams/` |
