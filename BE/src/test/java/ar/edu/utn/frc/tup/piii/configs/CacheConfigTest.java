package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.EmbeddedPostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest extends EmbeddedPostgresTestBase {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void cacheManager_shouldBeCaffeineInstance() {
        assertInstanceOf(CaffeineCacheManager.class, cacheManager);
    }

    @Test
    void cacheManager_shouldBeConfigured() {
        assertNotNull(cacheManager);
    }
}
