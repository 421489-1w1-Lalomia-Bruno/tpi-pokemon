package ar.edu.utn.frc.tup.piii.clients;

import ar.edu.utn.frc.tup.piii.clients.dtos.AbilityApiDTO;
import ar.edu.utn.frc.tup.piii.clients.dtos.AttackApiDTO;
import ar.edu.utn.frc.tup.piii.clients.dtos.CardSetImagesDTO;
import ar.edu.utn.frc.tup.piii.clients.dtos.WeakResistanceDTO;
import ar.edu.utn.frc.tup.piii.exceptions.ExternalApiException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class PokemonTcgApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PokemonTcgApiClient(
            @Qualifier("pokemonTcgRestTemplate") RestTemplate restTemplate,
            @Value("${pokemon.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public PokemonTcgResponse fetchCardsBySet(String setId) {
        return fetchCardsBySet(setId, 1, 250);
    }

    public PokemonTcgResponse fetchCardsBySet(String setId, int page, int pageSize) {
        String url = baseUrl + "/cards?q=set.id:" + setId + "&page=" + page + "&pageSize=" + pageSize;
        try {
            ResponseEntity<PokemonTcgResponse> response = restTemplate.getForEntity(
                    url, PokemonTcgResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new ExternalApiException(
                    "External API error: " + e.getStatusCode(), e, e.getStatusCode().value());
        } catch (ResourceAccessException e) {
            throw new ExternalApiException("External API timeout: " + e.getMessage(), e);
        }
    }

    @Data
    public static class PokemonTcgResponse {
        private List<PokemonTcgCardResponse> data;
        private int totalCount;
        private int page;
        private int pageSize;
        private int count;
    }

    @Data
    public static class PokemonTcgCardResponse {
        private String id;
        private String name;
        private String supertype;
        private List<String> subtypes;
        private String hp;
        private List<String> types;
        private String evolvesFrom;
        private List<AttackApiDTO> attacks;
        private List<AbilityApiDTO> abilities;
        private List<WeakResistanceDTO> weaknesses;
        private List<WeakResistanceDTO> resistances;
        private Integer convertedRetreatCost;
        private List<String> retreatCost;
        private CardSetImagesDTO set;
        private String number;
        private String rarity;
        private CardImagesDTO images;
        private List<String> rules;
    }

    @Data
    public static class CardImagesDTO {
        private String small;
        private String large;
    }
}
