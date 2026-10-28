package com.rexchain.cinema.config;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
@Configuration @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
 private static final String ALLOWED_TOPIC_REGEX = "^/topic/(showtimes/\\d+/seats|support/.*)$";
 private final String configuredOrigins;
 private final String paymentBaseUrl;

 public WebSocketConfig(@Value("${app.websocket.allowed-origins:}") String configuredOrigins,
                        @Value("${payment.base-url:}") String paymentBaseUrl) {
  this.configuredOrigins = configuredOrigins;
  this.paymentBaseUrl = paymentBaseUrl;
 }

 @Override public void configureMessageBroker(MessageBrokerRegistry r){r.enableSimpleBroker("/topic");r.setApplicationDestinationPrefixes("/app");}
 @Override public void registerStompEndpoints(StompEndpointRegistry r){r.addEndpoint("/ws").setAllowedOrigins(allowedOrigins()).withSockJS();}
 @Override public void configureClientInboundChannel(ChannelRegistration registration) {
  registration.interceptors(new ChannelInterceptor() {
   @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() == null) return message;
    validateInbound(accessor.getCommand(), accessor.getDestination());
    return message;
   }
  });
 }

 void validateInbound(StompCommand command, String destination) {
  if (StompCommand.SEND.equals(command)) throw new AccessDeniedException("Client STOMP SEND is not allowed");
  if (StompCommand.SUBSCRIBE.equals(command) && !String.valueOf(destination).matches(ALLOWED_TOPIC_REGEX))
   throw new AccessDeniedException("STOMP destination is not allowed");
 }

 String[] allowedOrigins() {
  Set<String> origins = new LinkedHashSet<>(Set.of("http://localhost:8080", "http://127.0.0.1:8080", "http://[::1]:8080"));
  addOrigins(origins, configuredOrigins);
  try {
   URI uri = URI.create(paymentBaseUrl);
   if (uri.getScheme() != null && uri.getAuthority() != null) origins.add(uri.getScheme() + "://" + uri.getAuthority());
  } catch (IllegalArgumentException ignored) { }
  return origins.stream().filter(origin -> !origin.contains("*")).toArray(String[]::new);
 }

 private void addOrigins(Set<String> origins, String values) {
  if (values == null || values.isBlank()) return;
  for (String origin : values.split(",")) {
   String value = origin.trim();
   if (!value.isBlank() && !value.contains("*")) origins.add(value);
  }
 }
}
