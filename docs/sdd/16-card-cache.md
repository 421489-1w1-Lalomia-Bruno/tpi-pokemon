# 16 - Card Cache Backend (P3): Especificación y Diseño Técnico

## 1. Alcance

Implementar en el backend Spring Boot un sistema de cache local con patrón **Cache-Aside** para el catálogo de cartas, con refresh programado desde la API externa cada 14 días (domingos a las 00:00).

1. **Cache en memoria** con Caffeine para respuestas rápidas de `/api/cards`
2. **Sincronización programada** vía `@Scheduled` que refresca datos y evicta el cache
3. **Persistencia de metadata** en tabla `sync_metadata` para rastrear última sincronización
4. **Cache warmup** al inicio de la aplicación para evitar cold-start en la primera consulta
5. **Sincronización manual** vía `POST /api/cards/sync` que evicta y refresca inmediatamente

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
│ @Cacheable("cards")                 │
│   listCards(filter, pageable)       │
│   getCardById(id)                   │
│ @Cacheable("cards-search")          │
│   searchCards(name)                 │
│ @CacheEvict(value, allEntries=true) │
│   syncCards(req)                    │
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
│ + syncCards(req)                    │
│   → persiste SyncMetadataEntity     │
│   → evicta cache                    │
│ + listCards(filter, pageable)       │
│ + getCardById(id)                   │
│ + searchCards(name)                 │
└──────────┬──────────────────────────┘
           │ inyecta
           ▼
┌─────────────────────────────────────┐
│         CacheConfig                 │
│─────────────────────────────────────│
│ @EnableCaching                      │
│ @Value maxCacheSize                 │
│ @Value cacheTtlDays                 │
├─────────────────────────────────────┤
│ + cacheManager(): CaffeineCacheMgr  │
│   → TTL configurable                │
│   → max-size configurable           │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│       CardSyncScheduler             │
│─────────────────────────────────────│
│ - cardService: CardService          │
│ - syncMetaRepo: SyncMetadataRepo    │
│ - intervalDays: int (14)            │
├─────────────────────────────────────┤
│ + scheduledSync()  @Scheduled(cron) │
│   → verifica last_sync_at           │
│   → si ≥ 14 días → syncCards()      │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│       CacheWarmupRunner             │
│─────────────────────────────────────│
│ - cardService: CardService          │
│ @Order(2)                           │
├─────────────────────────────────────┤
│ + run()  ApplicationRunner          │
│   → carga catalogo en cache         │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│       SyncMetadataEntity            │
│─────────────────────────────────────│
│ extends BaseEntity                  │
│ - setId: String                     │
│ - lastSyncAt: LocalDateTime         │
│ - syncedCount: Integer              │
└──────────┬──────────────────────────┘
           │ mapea
           ▼
┌─────────────────────────────────────┐
│     SyncMetadataRepository          │
│─────────────────────────────────────│
│ extends JpaRepository               │
├─────────────────────────────────────┤
│ + findBySetId(setId): Optional      │
└─────────────────────────────────────┘
```

---

## 3. Flujo de Ejecución

### 3.1 Read — Cache-Aside

```
Cliente     CardController    CardServiceImpl    Cache (Caffeine)    CardRepository
  │              │                  │                    │                  │
  │─GET /cards──►│                  │                    │                  │
  │              │─listCards()─────►│                    │                  │
  │              │                  │─@Cacheable("cards")│                  │
  │              │                  ├───────────────────►│                  │
  │              │                  │                    │  HIT → return    │
  │              │                  │◄───────────────────│                  │
  │              │◄─Page<CardDTO>──│                    │                  │
  │◄─200 + Page │                  │                    │                  │
  │              │                  │                    │                  │
  │              │                  │  MISS              │                  │
  │              │                  ├───────────────────►│                  │
  │              │                  │                    │  MISS            │
  │              │                  │                    ├─────────────────►│
  │              │                  │                    │◄─Page<Entity>───│
  │              │                  │                    │  → store in cache│
  │              │                  │◄─Page<CardDTO>─────│                  │
  │              │◄─Page<CardDTO>──│                    │                  │
  │◄─200 + Page │                  │                    │                  │
```

### 3.2 Scheduled Sync — Refresh cada 14 días

```
Scheduler     CardSyncScheduler    CardServiceImpl    PokemonTcgApiClient    DB
  │              │                     │                    │                │
  │─cron 00:00─►│                     │                    │                │
  │  (domingo)  │                     │                    │                │
  │              │─shouldSync()──────►│                    │                │
  │              │  check metadata    │                    │                │
  │              │◄───────────────────│                    │                │
  │              │  (≥14 days? YES)   │                    │                │
  │              │                     │                    │                │
  │              │─syncCards()───────►│                    │                │
  │              │                     │─fetchCardsBySet()─►│                │
  │              │                     │◄───cards──────────│                │
  │              │                     │─saveAll()─────────────────────────►│
  │              │                     │─save metadata─────────────────────►│
  │              │                     │─@CacheEvict()───► evict all caches │
  │              │◄─SyncResponseDTO───│                    │                │
  │              │  sync completed     │                    │                │
```

### 3.3 Startup — Warmup del Cache

```
Startup     DataInitializer    CacheWarmupRunner    CardService    Cache    DB
  │              │                    │                  │            │       │
  │─run()──────►│                    │                  │            │       │
  │ @Order(1)   │                    │                  │            │       │
  │              │─count()─────────────────────────────────────────────────►│
  │              │◄─0 (DB vacía)     │                  │            │       │
  │              │─syncCards()───────►│                  │            │       │
  │              │  (sync desde API) │                  │            │       │
  │              │                    │                  │            │       │
  │              │                    │─run()────────────│            │       │
  │              │                    │ @Order(2)        │            │       │
  │              │                    │─listCards()─────►│            │       │
  │              │                    │                  │─@Cacheable │       │
  │              │                    │                  │  MISS ────►│       │
  │              │                    │                  │◄──────────│       │
  │              │                    │                  │  → store   │       │
  │              │                    │◄─catalog cached ─│            │       │
  │              │  cache warmed      │                  │            │       │
```

---

## 4. Configuración

### 4.1 application.properties

```properties
##### Cache Configuration (Caffeine) #####
spring.cache.type=caffeine
app.cache.cards.max-size=500
app.cache.cards.ttl-days=14

##### Scheduler Configuration #####
app.sync.cron=0 0 0 ? * SUN
app.sync.interval-days=14
```

### 4.2 pom.xml

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 4.3 Migración V3

```sql
-- V3__create_sync_metadata.sql
CREATE TABLE sync_metadata (
    id UUID PRIMARY KEY,
    set_id VARCHAR(20) NOT NULL UNIQUE,
    last_sync_at TIMESTAMP NOT NULL,
    synced_count INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_sync_metadata_set_id ON sync_metadata(set_id);
```

---

## 5. Especificación de Componentes

### 5.1 CacheConfig

**Paquete:** `ar.edu.utn.frc.tup.piii.configs`

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${app.cache.cards.max-size}")
    private int maxCacheSize;

    @Value("${app.cache.cards.ttl-days}")
    private int cacheTtlDays;

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(maxCacheSize)
                .expireAfterWrite(Duration.ofDays(cacheTtlDays)));
        return cacheManager;
    }
}
```

### 5.2 SchedulingConfig

**Paquete:** `ar.edu.utn.frc.tup.piii.configs`

```java
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
```

### 5.3 CardSyncScheduler

**Paquete:** `ar.edu.utn.frc.tup.piii.scheduler`

```java
@Component
public class CardSyncScheduler {

    private final CardService cardService;
    private final SyncMetadataRepository syncMetadataRepository;

    @Value("${app.sync.interval-days}")
    private int intervalDays;

    @Scheduled(cron = "${app.sync.cron}")
    public void scheduledSync() {
        if (shouldSync()) {
            cardService.syncCards(new SyncRequestDTO());
        }
    }

    private boolean shouldSync() {
        Optional<SyncMetadataEntity> metadata =
            syncMetadataRepository.findBySetId("xy1");
        if (metadata.isEmpty()) return true;

        LocalDateTime lastSync = metadata.get().getLastSyncAt();
        return ChronoUnit.DAYS.between(lastSync, LocalDateTime.now()) >= intervalDays;
    }
}
```

### 5.4 CacheWarmupRunner

**Paquete:** `ar.edu.utn.frc.tup.piii.scheduler`

```java
@Component
@Order(2)
public class CacheWarmupRunner implements ApplicationRunner {

    private final CardService cardService;

    @Override
    public void run(ApplicationArguments args) {
        cardService.listCards(new CardFilterDTO(), PageRequest.of(0, 200));
    }
}
```

### 5.5 SyncMetadataEntity

**Paquete:** `ar.edu.utn.frc.tup.piii.entities`

```java
@Entity
@Table(name = "sync_metadata")
@Getter
@Setter
@NoArgsConstructor
public class SyncMetadataEntity extends BaseEntity {

    @Column(name = "set_id", nullable = false, unique = true, length = 20)
    private String setId;

    @Column(name = "last_sync_at", nullable = false)
    private LocalDateTime lastSyncAt;

    @Column(name = "synced_count", nullable = false)
    private Integer syncedCount;
}
```

### 5.6 SyncMetadataRepository

**Paquete:** `ar.edu.utn.frc.tup.piii.repositories`

```java
public interface SyncMetadataRepository extends JpaRepository<SyncMetadataEntity, UUID> {
    Optional<SyncMetadataEntity> findBySetId(String setId);
}
```

### 5.7 Modificaciones en CardServiceImpl

**Paquete:** `ar.edu.utn.frc.tup.piii.services`

```java
@Service
public class CardServiceImpl implements CardService {

    private final SyncMetadataRepository syncMetadataRepository;

    @Override
    @Cacheable(value = "cards", key = "#filter.toString() + '-' + #pageable")
    public Page<CardDTO> listCards(CardFilterDTO filter, Pageable pageable) {
        // ... existing implementation
    }

    @Override
    @Cacheable(value = "cards", key = "#id")
    public CardDTO getCardById(UUID id) {
        // ... existing implementation
    }

    @Override
    @Cacheable(value = "cards-search", key = "#name")
    public List<CardDTO> searchCards(String name) {
        // ... existing implementation
    }

    @Override
    @Transactional
    @CacheEvict(value = {"cards", "cards-search"}, allEntries = true)
    public SyncResponseDTO syncCards(SyncRequestDTO request) {
        // ... existing sync logic

        // Persist sync metadata
        SyncMetadataEntity metadata = new SyncMetadataEntity();
        metadata.setSetId(request.getSetId());
        metadata.setLastSyncAt(LocalDateTime.now());
        metadata.setSyncedCount(totalSynced);
        syncMetadataRepository.save(metadata);

        return new SyncResponseDTO(totalSynced);
    }
}
```

> **Nota sobre upsert:** Dado que `set_id` tiene constraint `UNIQUE`, si ya existe un registro para el mismo `setId`, se debe hacer un UPDATE en lugar de INSERT. La lógica de upsert se maneja con `syncMetadataRepository.findBySetId()` → si existe, actualizar; si no, insertar.

---

## 6. Reglas de Cache

| Método | Cache Name | Cache Key | Eviction |
|---|---|---|---|
| `listCards(filter, pageable)` | `cards` | `filter.toString() + '-' + pageable` | `syncCards()` evict all |
| `getCardById(id)` | `cards` | `id` (UUID) | `syncCards()` evict all |
| `searchCards(name)` | `cards-search` | `name` (String) | `syncCards()` evict all |
| `syncCards(request)` | — | — | `@CacheEvict(allEntries=true)` en ambos caches |

### 6.1 TTL y Eviction

- **TTL:** 14 días (`expireAfterWrite`) — las entradas expiran automáticamente después de 14 días
- **Max size:** 500 entradas — si se alcanza el límite, Caffeine evicta las entradas menos usadas (LRU)
- **Eviction manual:** `syncCards()` evicta todas las entradas de ambos caches inmediatamente

### 6.2 Cache Key para CardFilterDTO

`CardFilterDTO` usa Lombok `@Data` que genera `equals()`/`hashCode()` incluyendo `page` y `size`. Esto es correcto: cada combinación de filtro + paginación tiene su propia entrada en cache.

---

## 7. Manejo de Errores

| Escenario | Comportamiento |
|---|---|
| API externa down durante scheduled sync | Log error, cache sigue sirviendo datos stale |
| API externa down durante startup sync | `DataInitializer` catch exception, cache se calienta con DB existente |
| DB vacía y API down | Cache vacío, endpoints retornan 0 resultados |
| Cache lleno (500 entradas) | Caffeine evicta LRU automáticamente |
| Entrada expirada (TTL 14d) | Caffeine evicta, próxima lectura hace MISS → DB → cache |

---

## 8. Distribución de Archivos

### Nuevos (6 archivos)

```
configs/
├── CacheConfig.java
└── SchedulingConfig.java

entities/
└── SyncMetadataEntity.java

repositories/
└── SyncMetadataRepository.java

scheduler/
├── CacheWarmupRunner.java
└── CardSyncScheduler.java
```

### Modificados (3 archivos)

```
services/CardServiceImpl.java       → agregar @Cacheable, @CacheEvict, SyncMetadata persistence
configs/DataInitializer.java        → agregar @Order(1)
application.properties              → agregar propiedades de cache y scheduler (ya hecho)
```

### Configuración (ya hecha)

```
pom.xml                             → spring-boot-starter-cache + caffeine (ya hecho)
db/migration/V3__create_sync_metadata.sql → tabla sync_metadata (ya hecho)
```

---

## 9. Tasks de Implementación

- [x] T0: Agregar dependencias de Caffeine en `pom.xml`
- [x] T0: Agregar propiedades de cache y scheduler en `application.properties`
- [x] T0: Crear migración `V3__create_sync_metadata.sql`
- [x] T0: Agregar `@Order(1)` a `DataInitializer.java`
- [ ] T1: `CacheConfig` — `@EnableCaching`, `CaffeineCacheManager` con TTL y max-size
- [ ] T2: `SchedulingConfig` — `@EnableScheduling`
- [ ] T3: `SyncMetadataEntity` — entidad JPA extendiendo `BaseEntity`
- [ ] T4: `SyncMetadataRepository` — `findBySetId()`
- [ ] T5: `CardSyncScheduler` — `@Scheduled(cron)` con verificación de 14 días
- [ ] T6: `CacheWarmupRunner` — `ApplicationRunner` con `@Order(2)`
- [ ] T7: `CardServiceImpl` — agregar `@Cacheable` en read methods, `@CacheEvict` en `syncCards()`, persistir `SyncMetadataEntity`
- [ ] T8: Tests unitarios para `CardSyncScheduler`, `CacheConfig`, `CardServiceCacheTest`, `CacheWarmupRunnerTest`

---

## 10. Dependencias

### Dependencias de implementación

| Tarea | Depende de |
|---|---|
| T1-T6 | `feature/cards-sync-be` (ya mergeada a `develop`) |
| T7 | T1-T6 completadas |
| T8 | T7 completada |

### Dependencias con otras ramas

- `feature/card-cache-be` depende de `feature/cards-sync-be` (ya mergeada)
- No hay conflicto con `feature/deck-builder-be` (P1/P2 trabajan en mazos, no en cartas)
- `CardServiceImpl` es modificado por P3 — P2 ya mergeó su trabajo, no hay conflicto
- `application.properties` y `pom.xml` ya fueron preparados por P1 en `develop`

### Orden de integración

1. P1 mergea dependencias de Caffeine en `pom.xml` y `application.properties` (ya hecho en `develop`)
2. P3 crea `feature/card-cache-be` desde `develop` actualizado
3. P3 implementa T1-T7
4. P3 implementa T8 (tests)
5. P3 hace PR a `develop` (viernes)
6. Review cruzado → merge a `develop` (sábado)
