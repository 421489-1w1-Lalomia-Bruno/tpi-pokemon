package ar.edu.utn.frc.tup.piii;

import ar.edu.utn.frc.tup.piii.dtos.auth.AuthResponse;
import ar.edu.utn.frc.tup.piii.dtos.deck.CreateDeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardEntryDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.CreateMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.JoinMatchDTO;
import ar.edu.utn.frc.tup.piii.dtos.match.MatchDTO;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.entities.*;
import ar.edu.utn.frc.tup.piii.enums.*;
import ar.edu.utn.frc.tup.piii.repositories.*;
import ar.edu.utn.frc.tup.piii.services.GameStatePersistenceService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
class GameE2ETest {

    @Autowired private TestRestTemplate rest;
    @Autowired private CardRepository cardRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private MatchRepository matchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private GameEngine gameEngine;
    @Autowired private GameStatePersistenceService persistenceService;
    @Autowired private TransactionTemplate tx;

    private String p1Token;
    private String p2Token;
    private String p1Id;
    private String p2Id;

    @BeforeEach
    void setUp() {
        matchRepository.deleteAll();
        deckRepository.deleteAll();
        cardRepository.deleteAll();
        userRepository.deleteAll();

        CardEntity poke = cardRepository.save(CardEntity.builder()
                .externalId("e2e-poke").name("E2E Pokemon").supertype("Pokémon").hp(100)
                .stage(PokemonStage.BASIC).retreatCost(1)
                .attacksJson("[{\"name\":\"Tackle\",\"cost\":[\"COLORLESS\"],\"damage\":\"30\",\"text\":\"\"}]")
                .imageUrl("https://e.com/p.png").cardNumber("001").setId("e2e").rarity("Common")
                .build());
        CardEntity energy = cardRepository.save(CardEntity.builder()
                .externalId("e2e-eng").name("Colorless Energy").supertype("Energy")
                .types(Set.of(EnergyType.COLORLESS))
                .imageUrl("https://e.com/e.png").cardNumber("002").setId("e2e").rarity("Common")
                .subtypes(List.of("Basic")).build());

        registerUser("e2e-p1", "pass123");
        registerUser("e2e-p2", "pass123");

        p1Token = loginUser("e2e-p1", "pass123");
        p2Token = loginUser("e2e-p2", "pass123");

        p1Id = extractUserId(p1Token);
        p2Id = extractUserId(p2Token);

        createDeckViaRest(p1Token, "P1 Deck", poke.getId().toString(), energy.getId().toString());
        createDeckViaRest(p2Token, "P2 Deck", poke.getId().toString(), energy.getId().toString());
    }

    private void registerUser(String username, String password) {
        ResponseEntity<AuthResponse> resp = rest.postForEntity("/api/auth/register",
                Map.of("username", username, "password", password, "gender", "MALE"),
                AuthResponse.class);
        assertEquals(200, resp.getStatusCode().value());
    }

    private String loginUser(String username, String password) {
        ResponseEntity<AuthResponse> resp = rest.postForEntity("/api/auth/login",
                Map.of("username", username, "password", password),
                AuthResponse.class);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        return resp.getBody().getToken();
    }

    private String extractUserId(String token) {
        String body = new String(java.util.Base64.getDecoder()
                .decode(token.split("\\.")[1]));
        return body.replaceAll(".*\"sub\":\"([^\"]+)\".*", "$1");
    }

    private String createDeckViaRest(String token, String name, String pokeId, String energyId) {
        CreateDeckDTO dto = new CreateDeckDTO();
        dto.setName(name);
        dto.setCards(List.of(
                entry(pokeId, 4),
                entry(energyId, 56)
        ));
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<DeckDTO> resp = rest.exchange("/api/decks", HttpMethod.POST,
                new HttpEntity<>(dto, headers), DeckDTO.class);
        assertEquals(201, resp.getStatusCode().value(), "Deck creation failed");
        assertNotNull(resp.getBody());
        return resp.getBody().getId().toString();
    }

    private static DeckCardEntryDTO entry(String cardId, int qty) {
        DeckCardEntryDTO e = new DeckCardEntryDTO();
        e.setCardId(cardId);
        e.setQuantity(qty);
        return e;
    }

    private HttpEntity<?> authWithToken(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(h);
    }

    private String findBasicInHand(MatchSnapshot snap, String playerId) {
        PlayerBoard board = snap.getBoard().getPlayer1Board();
        if (board == null || !playerId.equals(board.getPlayerId()))
            board = snap.getBoard().getPlayer2Board();
        if (board == null) return null;
        return board.getHand().stream()
                .filter(c -> c instanceof PokemonCard p
                        && (p.getStage() == PokemonStage.BASIC || p.getStage() == PokemonStage.EX))
                .findFirst().map(GameCard::getId).orElse(null);
    }

    @Test
    void e2e_fullFlow() {
        // ---- 1. Get P1 deck id via list endpoint ----
        HttpEntity<?> p1Auth = authWithToken(p1Token);
        ResponseEntity<List<DeckDTO>> listResp = rest.exchange("/api/decks", HttpMethod.GET, p1Auth,
                new ParameterizedTypeReference<List<DeckDTO>>() {});
        assertEquals(200, listResp.getStatusCode().value());
        List<DeckDTO> p1Decks = listResp.getBody();
        assertNotNull(p1Decks);
        assertFalse(p1Decks.isEmpty());
        String p1DeckId = p1Decks.get(0).getId().toString();

        // ---- 2. P2 also needs a deck listing ----
        HttpEntity<?> p2Auth = authWithToken(p2Token);
        ResponseEntity<List<DeckDTO>> listResp2 = rest.exchange("/api/decks", HttpMethod.GET, p2Auth,
                new ParameterizedTypeReference<List<DeckDTO>>() {});
        String p2DeckId = listResp2.getBody().get(0).getId().toString();

        // ---- 3. Create match via REST ----
        CreateMatchDTO matchDTO = new CreateMatchDTO();
        matchDTO.setPlayer1DeckId(p1DeckId);
        ResponseEntity<MatchDTO> createResp = rest.exchange("/api/matches", HttpMethod.POST,
                new HttpEntity<>(matchDTO, p1Auth.getHeaders()), MatchDTO.class);
        assertEquals(201, createResp.getStatusCode().value(), "Create match failed");
        assertNotNull(createResp.getBody());
        String matchUuid = createResp.getBody().getId().toString();

        // ---- 4. Join match via REST ----
        JoinMatchDTO joinDTO = new JoinMatchDTO();
        joinDTO.setDeckId(p2DeckId);
        ResponseEntity<MatchDTO> joinResp = rest.exchange("/api/matches/" + matchUuid + "/join",
                HttpMethod.POST, new HttpEntity<>(joinDTO, p2Auth.getHeaders()), MatchDTO.class);
        assertEquals(200, joinResp.getStatusCode().value(), "Join match failed");

        // ---- 5. Both READY (triggers SetupState.enter) ----
        tx.execute(s -> {
            gameEngine.processAction(matchUuid, p1Id, "READY", Map.of());
            gameEngine.processAction(matchUuid, p2Id, "READY", Map.of());
            return null;
        });

        // ---- 6. Place Pokemon and confirm setup for both players ----
        MatchSnapshot snap = persistenceService.loadSnapshot(UUID.fromString(matchUuid));
        assertNotNull(snap);
        assertEquals(MatchStatus.SETUP, snap.getStatus());

        String p1PokeId = findBasicInHand(snap, p1Id);
        String p2PokeId = findBasicInHand(snap, p2Id);

        tx.execute(s -> {
            if (p1PokeId != null)
                gameEngine.processAction(matchUuid, p1Id, "PLACE_POKEMON",
                        Map.of("cardId", p1PokeId, "zone", "ACTIVE"));
            gameEngine.processAction(matchUuid, p1Id, "CONFIRM_SETUP", Map.of());
            return null;
        });
        tx.execute(s -> {
            MatchSnapshot snap2 = persistenceService.loadSnapshot(UUID.fromString(matchUuid));
            String p2IdFinal = findBasicInHand(snap2, p2Id);
            if (p2IdFinal != null)
                gameEngine.processAction(matchUuid, p2Id, "PLACE_POKEMON",
                        Map.of("cardId", p2IdFinal, "zone", "ACTIVE"));
            gameEngine.processAction(matchUuid, p2Id, "CONFIRM_SETUP", Map.of());
            return null;
        });

        // ---- 7. Coin call ----
        snap = persistenceService.loadSnapshot(UUID.fromString(matchUuid));
        assertTrue(snap.isAwaitingCoinCall());
        String callerId = snap.getCoinCallerId();
        String finalCallerId = callerId;
        tx.execute(s -> {
            gameEngine.processAction(matchUuid, finalCallerId, "CALL_COIN", Map.of("call", "HEADS"));
            return null;
        });

        // ---- 8. Verify game is ACTIVE ----
        snap = persistenceService.loadSnapshot(UUID.fromString(matchUuid));
        assertEquals(MatchStatus.ACTIVE, snap.getStatus());
        assertNotNull(snap.getTurnContext());
        String firstPlayer = snap.getFirstPlayerId();

        // ---- 9. First turn: PASS_TURN (first player can't attack) ----
        snap = persistenceService.loadSnapshot(UUID.fromString(matchUuid));
        String currentPlayer = snap.getCurrentPlayerId();
        tx.execute(s -> gameEngine.processAction(matchUuid, currentPlayer, "PASS_TURN", Map.of()));

        // ---- 10. Second turn: attach energy + attack ----
        snap = persistenceService.loadSnapshot(UUID.fromString(matchUuid));
        String nextPlayer = snap.getCurrentPlayerId();
        assertNotNull(nextPlayer);
        tx.execute(s -> {
            gameEngine.processAction(matchUuid, nextPlayer, "ATTACK", Map.of());
            return null;
        });

        // ---- 11. Verify state via REST ----
        ResponseEntity<String> finalStateResp = rest.exchange(
                "/api/matches/" + matchUuid + "/state", HttpMethod.GET, p1Auth, String.class);
        assertEquals(200, finalStateResp.getStatusCode().value(),
                "Final state: " + finalStateResp.getBody());
        assertTrue(finalStateResp.getBody().contains("ACTIVE")
                || finalStateResp.getBody().contains("FINISHED"));
    }
}
