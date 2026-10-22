package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.EmbeddedPostgresTestBase;
import ar.edu.utn.frc.tup.piii.dtos.card.SyncRequestDTO;
import ar.edu.utn.frc.tup.piii.repositories.CardRepository;
import ar.edu.utn.frc.tup.piii.services.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OtherConfigBeansTest extends EmbeddedPostgresTestBase {

    @Nested
    class MappersConfigTest {

        @Autowired
        private ModelMapper modelMapper;

        @Autowired
        @Qualifier("mergerMapper")
        private ModelMapper mergerMapper;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void modelMapper_shouldExist() {
            assertNotNull(modelMapper);
        }

        @Test
        void mergerMapper_shouldHaveIsNotNullCondition() {
            assertNotNull(mergerMapper);
            assertEquals(Conditions.isNotNull(), mergerMapper.getConfiguration().getPropertyCondition());
        }

        @Test
        void objectMapper_shouldHandleJavaTimeTypes() throws Exception {
            assertNotNull(objectMapper);
            LocalDate date = LocalDate.of(2025, 6, 16);
            String json = objectMapper.writeValueAsString(date);
            LocalDate deserialized = objectMapper.readValue(json, LocalDate.class);
            assertEquals(date, deserialized);
        }
    }

    @Nested
    class PasswordEncoderConfigTest {

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Test
        void passwordEncoder_shouldBeBCryptInstance() {
            assertNotNull(passwordEncoder);
            assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
        }

        @Test
        void passwordEncoder_shouldEncodeAndMatch() {
            String raw = "testPassword123";
            String encoded = passwordEncoder.encode(raw);
            assertNotEquals(raw, encoded);
            assertTrue(passwordEncoder.matches(raw, encoded));
            assertFalse(passwordEncoder.matches("wrongPassword", encoded));
        }
    }

    @Nested
    class RestTemplateConfigTest {

        private static final String TEST_API_KEY = "test-key-123";

        private final RestTemplateConfig config = new RestTemplateConfig();

        private static SimpleClientHttpRequestFactory unwrapFactory(RestTemplate restTemplate) {
            Object factory = restTemplate.getRequestFactory();
            while (factory instanceof AbstractClientHttpRequestFactoryWrapper wrapper) {
                factory = wrapper.getDelegate();
            }
            return (SimpleClientHttpRequestFactory) factory;
        }

        @Test
        void shouldCreateRestTemplate() {
            assertNotNull(config.pokemonTcgRestTemplate(TEST_API_KEY));
        }

        @Test
        void shouldUseSimpleClientHttpRequestFactory() {
            RestTemplate restTemplate = config.pokemonTcgRestTemplate(TEST_API_KEY);
            assertInstanceOf(SimpleClientHttpRequestFactory.class, unwrapFactory(restTemplate));
        }

        @Test
        void shouldConfigureConnectTimeout() throws Exception {
            RestTemplate restTemplate = config.pokemonTcgRestTemplate(TEST_API_KEY);
            SimpleClientHttpRequestFactory factory = unwrapFactory(restTemplate);
            Field connectTimeoutField = SimpleClientHttpRequestFactory.class
                    .getDeclaredField("connectTimeout");
            connectTimeoutField.setAccessible(true);
            assertEquals(5000, connectTimeoutField.getInt(factory));
        }

        @Test
        void shouldConfigureReadTimeout() throws Exception {
            RestTemplate restTemplate = config.pokemonTcgRestTemplate(TEST_API_KEY);
            SimpleClientHttpRequestFactory factory = unwrapFactory(restTemplate);
            Field readTimeoutField = SimpleClientHttpRequestFactory.class
                    .getDeclaredField("readTimeout");
            readTimeoutField.setAccessible(true);
            assertEquals(30000, readTimeoutField.getInt(factory));
        }

        @Test
        void shouldAddXApiKeyInterceptor() {
            RestTemplate restTemplate = config.pokemonTcgRestTemplate(TEST_API_KEY);
            assertFalse(restTemplate.getInterceptors().isEmpty());
            assertInstanceOf(ClientHttpRequestInterceptor.class, restTemplate.getInterceptors().get(0));
        }

        @Test
        void interceptorShouldAddXApiKeyHeader() throws Exception {
            RestTemplate restTemplate = config.pokemonTcgRestTemplate(TEST_API_KEY);
            ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);

            var request = mock(org.springframework.http.HttpRequest.class);
            byte[] body = new byte[0];
            var execution = mock(ClientHttpRequestExecution.class);
            HttpHeaders headers = new HttpHeaders();
            when(request.getHeaders()).thenReturn(headers);
            when(execution.execute(request, body)).thenReturn(new ClientHttpResponse() {
                @Override public InputStream getBody() { return new ByteArrayInputStream(new byte[0]); }
                @Override public HttpHeaders getHeaders() { return headers; }
                @Override public org.springframework.http.HttpStatusCode getStatusCode() { return HttpStatus.OK; }
                @Override public String getStatusText() { return "OK"; }
                @Override public void close() {}
            });

            interceptor.intercept(request, body, execution);

            verify(execution).execute(request, body);
            assertTrue(headers.containsKey("X-Api-Key"));
            assertEquals(TEST_API_KEY, headers.getFirst("X-Api-Key"));
        }
    }

    @Nested
    class SchedulingConfigTest {

        @Autowired
        private SchedulingConfig schedulingConfig;

        @Test
        void schedulingConfig_shouldBeLoaded() {
            assertNotNull(schedulingConfig);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class DataInitializerTest {

        @Mock
        private CardService cardService;

        @Mock
        private CardRepository cardRepository;

        @Mock
        private ApplicationArguments applicationArguments;

        @Captor
        private ArgumentCaptor<SyncRequestDTO> syncRequestCaptor;

        @Test
        void shouldSyncCardsWhenRepositoryIsEmpty() {
            when(cardRepository.count()).thenReturn(0L);

            DataInitializer dataInitializer = new DataInitializer(cardService, cardRepository);
            dataInitializer.run(applicationArguments);

            verify(cardService).syncCards(syncRequestCaptor.capture());
            assertEquals("xy1", syncRequestCaptor.getValue().getSetId());
        }

        @Test
        void shouldNotSyncCardsWhenRepositoryHasData() {
            when(cardRepository.count()).thenReturn(10L);

            DataInitializer dataInitializer = new DataInitializer(cardService, cardRepository);
            dataInitializer.run(applicationArguments);

            verify(cardService, never()).syncCards(any());
        }

        @Test
        void shouldCatchExceptionWhenSyncFails() {
            when(cardRepository.count()).thenReturn(0L);
            when(cardService.syncCards(any())).thenThrow(new RuntimeException("API error"));

            DataInitializer dataInitializer = new DataInitializer(cardService, cardRepository);
            assertDoesNotThrow(() -> dataInitializer.run(applicationArguments));

            verify(cardService).syncCards(any());
        }
    }
}
