# 15 - Card Sync Backend (P2): Especificación y Diseño Técnico

## 1. Alcance

Implementar en el backend Spring Boot:
1. **Cliente HTTP externo** para consumir `pokemontcg.io` v2 mediante `POKEMON_TCG_API_KEY`
2. **Sincronización batch** del set XY1 via `POST /api/cards/sync`
3. **Consulta de catálogo** via endpoints GET con paginación, filtros y búsqueda
4. **Mapeo** de respuestas API ↔ entidades JPA ↔ DTOs de salida
5. **Auto-sync** al iniciar la app si la DB está vacía + persistencia en archivo H2

---

## 2. Diagrama de Clases

```
┌─────────────────────────────────────┐
│         CardController              │
│─────────────────────────────────────│
│ - cardService: CardService          │
├─────────────────────────────────────┤
│ + listCards(...): Page<CardDTO>     │
│ + getCardById(id): CardDTO          │
│ + searchCards(name): List<CardDTO>  │
│ + syncCards(req): SyncResponseDTO   │
└──────────┬──────────────────────────┘
           │ inyecta
           ▼
┌─────────────────────────────────────┐
│      <<interface>> CardService      │
├─────────────────────────────────────┤
│ syncCards(req) → SyncResponseDTO    │
│ listCards(filter, pageable) → Page  │
│ getCardById(id) → CardDTO           │
│ searchCards(name) → List<CardDTO>   │
└──────────┬──────────────────────────┘
           │ implementa
           ▼
┌─────────────────────────────────────┐
│         CardServiceImpl             │
│─────────────────────────────────────│
│ - cardRepo: CardRepository          │
│ - cardMapper: CardMapper            │
│ - apiClient: PokemonTcgApiClient    │
│ - syncMetaRepo: SyncMetadataRepo    │
├─────────────────────────────────────┤
│ @Cacheable("cards")                 │
│   listCards(filter, pageable)       │
│   getCardById(id)                   │
│ @Cacheable("cards-search")          │
│   searchCards(name)                 │
│ @CacheEvict(value, allEntries=true) │
│   syncCards(req)                    │
└──────────┬──────────────────────────┘
           │ inyecta
           ▼
┌─────────────────────────────────────┐
│       PokemonTcgApiClient           │
│─────────────────────────────────────│
│ - restTemplate: RestTemplate        │
│ - baseUrl: String                   │
├─────────────────────────────────────┤
│ + fetchCardsBySet(setId): Response  │
└──────────┬──────────────────────────┘
           │ usa
           ▼
┌─────────────────────────────────────┐
│    RestTemplate (bean config)       │
│─────────────────────────────────────│
│ interceptors: [X-Api-Key]           │
│ factory: connect=5s, read=30s       │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│          CardMapper                 │
│─────────────────────────────────────│
│ - objectMapper: ObjectMapper        │
├─────────────────────────────────────┤
│ + toDTO(entity) → CardDTO           │
│ + toEntity(api) → CardEntity        │
│ - parseAttacksFromJson()            │
│ - parseAbilitiesFromJson()          │
│ - parseHp(), deriveStage()          │
│ - parseDamage(), writeJson()        │
└─────────────────────────────────────┘
```

---

## 3. Especificación de Contratos REST

### 3.1 POST /api/cards/sync

```
Request:  { "setId": "xy1" }
Response 200:  { "synced": 146 }
Response 400:  ErrorApi (Bad Request)
Response 502:  ErrorApi ("External API unavailable")
```

### 3.2 GET /api/cards

```
GET /api/cards?page=0&size=20&type=FIRE&stage=BASIC&supertype=Pokémon&setId=xy1&name=char
Response 200:  Page<CardDTO>
```

### 3.3 GET /api/cards/{id}

```
GET /api/cards/{uuid}
Response 200:  CardDTO
Response 404:  ErrorApi (Not Found)
```

### 3.4 GET /api/cards/search

```
GET /api/cards/search?name=charizard
Response 200:  [CardDTO, ...]
```

---

## 4. DTOs

### 4.1 CardDTO

| Campo | Tipo | Descripción |
|---|---|---|
| id | String (UUID) | ID interno |
| externalId | String | ID de API externa |
| name | String | Nombre de la carta |
| supertype | String | "Pokémon", "Trainer", "Energy" |
| subtypes | List<String> | ["Basic","EX"] |
| hp | Integer | null para Trainer/Energy |
| types | Set<EnergyType> | [FIRE, COLORLESS] |
| stage | PokemonStage | BASIC, STAGE1, etc. |
| evolvesFrom | String | Nombre pre-evolución |
| attacks | List<AttackDTO> | Ataques parseados |
| abilities | List<AbilityDTO> | Habilidades parseadas |
| weaknessType | String | Tipo de debilidad |
| resistanceType | String | Tipo de resistencia |
| retreatCost | Integer | Costo de retirada |
| imageUrl | String | URL imagen grande |
| rarity | String | Rareza |
| cardNumber | String | Número en el set |
| setId | String | "xy1" |

### 4.2 AttackDTO

| Campo | Tipo | Descripción |
|---|---|---|
| name | String | Nombre del ataque |
| cost | List<EnergyType> | Costo de energía |
| damage | int | Daño base (0 si solo efecto) |
| effectText | String | Texto del efecto (nullable) |

### 4.3 AbilityDTO

| Campo | Tipo | Descripción |
|---|---|---|
| name | String | Nombre de la habilidad |
| type | AbilityType | POKE_BODY o POKE_POWER |
| effectText | String | Texto del efecto |

### 4.4 CardFilterDTO

| Campo | Tipo | Descripción |
|---|---|---|
| name | String | Búsqueda case-insensitive |
| type | EnergyType | Filtro por tipo |
| stage | PokemonStage | Filtro por etapa |
| supertype | String | "Pokémon", "Trainer", "Energy" |
| setId | String | "xy1" |
| page | Integer | Default 0 |
| size | Integer | Default 20 |

### 4.5 SyncRequestDTO / SyncResponseDTO

```java
public class SyncRequestDTO {
    @NotBlank String setId = "xy1";
}
public class SyncResponseDTO {
    int synced;
}
```

---

## 5. Especificación de Componentes

### 5.1 PokemonTcgApiClient

**Paquete:** `ar.edu.utn.frc.tup.piii.clients`

```java
@Component
public class PokemonTcgApiClient {
    // Inyecta RestTemplate (qualifier "pokemonTcgRestTemplate") + baseUrl
    // Método único:
    PokemonTcgResponse fetchCardsBySet(String setId);
    
    // GET {baseUrl}/cards?q=set.id:{setId}
    // Headers: X-Api-Key: {apiKey}
    // Errores: HttpClientErrorException/HttpServerErrorException → ExternalApiException
    //          ResourceAccessException → ExternalApiException
}
```

**DTOs internos** (clases estáticas anidadas):
- `PokemonTcgResponse` → `data` (List), `totalCount`, `page`, `pageSize`, `count`
- `PokemonTcgCardResponse` → id, name, supertype, subtypes, hp, types, evolvesFrom, attacks, abilities, weaknesses, resistances, convertedRetreatCost, set, number, rarity, images, rules

**DTOs externos** (`clients/dtos/`):
- `AttackApiDTO` → name, cost, convertedEnergyCost, damage, text
- `AbilityApiDTO` → name, text, type
- `WeakResistanceDTO` → type, value
- `CardSetImagesDTO` → id, name, series, small, large

### 5.2 CardService / CardServiceImpl

**Paquete:** `ar.edu.utn.frc.tup.piii.services`

```
interface CardService:
  SyncResponseDTO syncCards(SyncRequestDTO request)
  Page<CardDTO> listCards(CardFilterDTO filter, Pageable pageable)
  CardDTO getCardById(UUID id)
  List<CardDTO> searchCards(String name)
```

**syncCards() — Reglas:**
1. Consumir `PokemonTcgApiClient.fetchCardsBySet(setId)`
2. Para cada carta: verificar si existe por `externalId` → skip si existe, mapear si no
3. `CardRepository.saveAll()` en batch
4. Persistir `SyncMetadataEntity` con `last_sync_at` y `synced_count`
5. Retornar cantidad de nuevas cartas
6. `@Transactional`
7. `@CacheEvict(value = {"cards", "cards-search"}, allEntries = true)`

**Auto-sync — DataInitializer (`ApplicationRunner` con `@Order(1)`):**
- Si `cardRepository.count() == 0`, ejecutar `syncCards(new SyncRequestDTO())`
- Con try-catch para no bloquear startup si falla
- Ver [16-card-cache.md](16-card-cache.md) para detalles del cache-aside y `CacheWarmupRunner`

**listCards() — Reglas:**
- Construir `Specification<CardEntity>` dinámica con filtros AND
- `CardRepository.findAll(spec, pageable)`
- Mapear a `Page<CardDTO>`

**getCardById() — Reglas:**
- Buscar por UUID → `EntityNotFoundException` si no existe

**searchCards() — Reglas:**
- `CardRepository.findByNameContainingIgnoreCase(name)`

### 5.3 CardController

**Paquete:** `ar.edu.utn.frc.tup.piii.controllers`

```
@RestController @RequestMapping("/api/cards")
  GET  /                 → listCards(pageable, type, stage, supertype, setId, name)
  GET  /{id}             → getCardById(@PathVariable UUID id)
  GET  /search           → searchCards(@RequestParam String name)
  POST /sync             → syncCards(@Valid @RequestBody SyncRequestDTO)
```

### 5.4 CardMapper

**Paquete:** `ar.edu.utn.frc.tup.piii.mappers`

Mapeo manual usando `ObjectMapper` (no ModelMapper):

| Método | Descripción |
|---|---|
| `toDTO(CardEntity)` | Entity → CardDTO (con parseo de JSON anidados) |
| `toDTOList(List<CardEntity>)` | Lista de entities → lista de DTOs |
| `toEntity(PokemonTcgCardResponse)` | API response → Entity (mapeo campo por campo) |
| `toEntityList(List<PokemonTcgCardResponse>)` | Lista de API responses → lista de entities |
| `parseDamage(AttackApiDTO)` | API Attack → AttackDTO (con manejo de `+`, `×`, vacío) |
| `deriveStage(subtypes, supertype)` | Subtipos → PokemonStage |

**Parseo de damage:**

| API `damage` | damage (int) | effectText |
|---|---|---|
| `"60"` | 60 | null |
| `""` (sin text) | 0 | null |
| `""` (con text) | 0 | text |
| `"120+"` | 120 | `"120+"` |
| `"40×"` | 0 | `"40×" + text` |

**Derivación de PokemonStage:**

```
if supertype != "Pokémon" → null
if subtypes contains "MEGA" → MEGA
if subtypes contains "EX" → EX
if subtypes contains "Stage 2" → STAGE2
if subtypes contains "Stage 1" → STAGE1
if subtypes contains "Basic" → BASIC
```

### 5.5 RestTemplateConfig

```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate pokemonTcgRestTemplate(
            @Value("${pokemon.api.key}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 5s
        factory.setReadTimeout(30000);     // 30s
        RestTemplate rt = new RestTemplate(factory);
        rt.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-Api-Key", apiKey);
            return execution.execute(request, body);
        });
        return rt;
    }
}
```

### 5.6 ExternalApiException

**Paquete:** `ar.edu.utn.frc.tup.piii.clients.exceptions`

```java
public class ExternalApiException extends RuntimeException {
    private final Integer statusCode;
    // Constructores: mensaje, causa, statusCode opcional
}
```

Manejada en `GlobalExceptionHandler` → HTTP 502 Bad Gateway.

---

## 6. Reglas de Mapeo Formal

### API → Entity

| API Response | Entity | Transformación |
|---|---|---|
| `.id` | `.externalId` | identity |
| `.name` | `.name` | identity |
| `.supertype` | `.supertype` | identity |
| `.subtypes` | `.subtypes` | identity (List<String>) |
| `.hp` | `.hp` | parseIntOrNull |
| `.types` | `.types` | toUpperCase → Set<EnergyType> |
| `.evolvesFrom` | `.evolvesFrom` | identity |
| `.attacks` | `.attacksJson` | writeJson |
| `.abilities` | `.abilitiesJson` | writeJson |
| `.weaknesses[0].type` | `.weaknessType` | firstOrNull |
| `.resistances[0].type` | `.resistanceType` | firstOrNull |
| `.convertedRetreatCost` | `.retreatCost` | identity |
| `.images.large` | `.imageUrl` | identity |
| `.rarity` | `.rarity` | identity |
| `.number` | `.cardNumber` | identity |
| `.set.id` | `.setId` | identity |

### Entity → DTO

| Entity | DTO | Transformación |
|---|---|---|
| `.id.toString()` | `.id` | uuidToString |
| `.externalId` | `.externalId` | identity |
| `.name` | `.name` | identity |
| `.supertype` | `.supertype` | identity |
| `.subtypes` | `.subtypes` | identity |
| `.hp` | `.hp` | identity |
| `.types` | `.types` | identity |
| `.stage` | `.stage` | identity |
| `.evolvesFrom` | `.evolvesFrom` | identity |
| `.attacksJson` | `.attacks` | parseAttacksFromJson |
| `.abilitiesJson` | `.abilities` | parseAbilitiesFromJson |
| `.weaknessType` | `.weaknessType` | identity |
| `.resistanceType` | `.resistanceType` | identity |
| `.retreatCost` | `.retreatCost` | identity |
| `.imageUrl` | `.imageUrl` | identity |
| `.rarity` | `.rarity` | identity |
| `.cardNumber` | `.cardNumber` | identity |
| `.setId` | `.setId` | identity |

---

## 7. Manejo de Errores

| Excepción | HTTP Status |
|---|---|
| `EntityNotFoundException` | 404 |
| `ExternalApiException` | 502 |
| `MethodArgumentNotValidException` | 400 |
| `IllegalArgumentException` | 400 |
| Cualquier otra | 500 |

---

## 8. Diagramas de Secuencia

### POST /api/cards/sync

```
Cliente     CardController    CardServiceImpl    PokemonTcgApiClient    CardRepository
  │              │                  │                    │                   │
  │─POST /sync──►│                  │                    │                   │
  │              │─syncCards()─────►│                    │                   │
  │              │                  │─fetchCardsBySet()─►│                   │
  │              │                  │                    │─GET /v2/cards────►│
  │              │                  │                    │◄───JSON response──│
  │              │                  │◄───List<Cards>─────│                   │
  │              │                  │                                     │
  │              │                  │ loop [cada carta]                   │
  │              │                  │─findByExternalId()─────────────────►│
  │              │                  │◄──Optional.empty()─────────────────│
  │              │                  │                                     │
  │              │                  │─toEntity() (CardMapper)             │
  │              │                  │                                     │
  │              │                  │─saveAll(newCards)──────────────────►│
  │              │                  │◄──List<CardEntity>─────────────────│
  │              │◄─SyncResponseDTO─│                                     │
  │◄─200 {146}──│                  │                                     │
```

### GET /api/cards

```
Cliente     CardController    CardServiceImpl    CardRepository
  │              │                  │                  │
  │─GET /api────►│                  │                  │
  │  ?type=FIRE │─listCards()─────►│                  │
  │             │                  │─buildSpec()       │
  │             │                  │─findAll(spec,p)──►│
  │             │                  │◄─Page<CardEntity>│
  │             │                  │─toDTOPage()       │
  │             │◄─Page<CardDTO>──│                  │
  │◄─200 + Page│                  │                  │
```

---

## 9. Distribución de Archivos

### Nuevos (11 archivos)

```
clients/
├── PokemonTcgApiClient.java
├── dtos/
│   ├── AttackApiDTO.java
│   ├── AbilityApiDTO.java
│   ├── WeakResistanceDTO.java
│   └── CardSetImagesDTO.java
└── exceptions/
    └── ExternalApiException.java

dtos/card/
├── SyncRequestDTO.java
└── SyncResponseDTO.java
```

### Modificados (10 archivos)

```
configs/RestTemplateConfig.java       → agregar bean RestTemplate
configs/DataInitializer.java          → ApplicationRunner con @Order(1) para auto-sync
controllers/CardController.java       → agregar 4 endpoints
services/CardService.java             → agregar 4 métodos en interfaz
services/CardServiceImpl.java         → implementar lógica + @Cacheable + @CacheEvict
mappers/CardMapper.java               → agregar toDTO/toEntity/parseDamage/deriveStage
dtos/card/CardDTO.java                → poblar 17 campos
dtos/card/AttackDTO.java              → poblar 4 campos
dtos/card/AbilityDTO.java             → poblar 3 campos
dtos/card/CardFilterDTO.java          → poblar 7 campos
exceptions/GlobalExceptionHandler.java → agregar handler ExternalApiException → 502
```

### Modificados (configuración)

```
application.properties                → H2 file en vez de mem
.gitignore                            → agregar BE/data/
```

---

## 10. Configuración de Persistencia y Auto-sync

### H2 en archivo (application.properties)

```properties
spring.datasource.url=jdbc:h2:file:./data/pokemon_tcg;DB_CLOSE_ON_EXIT=FALSE
```

Los datos persisten entre reinicios en `BE/data/pokemon_tcg.mv.db`.

### Auto-sync al iniciar (DataInitializer)

```java
@Component
@Order(1)
public class DataInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        if (cardRepository.count() == 0) {
            try {
                cardService.syncCards(new SyncRequestDTO());
                log.info("Card sync completed on startup");
            } catch (Exception e) {
                log.error("Auto-sync failed: {}", e.getMessage());
            }
        }
    }
}
```

> **Nota:** El cache se calienta inmediatamente después mediante `CacheWarmupRunner` (`@Order(2)`). Ver [16-card-cache.md](16-card-cache.md) para detalles del patrón Cache-Aside.

### .gitignore

```
### H2 Database files ###
BE/data/
**/data/*.mv.db
**/data/*.trace.db
```

---

## 11. Tasks de Implementación

### Pendientes (implementadas)

- [x] T1: RestTemplateConfig — bean con timeouts e interceptor X-Api-Key
- [x] T2: DTOs de API externa (AttackApiDTO, AbilityApiDTO, WeakResistanceDTO, CardSetImagesDTO)
- [x] T3: ExternalApiException
- [x] T4: PokemonTcgApiClient con fetchCardsBySet
- [x] T5: DTOs del sistema (CardDTO, AttackDTO, AbilityDTO, CardFilterDTO, SyncRequestDTO, SyncResponseDTO)
- [x] T6: CardMapper con toDTO, toEntity, parseDamage, deriveStage
- [x] T7: CardService + CardServiceImpl con métodos sync, list, getById, search + autoSyncOnStartup
- [x] T8: CardController con 4 endpoints REST
- [x] T9: GlobalExceptionHandler para ExternalApiException → 502
- [x] T10: H2 file + .gitignore

### Dependencias externas

- API key: `POKEMON_TCG_API_KEY` (o usar default en properties)
- API base URL: `https://api.pokemontcg.io/v2`
