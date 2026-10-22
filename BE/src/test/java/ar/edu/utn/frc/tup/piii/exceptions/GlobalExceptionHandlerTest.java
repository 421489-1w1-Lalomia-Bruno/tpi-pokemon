package ar.edu.utn.frc.tup.piii.exceptions;

import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler handler;

    private void assertErrorApi(ResponseEntity<ErrorApi> response, HttpStatus expectedStatus, String expectedError, String expectedMessage) {
        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        ErrorApi body = response.getBody();
        assertEquals(expectedStatus.value(), body.getStatus());
        assertEquals(expectedError, body.getError());
        assertTrue(body.getMessage().contains(expectedMessage));
        assertEquals("/test", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void handleEntityNotFound_shouldReturn404() {
        when(request.getRequestURI()).thenReturn("/test");
        EntityNotFoundException ex = new EntityNotFoundException("Card not found");
        ResponseEntity<ErrorApi> response = handler.handleEntityNotFound(ex, request);
        assertErrorApi(response, HttpStatus.NOT_FOUND, "Not Found", "Card not found");
    }

    @Test
    void handleDeckValidation_shouldReturn400() {
        when(request.getRequestURI()).thenReturn("/test");
        DeckValidationException ex = new DeckValidationException("Deck has 59 cards");
        ResponseEntity<ErrorApi> response = handler.handleDeckValidation(ex, request);
        assertErrorApi(response, HttpStatus.BAD_REQUEST, "Bad Request", "Deck has 59 cards");
    }

    @Test
    void handleGameRuleViolation_shouldReturn409() {
        when(request.getRequestURI()).thenReturn("/test");
        GameRuleViolationException ex = new GameRuleViolationException("Cannot play energy this turn");
        ResponseEntity<ErrorApi> response = handler.handleGameRuleViolation(ex, request);
        assertErrorApi(response, HttpStatus.CONFLICT, "Conflict", "Cannot play energy this turn");
    }

    @Test
    void handleDeckOwnership_shouldReturn409() {
        when(request.getRequestURI()).thenReturn("/test");
        DeckOwnershipException ex = new DeckOwnershipException("Deck does not belong to user");
        ResponseEntity<ErrorApi> response = handler.handleDeckOwnership(ex, request);
        assertErrorApi(response, HttpStatus.CONFLICT, "Conflict", "Deck does not belong to user");
    }

    @Test
    void handleMatchState_shouldReturn409() {
        when(request.getRequestURI()).thenReturn("/test");
        MatchStateException ex = new MatchStateException("Match is already finished");
        ResponseEntity<ErrorApi> response = handler.handleMatchState(ex, request);
        assertErrorApi(response, HttpStatus.CONFLICT, "Conflict", "Match is already finished");
    }

    @Test
    void handleInvalidAction_shouldReturn409() {
        when(request.getRequestURI()).thenReturn("/test");
        InvalidActionException ex = new InvalidActionException("Not your turn");
        ResponseEntity<ErrorApi> response = handler.handleInvalidAction(ex, request);
        assertErrorApi(response, HttpStatus.CONFLICT, "Conflict", "Not your turn");
    }

    @Test
    void handleDuplicateUsername_shouldReturn409() {
        when(request.getRequestURI()).thenReturn("/test");
        DuplicateUsernameException ex = new DuplicateUsernameException("Username already taken");
        ResponseEntity<ErrorApi> response = handler.handleDuplicateUsername(ex, request);
        assertErrorApi(response, HttpStatus.CONFLICT, "Conflict", "Username already taken");
    }

    @Test
    void handleInvalidCredentials_shouldReturn401() {
        when(request.getRequestURI()).thenReturn("/test");
        InvalidCredentialsException ex = new InvalidCredentialsException("Bad credentials");
        ResponseEntity<ErrorApi> response = handler.handleInvalidCredentials(ex, request);
        assertErrorApi(response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Bad credentials");
    }

    @Test
    void handleIllegalArgument_shouldReturn400() {
        when(request.getRequestURI()).thenReturn("/test");
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument value");
        ResponseEntity<ErrorApi> response = handler.handleIllegalArgument(ex, request);
        assertErrorApi(response, HttpStatus.BAD_REQUEST, "Bad Request", "Invalid argument value");
    }

    @Test
    void handleExternalApi_shouldReturn502() {
        when(request.getRequestURI()).thenReturn("/test");
        ExternalApiException ex = new ExternalApiException("Pokemon TCG API timeout");
        ResponseEntity<ErrorApi> response = handler.handleExternalApi(ex, request);
        assertErrorApi(response, HttpStatus.BAD_GATEWAY, "Bad Gateway", "Pokemon TCG API timeout");
    }

    @Test
    void handleValidation_shouldReturn400WithFieldErrors() {
        when(request.getRequestURI()).thenReturn("/test");
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "name", "Name is required"));
        bindingResult.addError(new FieldError("target", "hp", "HP must be positive"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<ErrorApi> response = handler.handleValidation(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        ErrorApi body = response.getBody();
        assertEquals(400, body.getStatus());
        assertEquals("Bad Request", body.getError());
        assertTrue(body.getMessage().contains("name: Name is required"));
        assertTrue(body.getMessage().contains("hp: HP must be positive"));
        assertEquals("/test", body.getPath());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void handleTypeMismatch_shouldReturn400() {
        when(request.getRequestURI()).thenReturn("/test");
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "id", null, null);
        ResponseEntity<ErrorApi> response = handler.handleTypeMismatch(ex, request);
        assertErrorApi(response, HttpStatus.BAD_REQUEST, "Bad Request", "Parameter 'id' should be of type Integer");
    }

    @Test
    void handleTypeMismatch_withNullRequiredType_shouldReturn400WithUnknownType() {
        when(request.getRequestURI()).thenReturn("/test");
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", null, "id", null, null);
        ResponseEntity<ErrorApi> response = handler.handleTypeMismatch(ex, request);
        assertErrorApi(response, HttpStatus.BAD_REQUEST, "Bad Request", "Parameter 'id' should be of type unknown");
    }

    @Test
    void handleGenericException_shouldReturn500() {
        when(request.getRequestURI()).thenReturn("/test");
        Exception ex = new RuntimeException("Unexpected error occurred");
        ResponseEntity<ErrorApi> response = handler.handleGenericException(ex, request);
        assertErrorApi(response, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected error occurred");
    }
}
