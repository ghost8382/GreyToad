package com.stock_tracker.grey_toad.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public JwtChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.CONNECT) {
            return message;
        }

        String header = resolveAuthorizationHeader(accessor);
        if (header == null || !header.startsWith("Bearer ")) {
            return message;
        }

        String token = header.substring(7);
        if (!jwtService.isValid(token)) {
            return message;
        }

        String email = jwtService.extractUsername(token);
        accessor.setUser(new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList()));
        return message;
    }

    private String resolveAuthorizationHeader(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header != null) {
            return header;
        }

        return accessor.getFirstNativeHeader("authorization");
    }
}
