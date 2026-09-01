package com.nailconnect.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
  private final SecretKey key; private final long expirationMinutes;
  public JwtService(@Value("${app.jwt-secret}") String secret,@Value("${app.jwt-expiration-minutes}") long expirationMinutes){this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));this.expirationMinutes=expirationMinutes;}
  public String issue(UUID userId,String email,String role){Instant now=Instant.now();return Jwts.builder().subject(userId.toString()).claim("email",email).claim("role",role).issuedAt(Date.from(now)).expiration(Date.from(now.plus(expirationMinutes,ChronoUnit.MINUTES))).signWith(key).compact();}
  public UserPrincipal parse(String token){var claims=Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();return new UserPrincipal(UUID.fromString(claims.getSubject()),claims.get("email",String.class),claims.get("role",String.class));}
}
