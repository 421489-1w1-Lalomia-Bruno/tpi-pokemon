package ar.edu.utn.frc.tup.piii.engine.state;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventPublisher;
import ar.edu.utn.frc.tup.piii.engine.events.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.utils.CoinFlipManager;
import ar.edu.utn.frc.tup.piii.enums.GamePhase;
import ar.edu.utn.frc.tup.piii.enums.MatchStatus;
import ar.edu.utn.frc.tup.piii.enums.PokemonStage;
import ar.edu.utn.frc.tup.piii.exceptions.InvalidActionException;
import ar.edu.utn.frc.tup.piii.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.engine.models.*;
import ar.edu.utn.frc.tup.piii.websocket.GameActionType;
import ar.edu.utn.frc.tup.piii.websocket.messages.GameActionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.utn.frc.tup.piii.enums.TurnPhase;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SetupState implements MatchState {

    private static final Logger log = LoggerFactory.getLogger(SetupState.class);

    private final Map<String, Set<String>> confirmedPlayers = new ConcurrentHashMap<>();

    @Override
    public GamePhase getPhase() {
        return GamePhase.SETUP;
    }

    @Override
    public void enter(GameEngine engine, String matchId) {
        log.info("[SetupState] enter: match=" + matchId);
        MatchSnapshot snapshot = engine.getSnapshot(matchId);
        if (snapshot == null) {
            log.info("[SetupState] enter: NO SNAPSHOT for match " + matchId);
            return;
        }

        String p1Id = snapshot.getPlayer1Id();
        String p2Id = snapshot.getPlayer2Id();
        log.info("[SetupState] players: p1=" + p1Id + " p2=" + p2Id);

        UUID p1DeckId = snapshot.getPlayer1DeckId();
        UUID p2DeckId = snapshot.getPlayer2DeckId();

        if (p1DeckId == null || p2DeckId == null) {
            MatchEntity matchEntity = engine.getMatchEntity(matchId);
            if (matchEntity != null) {
                p1DeckId = matchEntity.getPlayer1DeckId();
                p2DeckId = matchEntity.getPlayer2DeckId();
                snapshot.setPlayer1DeckId(p1DeckId);
                snapshot.setPlayer2DeckId(p2DeckId);
            }
        }

        if (p1DeckId == null || p2DeckId == null) {
            String missing = p1DeckId == null ? snapshot.getPlayer1Id() : snapshot.getPlayer2Id();
            throw new InvalidActionException("Deck " + (p1DeckId == null ? "1" : "2") +
                    " not found for player " + missing + ". Recreate your deck and try again.");
        }

        List<GameCard> p1Cards = engine.loadDeckCards(p1DeckId);
        if (p1Cards.isEmpty()) {
            throw new InvalidActionException("Deck not found or empty for player " + snapshot.getPlayer1Id() +
                    ". Deck ID: " + p1DeckId + ". Recreate your deck.");
        }
        List<GameCard> p2Cards = engine.loadDeckCards(p2DeckId);
        if (p2Cards.isEmpty()) {
            throw new InvalidActionException("Deck not found or empty for player " + snapshot.getPlayer2Id() +
                    ". Deck ID: " + p2DeckId + ". Recreate your deck.");
        }

        log.info("[SetupState] enter: decks loaded p1=" + p1Cards.size() + " cards p2=" + p2Cards.size() + " cards");

        GameEventPublisher ep = engine.getEventPublisher();

        confirmedPlayers.put(matchId, new HashSet<>());

        PlayerBoard p1Board = buildPlayerBoard(engine, p1Id, p1DeckId);
        PlayerBoard p2Board = buildPlayerBoard(engine, p2Id, p2DeckId);

        GameBoard board = new GameBoard();
        board.setPlayer1Board(p1Board);
        board.setPlayer2Board(p2Board);
        snapshot.setBoard(board);

        int p1Mulligans = performMulligan(matchId, p1Board, p1Id, ep);
        if (p1Mulligans < 0) {
            engine.declareLoser(matchId, p1Id, "No Basic Pokemon in opening hand after mulligan");
            return;
        }

        int p2Mulligans = performMulligan(matchId, p2Board, p2Id, ep);
        if (p2Mulligans < 0) {
            engine.declareLoser(matchId, p2Id, "No Basic Pokemon in opening hand after mulligan");
            return;
        }

        for (int i = 0; i < p2Mulligans && !p1Board.getDeck().isEmpty(); i++) {
            p1Board.getHand().add(p1Board.getDeck().remove(0));
        }
        for (int i = 0; i < p1Mulligans && !p2Board.getDeck().isEmpty(); i++) {
            p2Board.getHand().add(p2Board.getDeck().remove(0));
        }

        snapshot.setStatus(MatchStatus.SETUP);
        engine.saveSnapshot(matchId, snapshot);
        log.info("[SetupState] snapshot saved: status=" + snapshot.getStatus() +
                " p1Hand=" + snapshot.getBoard().getPlayer1Board().getHand().size() +
                " p2Hand=" + snapshot.getBoard().getPlayer2Board().getHand().size());
        if (ep != null) {
            ep.publish(GameEventType.PHASE_CHANGED, matchId, null, "Setup phase started");
        }
    }

    @Override
    public void exit(GameEngine engine, String matchId) {
        confirmedPlayers.remove(matchId);
    }

    @Override
    public void handleAction(GameEngine engine, GameActionMessage action, String playerId) {
        String matchId = action.getMatchId();
        MatchSnapshot snapshot = engine.getSnapshot(matchId);
        if (snapshot == null || snapshot.getBoard() == null) {
            throw new InvalidActionException("Match not found in SETUP state");
        }

        if (action.getType() == GameActionType.PLACE_POKEMON) {
            handlePlacePokemon(engine, snapshot, action, playerId);
        } else if (action.getType() == GameActionType.CONFIRM_SETUP) {
            handleConfirmSetup(engine, matchId, snapshot, playerId);
        } else if (action.getType() == GameActionType.CALL_COIN) {
            handleCoinCall(engine, matchId, snapshot, action, playerId);
        } else {
            throw new InvalidActionException("Only PLACE_POKEMON, CONFIRM_SETUP or CALL_COIN allowed in SETUP phase. Received: " + action.getType());
        }
    }

    private void handlePlacePokemon(GameEngine engine, MatchSnapshot snapshot, GameActionMessage action, String playerId) {
        String matchId = action.getMatchId();
        Map<String, Object> payload = action.getPayload();
        if (payload == null) {
            throw new InvalidActionException("Payload required for PLACE_POKEMON");
        }

        String cardId = (String) payload.get("cardId");
        String zone = (String) payload.get("zone");

        if (cardId == null || zone == null) {
            throw new InvalidActionException("cardId and zone are required");
        }
        if (!"ACTIVE".equals(zone) && !"BENCH".equals(zone)) {
            throw new InvalidActionException("zone must be ACTIVE or BENCH");
        }

        PlayerBoard playerBoard = getPlayerBoard(snapshot, playerId);
        if (playerBoard == null) {
            throw new InvalidActionException("Player not found in match");
        }

        GameCard cardToPlace = findCardInHand(playerBoard, cardId);
        if (cardToPlace == null) {
            throw new InvalidActionException("Card not found in your hand");
        }

        if (!(cardToPlace instanceof PokemonCard pokemon)) {
            throw new InvalidActionException("Only Pokemon cards can be placed");
        }
        if (pokemon.getStage() != PokemonStage.BASIC && pokemon.getStage() != PokemonStage.EX) {
            throw new InvalidActionException("Only Basic or EX Pokemon can be placed during setup");
        }

        playerBoard.getHand().remove(cardToPlace);

        PokemonInPlay pip = new PokemonInPlay(pokemon);

        if ("ACTIVE".equals(zone)) {
            if (playerBoard.getActivePokemon() != null) {
                if (playerBoard.getBench().size() >= 5) {
                    throw new InvalidActionException("Active spot taken and bench is full");
                }
                pip.setActive(false);
                playerBoard.getBench().add(pip);
            } else {
                pip.setActive(true);
                playerBoard.setActivePokemon(pip);
            }
        } else {
            if (playerBoard.getBench().size() >= 5) {
                throw new InvalidActionException("Bench is full (max 5)");
            }
            pip.setActive(false);
            playerBoard.getBench().add(pip);
        }

        GameEventPublisher epPub = engine.getEventPublisher();
        if (epPub != null) {
            epPub.publish(GameEventType.POKEMON_PLACED, matchId, playerId,
                    "Placed " + pokemon.getName() + " during setup");
        }

        engine.saveSnapshot(matchId, snapshot);
    }

    private void handleConfirmSetup(GameEngine engine, String matchId, MatchSnapshot snapshot, String playerId) {
        PlayerBoard playerBoard = getPlayerBoard(snapshot, playerId);
        if (playerBoard == null) {
            throw new InvalidActionException("Player not found in match");
        }
        if (playerBoard.getActivePokemon() == null) {
            throw new InvalidActionException("You must place at least one Pokemon in the Active spot before confirming");
        }

        Set<String> confirmed = confirmedPlayers.get(matchId);
        if (confirmed == null) {
            throw new InvalidActionException("Setup not initialized for this match");
        }

        confirmed.add(playerId);
        engine.saveSnapshot(matchId, snapshot);

        if (confirmed.size() >= 2) {
            finalizeSetup(engine, matchId, snapshot);
        }
    }

    private void finalizeSetup(GameEngine engine, String matchId, MatchSnapshot snapshot) {
        GameBoard board = snapshot.getBoard();

        for (PlayerBoard pb : List.of(board.getPlayer1Board(), board.getPlayer2Board())) {
            if (pb.getActivePokemon() == null) {
                throw new InvalidActionException("Player " + pb.getPlayerId()
                        + " must place at least one Basic Pokemon in the Active spot");
            }
            List<GameCard> prizes = new ArrayList<>();
            for (int i = 0; i < 6 && !pb.getDeck().isEmpty(); i++) {
                prizes.add(pb.getDeck().remove(0));
            }
            pb.setPrizeCards(prizes);
        }

        // En vez de decidir el primer jugador al azar, elegimos al azar quién
        // llama la moneda (cara/cruz) y esperamos su elección antes de tirarla.
        boolean p1Calls = CoinFlipManager.flip();
        String callerId = p1Calls ? snapshot.getPlayer1Id() : snapshot.getPlayer2Id();
        snapshot.setCoinCallerId(callerId);
        snapshot.setAwaitingCoinCall(true);
        snapshot.setCoinCall(null);
        snapshot.setCoinFlipResult(null);
        // El estado permanece en SETUP hasta que el llamador elija cara o cruz.
        snapshot.setStatus(MatchStatus.SETUP);

        engine.saveSnapshot(matchId, snapshot);
        GameEventPublisher ep = engine.getEventPublisher();
        if (ep != null) {
            ep.publish(GameEventType.PHASE_CHANGED, matchId, callerId,
                    "Setup completed. Waiting for coin call.");
        }
    }

    /**
     * Resuelve el sorteo cara/cruz: el llamador elige, se tira la moneda y, si
     * acierta, empieza él; si no, empieza el rival. Luego arranca la fase activa.
     */
    private void handleCoinCall(GameEngine engine, String matchId, MatchSnapshot snapshot,
                                GameActionMessage action, String playerId) {
        if (!snapshot.isAwaitingCoinCall()) {
            throw new InvalidActionException("No coin call is pending");
        }
        if (!playerId.equals(snapshot.getCoinCallerId())) {
            throw new InvalidActionException("It's not your turn to call the coin");
        }

        Map<String, Object> payload = action.getPayload();
        String call = payload != null && payload.get("call") != null
                ? String.valueOf(payload.get("call")).toUpperCase() : null;
        if (!"HEADS".equals(call) && !"TAILS".equals(call)) {
            throw new InvalidActionException("call must be HEADS or TAILS");
        }

        String flip = CoinFlipManager.flip() ? "HEADS" : "TAILS";
        boolean callerWins = flip.equals(call);
        String otherId = snapshot.getPlayer1Id().equals(snapshot.getCoinCallerId())
                ? snapshot.getPlayer2Id() : snapshot.getPlayer1Id();
        String firstPlayerId = callerWins ? snapshot.getCoinCallerId() : otherId;

        snapshot.setCoinCall(call);
        snapshot.setCoinFlipResult(flip);
        snapshot.setAwaitingCoinCall(false);

        snapshot.setCurrentPlayerId(firstPlayerId);
        snapshot.setFirstPlayerId(firstPlayerId);
        snapshot.setTurnNumber(1);
        snapshot.setStatus(MatchStatus.ACTIVE);

        TurnContext turnCtx = new TurnContext();
        turnCtx.setCurrentPlayerId(firstPlayerId);
        turnCtx.setPhase(TurnPhase.DRAW);
        turnCtx.setFirstTurn(true);
        snapshot.setTurnContext(turnCtx);

        engine.saveSnapshot(matchId, snapshot);
        GameEventPublisher ep = engine.getEventPublisher();
        if (ep != null) {
            ep.publish(GameEventType.COIN_FLIP_RESULT, matchId, firstPlayerId,
                    firstPlayerId + " goes first", call, flip, callerWins ? 1 : 0);
        }
        engine.transitionTo(matchId, new ActiveState());
    }

    private PlayerBoard buildPlayerBoard(GameEngine engine, String playerId, UUID deckId) {
        PlayerBoard board = new PlayerBoard();
        board.setPlayerId(playerId);

        List<GameCard> loadedDeck = engine.loadDeckCards(deckId);
        if (loadedDeck == null || loadedDeck.isEmpty()) {
            throw new InvalidActionException("Deck is empty or could not be loaded for player " + playerId);
        }

        boolean hasAnyBasic = loadedDeck.stream().anyMatch(c ->
                c instanceof PokemonCard p
                && (p.getStage() == PokemonStage.BASIC || p.getStage() == PokemonStage.EX));
        if (!hasAnyBasic) {
            throw new InvalidActionException("Deck has no Basic Pokemon - cannot start game");
        }

        List<GameCard> shuffled = new ArrayList<>(loadedDeck);
        Collections.shuffle(shuffled);
        List<GameCard> remaining = new ArrayList<>(shuffled);
        List<GameCard> hand = new ArrayList<>();
        for (int i = 0; i < 7 && !remaining.isEmpty(); i++) {
            hand.add(remaining.remove(0));
        }

        board.setHand(hand);
        board.setDeck(remaining);

        return board;
    }

    private int performMulligan(String matchId, PlayerBoard board, String playerId, GameEventPublisher ep) {
        int mulliganCount = 0;
        while (!hasBasicPokemon(board.getHand())) {
            if (ep != null) {
                List<GameCard> revealed = new ArrayList<>(board.getHand());
                String cardNames = revealed.stream()
                        .map(GameCard::getName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("empty");
                ep.publish(GameEventType.MULLIGAN_EXECUTED, matchId, playerId,
                        "Mulligan! No Basic Pokemon. Revealed: " + cardNames);
            }

            board.getDeck().addAll(board.getHand());
            board.getHand().clear();
            Collections.shuffle(board.getDeck());

            if (board.getDeck().isEmpty()) {
                return -1;
            }

            List<GameCard> newHand = new ArrayList<>();
            for (int i = 0; i < 7 && !board.getDeck().isEmpty(); i++) {
                newHand.add(board.getDeck().remove(0));
            }
            board.setHand(newHand);
            mulliganCount++;

            if (mulliganCount > 20) break;
        }
        if (!hasBasicPokemon(board.getHand())) {
            return -1;
        }
        return mulliganCount;
    }

    private boolean hasBasicPokemon(List<GameCard> hand) {
        for (GameCard card : hand) {
            if (card instanceof PokemonCard pokemon) {
                PokemonStage stage = pokemon.getStage();
                if (stage == PokemonStage.BASIC || stage == PokemonStage.EX) {
                    return true;
                }
            }
        }
        return false;
    }

    private PlayerBoard getPlayerBoard(MatchSnapshot snapshot, String playerId) {
        GameBoard board = snapshot.getBoard();
        if (board == null) return null;
        if (board.getPlayer1Board() != null && playerId.equals(board.getPlayer1Board().getPlayerId())) {
            return board.getPlayer1Board();
        }
        if (board.getPlayer2Board() != null && playerId.equals(board.getPlayer2Board().getPlayerId())) {
            return board.getPlayer2Board();
        }
        return null;
    }

    private GameCard findCardInHand(PlayerBoard board, String cardId) {
        for (GameCard card : board.getHand()) {
            if (card.getId().equals(cardId)) {
                return card;
            }
        }
        return null;
    }
}