package ar.edu.utn.frc.tup.piii.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider);
    }

    @Test
    void shouldNotFilter_pingPath() {
        assertTrue(filter.shouldNotFilter(requestWithUri("/ping")));
    }

    @Test
    void shouldNotFilter_authPath() {
        assertTrue(filter.shouldNotFilter(requestWithUri("/api/auth/login")));
    }

    @Test
    void shouldNotFilter_cardsPath() {
        assertTrue(filter.shouldNotFilter(requestWithUri("/api/cards/sync")));
    }

    @Test
    void shouldNotFilter_usersPath() {
        assertFalse(filter.shouldNotFilter(requestWithUri("/api/users/me")));
    }

    @Test
    void shouldNotFilter_decksPath() {
        assertFalse(filter.shouldNotFilter(requestWithUri("/api/decks")));
    }

    @Test
    void doFilter_missingHeader_returns401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilter_validToken_setsAttributeAndContinues() throws Exception {
        when(jwtTokenProvider.validate("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn("player-123");
        when(jwtTokenProvider.getAuthentication("valid-token"))
                .thenReturn(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "player-123", null, java.util.List.of()));

        MockHttpServletRequest request = requestWithAuthHeader("Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("player-123", request.getAttribute("playerId"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_invalidToken_returns401() throws Exception {
        when(jwtTokenProvider.validate("invalid-token")).thenReturn(false);

        MockHttpServletRequest request = requestWithAuthHeader("Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    private MockHttpServletRequest requestWithUri(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }

    private MockHttpServletRequest requestWithAuthHeader(String header) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", header);
        return request;
    }
}
