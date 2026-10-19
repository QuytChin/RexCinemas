package com.rexchain.cinema.security;

import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service;
import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.time.Instant; import java.util.Date; import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key; private final long expirationMs; private final long rememberExpirationMs;
    public JwtService(@Value("${app.jwt.secret}") String secret,@Value("${app.jwt.expiration-ms:86400000}") long expirationMs,@Value("${app.jwt.remember-expiration-ms:2592000000}") long rememberExpirationMs){
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32)
            throw new IllegalStateException("JWT_SECRET is required and must contain at least 32 UTF-8 bytes");
        this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.expirationMs=expirationMs; this.rememberExpirationMs=rememberExpirationMs;
    }
    public String generate(String email, String role){return generate(email,role,false);}
    public String generate(String email, String role, boolean remember){return generate(email, role, remember, 0);}
    public String generate(String email, String role, boolean remember, int credentialVersion){Instant now=Instant.now(); long ttl=remember?rememberExpirationMs:expirationMs; return Jwts.builder().id(UUID.randomUUID().toString()).subject(email).claim("role",role).claim("cv",credentialVersion).issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(ttl))).signWith(key).compact();}
    public String extractEmail(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();}
    public String extractId(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getId();}
    public int extractCredentialVersion(String token){Integer version=Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().get("cv", Integer.class); return version == null ? 0 : version;}
    public Instant extractExpiration(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getExpiration().toInstant();}
    public long expirationMs(boolean remember){return remember ? rememberExpirationMs : expirationMs;}
    public boolean valid(String token){try{Jwts.parser().verifyWith(key).build().parseSignedClaims(token);return true;}catch(JwtException|IllegalArgumentException e){return false;}}
}
