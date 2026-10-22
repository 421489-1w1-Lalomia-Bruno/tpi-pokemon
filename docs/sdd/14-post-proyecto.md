# 14 - Post-Proyecto y Roadmap

> Nota de Gestión: Este documento establece las directrices para la evolución del sistema posterior al lanzamiento de la versión 1.0.0 (Release Candidate). Define las características que quedan explícitamente fuera del alcance inicial y traza la hoja de ruta técnica para futuras iteraciones.

---

## Evolución de la Arquitectura (Fase 2)

Las siguientes mejoras estructurales se planifican para escalar el sistema una vez que la base de usuarios simultáneos supere la capacidad de la infraestructura inicial.

### 1. Sistema de Matchmaking Competitivo (ELO)
* **Estado Actual (V1):** Emparejamiento manual mediante un Lobby de salas públicas donde los jugadores eligen a qué partida unirse.
* **Propuesta (V2):** Implementar un algoritmo de emparejamiento automático basado en el puntaje de habilidad (ELO). Requiere agregar una tabla de clasificaciones (`leaderboards`) y un microservicio asíncrono que agrupe jugadores en cola según su rango y latencia.

### 2. Caché de Sesiones y Estado en Redis
* **Estado Actual (V1):** Los snapshots del tablero se escriben directamente en disco (PostgreSQL) usando formato `JSONB`.
* **Propuesta (V2):** Interponer un clúster de Redis para manejar el estado transitorio del `MatchSnapshot` en memoria RAM ultrarrápida. PostgreSQL pasará a utilizarse únicamente para el guardado definitivo al finalizar la partida o como backup periódico, aliviando la carga de I/O en la base de datos relacional.

### 3. Expansión del Motor de Reglas
* **Estado Actual (V1):** Soporte exclusivo para la generación de cartas del set XY1 y reglas clásicas de Pokémon-EX.
* **Propuesta (V2):** Escalar el patrón *Strategy* y la interfaz `AttackStep` para soportar mecánicas de generaciones posteriores (ej. Pokémon GX, VMAX, habilidades de un solo uso por partida y zonas de Descartado Perdido / Lost Zone).

---

## Monitorización y Observabilidad

Para operar el juego a gran escala, se debe reemplazar el monitoreo pasivo por un esquema de observabilidad proactiva.

### 1. Telemetría de WebSockets (Backend)
* **Implementación:** Integración de Spring Boot Actuator y Micrometer para exportar métricas en formato Prometheus.
* **Métricas Clave:** Rastreo en tiempo real de la cantidad de sesiones STOMP activas, el tiempo de procesamiento del pipeline de daño y la latencia del broadcast (Percentiles 95 y 99). Visualización centralizada mediante tableros de Grafana.

### 2. Trazabilidad de Errores en el Cliente (Frontend)
* **Implementación:** Integración de un SDK de monitoreo de errores (como Sentry o Datadog RUM) en la aplicación Angular.
* **Objetivo:** Capturar excepciones silenciosas de JavaScript, fallos de renderizado de Signals o problemas de desincronización del DOM en los navegadores de los usuarios sin depender de reportes manuales.

---

## Mejoras de Experiencia de Usuario (UX/UI)

### 1. Motor de Animaciones Avanzado
* **Estado Actual (V1):** Transiciones de estado limpias pero inmediatas, priorizando la estabilidad del control flow de Angular sobre el impacto visual.
* **Propuesta (V2):** Implementación de una capa de orquestación visual. Los mensajes de red (`STATE_UPDATE`) no actualizarán el tablero de golpe; en su lugar, se encolarán en el cliente para disparar animaciones CSS/Canvas (ej. proyectiles de daño, explosiones de cartas al sufrir KO) antes de resolver el estado final en el DOM.

### 2. Sistema de Comunicación en Partida
* **Estado Actual (V1):** Comunicación inhabilitada para prevenir toxicidad y reducir la complejidad de moderación.
* **Propuesta (V2):** Incorporación de una rueda de interacciones rápidas (Emotes/Pings) y mensajes de chat predefinidos ("¡Buena jugada!", "Paso mi turno"). Los payloads se transmitirán a través de un tópico STOMP paralelo (`/topic/chat`) para no saturar el hilo principal del motor de juego.

---

## Mantenimiento y Operaciones

### 1. Rotación de Secretos y Seguridad
* Establecer un ciclo automatizado para la rotación de las claves `JWT_SECRET` y credenciales de base de datos cada 90 días mediante herramientas de bóveda (como HashiCorp Vault o AWS Secrets Manager).

### 2. Estrategia de Purga de Datos
* **Política de Retención:** Los registros de la tabla `game_events` y los snapshots de partidas finalizadas (`game_states` en estado `FINISHED`) consumirán espacio masivo con el tiempo. Se debe implementar un CRON job que traslade las partidas de más de 30 días a un esquema de almacenamiento en frío (Cold Storage) o las elimine para mantener los índices de la base de datos óptimos.