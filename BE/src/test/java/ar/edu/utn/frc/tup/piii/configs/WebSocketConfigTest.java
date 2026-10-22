package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.EmbeddedPostgresTestBase;
import ar.edu.utn.frc.tup.piii.websocket.AuthChannelInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketConfigTest extends EmbeddedPostgresTestBase {

    @Autowired
    private WebSocketConfig webSocketConfig;

    @MockitoBean
    private AuthChannelInterceptor authChannelInterceptor;

    @Test
    void config_shouldLoad() {
        assertNotNull(webSocketConfig);
    }

    @Test
    void configureClientInboundChannel_shouldRegisterInterceptor() {
        ChannelRegistration registration = mock(ChannelRegistration.class);
        webSocketConfig.configureClientInboundChannel(registration);
        verify(registration).interceptors(authChannelInterceptor);
    }
}
