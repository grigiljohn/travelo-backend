package com.travelo.websocketservice.config;

import com.travelo.websocketservice.handler.ChatWebSocketHandler;
import com.travelo.websocketservice.interceptor.WebSocketAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler, WebSocketAuthInterceptor authInterceptor) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("*") // Configure based on your frontend domain
                .addInterceptors(authInterceptor);
    }
}

