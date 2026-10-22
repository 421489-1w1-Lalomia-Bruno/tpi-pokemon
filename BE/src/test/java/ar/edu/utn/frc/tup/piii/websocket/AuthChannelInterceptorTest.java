package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.exceptions.InvalidCredentialsException;
import ar.edu.utn.frc.tup.piii.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MessageChannel channel;

    @Captor
    private ArgumentCaptor<Principal> principalCaptor;

    private AuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuthChannelInterceptor(jwtTokenProvider);
    }

    private StompHeaderAccessor createAccessor(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        accessor.setSessionId("test-session");
        return accessor;
    }

    @Test
    void preSend_nonConnectFrame_passesThrough() {
        StompHeaderAccessor accessor = createAccessor(StompCommand.SUBSCRIBE);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void preSend_validJwt_setsUserAndReturnsMessage() {
        String token = "valid-jwt-token";
        String playerId = "player-123";
        when(jwtTokenProvider.validate(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(playerId);

        StompHeaderAccessor accessor = createAccessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
        verify(jwtTokenProvider).validate(token);
        verify(jwtTokenProvider).getUserIdFromToken(token);
    }

    @Test
    void preSend_validJwt_setsCorrectPrincipalOnAccessor() {
        String token = "valid-jwt-token";
        String playerId = "player-123";
        when(jwtTokenProvider.validate(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn(playerId);

        StompHeaderAccessor accessor = createAccessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, channel);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(message);
        assertNotNull(resultAccessor.getUser());
        assertEquals(playerId, resultAccessor.getUser().getName());
    }

    @Test
    void preSend_missingAuthHeader_throwsException() {
        StompHeaderAccessor accessor = createAccessor(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(InvalidCredentialsException.class, () ->
                interceptor.preSend(message, channel));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void preSend_missingBearerPrefix_throwsException() {
        StompHeaderAccessor accessor = createAccessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "not-bearer-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(InvalidCredentialsException.class, () ->
                interceptor.preSend(message, channel));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void preSend_invalidJwt_throwsException() {
        String token = "invalid-jwt-token";
        when(jwtTokenProvider.validate(token)).thenReturn(false);

        StompHeaderAccessor accessor = createAccessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(InvalidCredentialsException.class, () ->
                interceptor.preSend(message, channel));
        verify(jwtTokenProvider).validate(token);
        verify(jwtTokenProvider, never()).getUserIdFromToken(anyString());
    }

    @Test
    void preSend_blankPlayerId_throwsException() {
        String token = "valid-token-empty-subject";
        when(jwtTokenProvider.validate(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn("");

        StompHeaderAccessor accessor = createAccessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(InvalidCredentialsException.class, () ->
                interceptor.preSend(message, channel));
    }

    @Test
    void preSend_headerWithoutBearer_doesNotCallJwt() {
        StompHeaderAccessor accessor = createAccessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Basic some-token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(InvalidCredentialsException.class, () ->
                interceptor.preSend(message, channel));
        verifyNoInteractions(jwtTokenProvider);
    }
}
