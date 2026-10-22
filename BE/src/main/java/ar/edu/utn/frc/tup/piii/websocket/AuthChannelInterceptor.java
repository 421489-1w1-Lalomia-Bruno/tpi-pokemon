package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.exceptions.InvalidCredentialsException;
import ar.edu.utn.frc.tup.piii.security.JwtTokenProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public AuthChannelInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String playerId = extractPlayerId(accessor);
            if (playerId == null || playerId.isBlank()) {
                throw new InvalidCredentialsException("Missing or invalid authentication");
            }
            accessor.setUser(() -> playerId);
        }

        return message;
    }

    private String extractPlayerId(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            if (jwtTokenProvider.validate(token)) {
                return jwtTokenProvider.getUserIdFromToken(token);
            }
        }
        return null;
    }
}
