package com.rexchain.cinema.config;

import java.net.URI;
import java.security.Principal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import com.rexchain.cinema.entity.Role;
import com.rexchain.cinema.entity.User;
import com.rexchain.cinema.repository.UserRepository;
@Configuration @EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
 private static final String ALLOWED_TOPIC_REGEX =
         "^/topic/(showtimes/\\d+/seats|support/(session/[A-Za-z0-9_-]{1,64}|cinema/\\d+|admin))$";
 private static final String SESSION_PREFIX = "/topic/support/session/";
 private static final String CINEMA_PREFIX = "/topic/support/cinema/";
 private final String configuredOrigins;
 private final String paymentBaseUrl;
 private final UserRepository users;

 public WebSocketConfig(@Value("${app.websocket.allowed-origins:}") String configuredOrigins,
                        @Value("${payment.base-url:}") String paymentBaseUrl) {
  this(configuredOrigins, paymentBaseUrl, null);
 }

 @Autowired
 public WebSocketConfig(@Value("${app.websocket.allowed-origins:}") String configuredOrigins,
                        @Value("${payment.base-url:}") String paymentBaseUrl,
                        UserRepository users) {
  this.configuredOrigins = configuredOrigins;
  this.paymentBaseUrl = paymentBaseUrl;
  this.users = users;
 }

 @Override public void configureMessageBroker(MessageBrokerRegistry r){r.enableSimpleBroker("/topic");r.setApplicationDestinationPrefixes("/app");}
 @Override public void registerStompEndpoints(StompEndpointRegistry r){r.addEndpoint("/ws").setAllowedOrigins(allowedOrigins()).withSockJS();}
 @Override public void configureClientInboundChannel(ChannelRegistration registration) {
  registration.interceptors(new ChannelInterceptor() {
   @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() == null) return message;
    validateInbound(accessor.getCommand(), accessor.getDestination(), accessor.getUser());
    return message;
   }
  });
 }

 /** Kept for callers that only check the destination shape (no authenticated principal). */
 void validateInbound(StompCommand command, String destination) {
  validateInbound(command, destination, null);
 }

 void validateInbound(StompCommand command, String destination, Principal principal) {
  if (StompCommand.SEND.equals(command)) throw new AccessDeniedException("Client STOMP SEND is not allowed");
  if (!StompCommand.SUBSCRIBE.equals(command)) return;
  if (!String.valueOf(destination).matches(ALLOWED_TOPIC_REGEX))
   throw new AccessDeniedException("STOMP destination is not allowed");
  authorizeSupportSubscription(destination, principal);
 }

 /**
  * Support conversations carry personal data, so a subscription must be bound to the
  * authenticated account instead of a channel name that other clients can guess.
  */
 private void authorizeSupportSubscription(String destination, Principal principal) {
  if (destination == null || !destination.startsWith("/topic/support/")) return;
  if ("/topic/support/admin".equals(destination)) {
   requireUser(principal, Role.ADMIN);
   return;
  }
  if (destination.startsWith(CINEMA_PREFIX)) {
   User user = requireUser(principal, Role.ADMIN, Role.SUPERVISOR);
   if (user.getRole() == Role.SUPERVISOR) {
    Long cinemaId = parseId(destination.substring(CINEMA_PREFIX.length()));
    if (user.getCinema() == null || !user.getCinema().getId().equals(cinemaId))
     throw new AccessDeniedException("STOMP destination is not allowed");
   }
   return;
  }
  if (!destination.startsWith(SESSION_PREFIX))
   throw new AccessDeniedException("STOMP destination is not allowed");
  String sessionId = destination.substring(SESSION_PREFIX.length());
  // Guest sessions only carry AI answers and live in page memory; they are never persisted per account.
  if (sessionId.startsWith("guest_")) return;
  if (!sessionId.startsWith("user_"))
   throw new AccessDeniedException("STOMP destination is not allowed");
  User user = authenticatedUser(principal);
  if (user == null || !("user_" + user.getId()).equals(sessionId))
   throw new AccessDeniedException("STOMP destination is not allowed");
 }

 private User requireUser(Principal principal, Role... roles) {
  User user = authenticatedUser(principal);
  if (user == null) throw new AccessDeniedException("STOMP destination is not allowed");
  for (Role role : roles) {
   if (user.getRole() == role) return user;
  }
  for (GrantedAuthority authority : authorities(principal)) {
   for (Role role : roles) {
    if (("ROLE_" + role.name()).equals(authority.getAuthority())) return user;
   }
  }
  throw new AccessDeniedException("STOMP destination is not allowed");
 }

 private User authenticatedUser(Principal principal) {
  if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()) return null;
  String email = authentication.getName();
  if (email == null || email.isBlank() || "anonymousUser".equals(email)) return null;
  if (users == null) return null;
  return users.findByEmailIgnoreCase(email).orElse(null);
 }

 private Collection<? extends GrantedAuthority> authorities(Principal principal) {
  if (!(principal instanceof Authentication authentication)) return List.of();
  Collection<? extends GrantedAuthority> value = authentication.getAuthorities();
  return value == null ? List.of() : value;
 }

 private Long parseId(String value) {
  try { return Long.valueOf(value.trim()); }
  catch (NumberFormatException ex) { throw new AccessDeniedException("STOMP destination is not allowed"); }
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
