# 11 - Seguridad

> Nota de Arquitectura: La seguridad de este proyecto asume un enfoque de "Confianza Cero" (Zero Trust). El Frontend (Angular) se considera un entorno completamente comprometido e inseguro. Toda validación, cálculo y visibilidad de datos se dictamina de forma estricta y exclusiva en el Backend (Spring Boot).

---

## Autenticación y Autorización

El sistema implementa un enfoque por fases para la autenticación, comenzando con un mecanismo temporal y migrando a JWT en semanas posteriores.

### Fase 1: Header `X-Player-Id` (Semanas 1-3)

Durante las primeras semanas de desarrollo, el `playerId` se pasa mediante el header HTTP `X-Player-Id` en todas las peticiones REST y se almacena en la sesión STOMP al conectar el WebSocket.

* Se implementa un `PlayerIdInterceptor` (HandlerInterceptor) que intercepta todas las peticiones a `/api/decks/**` y `/api/matches/**`.
* Extrae el valor del header `X-Player-Id` y lo inyecta como atributo del request (`request.setAttribute("playerId", playerId)`).
* Los controllers recuperan el playerId via `@RequestAttribute("playerId") String playerId`.
* Rechaza peticiones sin el header devolviendo `401 Unauthorized`.
* Para WebSocket, el `GameWebSocketHandler` almacena el `playerId` en la sesión STOMP al momento del `CONNECT`.

### Fase 2: JWT (Semana 5, implementación completa)

En la Semana 5 se reemplaza el sistema `X-Player-Id` por autenticación JWT completa con registro y login de usuarios.

#### Entidades

```java
public enum Gender {
    MALE,
    FEMALE,
    PREFER_NOT_TO_SAY,
    OTHER
}

@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {
    @Column(unique = true, nullable = false, length = 30)
    private String username;

    @Column(nullable = false)
    private String passwordHash;   // BCrypt

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    // Future: level, experience, avatarUrl, wins, losses, etc.
}
```

#### Endpoints de Autenticación

| Método | Path | Cuerpo | Respuesta (200) |
|---|---|---|---|
| POST | `/api/auth/register` | `{ username, password, gender }` | `{ token, user: { id, username, gender, createdAt } }` |
| POST | `/api/auth/login` | `{ username, password }` | `{ token, user: { id, username, gender, createdAt } }` |

**Validaciones de registro:**
- `username`: obligatorio, 3-30 caracteres, solo alfanumérico, único en BD
- `password`: obligatorio, mínimo 8 caracteres
- `gender`: opcional, válido: `MALE`, `FEMALE`, `PREFER_NOT_TO_SAY`, `OTHER`

#### Flujo de autenticación

1. **Registro:** `POST /api/auth/register` → crea `UserEntity` con password hasheado (BCrypt) → devuelve JWT + datos del usuario
2. **Login:** `POST /api/auth/login` → verifica password contra hash → devuelve JWT + datos del usuario
3. **Peticiones autenticadas:** El frontend envía `Authorization: Bearer <token>` en cada request
4. **JwtAuthenticationFilter** (OncePerRequestFilter):
   - Intercepta todas las peticiones excepto `/api/auth/**`
   - Extrae el token del header `Authorization`
   - Valida la firma y expiración mediante `JwtTokenProvider`
   - Extrae el `userId` del token y lo inyecta como `request.setAttribute("playerId", userId)`
   - Rechaza con `401 Unauthorized` si el token falta o es inválido
5. **Los controllers no cambian** — siguen usando `@RequestAttribute("playerId") String playerId`, solo que ahora el atributo lo inyecta el filtro JWT en lugar del `PlayerIdInterceptor`

#### Perfil de usuario

| Método | Path | Descripción |
|---|---|---|
| GET | `/api/users/me` | Devuelve datos del usuario + lista de sus mazos |

Requiere JWT. El `userId` se extrae del token para identificar al usuario.

#### Protección de WebSockets (STOMP Channel Interceptor)

Debido a las limitaciones del estándar de WebSockets en navegadores web para enviar headers HTTP de autorización, la validación se delega a la capa del protocolo STOMP:
* **Fase 1 (legado):** El cliente Angular adjuntaba el `playerId` en el header nativo `X-Player-Id` durante la trama `CONNECT` de STOMP.
* **Fase 2 (JWT):** El cliente Angular adjunta el JWT en el header nativo `Authorization` durante la trama `CONNECT` de STOMP.
* El `AuthChannelInterceptor` (existente) se actualiza para leer el header `Authorization`, validar el JWT y extraer el `userId` como `Principal`.
* Si el token es inválido o no existe, el servidor rechaza el handshake y cierra el socket inmediatamente.
* Al validar, el servidor asigna el `Principal` autenticado a la sesión STOMP para su posterior auditoría.

#### Migración de datos legados

Los datos existentes (mazos, partidas) usan `creator_id`, `player1_id`, etc. como strings arbitrarios (ej. "player-123"). Al implementar el sistema de usuarios:
- Los nuevos registros usarán el UUID del `UserEntity` como `creatorId`
- Los datos legacy con IDs tipo "player-xxx" coexisten sin problemas
- Se documenta que una tarea futura de limpieza debe migrar o eliminar estos registros legacy

---

## Prevención de Trampas (Anti-Cheat System)

Para mantener la integridad competitiva, el motor del juego implementa capas defensivas contra la inspección de red (Network Sniffing) y la falsificación de acciones.

### 1. Enmascaramiento de Datos (Data Masking)
Bajo ninguna circunstancia el motor transmitirá a un cliente información que su jugador no deba conocer.
* Cuando el `GameEngine` calcula un nuevo estado, genera el `MatchSnapshot`.
* Antes de enviarlo mediante la plantilla de mensajería (SimpMessagingTemplate), el servicio pasa el snapshot por un filtro de enmascaramiento.
* **El Filtro:** Muta la estructura de datos para la perspectiva del usuario destino. Las cartas en la `Hand`, `Deck` y `PrizeCards` del oponente se transforman en objetos DTO genéricos que solo contienen un ID ofuscado y un indicador visual de "carta boca abajo", eliminando atributos como el nombre, HP o ataques.
* Se envían mensajes individualizados a canales privados: `/user/{playerId}/queue/match`.

### 2. Validación de Identidad por Acción (Cross-Identity Protection)
El cliente Angular envía acciones a través del socket hacia el destino `/app/game.action`. El DTO recibido NO contiene `playerId` — se extrae del `Principal` autenticado en la sesión STOMP.
* **Regla estricta:** El `GameActionMessage` no tiene campo `playerId`. El `TurnManager` y el `RuleValidator` nunca confían en IDs pasados en el body.
* El sistema inyecta el `SimpMessageHeaderAccessor` en el handler. Se extrae el ID del usuario real desde el `Principal` autenticado en el nivel de transporte (o desde `X-Player-Id` en Fase 1).
* Si el `Principal` de la sesión no coincide con el jugador al que le corresponde actuar en el `TurnPhase` actual, el servidor bloquea la ejecución de forma silenciosa o emite un evento de error de autorización.

---

## Gestión de Secretos y Entorno

El código fuente (repositorio Git) no contendrá credenciales, contraseñas ni claves de APIs. Todo valor sensible se gestiona de forma externa y se inyecta en el contenedor de ejecución.

### Variables de Entorno Obligatorias
El archivo `application-prod.properties` exige las siguientes variables a nivel sistema operativo:
* `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`: Credenciales de conexión a PostgreSQL.
* `JWT_SECRET`: Cadena alfanumérica de alta entropía (mínimo 256 bits) para la firma de tokens.
* `POKEMON_TCG_API_KEY`: Clave de acceso a `pokemontcg.io` utilizada exclusivamente por el endpoint de sincronización del Administrador en la Fase de Setup.

---

## Prevención de Abusos y Seguridad Perimetral

### 1. Rate Limiting (Lobby y APIs REST)
Para mitigar ataques de Denegación de Servicio (DDoS) o spam en la creación de salas, se implementa un filtro limitador de tasa mediante `Bucket4j`.
* Restringe la creación de partidas (`POST /api/matches`) a un máximo de 5 peticiones por minuto por dirección IP / ID de usuario.
* Limita las consultas al buscador de cartas del Deck Builder para proteger los recursos de la base de datos local.

### 2. CORS (Cross-Origin Resource Sharing)
Para los entornos que no sean de desarrollo local, Spring Boot debe rechazar conexiones desde orígenes arbitrarios.
* Se configura `CorsConfigurationSource` permitiendo exclusivamente el dominio de producción donde se aloje el cliente de Angular 20 (ej. `https://pokemon-tcg-app.com`).
* La restricción de CORS aplica tanto para los controladores REST como para el handshake inicial del `WebSocketMessageBrokerConfigurer`.

### 3. Sanitización de Entradas (XSS y SQL Injection)
* **XSS:** Angular 20 neutraliza por defecto los ataques de Cross-Site Scripting escapando el contenido en las directivas de interpolación y control flow.
* **SQLi:** El uso estricto de Spring Data JPA con parámetros nombrados y la exclusión de consultas dinámicas en texto crudo previenen las inyecciones SQL en toda la capa de repositorios.