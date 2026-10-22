package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = {ProdSecurityConfig.class, ProdSecurityConfigTest.TestConfig.class})
@ImportAutoConfiguration(WebMvcAutoConfiguration.class)
@ActiveProfiles("prod")
class ProdSecurityConfigTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public JwtAuthenticationFilter jwtAuthenticationFilter() {
            return mock(JwtAuthenticationFilter.class);
        }

    }

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void securityFilterChain_shouldBeConfigured() {
        assertNotNull(securityFilterChain);
    }
}
