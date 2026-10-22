# 13 - Checklist de Entrega (Actualizado)

> **Nota de Gestión:** Este documento es el criterio formal de "Definición de Terminado" (Definition of Done). Ninguna versión del sistema puede marcarse como Release Final (v1.0.0) si no cumple con el 100% de los ítems detallados a continuación.

---

## 1. Repositorio y Versionado

- [x] **Estructura de Ramas:** Todo el código final está fusionado en `develop` y promovido a `main` sin conflictos pendientes.
- [x] **Changelog Automatizado:** Se generó el `CHANGELOG.md` documentando features, fixes y cambios técnicos de la release.
- [x] **Limpieza del Repositorio:** No existen archivos basura (`.DS_Store`, `node_modules`, `target`, `.env` locales) rastreados en la rama principal. El archivo `.gitignore` está correctamente configurado.

---

## 2. Backend y Base de Datos (Spring Boot & PostgreSQL)

- [x] **Sincronización Inicial:** El endpoint `POST /api/cards/sync` descarga e inserta correctamente las 146 cartas del set XY1 desde `pokemontcg.io`.
- [x] **Migraciones:** Flyway ejecuta secuencialmente los scripts `V1` a `V14` levantando la estructura relacional completa desde cero contra PostgreSQL 16 sin errores.
- [x] **Motor de Juego:** El `TurnManager` respeta todas las restricciones (bloqueo de ataque en primer turno, bloqueo de evolución en primer turno).
- [x] **Combate en Cadena:** El cálculo de daño aplica correctamente debilidades ($\times2$), resistencias ($-20$) y lee el pool dinámico de energías especiales.
- [x] **Condiciones Especiales:** El sistema procesa los KOs inter-turnos por veneno/quemadura y exige forzosamente la acción `SET_ACTIVE` pausando el motor.
- [x] **Persistencia Atómica:** Los snapshots del tablero se guardan correctamente en la tabla `game_states` bajo el formato nativo `JSONB` de PostgreSQL.

---

## 3. Infraestructura de Red y WebSockets

- [x] **Handshake Seguro:** El `ChannelInterceptor` rechaza conexiones STOMP que no posean un JWT válido en las cabeceras.
- [x] **Enmascaramiento (Anti-Cheat):** El payload de `STATE_UPDATE` ofusca las cartas de la mano y mazo del oponente antes de enviarlo al cliente local.
- [x] **Cross-Identity Lock:** El sistema rechaza acciones cuyo campo `playerId` no coincida con el `Principal` inyectado en el socket.
- [ ] **Rendimiento:** El backend procesa las acciones y devuelve el broadcast del estado en menos de 50ms (RNF-01).

---

## 4. Frontend y Experiencia de Usuario (Angular 21)

- [x] **Arquitectura Standalone:** No existen `NgModules` en el código fuente. Se utiliza el Control Flow nativo (`@if`, `@for`).
- [x] **Gestión de Estado Centralizada:** La UI del tablero se renderiza puramente mediante Signals derivados (`computed`) a partir del servicio de estado principal, sin mutar arrays locales.
- [x] **Interactividad (Drag & Drop):** El arrastre de cartas utiliza exclusivamente `@angular/cdk/drag-drop`. Soltar una carta emite la acción al WebSocket en lugar de modificar el DOM.
- [x] **Bloqueos Modales:** El componente `PromoteModalComponent` aparece forzosamente y bloquea la interfaz cuando el backend pausa la partida tras un KO.
- [x] **Desconexión Limpia:** Al salir de la ruta `/game/:matchId`, el componente se destruye y ejecuta el cierre explícito del canal STOMP, evitando fugas de memoria.

---

## 5. Testing y Aseguramiento de Calidad (QA)

- [x] **Métricas de JaCoCo:** El reporte de cobertura asegura $\ge 90\%$ en los paquetes críticos (`engine.*`) y $\ge 80\%$ en el resto de la aplicación.
- [x] **Edge Cases Aprobados:** Se superaron con éxito los tests automatizados para "KOs Simultáneos", "Mulligans Infinitos" y "Sudden Death".
- [x] **Embedded PostgreSQL:** Los tests `@SpringBootTest` ejecutan contra PostgreSQL real mediante `EmbeddedPostgresTestBase`, sin Docker.
- [x] **Pruebas End-to-End:** Al menos un script E2E (Cypress/Playwright) compila y ejecuta una partida feliz desde la creación en el Lobby hasta la pantalla de victoria.
- [x] **Calidad de Código Estático:** El build de Maven compila sin arrojar advertencias (warnings) de Checkstyle ni PMD. Ningún archivo TypeScript en el Frontend contiene variables tipadas como `any`.

---

## 6. Documentación

- [x] **README.md del Proyecto:** Contiene instrucciones claras sobre cómo clonar, configurar variables de entorno (ej. base de datos local y JWT_SECRET) y levantar ambos entornos (Backend y Frontend).
- [x] **Documentación API REST:** Swagger UI (`/swagger-ui.html`) está configurado y lista correctamente los endpoints del Lobby y Deck Builder.
- [x] **Contratos WebSocket:** Los payloads de entrada (`GameActionMessage`) y salida (`MatchSnapshot`) están documentados y ejemplificados (ver `04-api-design.md`) para facilitar futuras integraciones.
