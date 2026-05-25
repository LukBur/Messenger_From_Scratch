package pl.communicator.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages sent to /topic destinations are broadcast by the in-memory broker.
        registry.enableSimpleBroker("/topic");

        // Messages sent by clients to /app destinations are routed to controller methods.
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // The STOMP endpoint used by the frontend to establish a WebSocket/SockJS connection.
        registry.addEndpoint("/ws").setAllowedOrigins(
                "http://localhost:3000",
                "https://messengerfromscratch.vercel.app"
        ).withSockJS();
    }
}