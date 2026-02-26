package com.pathvision.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import com.pathvision.entity.User;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    private Key getSigningKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException | NullPointerException ex) {
            // secret is not valid Base64 or missing; fall back to deriving a 256-bit key from the raw secret
            try {
                byte[] raw = (secret == null) ? new byte[0] : secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                // ensure 32 bytes key by hashing with SHA-256 if needed
                if (raw.length < 32) {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                    raw = md.digest(raw);
                }
                return Keys.hmacShaKeyFor(raw);
            } catch (Exception e) {
                // As a last resort, throw a clear runtime exception
                throw new RuntimeException("Invalid JWT secret configuration", e);
            }
        }
    }

    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiry);

        // Add extra claims if we have a concrete User
        if (userDetails instanceof User) {
            User u = (User) userDetails;
            if (u.getId() != null) builder.claim("id", u.getId());
            if (u.getRole() != null) builder.claim("role", u.getRole().name());
        } else {
            // fallback: derive role from authorities if present
            var authorities = userDetails.getAuthorities();
            if (authorities != null && authorities.iterator().hasNext()) {
                String auth = authorities.iterator().next().getAuthority();
                if (auth != null && auth.startsWith("ROLE_")) {
                    builder.claim("role", auth.substring(5));
                } else if (auth != null) {
                    builder.claim("role", auth);
                }
            }
        }

        return builder.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
}
