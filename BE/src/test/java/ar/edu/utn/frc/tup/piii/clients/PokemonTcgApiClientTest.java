package ar.edu.utn.frc.tup.piii.clients;

import ar.edu.utn.frc.tup.piii.exceptions.ExternalApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PokemonTcgApiClientTest {

    @Mock
    private RestTemplate pokemonTcgRestTemplate;

    @InjectMocks
    private PokemonTcgApiClient client;

    private static final String BASE_URL = "https://api.example.com";
    private static final String SET_ID = "base1";

    private void setUpBaseUrl() {
        ReflectionTestUtils.setField(client, "baseUrl", BASE_URL);
    }

    private PokemonTcgApiClient.PokemonTcgResponse buildResponse() {
        PokemonTcgApiClient.PokemonTcgResponse response =
                new PokemonTcgApiClient.PokemonTcgResponse();
        PokemonTcgApiClient.PokemonTcgCardResponse card =
                new PokemonTcgApiClient.PokemonTcgCardResponse();
        card.setId("base1-1");
        card.setName("Charizard");
        response.setData(List.of(card));
        response.setTotalCount(1);
        response.setPage(1);
        response.setPageSize(250);
        response.setCount(1);
        return response;
    }

    @Test
    void fetchCardsBySet_withSingleParam_delegatesToThreeParamWithPage1AndPageSize250() {
        setUpBaseUrl();
        PokemonTcgApiClient.PokemonTcgResponse expected = buildResponse();
        when(pokemonTcgRestTemplate.getForEntity(
                anyString(), eq(PokemonTcgApiClient.PokemonTcgResponse.class)))
                .thenReturn(ResponseEntity.ok(expected));

        PokemonTcgApiClient.PokemonTcgResponse result = client.fetchCardsBySet(SET_ID);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(pokemonTcgRestTemplate).getForEntity(
                urlCaptor.capture(), eq(PokemonTcgApiClient.PokemonTcgResponse.class));
        String capturedUrl = urlCaptor.getValue();
        assertTrue(capturedUrl.contains("page=1"));
        assertTrue(capturedUrl.contains("pageSize=250"));
        assertEquals(expected, result);
    }

    @Test
    void fetchCardsBySet_withThreeParams_returnsResponseBodyOnSuccess() {
        setUpBaseUrl();
        PokemonTcgApiClient.PokemonTcgResponse expected = buildResponse();
        String expectedUrl = BASE_URL + "/cards?q=set.id:" + SET_ID + "&page=1&pageSize=50";
        when(pokemonTcgRestTemplate.getForEntity(
                expectedUrl, PokemonTcgApiClient.PokemonTcgResponse.class))
                .thenReturn(ResponseEntity.ok(expected));

        PokemonTcgApiClient.PokemonTcgResponse result =
                client.fetchCardsBySet(SET_ID, 1, 50);

        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("Charizard", result.getData().get(0).getName());
    }

    @Test
    void fetchCardsBySet_throwsExternalApiExceptionOnHttpClientError() {
        setUpBaseUrl();
        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "Not Found", null, null, null);
        when(pokemonTcgRestTemplate.getForEntity(
                anyString(), eq(PokemonTcgApiClient.PokemonTcgResponse.class)))
                .thenThrow(ex);

        ExternalApiException thrown = assertThrows(ExternalApiException.class,
                () -> client.fetchCardsBySet(SET_ID, 1, 50));

        assertEquals(HttpStatus.NOT_FOUND.value(), thrown.getStatusCode());
        assertTrue(thrown.getMessage().contains("404"));
    }

    @Test
    void fetchCardsBySet_throwsExternalApiExceptionOnHttpServerError() {
        setUpBaseUrl();
        HttpServerErrorException ex = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", null, null, null);
        when(pokemonTcgRestTemplate.getForEntity(
                anyString(), eq(PokemonTcgApiClient.PokemonTcgResponse.class)))
                .thenThrow(ex);

        ExternalApiException thrown = assertThrows(ExternalApiException.class,
                () -> client.fetchCardsBySet(SET_ID, 1, 50));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), thrown.getStatusCode());
        assertTrue(thrown.getMessage().contains("500"));
    }

    @Test
    void fetchCardsBySet_throwsExternalApiExceptionOnResourceAccessException() {
        setUpBaseUrl();
        ResourceAccessException ex = new ResourceAccessException("timeout");
        when(pokemonTcgRestTemplate.getForEntity(
                anyString(), eq(PokemonTcgApiClient.PokemonTcgResponse.class)))
                .thenThrow(ex);

        ExternalApiException thrown = assertThrows(ExternalApiException.class,
                () -> client.fetchCardsBySet(SET_ID, 1, 50));

        assertNull(thrown.getStatusCode());
        assertTrue(thrown.getMessage().contains("timeout"));
    }
}
