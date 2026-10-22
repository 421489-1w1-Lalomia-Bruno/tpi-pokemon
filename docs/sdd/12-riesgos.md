# 12 - Gestión de Riesgos

> Nota de Gestión: Este documento identifica las vulnerabilidades críticas introducidas por la arquitectura en tiempo real y el acoplamiento asíncrono. Define los planes de mitigación técnicos obligatorios para asegurar la estabilidad del sistema y el cumplimiento de los RNF.

---

## Matriz de Riesgos Principales

La evaluación se clasifica según su **Probabilidad** (Alta/Media/Baja) y su **Impacto** (Crítico/Alto/Medio).

| ID | Riesgo Identificado | Probabilidad | Impacto |
|---|---|---|---|
| **R01** | Desincronización de Estado (Cliente vs Servidor) | Media | Crítico |
| **R02** | Condiciones de Carrera (Race Conditions) en WebSockets | Alta | Crítico |
| **R03** | Cuello de Botella en Serialización JSONB | Media | Alto |
| **R04** | Incompatibilidad de Contratos Drag & Drop | Alta | Alto |
| **R05** | Fallo de Integración de Efectos de Estado | Baja | Crítico |

---

## 1. Riesgos Arquitectónicos y de Sincronización

### R01: Desincronización de Estado (Cliente vs Servidor)
* **Descripción:** Pérdidas de paquetes de red o micro-cortes de conexión provocan que el cliente de Angular renderice una mesa distinta a la memoria del motor Java, permitiendo acciones inválidas.
* **Mitigación Preventiva:** El cliente no posee estado propio. El tablero se dibuja puramente mediante Signals derivados (`computed`) del último `MatchSnapshot` recibido. 
* **Plan de Contingencia:** Implementar lógica de reconexión automática en `websocket.service.ts`. Al reconectarse, el cliente debe forzar una petición de sincronización (`SYNC_STATE`) para que el backend lea la columna `JSONB` en PostgreSQL y retransmita el estado exacto actual.

### R02: Condiciones de Carrera en WebSockets
* **Descripción:** Dos jugadores ejecutan acciones exactas al mismo milisegundo (ej. ambos intentan promover un Pokémon activo simultáneamente tras un KO doble por veneno), corrompiendo el turno lógico.
* **Mitigación Preventiva:** El `TurnManager` bloquea el procesamiento en hilos mediante colas de mensajes serializadas por `matchId`. Se rechaza cualquier acción de WebSocket entrante que no coincida con el `Principal` del jugador al que le corresponde actuar en esa milésima de segundo.
* **Plan de Contingencia:** El motor responderá con una excepción `InvalidActionException` (tipo `ERROR`) a la acción rezagada/inválida, forzando a la UI a desestimar el intento sin alterar el `matchState`.

---

## 2. Riesgos de Rendimiento y Datos

### R03: Cuello de Botella en Serialización JSONB
* **Descripción:** A medida que la partida avanza, el log de eventos y el tamaño de las manos crece. Escribir y leer constantemente un objeto JSON gigante en PostgreSQL puede bloquear el hilo de la base de datos y violar el RNF-01 (latencia < 50ms).
* **Mitigación Preventiva:** Uso de la librería de Jackson altamente optimizada en Spring Boot y definición de la columna como `JSONB` nativo (no `TEXT`), lo que permite a PostgreSQL indexar y gestionar internamente la compresión binaria.
* **Plan de Contingencia:** Las pruebas de carga exigidas en el archivo 10 (Gatling a 50 parejas concurrentes) auditarán este riesgo. Si se detecta degradación de performance, se aislará el log de eventos del snapshot principal hacia la tabla `game_events`.

---

## 3. Riesgos de Integración (Backend - Frontend)

### R04: Incompatibilidad de Contratos Drag & Drop
* **Descripción:** El desarrollador Frontend envía el payload del arrastre de la carta (ej. `ATTACH_ENERGY`) con un formato JSON distinto al que espera el DTO de validación del Agente Backend, rompiendo la interacción principal del juego.
* **Mitigación Preventiva:** Aplicación estricta de la Matriz de Asignación por Feature (Archivo 09). La Célula de Infraestructura debe cerrar los DTOs de mensajería STOMP en la Semana 3, bloqueando firmas antes de que el Frontend inicie el desarrollo interactivo.
* **Plan de Contingencia:** Pruebas E2E automatizadas (Cypress) inyectadas en el pipeline de CI/CD para detectar disparidades de contratos antes del merge a `develop`.

### R05: Fallo de Integración de Efectos de Estado
* **Descripción:** Al delegar el cálculo del combate al patrón Chain of Responsibility y los efectos al patrón Strategy, el motor omite limpiar condiciones especiales transitorias o interrumpe erróneamente el turno.
* **Mitigación Preventiva:** Cobertura de JaCoCo obligatoria del 90% para el Core Crítico (Archivo 10), garantizando que los Edge Cases de KOs simultáneos e interrupciones por moneda (`CONFUSED`) estén probados unitariamente.
* **Plan de Contingencia:** Si un fallo de estado evade los tests y ocurre en producción, el log inmutable guardado en `game_events` permitirá reconstruir la trazabilidad paso a paso para depurar la falla lógica sin depender de reportes subjetivos del usuario.